package com.at.recallly.presentation.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isAuthResolved: Boolean = false,
    val error: String? = null
)
