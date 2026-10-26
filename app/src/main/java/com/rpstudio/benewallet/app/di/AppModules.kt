package com.rpstudio.benewallet.app.di

import com.rpstudio.benewallet.BuildConfig
import com.rpstudio.benewallet.app.AppViewModel
import com.rpstudio.benewallet.core.analytics.analyticsModule
import com.rpstudio.benewallet.core.auth.authModule
import com.rpstudio.benewallet.core.common.coreCommonModule
import com.rpstudio.benewallet.core.crash.crashModule
import com.rpstudio.benewallet.core.featureflags.featureFlagsModule
import com.rpstudio.benewallet.core.network.networkModule
import com.rpstudio.benewallet.feature.auth.di.loginModule
import com.rpstudio.benewallet.feature.wallet.di.walletModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val appModule = module {
    viewModelOf(::AppViewModel)
}

val appModules = listOf(
    coreCommonModule,
    crashModule,
    analyticsModule,
    networkModule(BuildConfig.BFF_BASE_URL, BuildConfig.DEBUG),
    authModule(BuildConfig.DEBUG),
    featureFlagsModule,
    appModule,
    loginModule,
    walletModule
)
