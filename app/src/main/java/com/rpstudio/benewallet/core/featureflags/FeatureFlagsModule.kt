package com.rpstudio.benewallet.core.featureflags

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rpstudio.benewallet.core.auth.SessionDataCleaner
import com.rpstudio.benewallet.core.common.DispatcherNames
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

private const val FEATURE_FLAGS_FILE_NAME = "feature_flags"

val featureFlagsModule: Module = module {
    single<FeatureFlagsService> { get<Retrofit>().create(FeatureFlagsService::class.java) }

    // DataStore requires a single instance per file in the process, hence a single
    single<DataStore<Preferences>>(named(FEATURE_FLAGS_FILE_NAME)) {
        PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile(FEATURE_FLAGS_FILE_NAME) }
    }

    single<FeatureFlagStore> { DataStoreFeatureFlagStore(get(named(FEATURE_FLAGS_FILE_NAME))) }

    single { FeatureFlagsSessionCleaner(get()) } bind SessionDataCleaner::class

    single<FeatureFlagsRepository> {
        DefaultFeatureFlagsRepository(
            service = get(),
            store = get(),
            crashReporter = get(),
            ioDispatcher = get<CoroutineDispatcher>(named(DispatcherNames.IO))
        )
    }
}
