package com.joymilk.app.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joymilk.app.ui.components.JoyMilkTopBar
import com.joymilk.app.ui.components.LoadingIndicator
import com.joymilk.app.util.Constants

@Composable
fun UserFormScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(formState) {
        if (formState is UserFormUiState.Saved) onSaved()
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Constants.ROLE_STAFF) }

    Scaffold(topBar = { JoyMilkTopBar(title = "Tambah Pengguna", onBack = onBack) }) { padding ->
        if (formState is UserFormUiState.Loading) {
            LoadingIndicator()
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Text("Role", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Constants.ROLE_STAFF to "Staff", Constants.ROLE_ADMIN to "Admin").forEach { (r, label) ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(label) }
                        )
                    }
                }

                if (formState is UserFormUiState.Error) {
                    Text(
                        (formState as UserFormUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.createUser(name, email, password, role) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6
                ) { Text("SIMPAN PENGGUNA") }
            }
        }
    }
}

