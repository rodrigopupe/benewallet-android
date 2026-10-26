package com.rpstudio.benewallet.feature.wallet.presentation.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpstudio.benewallet.core.analytics.Analytics
import com.rpstudio.benewallet.core.analytics.AnalyticsEvent
import com.rpstudio.benewallet.core.featureflags.FeatureFlag
import com.rpstudio.benewallet.core.featureflags.FeatureFlagsRepository
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.statementZone
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val LAST_7_DAYS_OFFSET = 6L
private const val LAST_30_DAYS_OFFSET = 29L

@OptIn(ExperimentalCoroutinesApi::class)
internal class StatementViewModel(
    private val walletRepository: WalletRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val analytics: Analytics,
    private val clock: Clock
) : ViewModel() {
    private val selection = MutableStateFlow(Selection())
    private val progress = MutableStateFlow(Progress())
    private val effectChannel = Channel<StatementEffect>(Channel.BUFFERED)
    private var refreshJob: Job? = null
    private val filtersEnabled = featureFlagsRepository.observeFlags()
        .map { it.isEnabled(FeatureFlag.STATEMENT_FILTERS) }
        .distinctUntilChanged()

    val effects: Flow<StatementEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<StatementState> = combine(
        selection.flatMapLatest { walletRepository.observeStatement(it.filter) },
        selection,
        progress,
        filtersEnabled
    ) { statement, selected, inProgress, filtersAvailable ->
        StatementState(
            filtersAvailable = filtersAvailable,
            items = statement.items,
            category = selected.filter.category,
            period = selected.period,
            hasMore = statement.hasMore,
            isPartial = statement.isPartial && inProgress.refreshFailed,
            isRefreshing = inProgress.isRefreshing || (statement.isPartial && !inProgress.refreshFailed),
            isLoadingMore = inProgress.isLoadingMore,
            loadMoreFailed = inProgress.loadMoreFailed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        StatementState()
    )

    init {
        refresh(force = false)
        // Flags never block the screen: the cached value (or the default) is used until the request answers
        viewModelScope.launch { featureFlagsRepository.refresh() }
        viewModelScope.launch {
            filtersEnabled.filter { !it }.collect { resetFilters() }
        }
    }

    fun onIntent(intent: StatementIntent) {
        when (intent) {
            is StatementIntent.SelectCategory -> selectCategory(intent.category)
            is StatementIntent.SelectPeriod -> selectPeriod(intent.period)
            StatementIntent.Refresh -> refresh(force = true)
            StatementIntent.LoadMore -> loadMore()
        }
    }

    private fun selectCategory(category: Category?) {
        selection.update {
            it.copy(
                filter = it.filter.copy(
                    category = category
                )
            )
        }
        trackFilterApplied()
        refresh(force = false)
    }

    private fun selectPeriod(period: PeriodFilter) {
        val today = LocalDate.now(clock.withZone(statementZone))
        val (from, to) = period.range(today)
        selection.update { it.copy(period = period, filter = it.filter.copy(from = from, to = to)) }
        trackFilterApplied()
        refresh(force = false)
    }

    /** Without the chips, a filter left over from before the flag was turned off could not be undone by the user. */
    private fun resetFilters() {
        if (selection.value == Selection()) return
        selection.value = Selection()
        refresh(force = false)
    }

    private fun refresh(force: Boolean) {
        // A newer filter makes the in-flight refresh obsolete, so it is canceled instead of ignored
        refreshJob?.cancel()
        // Marked before launching, so no frame is drawn between the filter change and the start of the request
        progress.update { it.copy(isRefreshing = true, loadMoreFailed = false, refreshFailed = false) }
        refreshJob = viewModelScope.launch {
            val result = walletRepository.refreshStatement(selection.value.filter, force)
            progress.update { it.copy(isRefreshing = false, refreshFailed = result is RefreshResult.Failure) }
            if (result is RefreshResult.Failure) {
                effectChannel.send(StatementEffect.ShowError(result.error))
            }
        }
    }

    private fun loadMore() {
        val current = progress.value
        if (current.isLoadingMore || current.isRefreshing) return
        viewModelScope.launch {
            progress.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            val result = walletRepository.loadMoreStatement()
            progress.update { it.copy(isLoadingMore = false, loadMoreFailed = result is RefreshResult.Failure) }
            if (result is RefreshResult.Failure) {
                effectChannel.send(StatementEffect.ShowError(result.error))
            }
        }
    }

    private fun trackFilterApplied() {
        val selected = selection.value
        analytics.track(
            AnalyticsEvent.StatementFilterApplied(
                selected.filter.category?.name,
                selected.period.name
            )
        )
    }
}

private data class Selection(
    val period: PeriodFilter = PeriodFilter.ALL,
    val filter: StatementFilter = StatementFilter()
)

private data class Progress(
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false
)

private fun PeriodFilter.range(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (this) {
    PeriodFilter.ALL -> null to null
    PeriodFilter.LAST_7_DAYS -> today.minusDays(LAST_7_DAYS_OFFSET) to today
    PeriodFilter.LAST_30_DAYS -> today.minusDays(LAST_30_DAYS_OFFSET) to today
    PeriodFilter.THIS_MONTH -> today.withDayOfMonth(1) to today
}
