package com.rpstudio.benewallet.feature.wallet.domain.model

import java.time.Instant

internal enum class Benefit { MEAL, FOOD }

internal enum class Category {
    RECHARGE,
    RESTAURANT,
    DELIVERY,
    SNACKS,
    GROCERY,
    BAKERY,
    GREENGROCER,
    OTHER
}

internal data class BenefitBalance(val benefit: Benefit, val balance: Money)

internal data class Transaction(
    val id: Long,
    val benefit: Benefit,
    val category: Category,
    val merchant: String,
    val amount: Money,
    val occurredAt: Instant
)

internal data class TransactionPage(val items: List<Transaction>, val nextCursor: String?)
