package com.randallengineering.finances.core.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(private val session: SessionManager) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Stream that flips true once a real Firebase session exists. */
    val isSignedIn: StateFlow<Boolean> = session.isSignedIn

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun createAccount(email: String, password: String, displayName: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            when (val r = session.createAccount(email, password, displayName)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Account created. Welcome!")
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = r.message)
                }
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            when (val r = session.login(email, password)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = r.message)
                }
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun handleGoogleResult(task: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val r = session.handleSignInResult(task)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is Resource.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = r.message)
                else -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /** Surfaces a Google sign-in exception (e.g. status 10 config error, cancelled). */
    fun handleGoogleSignInError(e: ApiException) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = session.googleSignInErrorMessage(e))
    }

    /** Set a visible error message directly (e.g. Google sign-in cancelled). */
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    fun signOut() {
        viewModelScope.launch { session.signOut() }
    }
}
