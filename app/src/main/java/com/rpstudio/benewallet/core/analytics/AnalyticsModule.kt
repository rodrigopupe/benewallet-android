package com.rpstudio.benewallet.core.analytics

import org.koin.dsl.module

val analyticsModule = module {
    single<Analytics> { LogcatAnalytics() }
}
