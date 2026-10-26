package com.rpstudio.benewallet.app.di

import com.rpstudio.benewallet.core.auth.FakeTokenStore
import com.rpstudio.benewallet.core.auth.SessionDataCleaner
import com.rpstudio.benewallet.core.auth.SessionManager
import com.rpstudio.benewallet.core.auth.TokenStore
import com.rpstudio.benewallet.core.auth.authModule
import com.rpstudio.benewallet.core.common.coreCommonModule
import com.rpstudio.benewallet.core.crash.CrashReporter
import com.rpstudio.benewallet.core.featureflags.FakeFeatureFlagStore
import com.rpstudio.benewallet.core.featureflags.FeatureFlagStore
import com.rpstudio.benewallet.core.featureflags.FeatureFlagsRepository
import com.rpstudio.benewallet.core.featureflags.FeatureFlagsSessionCleaner
import com.rpstudio.benewallet.core.featureflags.featureFlagsModule
import com.rpstudio.benewallet.core.network.networkModule
import com.rpstudio.benewallet.feature.auth.di.loginModule
import com.rpstudio.benewallet.feature.auth.presentation.login.LoginViewModel
import com.rpstudio.benewallet.feature.wallet.data.FakeCrashReporter
import com.rpstudio.benewallet.feature.wallet.data.FakeWalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.WalletSessionCleaner
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import com.rpstudio.benewallet.feature.wallet.di.walletModule
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import org.junit.Assert.assertEquals
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
        single<TokenStore> { FakeTokenStore() }
    }

    @Test
    fun `repository resolves with real definitions`() {
        val koin = koinApplication {
            modules(
                coreCommonModule,
                networkModule("http://localhost:8080/", isDebug = false),
                authModule(isDebug = false),
                featureFlagsModule,
                loginModule,
                walletModule,
                androidFreeOverrides
            )
        }.koin

        assertNotNull(
            "SessionManager must resolve (service on the unauthenticated client, store fake, cleaners)",
            koin.get<SessionManager>()
        )
        assertEquals(
            "sign-out must wipe the wallet cache and the cached flags: each module registers its own cleaner",
            setOf(WalletSessionCleaner::class, FeatureFlagsSessionCleaner::class),
            koin.getAll<SessionDataCleaner>().map { it::class }.toSet()
        )
        assertNotNull(
            "LoginViewModel must resolve (SessionManager from the real auth module)",
            koin.get<LoginViewModel>()
        )
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
