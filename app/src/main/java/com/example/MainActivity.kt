package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.DriverViewModel
import com.example.ui.Screen
import com.example.ui.screens.ActiveTripScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ComplaintsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentsScreen
import com.example.ui.screens.EarningsScreen
import com.example.ui.screens.IncentivesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RejectedScreen
import com.example.ui.screens.SuspendedScreen
import com.example.ui.screens.UnderReviewScreen
import com.example.ui.screens.VehiclesScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.DriverDarkBackground
import com.example.ui.theme.RiderDriverTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DriverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiderDriverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DriverDarkBackground
                ) {
                    RiderAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun RiderAppContent(viewModel: DriverViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle back button presses according to Android guidelines
    BackHandler(enabled = currentScreen != Screen.Auth && currentScreen != Screen.Dashboard) {
        when (currentScreen) {
            Screen.Vehicles, Screen.Documents, Screen.Incentives, Screen.Complaints -> {
                viewModel.navigateTo(Screen.Profile)
            }
            Screen.Earnings, Screen.Wallet, Screen.Profile, Screen.ActiveTrip -> {
                viewModel.navigateTo(Screen.Dashboard)
            }
            else -> {
                viewModel.navigateTo(Screen.Dashboard)
            }
        }
    }

    when (currentScreen) {
        is Screen.Auth -> AuthScreen(viewModel = viewModel)
        is Screen.Onboarding -> OnboardingScreen(viewModel = viewModel)
        is Screen.UnderReview -> UnderReviewScreen(viewModel = viewModel)
        is Screen.Suspended -> SuspendedScreen(viewModel = viewModel)
        is Screen.Rejected -> RejectedScreen(viewModel = viewModel)
        is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
        is Screen.ActiveTrip -> ActiveTripScreen(viewModel = viewModel)
        is Screen.Earnings -> EarningsScreen(viewModel = viewModel)
        is Screen.Wallet -> WalletScreen(viewModel = viewModel)
        is Screen.Vehicles -> VehiclesScreen(viewModel = viewModel)
        is Screen.Documents -> DocumentsScreen(viewModel = viewModel)
        is Screen.Incentives -> IncentivesScreen(viewModel = viewModel)
        is Screen.Complaints -> ComplaintsScreen(viewModel = viewModel)
        is Screen.Profile -> ProfileScreen(viewModel = viewModel)
    }
}
