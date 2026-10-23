package com.rpstudio.benewallet.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

object DispatcherNames {
    const val IO = "io"
    const val DEFAULT = "default"
}

val coreCommonModule = module {
    single<CoroutineDispatcher>(named(DispatcherNames.IO)) { Dispatchers.IO }
    single<CoroutineDispatcher>(named(DispatcherNames.DEFAULT)) { Dispatchers.Default }
}