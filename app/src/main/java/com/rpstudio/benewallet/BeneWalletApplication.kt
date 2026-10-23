package com.rpstudio.benewallet

import android.app.Application
import com.rpstudio.benewallet.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BeneWalletApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BeneWalletApplication)
            modules(appModules)
        }
    }
}