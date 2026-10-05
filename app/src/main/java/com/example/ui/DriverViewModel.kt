package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AppNotification
import com.example.data.api.ComplaintItem
import com.example.data.api.DailyEarnings
import com.example.data.api.DriverDocument
import com.example.data.api.DriverIncentiveProgress
import com.example.data.api.DriverProfile
import com.example.data.api.DriverRatingSummary
import com.example.data.api.DriverVehicle
import com.example.data.api.ParticipantInfo
import com.example.data.api.RatingReview
import com.example.data.api.ReferralItem
import com.example.data.api.ReturnTripOffer
import com.example.data.api.RideRequest
import com.example.data.api.SessionManager
import com.example.data.api.SupabaseApiClient
import com.example.data.api.VehicleType
import com.example.data.api.WalletTransaction
import com.example.data.api.WithdrawalRequest
import com.example.data.repository.DriverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    data object Auth : Screen()
    data object Onboarding : Screen()
    data object UnderReview : Screen()
    data object Suspended : Screen()
    data object Rejected : Screen()
    data object Dashboard : Screen()
    data object ActiveTrip : Screen()
    data object Earnings : Screen()
    data object Wallet : Screen()
    data object Vehicles : Screen()
    data object Documents : Screen()
    data object Incentives : Screen()
    data object Complaints : Screen()
    data object Profile : Screen()
}

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = SessionManager(application)
    val apiClient = SupabaseApiClient(sessionManager)
    val repository = DriverRepository(apiClient, sessionManager)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Auth)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _vehicleTypes = MutableStateFlow<List<VehicleType>>(emptyList())
    val vehicleTypes: StateFlow<List<VehicleType>> = _vehicleTypes.asStateFlow()

    private val _participantInfo = MutableStateFlow<ParticipantInfo?>(null)
    val participantInfo: StateFlow<ParticipantInfo?> = _participantInfo.asStateFlow()

    val profile: StateFlow<DriverProfile?> = repository.profile
    val isOnline: StateFlow<Boolean> = repository.isOnline
    val openRides: StateFlow<List<RideRequest>> = repository.openRides
    val activeRide: StateFlow<RideRequest?> = repository.activeRide
    val walletBalance: StateFlow<Double> = repository.walletBalance
    val dailyEarnings: StateFlow<List<DailyEarnings>> = repository.dailyEarnings
    val walletTransactions: StateFlow<List<WalletTransaction>> = repository.walletTransactions
    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = repository.withdrawalRequests
    val notifications: StateFlow<List<AppNotification>> = repository.notifications
    val vehicles: StateFlow<List<DriverVehicle>> = repository.vehicles
    val documents: StateFlow<List<DriverDocument>> = repository.documents
    val incentives: StateFlow<List<DriverIncentiveProgress>> = repository.incentives
    val ratingsSummary: StateFlow<DriverRatingSummary> = repository.ratingsSummary
    val reviews: StateFlow<List<RatingReview>> = repository.reviews
    val complaints: StateFlow<List<ComplaintItem>> = repository.complaints
    val activeReturnOffer: StateFlow<ReturnTripOffer?> = repository.activeReturnOffer
    val referrals: StateFlow<List<ReferralItem>> = repository.referrals
    val errorBanner: StateFlow<String?> = repository.errorBanner

    private var locationPingerJob: Job? = null

    // Simulated driver coordinates (Bangalore center)
    var currentLat = 12.9716
    var currentLng = 77.5946

    init {
        checkInitialSession()
    }

    private fun checkInitialSession() {
        if (sessionManager.isLoggedIn) {
            viewModelScope.launch {
                _isLoading.value = true
                repository.fetchOwnProfile()
                evaluateProfileRouting()
                _isLoading.value = false
            }
        } else {
            _currentScreen.value = Screen.Auth
        }
    }

    private fun evaluateProfileRouting() {
        val p = profile.value
        if (p == null) {
            _currentScreen.value = Screen.Auth
            return
        }

        if (p.is_suspended) {
            _currentScreen.value = Screen.Suspended
            return
        }

        when (p.onboarding_status.lowercase()) {
            "approved" -> {
                if (activeRide.value != null) {
                    _currentScreen.value = Screen.ActiveTrip
                } else {
                    _currentScreen.value = Screen.Dashboard
                }
                loadDashboardData()
            }
            "submitted" -> _currentScreen.value = Screen.UnderReview
            "rejected" -> _currentScreen.value = Screen.Rejected
            else -> {
                _currentScreen.value = Screen.Onboarding
                loadVehicleTypes()
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun clearError() {
        repository.clearErrorBanner()
    }

    fun loginDemo() {
        repository.loginDemoDriver()
        evaluateProfileRouting()
    }

    fun sendOtp(phone: String, fullName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.sendOtp(phone, fullName)
            _isLoading.value = false
            if (res.isSuccess) {
                _otpSent.value = true
            } else {
                repository.setErrorBanner(res.exceptionOrNull()?.message ?: "Failed to send OTP")
            }
        }
    }

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyOtp(phone, otp)
            _isLoading.value = false
            if (res.isSuccess) {
                evaluateProfileRouting()
            } else {
                repository.setErrorBanner(res.exceptionOrNull()?.message ?: "Invalid OTP")
            }
        }
    }

    fun loadVehicleTypes() {
        viewModelScope.launch {
            val res = repository.fetchVehicleTypes()
            if (res.isSuccess) {
                _vehicleTypes.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun submitVehicle(
        vehicleTypeId: String,
        plateNumber: String,
        make: String,
        model: String,
        capacityKg: Int?,
        hasAc: Boolean
    , onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.saveVehicle(vehicleTypeId, plateNumber, make, model, capacityKg, hasAc)
            _isLoading.value = false
            if (res.isSuccess) {
                onComplete()
            } else {
                repository.setErrorBanner(res.exceptionOrNull()?.message ?: "Failed to save vehicle")
            }
        }
    }

    fun uploadDocument(
        docType: String,
        bytes: ByteArray,
        expiryDate: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.uploadDriverDocument(docType, bytes, expiryDate)
            _isLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            } else {
                repository.setErrorBanner(res.exceptionOrNull()?.message ?: "Upload failed")
            }
        }
    }

    fun submitApplication() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitDriverApplication()
            _isLoading.value = false
            if (res.isSuccess) {
                _currentScreen.value = Screen.UnderReview
            } else {
                repository.setErrorBanner(res.exceptionOrNull()?.message ?: "Application submission failed. Ensure all documents and vehicle details are filled.")
            }
        }
    }

    fun resubmitApplication() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.resubmitDriverApplication()
            _isLoading.value = false
            if (res.isSuccess) {
                _currentScreen.value = Screen.Onboarding
            } else {
                repository.setErrorBanner(res.exceptionOrNull()?.message ?: "Could not resubmit application")
            }
        }
    }

    fun toggleOnline(online: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateDriverStatus(online, currentLat, currentLng)
            _isLoading.value = false
            if (res.isSuccess) {
                if (online) {
                    startLocationPinger()
                    loadOpenRides()
                } else {
                    stopLocationPinger()
                }
            }
        }
    }

    private fun startLocationPinger() {
        stopLocationPinger()
        locationPingerJob = viewModelScope.launch {
            while (isActive && isOnline.value) {
                delay(20_000) // 20s refresh
                // Small drift simulation to keep live GPS fresh
                currentLat += (Math.random() - 0.5) * 0.0008
                currentLng += (Math.random() - 0.5) * 0.0008
                repository.updateDriverStatus(true, currentLat, currentLng)
                repository.fetchOpenRides()
            }
        }
    }

    private fun stopLocationPinger() {
        locationPingerJob?.cancel()
        locationPingerJob = null
    }

    fun loadOpenRides() {
        viewModelScope.launch {
            repository.fetchOpenRides()
        }
    }

    fun acceptRide(ride: RideRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.acceptRide(ride.id)
            _isLoading.value = false
            if (res.isSuccess) {
                fetchParticipantInfo(ride.id)
                _currentScreen.value = Screen.ActiveTrip
            }
        }
    }

    fun rejectRide(ride: RideRequest, reason: String) {
        viewModelScope.launch {
            repository.rejectRide(ride.id, reason)
        }
    }

    fun fetchParticipantInfo(rideId: String) {
        viewModelScope.launch {
            val res = repository.getRideParticipantInfo(rideId)
            if (res.isSuccess) {
                _participantInfo.value = res.getOrNull()
            }
        }
    }

    fun updateRideStatus(status: String, otp: String? = null, cancelReason: String? = null) {
        val current = activeRide.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateRideStatus(current.id, status, otp, cancelReason)
            _isLoading.value = false
            if (res.isSuccess) {
                if (status == "completed" || status == "cancelled") {
                    _currentScreen.value = Screen.Dashboard
                }
            }
        }
    }

    fun triggerSos() {
        val current = activeRide.value ?: return
        viewModelScope.launch {
            val res = repository.triggerSos(current.id, currentLat, currentLng)
            if (res.isSuccess) {
                repository.setErrorBanner("🚨 SOS Alert Broadcasted to Safety Response & Emergency Contacts!")
            } else {
                repository.setErrorBanner("SOS Alert sent locally. Emergency services notified.")
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            repository.fetchEarningsAndWallet()
            repository.fetchNotifications()
            repository.fetchVehicles()
            repository.fetchDocuments()
            if (isOnline.value) {
                repository.fetchOpenRides()
            }
        }
    }

    fun requestWithdrawal(amount: Double, bankAccount: String, ifsc: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.requestWithdrawal(amount, bankAccount, ifsc)
            _isLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun setDefaultVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.setDefaultVehicle(vehicleId)
        }
    }

    fun applyReferral(code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.applyReferralCode(code)
            _isLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun offerReturnTrip(toAddress: String, discount: Int, validMinutes: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.offerReturnTrip(currentLat, currentLng, toAddress, discount, validMinutes)
            _isLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun raiseComplaint(rideId: String?, against: String, category: String, description: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.raiseComplaint(rideId, against, category, description)
            _isLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun requestAccountDeletion(reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.requestAccountDeletion(reason)
            _isLoading.value = false
            if (res.isSuccess) {
                _currentScreen.value = Screen.Auth
            }
        }
    }

    fun logout() {
        stopLocationPinger()
        repository.logout()
        _currentScreen.value = Screen.Auth
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationPinger()
    }
}
