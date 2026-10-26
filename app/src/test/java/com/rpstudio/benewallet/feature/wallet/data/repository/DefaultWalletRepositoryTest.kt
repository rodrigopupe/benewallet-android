package com.rpstudio.benewallet.feature.wallet.data.repository

import com.rpstudio.benewallet.feature.wallet.data.FakeCrashReporter
import com.rpstudio.benewallet.feature.wallet.data.FakeWalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.FakeWalletService
import com.rpstudio.benewallet.feature.wallet.data.MutableClock
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BalanceResponseDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BenefitBalanceDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.BenefitDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.CategoryDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionDto
import com.rpstudio.benewallet.feature.wallet.data.remote.dto.TransactionsResponseDto
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.domain.model.RefreshResult
import com.rpstudio.benewallet.feature.wallet.domain.model.StatementFilter
import com.rpstudio.benewallet.feature.wallet.domain.model.WalletError
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWalletRepositoryTest {
    private val service = FakeWalletService()
    private val local = FakeWalletLocalDataSource()
    private val crashReporter = FakeCrashReporter()
    private val clock = MutableClock(Instant.parse("2026-10-23T12:00:00Z"))

    private fun TestScope.repository() = DefaultWalletRepository(
        service = service,
        local = local,
        crashReporter = crashReporter,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        clock = clock
    )

    private fun balanceResponse(cents: Long = 100_000) = BalanceResponseDto(
        balances = listOf(BenefitBalanceDto(BenefitDto.MEAL, cents)),
        updatedAt = "2026-10-23T12:00:00Z"
    )

    private fun page(vararg ids: Long, nextCursor: String? = null, category: CategoryDto = CategoryDto.RESTAURANT) =
        TransactionsResponseDto(
            items = ids.map { id ->
                TransactionDto(
                    id = id,
                    benefit = BenefitDto.MEAL,
                    category = category,
                    merchant = "Merchant $id",
                    amountCents = -1000,
                    occurredAt = "2026-10-23T14:38:00Z"
                )
            },
            nextCursor = nextCursor
        )

    @Test
    fun `refresh balances skips network while fresh and refetches after TTL`() = runTest {
        service.balanceResult = { balanceResponse() }
        val repository = repository()

        assertEquals(RefreshResult.Success, repository.refreshBalances())
        clock.advance(Duration.ofMinutes(1))
        repository.refreshBalances()
        assertEquals("fresh cache must not hit the network", 1, service.balanceCalls)

        clock.advance(Duration.ofMinutes(1))
        repository.refreshBalances()
        assertEquals("stale cache must hit the network", 2, service.balanceCalls)
    }

    @Test
    fun `forced refresh ignores TTL`() = runTest {
        service.balanceResult = { balanceResponse() }
        val repository = repository()

        repository.refreshBalances()
        repository.refreshBalances(force = true)

        assertEquals("force must bypass the TTL", 2, service.balanceCalls)
    }

    @Test
    fun `network failure keeps cached balances and is not reported`() = runTest {
        service.balanceResult = { balanceResponse(cents = 5_000) }
        val repository = repository()
        repository.refreshBalances()
        clock.advance(Duration.ofMinutes(3))
        service.balanceResult = { throw IOException("offline") }

        val result = repository.refreshBalances()

        assertEquals(RefreshResult.Failure(WalletError.Network), result)
        assertEquals(
            "a failed refresh must not wipe the cache",
            listOf(BenefitBalance(Benefit.MEAL, Money(5_000))),
            repository.observeBalances().first().items
        )
        assertTrue("network errors are expected noise", crashReporter.recorded.isEmpty())
    }

    @Test
    fun `server error is mapped and only logged as breadcrumb`() = runTest {
        service.balanceResult = {
            throw HttpException(Response.error<Any>(500, "".toResponseBody(null)))
        }

        val result = repository().refreshBalances()

        assertEquals(RefreshResult.Failure(WalletError.Server(500)), result)
        assertEquals("5xx leaves a breadcrumb", 1, crashReporter.logs.size)
        assertTrue("5xx must not raise an alert", crashReporter.recorded.isEmpty())
    }

    @Test
    fun `unexpected error is reported to crash reporter`() = runTest {
        service.balanceResult = { throw IllegalStateException("boom") }

        val result = repository().refreshBalances()

        assertTrue(
            "unexpected failures are surfaced as Unexpected",
            result is RefreshResult.Failure && result.error is WalletError.Unexpected
        )
        assertEquals(
            "only unexpected errors reach the crash reporter",
            1,
            crashReporter.recorded.size
        )
    }

    @Test
    fun `cancellation is rethrown and not reported`() = runTest {
        service.balanceResult = { throw CancellationException("cancelled") }
        var rethrown = false

        try {
            repository().refreshBalances()
        } catch (expected: CancellationException) {
            rethrown = true
        }

        assertTrue("cancellation must propagate", rethrown)
        assertTrue("cancellation is not an error", crashReporter.recorded.isEmpty())
    }

    @Test
    fun `load more appends older page using stored cursor and filter`() = runTest {
        val filter = StatementFilter(category = Category.RESTAURANT)
        service.transactionsResult = { call ->
            if (call.cursor == null) page(30, 29, nextCursor = "c1") else page(28, 27)
        }
        val repository = repository()

        repository.refreshStatement(filter)
        val first = repository.observeStatement(filter).first()
        assertEquals(listOf(30L, 29L), first.items.map { it.id })
        assertTrue("a cursor means there are more pages", first.hasMore)

        assertEquals(RefreshResult.Success, repository.loadMoreStatement())

        val secondCall = service.transactionCalls.last()
        assertEquals("c1", secondCall.cursor)
        assertEquals("the stored filter must be reused", "RESTAURANT", secondCall.category)
        val second = repository.observeStatement(filter).first()
        assertEquals(listOf(30L, 29L, 28L, 27L), second.items.map { it.id })
        assertFalse("no cursor means the end of the list", second.hasMore)
    }

    @Test
    fun `failed statement refresh keeps snapshot`() = runTest {
        val filter = StatementFilter()
        service.transactionsResult = { page(10, 9) }
        val repository = repository()
        repository.refreshStatement(filter)
        clock.advance(Duration.ofMinutes(6))
        service.transactionsResult = { throw IOException("offline") }

        val result = repository.refreshStatement(filter)

        assertEquals(RefreshResult.Failure(WalletError.Network), result)
        assertEquals(listOf(10L, 9L), repository.observeStatement(filter).first().items.map { it.id })
    }

    @Test
    fun `changing filter marks snapshot as partial until refreshed`() = runTest {
        val all = StatementFilter()
        val delivery = StatementFilter(category = Category.DELIVERY)
        service.transactionsResult = { page(3, 2, nextCursor = "c1") }
        val repository = repository()
        repository.refreshStatement(all)

        val partial = repository.observeStatement(delivery).first()
        assertTrue("snapshot belongs to another filter", partial.isPartial)
        assertFalse("cannot paginate a snapshot of another filter", partial.hasMore)

        service.transactionsResult = { page(8, category = CategoryDto.DELIVERY) }
        repository.refreshStatement(delivery)

        assertEquals(
            "a new filter must hit the network",
            "DELIVERY",
            service.transactionCalls.last().category
        )
        val fresh = repository.observeStatement(delivery).first()
        assertFalse(fresh.isPartial)
        assertEquals(listOf(8L), fresh.items.map { it.id })
    }

    @Test
    fun `fresh snapshot skips network and filter dates are sent as ISO days`() = runTest {
        val filter = StatementFilter(
            from = LocalDate.of(2026, 10, 1),
            to = LocalDate.of(2026, 10, 20)
        )
        service.transactionsResult = { page(1) }
        val repository = repository()

        repository.refreshStatement(filter)
        repository.refreshStatement(filter)
        assertEquals(
            "fresh snapshot for the same filter skips the network",
            1,
            service.transactionCalls.size
        )

        repository.refreshStatement(filter, force = true)
        assertEquals(2, service.transactionCalls.size)
        assertEquals("2026-10-01", service.transactionCalls.last().from)
        assertEquals("2026-10-20", service.transactionCalls.last().to)
    }
}
