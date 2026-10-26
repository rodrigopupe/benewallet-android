package com.rpstudio.benewallet.bff

import kotlinx.serialization.Serializable

@Serializable
enum class Benefit { MEAL, FOOD }

@Serializable
enum class Category { RECHARGE, RESTAURANT, DELIVERY, SNACKS, GROCERY, BAKERY, GREENGROCER }

@Serializable
data class BenefitBalanceDto(val benefit: Benefit, val balanceCents: Long)

@Serializable
data class BalanceResponse(val balances: List<BenefitBalanceDto>, val updatedAt: String)

@Serializable
data class TransactionDto(
    val id: Long,
    val benefit: Benefit,
    val category: Category,
    val merchant: String,
    val amountCents: Long,
    val occurredAt: String
)

@Serializable
data class TransactionsResponse(val items: List<TransactionDto>, val nextCursor: String?)

@Serializable
data class FeatureFlagDto(val key: String, val enabled: Boolean, val rolloutPercent: Int)

@Serializable
data class FeatureFlagsResponse(val flags: List<FeatureFlagDto>)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)
