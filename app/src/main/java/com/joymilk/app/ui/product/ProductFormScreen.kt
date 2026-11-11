package com.joymilk.app.ui.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.data.model.Product
import com.joymilk.app.ui.components.JoyMilkTopBar
import com.joymilk.app.ui.components.LoadingIndicator

private val UNITS = listOf("botol", "pcs", "kg", "liter", "pack")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }
    LaunchedEffect(formState) {
        if (formState is ProductFormUiState.Saved) onSaved()
    }

    val isEdit = productId != null
    val title = if (isEdit) "Edit Produk" else "Tambah Produk"

    Scaffold(topBar = { JoyMilkTopBar(title = title, onBack = onBack) }) { padding ->
        when (val state = formState) {
            is ProductFormUiState.Loading -> LoadingIndicator()
            is ProductFormUiState.Loaded -> ProductForm(
                product = state.product,
                categories = categories,
                isEdit = isEdit,
                isSaving = false,
                onSave = { viewModel.saveProduct(it) },
                onAddCategory = { viewModel.addCategory(it) },
                modifier = Modifier.padding(padding)
            )
            is ProductFormUiState.Idle -> LoadingIndicator()
            is ProductFormUiState.Saved -> LoadingIndicator() // navigating away
            is ProductFormUiState.Error -> ProductForm(
                product = state.product,
                categories = categories,
                isEdit = isEdit,
                isSaving = false,
                errorMessage = state.message,
                onSave = { viewModel.saveProduct(it) },
                onAddCategory = { viewModel.addCategory(it) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductForm(
    product: Product,
    categories: List<String>,
    isEdit: Boolean,
    isSaving: Boolean,
    errorMessage: String? = null,
    onSave: (Product) -> Unit,
    onAddCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(product.id) { mutableStateOf(product.name) }
    var category by remember(product.id) { mutableStateOf(product.category) }
    var size by remember(product.id) { mutableStateOf(product.size) }
    var priceText by remember(product.id) { mutableStateOf(if (product.currentPrice > 0) product.currentPrice.toString() else "") }
    var unit by remember(product.id) { mutableStateOf(product.unit.ifEmpty { UNITS[0] }) }
    var description by remember(product.id) { mutableStateOf(product.description) }
    var isActive by remember(product.id) { mutableStateOf(product.isActive) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Auto-select first category when categories load and category not yet set
    LaunchedEffect(categories) {
        if (category.isEmpty() && categories.isNotEmpty()) category = categories.first()
    }

    val originalPrice = product.currentPrice
    val newPrice = priceText.toLongOrNull() ?: 0L
    val priceChanged = isEdit && originalPrice > 0 && newPrice != originalPrice

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onConfirm = { newCat ->
                onAddCategory(newCat)
                category = newCat
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Produk") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Category dropdown — from Firestore + option to add new
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false })
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("+ Tambah Kategori Baru") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { categoryExpanded = false; showAddCategoryDialog = true }
                )
            }
        }

        // Size field
        OutlinedTextField(
            value = size,
            onValueChange = { size = it },
            label = { Text("Ukuran (opsional, misal: 500ml, 1L)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it.filter { c -> c.isDigit() } },
            label = { Text("Harga (Rp)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = { Text("Rp ") },
            singleLine = true
        )

        // Price change warning
        if (priceChanged) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(
                    text = "⚠️ Perubahan harga akan dicatat dalam riwayat harga",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Unit dropdown
        ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
            OutlinedTextField(
                value = unit,
                onValueChange = {},
                readOnly = true,
                label = { Text("Satuan") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                UNITS.forEach { u ->
                    DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; unitExpanded = false })
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Deskripsi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        if (isEdit) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Produk Aktif", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
        }

        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                onSave(
                    product.copy(
                        name = name.trim(),
                        category = category,
                        size = size.trim(),
                        currentPrice = newPrice,
                        unit = unit,
                        description = description.trim(),
                        isActive = isActive
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isSaving && name.isNotBlank() && newPrice > 0
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("SIMPAN PRODUK")
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kategori Baru") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nama Kategori") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text("Tambah")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
