package com.rpstudio.benewallet.core.crash

import com.rpstudio.benewallet.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val crashModule = module {
    single<CrashReporter> {
        SentryCrashReporter(androidContext(), BuildConfig.SENTRY_DSN)
    }
}
