package com.rpstudio.benewallet.feature.wallet.domain.model

import java.time.LocalDate

/** Statement filter. Dates are calendar days (inclusive), interpreted in the BFF time zone. */
internal data class StatementFilter(
    val category: Category? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)
