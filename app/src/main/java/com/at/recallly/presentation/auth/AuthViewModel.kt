package com.at.recallly.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.at.recallly.core.result.Result
import com.at.recallly.domain.usecase.auth.GetCurrentUserUseCase
import com.at.recallly.domain.usecase.auth.LoginWithEmailUseCase
import com.at.recallly.domain.usecase.auth.LoginWithGoogleUseCase
import com.at.recallly.domain.usecase.auth.LogoutUseCase
import com.at.recallly.domain.usecase.auth.SignUpWithEmailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val logoutUseCase: LogoutUseCase,
    getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        getCurrentUserUseCase()
            .onEach { user ->
                _uiState.update { it.copy(isLoggedIn = user != null) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AuthUiEvent, context: Context? = null) {
        when (event) {
            is AuthUiEvent.LoginWithEmail -> loginWithEmail(event.email, event.password)
            is AuthUiEvent.SignUpWithEmail -> signUpWithEmail(event.name, event.email, event.password)
            is AuthUiEvent.LoginWithGoogle -> loginWithGoogle(context!!)
            is AuthUiEvent.Logout -> logout()
            is AuthUiEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loginWithEmailUseCase(email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun signUpWithEmail(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = signUpWithEmailUseCase(name, email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun loginWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loginWithGoogleUseCase(context)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}
