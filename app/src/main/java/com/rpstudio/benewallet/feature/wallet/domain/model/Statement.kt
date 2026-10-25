package com.rpstudio.benewallet.feature.wallet.domain.model

import java.time.Instant

/** Balances as stored locally. [lastUpdatedAt] is null if they were never fetched. */
internal data class WalletBalances(val items: List<BenefitBalance>, val lastUpdatedAt: Instant?)

/**
 * Statement as stored locally for a given filter.
 *
 * @property hasMore whether the server still has older transactions for this snapshot.
 * @property isPartial true when the cached snapshot was fetched for a different filter, so [items]
 * may be incomplete until the next successful refresh (e.g. user changed the filter while offline).
 * @property lastUpdatedAt when the head of the list was last fetched, or null if never.
 */
internal data class Statement(
    val items: List<Transaction>,
    val hasMore: Boolean,
    val isPartial: Boolean,
    val lastUpdatedAt: Instant?
)
