package com.rpstudio.benewallet.core.featureflags

import com.rpstudio.benewallet.feature.wallet.data.FakeCrashReporter
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFeatureFlagsRepositoryTest {
    private val service = FakeFeatureFlagsService()
    private val store = FakeFeatureFlagStore()
    private val crashReporter = FakeCrashReporter()

    private fun TestScope.repository() = DefaultFeatureFlagsRepository(
        service = service,
        store = store,
        crashReporter = crashReporter,
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun response(vararg flags: Pair<String, Boolean>) =
        FeatureFlagsResponseDto(flags.map { FeatureFlagDto(it.first, it.second) })

    @Test
    fun `without cache the safe defaults apply`() = runTest {
        val flags = repository().observeFlags().first()

        assertTrue("statement_filters is stable, so it defaults to on", flags.isEnabled(FeatureFlag.STATEMENT_FILTERS))
        assertEquals("goals has no screen, so it defaults to off", false, flags.isEnabled(FeatureFlag.GOALS))
    }

    @Test
    fun `refresh stores the server values`() = runTest {
        service.result = { response("statement_filters" to false, "goals" to true) }
        val repository = repository()

        repository.refresh()

        val flags = repository.observeFlags().first()
        assertEquals(false, flags.isEnabled(FeatureFlag.STATEMENT_FILTERS))
        assertEquals(true, flags.isEnabled(FeatureFlag.GOALS))
    }

    @Test
    fun `network failure keeps the cached values`() = runTest {
        store.flags.value = mapOf("statement_filters" to false)
        service.result = { throw IOException("offline") }
        val repository = repository()

        repository.refresh()

        assertEquals(
            "an offline refresh must not bring a disabled flag back",
            false,
            repository.observeFlags().first().isEnabled(FeatureFlag.STATEMENT_FILTERS)
        )
        assertTrue("offline is expected, nothing to report", crashReporter.recorded.isEmpty())
    }

    @Test
    fun `server error keeps the cache and only leaves a breadcrumb`() = runTest {
        store.flags.value = mapOf("goals" to true)
        service.result =
            { throw HttpException(Response.error<Any>(503, "".toResponseBody("text/plain".toMediaType()))) }
        val repository = repository()

        repository.refresh()

        assertEquals(true, repository.observeFlags().first().isEnabled(FeatureFlag.GOALS))
        assertEquals(1, crashReporter.logs.size)
        assertTrue(crashReporter.recorded.isEmpty())
    }

    @Test
    fun `unexpected failure is reported and keeps the cache`() = runTest {
        service.result = { throw IllegalStateException("bad payload") }
        val repository = repository()

        repository.refresh()

        assertEquals(1, crashReporter.recorded.size)
        assertTrue(repository.observeFlags().first().isEnabled(FeatureFlag.STATEMENT_FILTERS))
    }

    @Test
    fun `a flag removed from the server goes back to its default`() = runTest {
        store.flags.value = mapOf("statement_filters" to false)
        service.result = { response("goals" to true) }
        val repository = repository()

        repository.refresh()

        assertTrue(repository.observeFlags().first().isEnabled(FeatureFlag.STATEMENT_FILTERS))
    }

    @Test
    fun `refresh identifies the user with the installation id from the store`() = runTest {
        service.result = { response() }
        val repository = repository()

        repository.refresh()

        assertEquals(listOf<String?>(store.installId()), service.userIds)
    }
}
