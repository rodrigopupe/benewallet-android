package com.rpstudio.benewallet.feature.wallet.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpstudio.benewallet.core.auth.SessionManager
import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

internal class HomeViewModel(private val repository: WalletRepository, private val sessionManager: SessionManager) :
    ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val confirmingLogout = MutableStateFlow(false)
    private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<HomeState> = combine(
        repository.observeBalances(),
        isRefreshing,
        confirmingLogout
    ) { balances, refreshing, confirming ->
        HomeState(
            balances = balances.items,
            lastUpdatedAt = balances.lastUpdatedAt,
            isRefreshing = refreshing,
            confirmingLogout = confirming
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        HomeState()
    )

    init {
        // Stale-while-revalidate: the UI shows the cache right away; this only hits the network if the TTL expired
        refresh(force = false)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh(force = true)
            HomeIntent.RequestLogout -> confirmingLogout.value = true
            HomeIntent.CancelLogout -> confirmingLogout.value = false
            HomeIntent.ConfirmLogout -> logout()
        }
    }

    private fun logout() {
        confirmingLogout.value = false
        // The session turns SignedOut and the root swaps this screen for the login: nothing else to do here
        viewModelScope.launch { sessionManager.logout() }
    }

    private fun refresh(force: Boolean) {
        if (isRefreshing.value) return

        isRefreshing.value = true
        viewModelScope.launch {
            val result = repository.refreshBalances(force)
            isRefreshing.value = false
            if (result is RefreshResult.Failure) {
                effectChannel.send(HomeEffect.ShowError(result.error))
            }
        }
    }
}
