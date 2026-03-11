package com.at.recallly.presentation.auth

sealed interface AuthUiEvent {
    data class LoginWithEmail(val email: String, val password: String) : AuthUiEvent
    data class SignUpWithEmail(val name: String, val email: String, val password: String) : AuthUiEvent
    data object LoginWithGoogle : AuthUiEvent
    data object Logout : AuthUiEvent
    data object ClearError : AuthUiEvent
}
