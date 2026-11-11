package com.joymilk.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.joymilk.app.ui.order.OrderCard
import com.joymilk.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userRole: String,
    onCreateOrder: () -> Unit,
    onOrderClick: (String) -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMore: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Selamat datang", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.userName.ifEmpty { "JoyMilk" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Logout is in More screen; just show app icon here
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "dashboard",
                onDashboard = {},
                onOrders = onNavigateToOrders,
                onReports = onNavigateToReports,
                onMore = onNavigateToMore
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's metric cards
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            label = "Order Hari Ini",
                            value = uiState.todaySummary.orderCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Pendapatan Hari Ini",
                            value = CurrencyUtils.formatRupiah(uiState.todaySummary.totalRevenue),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // CTA
                item {
                    Button(
                        onClick = onCreateOrder,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("BUAT ORDER BARU", fontWeight = FontWeight.Bold) }
                }

                // Recent orders
                item {
                    Text("Order Terbaru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (uiState.recentOrders.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada order hari ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(uiState.recentOrders, key = { it.id }) { order ->
                        OrderCard(order = order, onClick = { onOrderClick(order.id) })
                    }
                    item {
                        TextButton(onClick = onNavigateToOrders, modifier = Modifier.fillMaxWidth()) {
                            Text("Lihat Semua Order")
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
