package com.rpstudio.benewallet.feature.wallet.presentation.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpstudio.benewallet.R
import com.rpstudio.benewallet.feature.wallet.domain.model.Benefit
import com.rpstudio.benewallet.feature.wallet.domain.model.BenefitBalance
import com.rpstudio.benewallet.feature.wallet.domain.model.Money
import com.rpstudio.benewallet.feature.wallet.presentation.format
import com.rpstudio.benewallet.feature.wallet.presentation.labelRes
import com.rpstudio.benewallet.feature.wallet.presentation.messageRes
import java.time.Instant
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun HomeRoute(viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.ShowError ->
                    snackBarHostState.showSnackbar(context.getString(effect.error.messageRes()))
            }
        }
    }

    HomeScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        onRefresh = { viewModel.onIntent(HomeIntent.Refresh) }
    )
}

@Composable
internal fun HomeScreen(state: HomeState, snackBarHostState: SnackbarHostState, onRefresh: () -> Unit) {
    Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) }) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (state.balances.isEmpty()) {
                    item { EmptyState(isLoading = state.isRefreshing) }
                } else {
                    item { TotalCard(state.total) }
                    items(state.balances, key = { it.benefit }) { BenefitCard(it) }
                    state.lastUpdatedAt?.let { item { UpdatedAtLabel(it) } }
                }
            }
        }
    }
}

@Composable
private fun TotalCard(total: Money) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.home_total_balance),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = total.format(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BenefitCard(item: BenefitBalance) {
    val container = when (item.benefit) {
        Benefit.MEAL -> MaterialTheme.colorScheme.secondaryContainer
        Benefit.FOOD -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(item.benefit.labelRes()), style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.balance.format(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun UpdatedAtLabel(updatedAt: Instant) {
    val relative = DateUtils.getRelativeTimeSpanString(
        updatedAt.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )
    Text(
        text = stringResource(R.string.home_updated_at, relative.toString()),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyState(isLoading: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(text = stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
