package com.at.recallly.presentation.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.at.recallly.presentation.settings.SettingsUiEvent
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.at.recallly.domain.model.OnboardingStep
import com.at.recallly.presentation.auth.AuthUiEvent
import com.at.recallly.presentation.auth.AuthViewModel
import com.at.recallly.presentation.auth.LoginScreen
import com.at.recallly.presentation.auth.SignUpScreen
import com.at.recallly.presentation.consent.DataConsentScreen
import com.at.recallly.presentation.home.HomeScreen
import com.at.recallly.presentation.home.HomeViewModel
import com.at.recallly.presentation.onboarding.FieldSelectionScreen
import com.at.recallly.presentation.onboarding.LanguageSelectionScreen
import com.at.recallly.presentation.onboarding.OnboardingViewModel
import com.at.recallly.presentation.onboarding.PersonaSelectionScreen
import com.at.recallly.presentation.onboarding.WorkScheduleScreen
import com.at.recallly.presentation.settings.SettingsFieldSelectionScreen
import com.at.recallly.presentation.settings.SettingsPersonaSelectionScreen
import com.at.recallly.presentation.settings.SettingsScreen
import com.at.recallly.presentation.settings.SettingsViewModel
import com.at.recallly.presentation.settings.SettingsWorkScheduleScreen
import com.at.recallly.presentation.splash.SplashScreen
import com.at.recallly.core.util.LanguageManager
import com.at.recallly.data.local.datastore.PreferencesManager
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun RecalllyNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val onboardingViewModel: OnboardingViewModel = koinViewModel()
    val onboardingState by onboardingViewModel.uiState.collectAsState()
    var splashCompleted by rememberSaveable { mutableStateOf(false) }

    // Observe data consent state
    val consentAccepted by onboardingViewModel.dataConsentAccepted.collectAsState()

    // React to auth + onboarding + consent state after splash
    LaunchedEffect(authState.isLoggedIn, authState.isAuthResolved, splashCompleted, onboardingState.onboardingStep, onboardingState.isLoading, consentAccepted) {
        if (!splashCompleted) return@LaunchedEffect
        if (!authState.isAuthResolved) return@LaunchedEffect

        // If user is not logged in, go to Login immediately (don't wait for onboarding to load)
        if (!authState.isLoggedIn) {
            navController.navigate(Route.Login) {
                popUpTo(0) { inclusive = true }
            }
            return@LaunchedEffect
        }

        // User is logged in — wait for onboarding state to finish loading
        if (onboardingState.isLoading) return@LaunchedEffect

        val destination = when {
            onboardingState.onboardingStep == OnboardingStep.NOT_STARTED -> Route.LanguageSelection
            onboardingState.onboardingStep == OnboardingStep.PERSONA_COMPLETED -> Route.FieldSelection
            onboardingState.onboardingStep == OnboardingStep.FIELDS_COMPLETED -> Route.WorkSchedule
            onboardingState.onboardingStep == OnboardingStep.COMPLETED && !consentAccepted -> Route.DataConsent
            else -> Route.Home
        }
        navController.navigate(destination) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash
    ) {
        composable<Route.Splash> {
            SplashScreen(
                onSplashFinished = { splashCompleted = true }
            )
        }
        composable<Route.Login> {
            val context = LocalContext.current
            val prefsManager: PreferencesManager = koinInject()
            val scope = rememberCoroutineScope()
            LoginScreen(
                uiState = authState,
                onEvent = { authViewModel.onEvent(it) },
                onGoogleSignIn = { authViewModel.onEvent(AuthUiEvent.LoginWithGoogle, context) },
                onNavigateToSignUp = {
                    navController.navigate(Route.SignUp)
                },
                onLanguageChanged = { code ->
                    scope.launch { prefsManager.setAppLanguage(code) }
                    LanguageManager.applyLanguage(code)
                }
            )
        }
        composable<Route.SignUp> {
            val context = LocalContext.current
            val prefsManager: PreferencesManager = koinInject()
            val scope = rememberCoroutineScope()
            SignUpScreen(
                uiState = authState,
                onEvent = { authViewModel.onEvent(it) },
                onGoogleSignIn = { authViewModel.onEvent(AuthUiEvent.LoginWithGoogle, context) },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onLanguageChanged = { code ->
                    scope.launch { prefsManager.setAppLanguage(code) }
                    LanguageManager.applyLanguage(code)
                }
            )
        }
        composable<Route.LanguageSelection> {
            val prefsManager: PreferencesManager = koinInject()
            val scope = rememberCoroutineScope()
            LanguageSelectionScreen(
                currentLanguageCode = LanguageManager.getCurrentLanguageCode(),
                onLanguageSelected = { code ->
                    scope.launch { prefsManager.setAppLanguage(code) }
                    LanguageManager.applyLanguage(code)
                },
                onContinue = {
                    navController.navigate(Route.PersonaSelection)
                }
            )
        }
        composable<Route.PersonaSelection> {
            PersonaSelectionScreen(
                uiState = onboardingState,
                onEvent = { onboardingViewModel.onEvent(it) },
                onContinue = {
                    navController.navigate(Route.FieldSelection)
                }
            )
        }
        composable<Route.FieldSelection> {
            FieldSelectionScreen(
                uiState = onboardingState,
                onEvent = { onboardingViewModel.onEvent(it) },
                onContinue = {
                    navController.navigate(Route.WorkSchedule)
                }
            )
        }
        composable<Route.WorkSchedule> {
            WorkScheduleScreen(
                uiState = onboardingState,
                onEvent = { onboardingViewModel.onEvent(it) },
                onComplete = {
                    navController.navigate(Route.DataConsent) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.DataConsent> {
            DataConsentScreen(
                onAccept = { driveBackupEnabled ->
                    onboardingViewModel.saveDataConsent(driveBackupEnabled)
                    navController.navigate(Route.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.Home> {
            val homeViewModel: HomeViewModel = koinViewModel()
            val homeState by homeViewModel.uiState.collectAsState()
            HomeScreen(
                uiState = homeState,
                onEvent = { homeViewModel.onEvent(it) },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings)
                }
            )
        }
        composable<Route.Settings> {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            val driveAuthLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    settingsViewModel.onEvent(SettingsUiEvent.DriveAuthCompleted)
                }
            }

            LaunchedEffect(settingsState.needsDriveAuth) {
                if (settingsState.needsDriveAuth) {
                    driveAuthLauncher.launch(settingsViewModel.getAuthIntent())
                }
            }

            SettingsScreen(
                uiState = settingsState,
                onEvent = { settingsViewModel.onEvent(it) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPersonaSelection = {
                    navController.navigate(Route.SettingsPersonaSelection)
                },
                onNavigateToFieldSelection = {
                    navController.navigate(Route.SettingsFieldSelection())
                },
                onNavigateToSchedule = {
                    navController.navigate(Route.SettingsWorkSchedule)
                },
                onLogout = {
                    settingsViewModel.logout()
                }
            )
        }
        composable<Route.SettingsPersonaSelection> {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            SettingsPersonaSelectionScreen(
                uiState = settingsState,
                onEvent = { settingsViewModel.onEvent(it) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFieldSelection = {
                    navController.navigate(Route.SettingsFieldSelection(fromPersonaChange = true)) {
                        popUpTo(Route.Settings)
                    }
                }
            )
        }
        composable<Route.SettingsFieldSelection> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.SettingsFieldSelection>()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                if (route.fromPersonaChange) {
                    settingsViewModel.loadFieldsForNewPersona()
                } else {
                    settingsViewModel.loadFieldsForCurrentPersona()
                }
            }
            SettingsFieldSelectionScreen(
                uiState = settingsState,
                onEvent = { settingsViewModel.onEvent(it) },
                fromPersonaChange = route.fromPersonaChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Route.SettingsWorkSchedule> {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                settingsViewModel.loadCurrentSchedule()
            }
            SettingsWorkScheduleScreen(
                uiState = settingsState,
                onEvent = { settingsViewModel.onEvent(it) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
