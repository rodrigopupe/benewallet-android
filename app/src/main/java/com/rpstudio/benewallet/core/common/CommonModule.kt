package com.rpstudio.benewallet.core.common

import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreCommonModule = module {
    single<CoroutineDispatcher>(named(DispatcherNames.IO)) { Dispatchers.IO }
    single<CoroutineDispatcher>(named(DispatcherNames.DEFAULT)) { Dispatchers.Default }
    single<Clock> { Clock.systemUTC() }
}

object DispatcherNames {
    const val IO = "io"
    const val DEFAULT = "default"
}
