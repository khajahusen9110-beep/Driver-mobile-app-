package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.needsNotificationPermission
import com.example.ui.screens.ActiveTripScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BlockedScreen
import com.example.ui.screens.ComplaintsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.EarningsScreen
import com.example.ui.screens.IncentivesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RejectedScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SuspendedScreen
import com.example.ui.screens.UnderReviewScreen
import com.example.ui.screens.VehiclesScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.RiderDriverTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DriverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiderDriverTheme {
                DriverAppRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}

@Composable
fun DriverAppRoot(viewModel: DriverViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { snackbar.showSnackbar(it) }
    }

    // Ride requests arrive as notifications, so ask once the driver can start receiving them.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val signedIn = screen !is Screen.Auth && screen !is Screen.Splash
    LaunchedEffect(signedIn) {
        if (signedIn && needsNotificationPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    BackHandler(enabled = screen in subScreens || screen in tabScreens) {
        viewModel.navigate(if (screen in subScreens) Screen.Profile else Screen.Dashboard)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = screen) {
            Screen.Splash -> SplashScreen(viewModel)
            Screen.Auth -> AuthScreen(viewModel)
            Screen.Onboarding -> OnboardingScreen(viewModel)
            Screen.UnderReview -> UnderReviewScreen(viewModel)
            Screen.Rejected -> RejectedScreen(viewModel)
            Screen.Suspended -> SuspendedScreen(viewModel)
            is Screen.Blocked -> BlockedScreen(viewModel, s.message)
            Screen.Dashboard -> DashboardScreen(viewModel)
            Screen.ActiveTrip -> ActiveTripScreen(viewModel)
            Screen.Earnings -> EarningsScreen(viewModel)
            Screen.Wallet -> WalletScreen(viewModel)
            Screen.Profile -> ProfileScreen(viewModel)
            Screen.Vehicles -> VehiclesScreen(viewModel)
            Screen.Documents -> DocumentsScreen(viewModel)
            Screen.Incentives -> IncentivesScreen(viewModel)
            Screen.Complaints -> ComplaintsScreen(viewModel)
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (screen in tabScreens || screen == Screen.Dashboard) 88.dp else 16.dp),
        )
    }
}

private val tabScreens = setOf<Screen>(Screen.Earnings, Screen.Wallet, Screen.Profile)
private val subScreens = setOf<Screen>(Screen.Vehicles, Screen.Documents, Screen.Incentives, Screen.Complaints)
