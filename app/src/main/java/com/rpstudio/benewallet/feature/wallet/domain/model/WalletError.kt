package com.rpstudio.benewallet.feature.wallet.domain.model

internal sealed interface WalletError {
    /** No connectivity, timeout or DNS failure. Expected, not reported to the crash reporter. */
    object Network : WalletError

    /** The server answered with an HTTP error. */
    data class Server(val code: Int) : WalletError

    /** Anything else (e.g. malformed payload). Likely a bug or a broken contract. */
    data class Unexpected(val cause: Throwable) : WalletError
}

internal sealed interface RefreshResult {
    object Success : RefreshResult

    data class Failure(val error: WalletError) : RefreshResult
}
