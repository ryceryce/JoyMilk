package com.joymilk.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val role: String) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email dan password tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(email, password)
                .onSuccess { role -> _uiState.value = LoginUiState.Success(role) }
                .onFailure { e -> _uiState.value = LoginUiState.Error(e.message ?: "Login gagal") }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
