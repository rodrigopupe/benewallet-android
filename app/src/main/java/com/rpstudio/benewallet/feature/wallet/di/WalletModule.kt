package com.rpstudio.benewallet.feature.wallet.di

import androidx.room.Room
import com.rpstudio.benewallet.core.common.DispatcherNames
import com.rpstudio.benewallet.feature.wallet.data.local.RoomWalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.local.WALLET_DATABASE_NAME
import com.rpstudio.benewallet.feature.wallet.data.local.WalletDatabase
import com.rpstudio.benewallet.feature.wallet.data.local.WalletLocalDataSource
import com.rpstudio.benewallet.feature.wallet.data.remote.WalletService
import com.rpstudio.benewallet.feature.wallet.data.repository.DefaultWalletRepository
import com.rpstudio.benewallet.feature.wallet.domain.repository.WalletRepository
import com.rpstudio.benewallet.feature.wallet.presentation.home.HomeViewModel
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val walletModule: Module = module {
    single<WalletService> { get<Retrofit>().create(WalletService::class.java) }

    single<WalletDatabase> {
        Room.databaseBuilder(androidContext(), WalletDatabase::class.java, WALLET_DATABASE_NAME)
            // The database is only a cache: wiping it on a schema change is cheaper than writing migrations
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single<WalletLocalDataSource> { RoomWalletLocalDataSource(get()) }

    single<WalletRepository> {
        DefaultWalletRepository(
            service = get(),
            local = get(),
            crashReporter = get(),
            ioDispatcher = get<CoroutineDispatcher>(named(DispatcherNames.IO)),
            clock = get<Clock>()
        )
    }

    viewModelOf(::HomeViewModel)
}
