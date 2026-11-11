package com.joymilk.app.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.data.model.OrderItem
import com.joymilk.app.data.model.SweetLevel
import com.joymilk.app.ui.components.JoyMilkTopBar
import com.joymilk.app.ui.components.LoadingIndicator
import com.joymilk.app.ui.components.StatusChip
import com.joymilk.app.ui.components.OfflineBanner
import com.joymilk.app.util.Constants
import com.joymilk.app.util.CurrencyUtils
import com.joymilk.app.util.DateUtils

@Composable
fun OrderDetailScreen(
    orderId: String,
    userRole: String,
    onBack: () -> Unit,
    onEditOrder: (String) -> Unit,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    var showStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) { viewModel.loadOrderDetail(orderId) }

    Scaffold(
        topBar = {
            JoyMilkTopBar(
                title = "Detail Order",
                onBack = onBack,
                actions = {
                    val order = (detailState as? OrderDetailUiState.Success)?.order
                    if (order?.status == Constants.STATUS_PENDING) {
                        IconButton(onClick = { onEditOrder(orderId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Order")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = detailState) {
            is OrderDetailUiState.Loading -> LoadingIndicator()
            is OrderDetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

            is OrderDetailUiState.Success -> {
                val order = state.order
                val items = state.items

                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { OfflineBanner(isOffline = isOffline) }
                    // Status + date
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusChip(order.status)
                            Text(
                                DateUtils.formatDate(order.createdAt.toDate()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Customer info card
                    item {
                        ElevatedCard(elevation = CardDefaults.elevatedCardElevation(1.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Info Pelanggan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    order.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (order.customerPhone.isNotBlank())
                                    Text(order.customerPhone, style = MaterialTheme.typography.bodyMedium)
                                if (order.deliveryAddress.isNotBlank())
                                    Text(order.deliveryAddress, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Pembayaran: ${order.paymentMethod.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (order.notes.isNotBlank())
                                    Text(
                                        "Catatan: ${order.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            }
                        }
                    }

                    // Items header
                    item {
                        Text(
                            "Item Pesanan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(items) { item -> OrderItemRow(item) }

                    // Total box
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    CurrencyUtils.formatRupiah(order.totalAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Update status button
                    if (order.status != Constants.STATUS_DIBATALKAN && order.status != Constants.STATUS_SELESAI) {
                        item {
                            Button(
                                onClick = { showStatusDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Update Status") }
                        }
                    }

                    // Cancel button — only for pending
                    if (order.status == Constants.STATUS_PENDING) {
                        item {
                            OutlinedButton(
                                onClick = { viewModel.updateOrderStatus(orderId, Constants.STATUS_DIBATALKAN) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Batalkan Order") }
                        }
                    }
                }

                // Status dialog
                if (showStatusDialog) {
                    val currentOrder = (detailState as OrderDetailUiState.Success).order
                    AlertDialog(
                        onDismissRequest = { showStatusDialog = false },
                        title = { Text("Update Status Order") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val nextStatuses = when (currentOrder.status) {
                                    Constants.STATUS_PENDING -> listOf(Constants.STATUS_DIPROSES)
                                    Constants.STATUS_DIPROSES -> listOf(Constants.STATUS_SELESAI)
                                    else -> emptyList()
                                }
                                nextStatuses.forEach { status ->
                                    val label = when (status) {
                                        Constants.STATUS_DIPROSES -> "Tandai Diproses"
                                        Constants.STATUS_SELESAI -> "Tandai Selesai"
                                        else -> status
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatus(orderId, status)
                                            showStatusDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(label) }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showStatusDialog = false }) { Text("Batal") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    val sweetLabel = SweetLevel.entries.find { it.name == item.sweetLevel }?.displayName ?: item.sweetLevel
    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "$sweetLabel · ${item.quantity}x ${CurrencyUtils.formatRupiah(item.priceAtOrder)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                CurrencyUtils.formatRupiah(item.subtotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

