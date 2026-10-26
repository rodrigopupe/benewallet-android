package com.rpstudio.benewallet.feature.wallet.presentation

import androidx.annotation.StringRes
import com.rpstudio.benewallet.R
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError

@StringRes
internal fun Benefit.labelRes(): Int = when (this) {
    Benefit.MEAL -> R.string.benefit_meal
    Benefit.FOOD -> R.string.benefit_food
}

@StringRes
internal fun WalletError.messageRes(): Int = when (this) {
    WalletError.Network -> R.string.error_network
    is WalletError.Server -> R.string.error_server
    is WalletError.Unexpected -> R.string.error_unexpected
}
