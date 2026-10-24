package com.rpstudio.benewallet.feature.wallet.di

import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit

val walletModule: Module = module {
    single<WalletService> { get<Retrofit>().create(WalletService::class.java) }
}
