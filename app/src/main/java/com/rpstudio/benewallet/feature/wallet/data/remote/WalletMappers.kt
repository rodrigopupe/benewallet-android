package com.rpstudio.benewallet.feature.wallet.data.remote

import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BalanceResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BenefitDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.CategoryDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionsResponseDto
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import com.rpstudio.benewallet.feature.wallet.domain.model.TransactionPage
import java.time.Instant

internal fun BenefitDto.toDomainOrNull(): Benefit? = when (this) {
    BenefitDto.MEAL -> Benefit.MEAL
    BenefitDto.FOOD -> Benefit.FOOD
    BenefitDto.UNKNOWN -> null
}

internal fun CategoryDto.toDomain(): Category = when (this) {
    CategoryDto.RECHARGE -> Category.RECHARGE
    CategoryDto.RESTAURANT -> Category.RESTAURANT
    CategoryDto.DELIVERY -> Category.DELIVERY
    CategoryDto.SNACKS -> Category.SNACKS
    CategoryDto.GROCERY -> Category.GROCERY
    CategoryDto.BAKERY -> Category.BAKERY
    CategoryDto.GREENGROCER -> Category.GREENGROCER
    CategoryDto.UNKNOWN -> Category.OTHER
}

internal fun BalanceResponseDto.toDomain(): List<BenefitBalance> = balances.mapNotNull { dto ->
    dto.benefit.toDomainOrNull()?.let { BenefitBalance(it, Money(dto.balanceCents)) }
}

internal fun TransactionDto.toDomainOrNull(): Transaction? {
    val domainBenefit = benefit.toDomainOrNull() ?: return null
    return Transaction(
        id = id,
        benefit = domainBenefit,
        category = category.toDomain(),
        merchant = merchant,
        amount = Money(amountCents),
        occurredAt = Instant.parse(occurredAt)
    )
}

internal fun TransactionsResponseDto.toDomain(): TransactionPage =
    TransactionPage(items.mapNotNull { it.toDomainOrNull() }, nextCursor)
