package com.rpstudio.benewallet.feature.wallet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

internal const val WALLET_DATABASE_NAME = "benewallet.db"

@Database(
    entities = [
        TransactionEntity::class,
        BalanceEntity::class,
        BalanceSyncEntity::class,
        TransactionsSyncEntity::class
    ],
    version = 1,
    exportSchema = true
)
internal abstract class WalletDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun balanceDao(): BalanceDao

    abstract fun syncDao(): SyncDao
}
