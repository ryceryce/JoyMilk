package com.joymilk.app.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.data.model.SweetLevel
import com.joymilk.app.ui.components.JoyMilkTopBar
import com.joymilk.app.ui.components.LoadingIndicator
import com.joymilk.app.ui.components.OfflineBanner
import com.joymilk.app.util.Constants
import com.joymilk.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    orderId: String?,
    onOrderSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadFormProducts() }
    LaunchedEffect(formState) {
        if (formState is OrderFormUiState.Saved) onOrderSaved()
    }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(Constants.PAYMENT_TUNAI) }
    var notes by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf(FormItem())) }

    val isSaving = formState is OrderFormUiState.Loading
    val products = (formState as? OrderFormUiState.Ready)?.products ?: emptyList()
    val totalAmount = items.sumOf { it.subtotal }

    Scaffold(
        topBar = { JoyMilkTopBar(title = if (orderId == null) "Buat Order" else "Edit Order", onBack = onBack) }
    ) { padding ->
        when (formState) {
            is OrderFormUiState.Idle, is OrderFormUiState.Loading -> LoadingIndicator()
            is OrderFormUiState.Saved -> LoadingIndicator()
            else -> {
                Box(modifier = Modifier.padding(padding)) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { OfflineBanner(isOffline = isOffline) }
                        // Customer info
                        item {
                            Text("Info Pelanggan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        item {
                            OutlinedTextField(value = customerName, onValueChange = { customerName = it },
                                label = { Text("Nama Pelanggan *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        item {
                            OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it },
                                label = { Text("No. Telepon") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        }
                        item {
                            OutlinedTextField(value = deliveryAddress, onValueChange = { deliveryAddress = it },
                                label = { Text("Alamat Pengiriman") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        }

                        // Items
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("Item Pesanan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        itemsIndexed(items) { index, item ->
                            OrderItemCard(
                                item = item,
                                products = products,
                                onUpdate = { updated -> items = items.toMutableList().also { it[index] = updated } },
                                onDelete = if (items.size > 1) ({ items = items.toMutableList().also { it.removeAt(index) } }) else null
                            )
                        }
                        item {
                            OutlinedButton(onClick = { items = items + FormItem() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Tambah Item")
                            }
                        }

                        // Payment
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(Constants.PAYMENT_TUNAI to "Tunai", Constants.PAYMENT_TRANSFER to "Transfer", Constants.PAYMENT_QRIS to "QRIS")
                                    .forEach { (method, label) ->
                                        FilterChip(
                                            selected = paymentMethod == method,
                                            onClick = { paymentMethod = method },
                                            label = { Text(label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                            }
                        }

                        item {
                            OutlinedTextField(value = notes, onValueChange = { notes = it },
                                label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        }

                        if (formState is OrderFormUiState.Error) {
                            item {
                                Text((formState as OrderFormUiState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Sticky bottom bar
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyUtils.formatRupiah(totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = { viewModel.saveOrder(customerName, customerPhone, deliveryAddress, paymentMethod, notes, items) },
                                enabled = !isSaving && customerName.isNotBlank() && items.any { it.productId.isNotBlank() }
                            ) {
                                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("SIMPAN ORDER")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderItemCard(
    item: FormItem,
    products: List<com.joymilk.app.data.model.Product>,
    onUpdate: (FormItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    var productExpanded by remember { mutableStateOf(false) }

    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Product dropdown
            ExposedDropdownMenuBox(expanded = productExpanded, onExpandedChange = { productExpanded = it }) {
                OutlinedTextField(
                    value = item.productName.ifEmpty { "Pilih Produk" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Produk") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)                )
                ExposedDropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                    products.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.name} — ${CurrencyUtils.formatRupiah(p.currentPrice)}/${p.unit}") },
                            onClick = {
                                onUpdate(item.copy(productId = p.id, productName = p.name, priceAtOrder = p.currentPrice))
                                productExpanded = false
                            }
                        )
                    }
                }
            }

            // Sweet level chips
            Text("Tingkat Manis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SweetLevel.entries.forEach { level ->
                    FilterChip(
                        selected = item.sweetLevel == level.name,
                        onClick = { onUpdate(item.copy(sweetLevel = level.name)) },
                        label = { Text(level.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Quantity stepper + subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { if (item.quantity > 1) onUpdate(item.copy(quantity = item.quantity - 1)) }) {
                        Text("−")
                    }
                    Text(
                        text = item.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FilledTonalIconButton(onClick = { onUpdate(item.copy(quantity = item.quantity + 1)) }) {
                        Text("+")
                    }
                }
                Text(
                    text = CurrencyUtils.formatRupiah(item.subtotal),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Item", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
