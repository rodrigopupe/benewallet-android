package com.rpstudio.benewallet.feature.wallet.data

import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.TransactionPage
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletSessionCleanerTest {
    private val local = FakeWalletLocalDataSource()

    @Test
    fun `clearing wipes balances, transactions and the sync markers`() = runTest {
        val now = Instant.parse("2026-10-26T15:00:00Z")
        local.replaceBalances(listOf(BenefitBalance(Benefit.MEAL, Money(100_000))), now)
        local.replaceTransactions(TransactionPage(emptyList(), nextCursor = "cursor"), StatementFilter(), now)

        WalletSessionCleaner(local).clear()

        assertEquals(emptyList<BenefitBalance>(), local.observeBalances().first())
        assertNull(
            "a stale fetch time would make the next account skip its first refresh",
            local.observeBalancesFetchedAt().first()
        )
        assertEquals(emptyList<Any>(), local.observeStatement(StatementFilter()).first().items)
        assertNull(local.observeTransactionsSync().first())
    }
}
