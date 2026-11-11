package com.joymilk.app.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.ui.components.BottomNavBar
import com.joymilk.app.ui.components.LoadingIndicator
import com.joymilk.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToMore: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Laporan", fontWeight = FontWeight.SemiBold) })
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "reports",
                onDashboard = onNavigateToDashboard,
                onOrders = onNavigateToOrders,
                onReports = {},
                onMore = onNavigateToMore
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Period tabs
            TabRow(selectedTabIndex = uiState.period.ordinal) {
                ReportPeriod.entries.forEach { period ->
                    Tab(
                        selected = uiState.period == period,
                        onClick = { viewModel.loadReport(period) },
                        text = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (uiState.isLoading) {
                LoadingIndicator()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard("Total Pendapatan", CurrencyUtils.formatRupiah(uiState.data.totalRevenue), Modifier.weight(1f))
                            MetricCard("Jumlah Order", uiState.data.orderCount.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        Text("Produk Terlaris", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (uiState.data.topProducts.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        itemsIndexed(uiState.data.topProducts) { index, (name, qty) ->
                            TopProductRow(rank = index + 1, name = name, qty = qty)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, elevation = CardDefaults.elevatedCardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TopProductRow(rank: Int, name: String, qty: Int) {
    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$rank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                Text(name, style = MaterialTheme.typography.bodyMedium)
            }
            Text("$qty unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
