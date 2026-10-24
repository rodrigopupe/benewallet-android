package com.rpstudio.benewallet.feature.wallet.domain.model

@JvmInline
internal value class Money(val cents: Long) {
    val isNegative: Boolean get() = cents < 0

    operator fun plus(other: Money) = Money(cents + other.cents)

    operator fun unaryMinus() = Money(-cents)
}
