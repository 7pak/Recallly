package com.at.recallly.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable data object Login : Route()
    @Serializable data object SignUp : Route()
    @Serializable data object Home : Route()
}
