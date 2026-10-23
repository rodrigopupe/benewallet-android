package com.rpstudio.benewallet.app.di

import com.rpstudio.benewallet.core.analytics.analyticsModule
import com.rpstudio.benewallet.core.common.coreCommonModule
import com.rpstudio.benewallet.core.crash.crashModule

val appModules = listOf(
    coreCommonModule,
    crashModule,
    analyticsModule
)
