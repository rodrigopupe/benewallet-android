package com.rpstudio.benewallet.feature.wallet.presentation

import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal val brazilianLocale: Locale = Locale.forLanguageTag("pt-BR")

/** Formats cents as BRL, e.g. "R$ 1.234,56". A new NumberFormat per call because it is not thread-safe. */
internal fun Money.format(): String = NumberFormat
    .getCurrencyInstance(brazilianLocale)
    .format(BigDecimal.valueOf(cents, 2))
