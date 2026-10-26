package com.rpstudio.benewallet.feature.auth.di

import com.rpstudio.benewallet.feature.auth.presentation.login.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loginModule: Module = module {
    viewModelOf(::LoginViewModel)
}
