package com.rpstudio.benewallet.feature.wallet.presentation

import androidx.annotation.StringRes
import com.rpstudio.benewallet.R
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
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

@StringRes
internal fun Benefit.shortLabelRes(): Int = when (this) {
    Benefit.MEAL -> R.string.benefit_meal_short
    Benefit.FOOD -> R.string.benefit_food_short
}

@StringRes
internal fun Category.labelRes(): Int = when (this) {
    Category.RECHARGE -> R.string.category_recharge
    Category.RESTAURANT -> R.string.category_restaurant
    Category.DELIVERY -> R.string.category_delivery
    Category.SNACKS -> R.string.category_snacks
    Category.GROCERY -> R.string.category_grocery
    Category.BAKERY -> R.string.category_bakery
    Category.GREENGROCER -> R.string.category_greengrocer
    Category.OTHER -> R.string.category_other
}
