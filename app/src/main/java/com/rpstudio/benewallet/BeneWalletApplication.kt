package com.rpstudio.benewallet

import android.app.Application
import com.rpstudio.benewallet.app.di.appModules
import com.rpstudio.benewallet.core.crash.CrashReporter
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BeneWalletApplication : Application() {
    private val crashReporter: CrashReporter by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BeneWalletApplication)
            modules(appModules)
        }

        crashReporter.initialize()
    }
}