package com.rpstudio.benewallet.app.di

import com.rpstudio.benewallet.core.common.coreCommonModule
import com.rpstudio.benewallet.core.crash.CrashReporter
import com.rpstudio.benewallet.core.featureflags.FakeFeatureFlagStore
import com.rpstudio.benewallet.core.featureflags.FeatureFlagStore
import com.rpstudio.benewallet.core.featureflags.FeatureFlagsRepository
import com.rpstudio.benewallet.core.featureflags.featureFlagsModule
import com.rpstudio.benewallet.core.network.networkModule
import com.rpstudio.benewallet.feature.wallet.data.FakeCrashReporter
import com.rpstudio.benewallet.feature.wallet.data.FakeWalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import com.rpstudio.benewallet.feature.wallet.di.walletModule
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class WalletWiringTest {
    // Replaces only what needs an Android Context (Room, DataStore, Sentry). Everything else is the real wiring.
    private val androidFreeOverrides = module {
        single<CrashReporter> { FakeCrashReporter() }
        single<WalletLocalDataSource> { FakeWalletLocalDataSource() }
        single<FeatureFlagStore> { FakeFeatureFlagStore() }
    }

    @Test
    fun `repository resolves with real definitions`() {
        val koin = koinApplication {
            modules(
                coreCommonModule,
                networkModule("http://localhost:8080/", isDebug = false),
                featureFlagsModule,
                walletModule,
                androidFreeOverrides
            )
        }.koin

        assertNotNull(
            "FeatureFlagsRepository must resolve (service, store fake, qualifier)",
            koin.get<FeatureFlagsRepository>()
        )

        assertNotNull(
            "WalletService must resolve from the real network module",
            koin.get<WalletService>()
        )
        assertNotNull(
            "WalletRepository must resolve (qualifier, clock, service, fakes)",
            koin.get<WalletRepository>()
        )
    }
}
