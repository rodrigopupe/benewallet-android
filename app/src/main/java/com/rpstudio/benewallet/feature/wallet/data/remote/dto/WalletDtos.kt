package com.rpstudio.benewallet.feature.wallet.data.remote.dto

import kotlinx.serialization.Serializable

internal enum class BenefitDto { MEAL, FOOD, UNKNOWN }

internal enum class CategoryDto {
    RECHARGE,
    RESTAURANT,
    DELIVERY,
    SNACKS,
    GROCERY,
    BAKERY,
    GREENGROCER,
    UNKNOWN
}

@Serializable
internal data class BenefitBalanceDto(val benefit: BenefitDto = BenefitDto.UNKNOWN, val balanceCents: Long)

@Serializable
internal data class BalanceResponseDto(val balances: List<BenefitBalanceDto>, val updatedAt: String)

@Serializable
internal data class TransactionDto(
    val id: Long,
    val benefit: BenefitDto = BenefitDto.UNKNOWN,
    val category: CategoryDto = CategoryDto.UNKNOWN,
    val merchant: String,
    val amountCents: Long,
    val occurredAt: String
)

@Serializable
internal data class TransactionsResponseDto(val items: List<TransactionDto>, val nextCursor: String? = null)

@Serializable
internal data class FeatureFlagDto(val key: String, val enabled: Boolean, val rolloutPercent: Int = 0)

@Serializable
internal data class FeatureFlagsResponseDto(val flags: List<FeatureFlagDto>)
