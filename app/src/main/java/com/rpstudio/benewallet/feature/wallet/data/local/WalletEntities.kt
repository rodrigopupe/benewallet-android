package com.rpstudio.benewallet.feature.wallet.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

internal const val SYNC_SINGLETON_ID = 1

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAtMillis", "id"]),
        Index(value = ["category", "occurredAtMillis"])
    ]
)
internal data class TransactionEntity(
    @PrimaryKey val id: Long,
    val benefit: String,
    val category: String,
    val merchant: String,
    val amountCents: Long,
    val occurredAtMillis: Long
)

@Entity(tableName = "balances")
internal data class BalanceEntity(@PrimaryKey val benefit: String, val balanceCents: Long)

@Entity(tableName = "balance_sync")
internal data class BalanceSyncEntity(@PrimaryKey val id: Int = SYNC_SINGLETON_ID, val fetchedAtMillis: Long)

@Entity(tableName = "transactions_sync")
internal data class TransactionsSyncEntity(
    @PrimaryKey val id: Int = SYNC_SINGLETON_ID,
    val fetchedAtMillis: Long,
    val nextCursor: String?,
    val endReached: Boolean,
    val category: String?,
    val fromDate: String?,
    val toDate: String?
)
