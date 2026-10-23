package com.rpstudio.benewallet.core.crash

interface CrashReporter {
    fun initialize()
    fun recordException(throwable: Throwable)
    fun log(message: String)
}