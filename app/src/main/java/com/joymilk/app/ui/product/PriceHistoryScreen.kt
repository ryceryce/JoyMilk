package com.joymilk.app.ui.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.data.model.PriceHistory
import com.joymilk.app.ui.components.JoyMilkTopBar
import com.joymilk.app.ui.components.LoadingIndicator
import com.joymilk.app.util.CurrencyUtils
import com.joymilk.app.util.DateUtils

@Composable
fun PriceHistoryScreen(
    productId: String,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val state by viewModel.priceHistoryState.collectAsStateWithLifecycle()
    LaunchedEffect(productId) { viewModel.loadPriceHistory(productId) }

    Scaffold(topBar = { JoyMilkTopBar(title = "Riwayat Harga", onBack = onBack) }) { padding ->
        when (val s = state) {
            is PriceHistoryUiState.Loading -> LoadingIndicator()
            is PriceHistoryUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(s.message, color = MaterialTheme.colorScheme.error) }
            is PriceHistoryUiState.Success -> {
                if (s.history.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat harga", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.history, key = { it.id }) { entry ->
                            PriceHistoryItem(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryItem(entry: PriceHistory) {
    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = CurrencyUtils.formatRupiah(entry.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Diubah oleh: ${entry.changedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = DateUtils.formatDate(entry.effectiveDate.toDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
