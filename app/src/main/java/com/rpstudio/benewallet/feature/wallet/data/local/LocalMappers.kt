package com.rpstudio.benewallet.feature.wallet.data.local

import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import java.time.Instant

private fun String.toBenefitOrNull(): Benefit? = Benefit.entries.firstOrNull { it.name == this }

private fun String.toCategory(): Category = Category.entries.firstOrNull { it.name == this } ?: Category.OTHER

internal fun Transaction.toEntity() = TransactionEntity(
    id = id,
    benefit = benefit.name,
    category = category.name,
    merchant = merchant,
    amountCents = amount.cents,
    occurredAtMillis = occurredAt.toEpochMilli()
)

internal fun TransactionEntity.toDomainOrNull(): Transaction? {
    val domainBenefit = benefit.toBenefitOrNull() ?: return null
    return Transaction(
        id = id,
        benefit = domainBenefit,
        category = category.toCategory(),
        merchant = merchant,
        amount = Money(amountCents),
        occurredAt = Instant.ofEpochMilli(occurredAtMillis)
    )
}

internal fun BenefitBalance.toEntity() = BalanceEntity(benefit = benefit.name, balanceCents = balance.cents)

internal fun BalanceEntity.toDomainOrNull(): BenefitBalance? =
    benefit.toBenefitOrNull()?.let { BenefitBalance(it, Money(balanceCents)) }
