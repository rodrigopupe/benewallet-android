package com.rpstudio.benewallet.feature.wallet.data.repository

import com.rpstudio.benewallet.core.crash.CrashReporter
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import com.rpstudio.benewallet.feature.wallet.data.remote.toDomain
import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.Statement
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.TransactionPage
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletBalances
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

private const val BALANCE_TTL_MINUTES = 2L
private const val STATEMENT_TTL_MINUTES = 5L
private const val PAGE_SIZE = 20
private const val SERVER_ERROR_MIN_CODE = 500

internal class DefaultWalletRepository(
    private val service: WalletService,
    private val local: WalletLocalDataSource,
    private val crashReporter: CrashReporter,
    private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock
) : WalletRepository {
    private val balancesMutex = Mutex()
    private val statementMutex = Mutex()

    override fun observeBalances(): Flow<WalletBalances> =
        combine(local.observeBalances(), local.observeBalancesFetchedAt()) { items, fetchedAt ->
            WalletBalances(items, fetchedAt)
        }

    override fun observeStatement(filter: StatementFilter): Flow<Statement> =
        combine(local.observeTransactions(filter), local.observeTransactionsSync()) { items, sync ->
            val sameFilter = sync?.filter == filter
            Statement(
                items = items,
                hasMore = sameFilter && sync?.hasMore == true,
                isPartial = sync != null && !sameFilter,
                lastUpdatedAt = sync?.fetchedAt
            )
        }

    override suspend fun refreshBalances(force: Boolean): RefreshResult = withContext(ioDispatcher) {
        // The mutex makes concurrent callers wait, then re-check freshness instead of duplicating the request
        balancesMutex.withLock {
            val fetchedAt = local.observeBalancesFetchedAt().first()
            if (!force && !isStale(fetchedAt, BALANCE_TTL_MINUTES)) {
                RefreshResult.Success
            } else {
                refreshCatching {
                    local.replaceBalances(service.getBalance().toDomain(), clock.instant())
                }
            }
        }
    }

    override suspend fun refreshStatement(filter: StatementFilter, force: Boolean): RefreshResult =
        withContext(ioDispatcher) {
            statementMutex.withLock {
                val sync = local.observeTransactionsSync().first()
                val isFresh = sync != null &&
                    sync.filter == filter &&
                    !isStale(sync.fetchedAt, STATEMENT_TTL_MINUTES)
                if (isFresh && !force) {
                    RefreshResult.Success
                } else {
                    refreshCatching {
                        val page = fetchPage(cursor = null, filter = filter)
                        local.replaceTransactions(page, filter, clock.instant())
                    }
                }
            }
        }

    override suspend fun loadMoreStatement(): RefreshResult = withContext(ioDispatcher) {
        statementMutex.withLock {
            val sync = local.observeTransactionsSync().first()
            val cursor = sync?.nextCursor
            if (sync == null || cursor == null) {
                RefreshResult.Success
            } else {
                refreshCatching {
                    local.appendTransactions(fetchPage(cursor = cursor, filter = sync.filter))
                }
            }
        }
    }

    private suspend fun fetchPage(cursor: String?, filter: StatementFilter): TransactionPage = service
        .getTransactions(
            cursor = cursor,
            category = filter.category?.name,
            from = filter.from?.toString(),
            to = filter.to?.toString(),
            limit = PAGE_SIZE
        )
        .toDomain()

    private fun isStale(fetchedAt: Instant?, ttlMinutes: Long): Boolean =
        fetchedAt == null || Duration.between(fetchedAt, clock.instant()) >= Duration.ofMinutes(ttlMinutes)

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun refreshCatching(block: suspend () -> Unit): RefreshResult = try {
        block()
        RefreshResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        RefreshResult.Failure(WalletError.Network)
    } catch (e: HttpException) {
        if (e.code() >= SERVER_ERROR_MIN_CODE) {
            // Breadcrumb only: no body, no alert
            crashReporter.log("Server error ${e.code()} while refreshing wallet data")
        }
        RefreshResult.Failure(WalletError.Server(e.code()))
    } catch (e: Exception) {
        crashReporter.recordException(e)
        RefreshResult.Failure(WalletError.Unexpected(e))
    }
}
