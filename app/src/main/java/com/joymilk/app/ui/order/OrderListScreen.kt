package com.joymilk.app.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.data.model.Order
import com.joymilk.app.ui.components.BottomNavBar
import com.joymilk.app.ui.components.StatusChip
import com.joymilk.app.ui.components.LoadingIndicator
import com.joymilk.app.ui.components.OfflineBanner
import com.joymilk.app.util.Constants
import com.joymilk.app.util.CurrencyUtils
import com.joymilk.app.util.DateUtils

private val STATUS_FILTERS = listOf(
    null to "Semua",
    Constants.STATUS_PENDING to "Pending",
    Constants.STATUS_DIPROSES to "Diproses",
    Constants.STATUS_SELESAI to "Selesai",
    Constants.STATUS_DIBATALKAN to "Dibatalkan"
)

@Composable
fun OrderListScreen(
    onOrderClick: (orderId: String) -> Unit,
    onCreateOrder: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToMore: () -> Unit,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadOrders() }
    LaunchedEffect(selectedStatus) { viewModel.statusFilter.value = selectedStatus }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("Daftar Order", fontWeight = FontWeight.SemiBold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateOrder,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Buat Order") }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "orders",
                onDashboard = onNavigateToDashboard,
                onOrders = {},
                onReports = onNavigateToReports,
                onMore = onNavigateToMore
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OfflineBanner(isOffline = isOffline)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(STATUS_FILTERS) { (status, label) ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(label) }
                    )
                }
            }
            when (val state = listState) {
                is OrderListUiState.Loading -> LoadingIndicator()
                is OrderListUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is OrderListUiState.Success -> {
                    if (state.orders.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belum ada order", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.orders, key = { it.id }) { order ->
                                OrderCard(order = order, onClick = { onOrderClick(order.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusChip(status = order.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = DateUtils.formatDate(order.createdAt.toDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatRupiah(order.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
