package com.rpstudio.benewallet.core.featureflags

import com.rpstudio.benewallet.core.crash.CrashReporter
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal class DefaultFeatureFlagsRepository(
    private val service: FeatureFlagsService,
    private val store: FeatureFlagStore,
    private val crashReporter: CrashReporter,
    private val ioDispatcher: CoroutineDispatcher
) : FeatureFlagsRepository {
    override fun observeFlags(): Flow<FeatureFlags> = store.observeFlags().map { FeatureFlags(it) }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun refresh(): Unit = withContext(ioDispatcher) {
        try {
            val response = service.getFeatureFlags(store.installId())
            store.replaceFlags(response.flags.associate { it.key to it.enabled })
        } catch (e: CancellationException) {
            throw e
        } catch (ioe: IOException) {
            // Offline is expected: the cache (or the defaults) stays in place
        } catch (e: HttpException) {
            crashReporter.log("Server error ${e.code()} while refreshing feature flags")
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }
    }
}
