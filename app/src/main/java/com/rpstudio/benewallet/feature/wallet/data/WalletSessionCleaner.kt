package com.rpstudio.benewallet.feature.wallet.data

import com.rpstudio.benewallet.core.auth.SessionDataCleaner
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource

internal class WalletSessionCleaner(private val local: WalletLocalDataSource) : SessionDataCleaner {
    override suspend fun clear() = local.clearAll()
}
