package com.joymilk.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joymilk.app.data.model.User
import com.joymilk.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserListUiState {
    data object Loading : UserListUiState
    data class Success(val users: List<User>) : UserListUiState
    data class Error(val message: String) : UserListUiState
}

sealed interface UserFormUiState {
    data object Idle : UserFormUiState
    data object Loading : UserFormUiState
    data object Saved : UserFormUiState
    data class Error(val message: String) : UserFormUiState
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _listState = MutableStateFlow<UserListUiState>(UserListUiState.Loading)
    val listState: StateFlow<UserListUiState> = _listState

    private val _formState = MutableStateFlow<UserFormUiState>(UserFormUiState.Idle)
    val formState: StateFlow<UserFormUiState> = _formState

    fun loadUsers() {
        viewModelScope.launch {
            userRepository.getUsers()
                .catch { e -> _listState.value = UserListUiState.Error(e.message ?: "Gagal memuat pengguna") }
                .collect { _listState.value = UserListUiState.Success(it) }
        }
    }

    fun createUser(name: String, email: String, password: String, role: String) {
        viewModelScope.launch {
            _formState.value = UserFormUiState.Loading
            userRepository.createUser(name, email, password, role)
                .onSuccess { _formState.value = UserFormUiState.Saved }
                .onFailure { e -> _formState.value = UserFormUiState.Error(e.message ?: "Gagal membuat pengguna") }
        }
    }

    fun toggleActive(userId: String, isActive: Boolean) {
        viewModelScope.launch { userRepository.toggleUserActive(userId, isActive) }
    }

    fun resetFormState() { _formState.value = UserFormUiState.Idle }
}

