package com.rpstudio.benewallet.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rpstudio.benewallet.feature.wallet.presentation.home.HomeRoute
import com.rpstudio.benewallet.feature.wallet.presentation.statement.StatementRoute
import kotlinx.serialization.Serializable

@Serializable
internal object HomeDestination

@Serializable
internal object StatementDestination

@Composable
internal fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeDestination
    ) {
        composable<HomeDestination> {
            HomeRoute(
                onOpenStatement = {
                    navController.navigate(StatementDestination)
                }
            )
        }

        composable<StatementDestination> { StatementRoute() }
    }
}
