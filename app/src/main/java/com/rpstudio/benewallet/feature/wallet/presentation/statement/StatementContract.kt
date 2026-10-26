package com.rpstudio.benewallet.feature.wallet.presentation.statement

import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError

internal enum class PeriodFilter { ALL, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH }

internal data class StatementState(
    val items: List<Transaction> = emptyList(),
    val category: Category? = null,
    val period: PeriodFilter = PeriodFilter.ALL,
    val hasMore: Boolean = false,
    val isPartial: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false
)

internal sealed interface StatementIntent {
    data class SelectCategory(val category: Category?) : StatementIntent

    data class SelectPeriod(val period: PeriodFilter) : StatementIntent

    object Refresh : StatementIntent

    object LoadMore : StatementIntent
}

internal sealed interface StatementEffect {
    data class ShowError(val error: WalletError) : StatementEffect
}
