package com.rpstudio.benewallet.app.di

import com.rpstudio.benewallet.BuildConfig
import com.rpstudio.benewallet.core.analytics.analyticsModule
import com.rpstudio.benewallet.core.common.coreCommonModule
import com.rpstudio.benewallet.core.crash.crashModule
import com.rpstudio.benewallet.core.featureflags.featureFlagsModule
import com.rpstudio.benewallet.core.network.networkModule
import com.rpstudio.benewallet.feature.wallet.di.walletModule

val appModules = listOf(
    coreCommonModule,
    crashModule,
    analyticsModule,
    networkModule(BuildConfig.BFF_BASE_URL, BuildConfig.DEBUG),
    featureFlagsModule,
    walletModule
)
