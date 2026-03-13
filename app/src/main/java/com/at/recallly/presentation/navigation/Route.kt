package com.at.recallly.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable data object Splash : Route()
    @Serializable data object Login : Route()
    @Serializable data object SignUp : Route()
    @Serializable data object LanguageSelection : Route()
    @Serializable data object PersonaSelection : Route()
    @Serializable data object FieldSelection : Route()
    @Serializable data object WorkSchedule : Route()
    @Serializable data object DataConsent : Route()
    @Serializable data object Home : Route()
    @Serializable data object Settings : Route()
    @Serializable data object SettingsPersonaSelection : Route()
    @Serializable data class SettingsFieldSelection(val fromPersonaChange: Boolean = false) : Route()
    @Serializable data object SettingsWorkSchedule : Route()
}
