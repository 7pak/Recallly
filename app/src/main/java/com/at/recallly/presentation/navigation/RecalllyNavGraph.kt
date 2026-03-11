package com.at.recallly.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.at.recallly.presentation.auth.AuthUiEvent
import com.at.recallly.presentation.auth.AuthViewModel
import com.at.recallly.presentation.auth.LoginScreen
import com.at.recallly.presentation.auth.SignUpScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecalllyNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Navigate based on auth state, clear backstack
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(Route.Home) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Route.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Login
    ) {
        composable<Route.Login> {
            val context = LocalContext.current
            LoginScreen(
                uiState = authState,
                onEvent = { authViewModel.onEvent(it) },
                onGoogleSignIn = { authViewModel.onEvent(AuthUiEvent.LoginWithGoogle, context) },
                onNavigateToSignUp = {
                    navController.navigate(Route.SignUp)
                }
            )
        }
        composable<Route.SignUp> {
            val context = LocalContext.current
            SignUpScreen(
                uiState = authState,
                onEvent = { authViewModel.onEvent(it) },
                onGoogleSignIn = { authViewModel.onEvent(AuthUiEvent.LoginWithGoogle, context) },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable<Route.Home> {
            // Temporary Home with logout — replace with real HomeScreen later
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Welcome to Recallly",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = { authViewModel.onEvent(AuthUiEvent.Logout) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
