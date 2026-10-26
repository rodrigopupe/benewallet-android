package com.rpstudio.benewallet.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpstudio.benewallet.core.auth.SessionManager
import com.rpstudio.benewallet.core.auth.SessionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Reads the saved session once when the app opens and exposes it, so the root can pick the right screen. */
internal class AppViewModel(sessionManager: SessionManager) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.state

    init {
        viewModelScope.launch { sessionManager.restore() }
    }
}
