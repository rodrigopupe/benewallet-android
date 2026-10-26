package com.rpstudio.benewallet.feature.wallet.presentation.home

import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError
import java.time.Instant

internal data class HomeState(
    val balances: List<BenefitBalance> = emptyList(),
    val lastUpdatedAt: Instant? = null,
    val isRefreshing: Boolean = false
) {
    val total: Money get() = balances.fold(Money(0)) { accumulated, item ->
        accumulated + item.balance
    }
}

internal sealed interface HomeIntent {
    data object Refresh : HomeIntent
}

internal sealed interface HomeEffect {
    data class ShowError(val error: WalletError) : HomeEffect
}
