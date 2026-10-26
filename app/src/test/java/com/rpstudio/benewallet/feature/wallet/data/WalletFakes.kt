package com.rpstudio.benewallet.feature.wallet.data

import com.rpstudio.benewallet.core.crash.CrashReporter
import com.rpstudio.benewallet.feature.wallet.data.local.TransactionsSync
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BalanceResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.FeatureFlagsResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionsResponseDto
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import com.rpstudio.benewallet.feature.wallet.domain.model.TransactionPage
import com.rpstudio.benewallet.feature.wallet.domain.model.statementZone
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal data class TransactionsCall(
    val cursor: String?,
    val category: String?,
    val from: String?,
    val to: String?,
    val limit: Int?
)

internal class FakeWalletService : WalletService {
    var balanceResult: () -> BalanceResponseDto = { error("balance not stubbed") }
    var transactionsResult: (TransactionsCall) -> TransactionsResponseDto = { error("transactions not stubbed") }
    var balanceCalls = 0
    val transactionCalls = mutableListOf<TransactionsCall>()

    override suspend fun getBalance(): BalanceResponseDto {
        balanceCalls++
        return balanceResult()
    }

    override suspend fun getTransactions(
        cursor: String?,
        category: String?,
        from: String?,
        to: String?,
        limit: Int?
    ): TransactionsResponseDto {
        val call = TransactionsCall(cursor, category, from, to, limit)
        transactionCalls.add(call)
        return transactionsResult(call)
    }

    override suspend fun getFeatureFlags(userId: String?) = FeatureFlagsResponseDto(emptyList())
}

internal class FakeWalletLocalDataSource : WalletLocalDataSource {
    private val balances = MutableStateFlow<List<BenefitBalance>>(emptyList())
    private val balancesFetchedAt = MutableStateFlow<Instant?>(null)
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val sync = MutableStateFlow<TransactionsSync?>(null)
    private val newestFirst = compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id }

    override fun observeBalances(): Flow<List<BenefitBalance>> = balances

    override fun observeBalancesFetchedAt(): Flow<Instant?> = balancesFetchedAt

    override fun observeTransactions(filter: StatementFilter): Flow<List<Transaction>> =
        transactions.map { all -> all.filter { filter.matches(it) }.sortedWith(newestFirst) }

    override fun observeTransactionsSync(): Flow<TransactionsSync?> = sync

    override suspend fun replaceBalances(balances: List<BenefitBalance>, fetchedAt: Instant) {
        this.balances.value = balances
        balancesFetchedAt.value = fetchedAt
    }

    override suspend fun replaceTransactions(page: TransactionPage, filter: StatementFilter, fetchedAt: Instant) {
        transactions.value = page.items
        sync.value = TransactionsSync(fetchedAt, page.nextCursor, filter)
    }

    override suspend fun appendTransactions(page: TransactionPage) {
        transactions.value = (transactions.value + page.items).associateBy { it.id }.values.toList()
        sync.value = sync.value?.copy(nextCursor = page.nextCursor)
    }

    private fun StatementFilter.matches(transaction: Transaction): Boolean {
        val day = transaction.occurredAt.atZone(statementZone).toLocalDate()
        return (category == null || transaction.category == category) &&
            (from == null || !day.isBefore(from)) &&
            (to == null || !day.isAfter(to))
    }
}

internal class FakeCrashReporter : CrashReporter {
    val recorded = mutableListOf<Throwable>()
    val logs = mutableListOf<String>()

    override fun initialize() = Unit

    override fun recordException(throwable: Throwable) {
        recorded.add(throwable)
    }

    override fun log(message: String) {
        logs.add(message)
    }
}

internal class MutableClock(private var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }
}
