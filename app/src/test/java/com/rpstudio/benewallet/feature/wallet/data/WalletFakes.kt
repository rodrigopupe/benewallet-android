package com.rpstudio.benewallet.feature.wallet.data

import com.rpstudio.benewallet.core.crash.CrashReporter
import com.rpstudio.benewallet.feature.wallet.data.local.LocalStatement
import com.rpstudio.benewallet.feature.wallet.data.local.TransactionsSync
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BalanceResponseDto
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
import kotlinx.coroutines.flow.update

internal data class TransactionsCall(
    val cursor: String?,
    val category: String?,
    val from: String?,
    val to: String?,
    val limit: Int?
)

internal class FakeWalletService : WalletService {
    var balanceResult: () -> BalanceResponseDto = { error("balance not stubbed") }

    // suspend, so a test can hold the "network" open while it looks at what the screen would show
    var transactionsResult: suspend (
        TransactionsCall
    ) -> TransactionsResponseDto = { error("transactions not stubbed") }
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
}

internal class FakeWalletLocalDataSource : WalletLocalDataSource {
    private val balances = MutableStateFlow<List<BenefitBalance>>(emptyList())
    private val balancesFetchedAt = MutableStateFlow<Instant?>(null)
    private val statement = MutableStateFlow(LocalStatement(emptyList(), null))
    private val newestFirst = compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id }

    /**
     * Room commits a transaction first and tells the observers afterwards, a little later. With this on, a replace is
     * only seen by the flows after [flushNotifications], which is how a test can look at that gap.
     */
    var holdNotifications = false
    private var pendingNotification: (() -> Unit)? = null

    private fun publish(change: () -> Unit) {
        if (holdNotifications) pendingNotification = change else change()
    }

    fun flushNotifications() {
        pendingNotification?.invoke()
        pendingNotification = null
    }

    override fun observeBalances(): Flow<List<BenefitBalance>> = balances

    override fun observeBalancesFetchedAt(): Flow<Instant?> = balancesFetchedAt

    override fun observeStatement(filter: StatementFilter): Flow<LocalStatement> = statement.map {
        it.copy(items = it.items.filter { transaction -> filter.matches(transaction) }.sortedWith(newestFirst))
    }

    override fun observeTransactionsSync(): Flow<TransactionsSync?> = statement.map { it.sync }

    override suspend fun replaceBalances(balances: List<BenefitBalance>, fetchedAt: Instant) {
        this.balances.value = balances
        balancesFetchedAt.value = fetchedAt
    }

    override suspend fun replaceTransactions(page: TransactionPage, filter: StatementFilter, fetchedAt: Instant) {
        publish { statement.value = LocalStatement(page.items, TransactionsSync(fetchedAt, page.nextCursor, filter)) }
    }

    override suspend fun appendTransactions(page: TransactionPage) {
        statement.update { current ->
            LocalStatement(
                items = (current.items + page.items).associateBy { it.id }.values.toList(),
                sync = current.sync?.copy(nextCursor = page.nextCursor)
            )
        }
    }

    override suspend fun clearAll() {
        balances.value = emptyList()
        balancesFetchedAt.value = null
        statement.value = LocalStatement(emptyList(), null)
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
