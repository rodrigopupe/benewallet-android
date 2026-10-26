package com.rpstudio.benewallet.feature.wallet.presentation.statement

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpstudio.benewallet.R
import com.rpstudio.benewallet.feature.wallet.domain.model.Category
import com.rpstudio.benewallet.feature.wallet.domain.model.Transaction
import com.rpstudio.benewallet.feature.wallet.domain.model.statementZone
import com.rpstudio.benewallet.feature.wallet.presentation.brazilianLocale
import com.rpstudio.benewallet.feature.wallet.presentation.format
import com.rpstudio.benewallet.feature.wallet.presentation.labelRes
import com.rpstudio.benewallet.feature.wallet.presentation.messageRes
import com.rpstudio.benewallet.feature.wallet.presentation.shortLabelRes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", brazilianLocale)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val categoryFilters = Category.entries.filter { it != Category.OTHER }

@StringRes
private fun PeriodFilter.labelRes(): Int = when (this) {
    PeriodFilter.ALL -> R.string.period_all
    PeriodFilter.LAST_7_DAYS -> R.string.period_7_days
    PeriodFilter.LAST_30_DAYS -> R.string.period_30_days
    PeriodFilter.THIS_MONTH -> R.string.period_this_month
}

@Composable
internal fun StatementRoute(viewModel: StatementViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StatementEffect.ShowError ->
                    snackBarHostState.showSnackbar(context.getString(effect.error.messageRes()))
            }
        }
    }

    StatementScreen(state = state, snackbarHostState = snackBarHostState, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatementScreen(
    state: StatementState,
    snackbarHostState: SnackbarHostState,
    onIntent: (StatementIntent) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.statement_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.filtersAvailable) {
                FilterChips(state, onIntent)
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(StatementIntent.Refresh) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                TransactionList(state, onIntent)
            }
        }
    }
}

@Composable
private fun FilterChips(state: StatementState, onIntent: (StatementIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PeriodFilter.entries) { period ->
                FilterChip(
                    selected = state.period == period,
                    onClick = { onIntent(StatementIntent.SelectPeriod(period)) },
                    label = { Text(stringResource(period.labelRes())) }
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.category == null,
                    onClick = { onIntent(StatementIntent.SelectCategory(null)) },
                    label = { Text(stringResource(R.string.filter_all_categories)) }
                )
            }
            items(categoryFilters) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { onIntent(StatementIntent.SelectCategory(category)) },
                    label = { Text(stringResource(category.labelRes())) }
                )
            }
        }
    }
}

@Composable
private fun TransactionList(state: StatementState, onIntent: (StatementIntent) -> Unit) {
    val groups = remember(state.items) {
        state.items.groupBy { it.occurredAt.atZone(statementZone).toLocalDate() }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (state.isPartial) {
            item(key = "partial") {
                PartialBanner()
            }
        }

        if (state.items.isEmpty()) {
            item(key = "empty") {
                EmptyState(isLoading = state.isRefreshing)
            }
        }

        groups.forEach { (day, transactions) ->
            item(key = "day-$day") { DayHeader(day) }
            items(transactions, key = { it.id }) { TransactionRow(it) }
        }
        if (state.hasMore) item(key = "footer") { LoadMoreFooter(state, onIntent) }
    }
}

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        text = day.format(dayFormatter).replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val time = transaction.occurredAt.atZone(statementZone).format(timeFormatter)
    val subtitle = "${stringResource(transaction.category.labelRes())} · $time · " +
        stringResource(transaction.benefit.shortLabelRes())
    val amountColor = if (transaction.amount.isNegative) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = transaction.amount.format(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

@Composable
private fun PartialBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = stringResource(R.string.statement_partial),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EmptyState(isLoading: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = stringResource(R.string.statement_empty),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LoadMoreFooter(state: StatementState, onIntent: (StatementIntent) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.loadMoreFailed) {
            TextButton(
                onClick = { onIntent(StatementIntent.LoadMore) }
            ) {
                Text(stringResource(R.string.statement_load_more_retry))
            }
        } else {
            CircularProgressIndicator()
            // Entering the composition (or the list growing while still visible) asks for the next page
            LaunchedEffect(state.items.size, state.isRefreshing) {
                onIntent(StatementIntent.LoadMore)
            }
        }
    }
}
