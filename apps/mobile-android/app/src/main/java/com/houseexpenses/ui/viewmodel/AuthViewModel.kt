package com.houseexpenses.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.houseexpenses.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository(application)
    val sessionManager = repository.sessionManager

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _serverUrl = MutableStateFlow(sessionManager.serverUrl)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun updateServerUrl(newUrl: String) {
        sessionManager.serverUrl = newUrl
        _serverUrl.value = sessionManager.serverUrl
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.login(email.trim(), pass)
            _loading.value = false
            result.onSuccess {
                _isLoggedIn.value = true
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Error al autenticar")
            }
        }
    }

    fun register(email: String, pass: String, name: String, taxId: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.register(email.trim(), pass, name.trim(), taxId?.trim()?.ifBlank { null })
            _loading.value = false
            result.onSuccess {
                _isLoggedIn.value = true
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Error al registrarse")
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        sessionManager.clearSession()
        _isLoggedIn.value = false
        onLoggedOut()
    }
}
