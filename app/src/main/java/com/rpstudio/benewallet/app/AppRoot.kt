package com.rpstudio.benewallet.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpstudio.benewallet.app.navigation.AppNavHost
import com.rpstudio.benewallet.core.auth.SessionState
import com.rpstudio.benewallet.feature.auth.presentation.login.LoginRoute
import org.koin.androidx.compose.koinViewModel

/**
 * The session decides what is on screen. Because the whole tree is swapped, leaving the session (sign-out, or a
 * refresh token the server refused) always lands on the login, and there is no back stack to return to the wallet.
 */
@Composable
internal fun AppRoot(viewModel: AppViewModel = koinViewModel()) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    when (session) {
        SessionState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        SessionState.SignedOut -> LoginRoute()

        SessionState.SignedIn -> AppNavHost()
    }
}
