package com.rpstudio.benewallet.feature.wallet.presentation.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val LAST_7_DAYS_OFFSET = 6L
private const val LAST_30_DAYS_OFFSET = 29L

@OptIn(ExperimentalCoroutinesApi::class)
internal class StatementViewModel(private val repository: WalletRepository, private val clock: Clock) : ViewModel() {
    private val selection = MutableStateFlow(Selection())
    private val progress = MutableStateFlow(Progress())
    private val effectChannel = Channel<StatementEffect>(Channel.BUFFERED)
    private var refreshJob: Job? = null

    val effects: Flow<StatementEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<StatementState> = combine(
        selection.flatMapLatest { repository.observeStatement(it.filter) },
        selection,
        progress
    ) { statement, selected, inProgress ->
        StatementState(
            items = statement.items,
            category = selected.filter.category,
            period = selected.period,
            hasMore = statement.hasMore,
            isPartial = statement.isPartial,
            isRefreshing = inProgress.isRefreshing,
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
        refresh(force = false)
    }

    private fun selectPeriod(period: PeriodFilter) {
        val today = LocalDate.now(clock.withZone(statementZone))
        val (from, to) = period.range(today)
        selection.update { it.copy(period = period, filter = it.filter.copy(from = from, to = to)) }
        refresh(force = false)
    }

    private fun refresh(force: Boolean) {
        // A newer filter makes the in-flight refresh obsolete, so it is canceled instead of ignored
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            progress.update { it.copy(isRefreshing = true, loadMoreFailed = false) }
            val result = repository.refreshStatement(selection.value.filter, force)
            progress.update { it.copy(isRefreshing = false) }
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
            val result = repository.loadMoreStatement()
            progress.update { it.copy(isLoadingMore = false, loadMoreFailed = result is RefreshResult.Failure) }
            if (result is RefreshResult.Failure) {
                effectChannel.send(StatementEffect.ShowError(result.error))
            }
        }
    }
}

private data class Selection(
    val period: PeriodFilter = PeriodFilter.ALL,
    val filter: StatementFilter = StatementFilter()
)

private data class Progress(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false
)

private fun PeriodFilter.range(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (this) {
    PeriodFilter.ALL -> null to null
    PeriodFilter.LAST_7_DAYS -> today.minusDays(LAST_7_DAYS_OFFSET) to today
    PeriodFilter.LAST_30_DAYS -> today.minusDays(LAST_30_DAYS_OFFSET) to today
    PeriodFilter.THIS_MONTH -> today.withDayOfMonth(1) to today
}
