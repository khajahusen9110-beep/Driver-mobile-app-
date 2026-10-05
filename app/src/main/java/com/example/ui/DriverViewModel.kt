package com.example.ui

import android.app.Application
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DriverApp
import com.example.data.ApiException
import com.example.data.AppNotification
import com.example.data.Complaint
import com.example.data.DocType
import com.example.data.DriverDocument
import com.example.data.DriverStatus
import com.example.data.EarningsDay
import com.example.data.FileReader
import com.example.data.Incentive
import com.example.data.OnboardingStatus
import com.example.data.Profile
import com.example.data.Rating
import com.example.data.RatingSummary
import com.example.data.ReturnTripOffer
import com.example.data.Ride
import com.example.data.RideParticipants
import com.example.data.RideStatus
import com.example.data.SessionExpiredException
import com.example.data.Validators
import com.example.data.Vehicle
import com.example.data.VehicleType
import com.example.data.LiveTable
import com.example.data.WalletTransaction
import com.example.data.Withdrawal
import com.example.location.LocationProvider
import com.example.location.OnlineLocationService
import com.example.push.PushMessagingService
import com.example.push.RideAlert
import com.example.ui.components.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

sealed interface Screen {
    data object Splash : Screen
    data object Auth : Screen
    data object Onboarding : Screen
    data object UnderReview : Screen
    data object Rejected : Screen
    data object Suspended : Screen
    data class Blocked(val message: String) : Screen
    data object Dashboard : Screen
    data object ActiveTrip : Screen
    data object Earnings : Screen
    data object Wallet : Screen
    data object Profile : Screen
    data object Vehicles : Screen
    data object Documents : Screen
    data object Incentives : Screen
    data object Complaints : Screen
}

data class AuthState(
    val otpSentTo: String? = null,
    val resendAvailableAt: Long = 0L,
)

data class DashboardState(
    val status: DriverStatus? = null,
    val requests: List<Ride> = emptyList(),
    val today: EarningsDay? = null,
    val returnOffer: ReturnTripOffer? = null,
    val goingOnline: Boolean = false,
)

data class TripState(
    val ride: Ride? = null,
    val participants: RideParticipants? = null,
    /** Set after the driver completes a trip, for the fare summary and customer rating. */
    val finished: Ride? = null,
)

data class EarningsState(
    val days: List<EarningsDay> = emptyList(),
    val history: List<Ride> = emptyList(),
    val ratingSummary: RatingSummary? = null,
    val ratings: List<Rating> = emptyList(),
)

data class WalletState(
    val balance: Double = 0.0,
    val transactions: List<WalletTransaction> = emptyList(),
    val withdrawals: List<Withdrawal> = emptyList(),
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DriverApp
    private val repo = app.repository

    private val _screen = MutableStateFlow<Screen>(if (repo.isLoggedIn) Screen.Splash else Screen.Auth)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    /** Label of the action in progress, so buttons can show a spinner and ignore double taps. */
    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Shown on the splash screen when the first load fails, with a retry button. */
    private val _startupError = MutableStateFlow<String?>(null)
    val startupError: StateFlow<String?> = _startupError.asStateFlow()

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    private val _auth = MutableStateFlow(AuthState())
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    private val _vehicleTypes = MutableStateFlow<List<VehicleType>>(emptyList())
    val vehicleTypes: StateFlow<List<VehicleType>> = _vehicleTypes.asStateFlow()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _documents = MutableStateFlow<List<DriverDocument>>(emptyList())
    val documents: StateFlow<List<DriverDocument>> = _documents.asStateFlow()

    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private val _trip = MutableStateFlow(TripState())
    val trip: StateFlow<TripState> = _trip.asStateFlow()

    private val _earnings = MutableStateFlow(EarningsState())
    val earnings: StateFlow<EarningsState> = _earnings.asStateFlow()

    private val _wallet = MutableStateFlow(WalletState())
    val wallet: StateFlow<WalletState> = _wallet.asStateFlow()

    private val _incentives = MutableStateFlow<List<Incentive>>(emptyList())
    val incentives: StateFlow<List<Incentive>> = _incentives.asStateFlow()

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _selectedVehicleId = MutableStateFlow(repo.selectedVehicleId)
    val selectedVehicleId: StateFlow<String?> = _selectedVehicleId.asStateFlow()

    private val _rideAlertSound = MutableStateFlow(app.session.rideAlertSound)
    val rideAlertSound: StateFlow<Boolean> = _rideAlertSound.asStateFlow()

    fun setRideAlertSound(enabled: Boolean) {
        app.session.rideAlertSound = enabled
        _rideAlertSound.value = enabled
    }

    /** Called when the app comes to the front, e.g. after accepting from the full-screen alert. */
    fun onAppResumed() {
        if (_profile.value?.onboardingStatus == OnboardingStatus.APPROVED) launchQuiet { onRidesChanged() }
    }

    val serviceRunning = OnlineLocationService.running
    val lastLocation = LocationProvider.lastLocation

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            repo.sessionExpired.collect {
                resetToLogin()
                messages.send("Your session has expired. Please log in again.")
            }
        }
        viewModelScope.launch {
            app.realtime.changes.collect { table ->
                when (table) {
                    LiveTable.RIDES -> onRidesChanged()
                    LiveTable.NOTIFICATIONS -> onNotification()
                }
            }
        }
        viewModelScope.launch {
            OnlineLocationService.stoppedReason.collect { reason ->
                if (reason != null) {
                    messages.send("You were taken offline: $reason")
                    OnlineLocationService.clearStoppedReason()
                    refreshStatus()
                }
            }
        }
        if (repo.isLoggedIn) bootstrap()
    }

    // ---------- Startup and routing ----------

    fun bootstrap() {
        viewModelScope.launch {
            _startupError.value = null
            _screen.value = Screen.Splash
            try {
                routeFor(repo.profile())
            } catch (e: SessionExpiredException) {
                resetToLogin()
            } catch (e: Exception) {
                _startupError.value = e.userMessage()
            }
        }
    }

    private suspend fun routeFor(profile: Profile) {
        _profile.value = profile
        val target = when {
            !profile.isDriver -> Screen.Blocked(
                "This number is registered as a customer account. Use the GoRide app to book rides, or sign up as a driver with a different number."
            )
            profile.isDeleted -> Screen.Blocked("This account has been deleted.")
            !profile.isActive -> Screen.Blocked("This account is disabled. Please contact support.")
            profile.isCurrentlySuspended() -> Screen.Suspended
            profile.onboardingStatus == OnboardingStatus.INCOMPLETE -> Screen.Onboarding
            profile.onboardingStatus == OnboardingStatus.SUBMITTED -> Screen.UnderReview
            profile.onboardingStatus == OnboardingStatus.REJECTED -> Screen.Rejected
            else -> null
        }
        app.realtime.start(profile.id)
        viewModelScope.launch { PushMessagingService.register(app) }
        if (target != null) {
            if (serviceRunning.value) OnlineLocationService.stop(app)
            _screen.value = target
            if (target == Screen.Onboarding || target == Screen.Rejected) loadOnboarding()
            loadNotifications()
            return
        }
        loadVehicles()
        val active = runCatching { repo.activeRide() }.getOrNull()
        if (active != null) {
            openTrip(active)
        } else {
            _screen.value = Screen.Dashboard
        }
        refreshDashboard()
        resumeOnlineIfNeeded()
        loadNotifications()
        startPolling()
    }

    fun navigate(target: Screen) {
        _screen.value = target
        when (target) {
            Screen.Dashboard -> launchQuiet { refreshDashboard() }
            Screen.Earnings -> loadEarnings()
            Screen.Wallet -> loadWallet()
            Screen.Vehicles -> launchQuiet { loadVehicles(); loadVehicleTypes() }
            Screen.Documents -> launchQuiet { _documents.value = repo.documents() }
            Screen.Incentives -> loadIncentives()
            Screen.Complaints -> loadComplaints()
            Screen.Profile -> launchQuiet { _earnings.update { it.copy(ratingSummary = repo.ratingSummary()) } }
            else -> Unit
        }
    }

    /** Re-reads the profile, for review / suspension screens and pull-to-refresh. */
    fun refreshProfile() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                routeFor(repo.profile())
            } catch (e: Exception) {
                report(e)
            } finally {
                _refreshing.value = false
            }
        }
    }

    // ---------- Auth ----------

    fun sendOtp(phoneInput: String, name: String?) {
        val phone = Validators.normalizePhone(phoneInput)
        if (phone == null) {
            toast("Enter a valid 10-digit mobile number")
            return
        }
        action("Sending code") {
            repo.sendOtp(phone, name)
            _auth.value = AuthState(otpSentTo = phone, resendAvailableAt = System.currentTimeMillis() + 30_000)
        }
    }

    fun resendOtp() {
        val phone = _auth.value.otpSentTo ?: return
        if (System.currentTimeMillis() < _auth.value.resendAvailableAt) return
        action("Sending code") {
            repo.sendOtp(phone, null)
            _auth.update { it.copy(resendAvailableAt = System.currentTimeMillis() + 30_000) }
            toast("A new code has been sent")
        }
    }

    fun changeNumber() {
        _auth.value = AuthState()
    }

    fun verifyOtp(code: String) {
        val phone = _auth.value.otpSentTo ?: return
        if (!Validators.isValidOtp(code)) {
            toast("Enter the 6-digit code from the SMS")
            return
        }
        action("Verifying") {
            repo.verifyOtp(phone, code)
            _auth.value = AuthState()
            routeFor(repo.profile())
        }
    }

    fun logout() {
        action("Logging out") {
            goOfflineQuietly()
            PushMessagingService.unregister(app)
            repo.signOut()
            resetToLogin()
        }
    }

    private suspend fun resetToLogin() {
        pollJob?.cancel()
        app.realtime.stop()
        if (serviceRunning.value) OnlineLocationService.stop(app)
        _profile.value = null
        _dashboard.value = DashboardState()
        _trip.value = TripState()
        _earnings.value = EarningsState()
        _wallet.value = WalletState()
        _vehicles.value = emptyList()
        _documents.value = emptyList()
        _notifications.value = emptyList()
        _auth.value = AuthState()
        _screen.value = Screen.Auth
    }

    // ---------- Onboarding and review ----------

    fun loadOnboarding() = launchQuiet {
        loadVehicleTypes()
        loadVehicles()
        _documents.value = repo.documents()
    }

    fun saveName(name: String, onDone: () -> Unit = {}) {
        if (name.trim().length < 3) {
            toast("Enter your full name as on your driving licence")
            return
        }
        action("Saving") {
            repo.updateName(name)
            _profile.value = repo.profile()
            onDone()
        }
    }

    fun addVehicle(
        typeId: String?, make: String, model: String, plate: String,
        capacity: String, capacityKg: String, hasAc: Boolean, onDone: () -> Unit,
    ) {
        val type = _vehicleTypes.value.firstOrNull { it.id == typeId }
        val seats = capacity.toIntOrNull()
        val kg = capacityKg.toDoubleOrNull()
        val error = when {
            type == null -> "Choose a vehicle type"
            make.isBlank() || model.isBlank() -> "Enter the vehicle make and model"
            !Validators.isValidPlate(plate) -> "Enter a valid registration number, for example KA01AB1234"
            !type.isGoods && (seats == null || seats !in 1..60) -> "Enter the number of passenger seats"
            type.isGoods && (kg == null || kg <= 0) -> "Enter the load capacity in kg"
            else -> null
        }
        if (error != null) {
            toast(error)
            return
        }
        action("Saving vehicle") {
            repo.addVehicle(
                typeId = type!!.id, make = make, model = model, plate = plate,
                capacity = seats ?: 1, capacityKg = kg, hasAc = hasAc,
                makeDefault = _vehicles.value.isEmpty(),
            )
            loadVehicles()
            toast("Vehicle added. It will be verified with your documents.")
            onDone()
        }
    }

    fun setDefaultVehicle(vehicle: Vehicle) = action("Updating") {
        repo.setDefaultVehicle(vehicle.id)
        if (vehicle.isVerified) setSelectedVehicle(vehicle.id)
        loadVehicles()
    }

    fun uploadDocument(type: DocType, uri: Uri, expiry: LocalDate?) {
        if (type.hasExpiry && expiry != null && !expiry.isAfter(LocalDate.now())) {
            toast("This document has already expired. Upload a valid one.")
            return
        }
        action("Uploading ${type.label}") {
            val file = FileReader.read(app, uri)
            repo.uploadDocument(type, file, expiry)
            _documents.value = repo.documents()
            toast("${type.label} uploaded")
        }
    }

    fun openDocument(document: DriverDocument, open: (String) -> Unit) = action("Opening") {
        open(repo.documentUrl(document.filePath))
    }

    fun submitApplication() = action("Submitting") {
        repo.submitApplication()
        routeFor(repo.profile())
    }

    fun resubmitApplication() = action("Reopening application") {
        repo.resubmitApplication()
        routeFor(repo.profile())
    }

    private suspend fun loadVehicleTypes() {
        if (_vehicleTypes.value.isEmpty()) _vehicleTypes.value = repo.vehicleTypes()
    }

    private suspend fun loadVehicles() {
        val list = repo.vehicles()
        _vehicles.value = list
        val selected = repo.selectedVehicleId
        if (list.none { it.id == selected && it.isVerified }) {
            setSelectedVehicle((list.firstOrNull { it.isDefault && it.isVerified } ?: list.firstOrNull { it.isVerified })?.id)
        }
    }

    private fun setSelectedVehicle(id: String?) {
        repo.selectedVehicleId = id
        _selectedVehicleId.value = id
    }

    // ---------- Online / offline ----------

    fun selectVehicle(vehicle: Vehicle) {
        if (!vehicle.isVerified) {
            toast("This vehicle is waiting for verification")
            return
        }
        setSelectedVehicle(vehicle.id)
        if (_dashboard.value.status?.isOnline == true) {
            action("Switching vehicle") {
                val location = LocationProvider.current(app)
                _dashboard.update { it.copy(status = repo.updateStatus(true, location?.latitude, location?.longitude, vehicle.id)) }
                OnlineLocationService.start(app, vehicle.id)
            }
        }
    }

    /** Call after location permission is granted. */
    fun goOnline() {
        val vehicleId = repo.selectedVehicleId
        if (vehicleId == null) {
            toast(
                if (_vehicles.value.isEmpty()) "Add a vehicle first"
                else "Your vehicle is not verified yet. You can go online once it is approved."
            )
            return
        }
        if (!LocationProvider.hasPermission(app)) {
            toast("Allow location access to go online")
            return
        }
        viewModelScope.launch {
            _dashboard.update { it.copy(goingOnline = true) }
            try {
                val location = LocationProvider.current(app)
                    ?: throw ApiException("Could not get your location. Turn on GPS and try again.")
                val status = repo.updateStatus(true, location.latitude, location.longitude, vehicleId)
                _dashboard.update { it.copy(status = status) }
                OnlineLocationService.start(app, vehicleId)
                refreshRequests()
                startPolling()
            } catch (e: Exception) {
                report(e)
            } finally {
                _dashboard.update { it.copy(goingOnline = false) }
            }
        }
    }

    fun goOffline() = action("Going offline") {
        goOfflineQuietly(throwErrors = true)
    }

    private suspend fun goOfflineQuietly(throwErrors: Boolean = false) {
        OnlineLocationService.stop(app)
        _dashboard.value.requests.forEach { RideAlert.cancel(app, it.id) }
        val location = LocationProvider.lastLocation.value
        try {
            val status = repo.updateStatus(false, location?.latitude, location?.longitude, repo.selectedVehicleId)
            _dashboard.update { it.copy(status = status, requests = emptyList()) }
        } catch (e: Exception) {
            if (throwErrors) throw e
        }
    }

    private suspend fun refreshStatus() {
        runCatching { repo.driverStatus() }.getOrNull()?.let { s -> _dashboard.update { it.copy(status = s) } }
    }

    /** After the app was killed while online, restart location sharing (or mark offline if it can't). */
    private suspend fun resumeOnlineIfNeeded() {
        val status = _dashboard.value.status ?: return
        if (!status.isOnline || serviceRunning.value) return
        if (LocationProvider.hasPermission(app) && repo.selectedVehicleId != null) {
            OnlineLocationService.start(app, repo.selectedVehicleId)
        } else {
            goOfflineQuietly()
        }
    }

    // ---------- Dashboard ----------

    fun refreshDashboardNow() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                refreshDashboard(throwErrors = true)
            } catch (e: Exception) {
                report(e)
            } finally {
                _refreshing.value = false
            }
        }
    }

    private suspend fun refreshDashboard(throwErrors: Boolean = false) {
        try {
            val status = repo.driverStatus()
            val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
            val todayEarnings = repo.earnings(1).firstOrNull { it.day == today }
            val offer = repo.activeReturnOffer()
            _dashboard.update { it.copy(status = status, today = todayEarnings, returnOffer = offer) }
            if (status?.isOnline == true) refreshRequests() else _dashboard.update { it.copy(requests = emptyList()) }
        } catch (e: Exception) {
            if (throwErrors) throw e
        }
    }

    private suspend fun refreshRequests() {
        val requests = runCatching { repo.openRequests() }.getOrNull() ?: return
        val previous = _dashboard.value.requests.map { it.id }.toSet()
        _dashboard.update { it.copy(requests = requests) }
        val currentIds = requests.map { it.id }.toSet()
        // Requests that went away (taken by another driver, cancelled) stop ringing.
        previous.filterNot { it in currentIds }.forEach { RideAlert.cancel(app, it) }
        requests.filter { it.id !in previous && isFresh(it) }.forEach { ride ->
            RideAlert.show(
                app, ride.id,
                "New order · ${Format.money(ride.fare)}",
                listOfNotNull(ride.pickupAddress, ride.dropAddress).joinToString(" → ").ifBlank { "Tap to view" },
            )
        }
    }

    private fun isFresh(ride: Ride): Boolean {
        val at = ride.requestedAt ?: return true
        return ride.isScheduled || Duration.between(at, Instant.now()).toMinutes() < 10
    }

    fun acceptRide(ride: Ride) = action("Accepting") {
        RideAlert.cancel(app, ride.id)
        try {
            val accepted = repo.acceptRide(ride.id)
            openTrip(accepted)
        } catch (e: ApiException) {
            refreshRequests()
            throw e
        }
    }

    fun skipRide(ride: Ride) {
        RideAlert.cancel(app, ride.id)
        _dashboard.update { s -> s.copy(requests = s.requests.filterNot { it.id == ride.id }) }
        launchQuiet { repo.rejectRide(ride.id, "Skipped by driver") }
    }

    fun offerReturnTrip(destination: String, discountPercent: Int, validHours: Int, onDone: () -> Unit) {
        if (destination.isBlank()) {
            toast("Enter the city you are heading back to")
            return
        }
        action("Posting return trip") {
            val (lat, lng, label) = geocode(destination)
                ?: throw ApiException("Could not find \"$destination\". Try a city or area name.")
            repo.offerReturnTrip(lat, lng, label, discountPercent, validHours * 60)
            _dashboard.update { it.copy(returnOffer = repo.activeReturnOffer()) }
            toast("Return trip posted. Customers going that way can book it.")
            onDone()
        }
    }

    fun cancelReturnOffer() {
        val offer = _dashboard.value.returnOffer ?: return
        action("Cancelling") {
            repo.cancelReturnOffer(offer.id)
            _dashboard.update { it.copy(returnOffer = null) }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocode(query: String): Triple<Double, Double, String>? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val result = runCatching { Geocoder(app, Locale.forLanguageTag("en-IN")).getFromLocationName("$query, India", 1) }
            .getOrNull()?.firstOrNull() ?: return@withContext null
        val label = listOfNotNull(result.locality ?: result.subAdminArea, result.adminArea).distinct().joinToString(", ")
            .ifBlank { query.trim() }
        Triple(result.latitude, result.longitude, label)
    }

    // ---------- Active trip ----------

    private suspend fun openTrip(ride: Ride) {
        _trip.update { it.copy(ride = ride, participants = if (it.ride?.id == ride.id) it.participants else null) }
        _screen.value = Screen.ActiveTrip
        startPolling()
        runCatching { repo.participants(ride.id) }.getOrNull()?.let { p -> _trip.update { it.copy(participants = p) } }
    }

    fun markArrived() = rideAction("Updating", RideStatus.ARRIVED)

    fun startTrip(otp: String) {
        if (otp.length != 4 || !otp.all { it.isDigit() }) {
            toast("Ask the customer for the 4-digit ride code")
            return
        }
        rideAction("Starting trip", RideStatus.ONGOING, otp = otp)
    }

    fun completeTrip() = rideAction("Completing trip", RideStatus.COMPLETED)

    fun cancelTrip(reason: String) {
        if (reason.isBlank()) {
            toast("Choose a reason")
            return
        }
        rideAction("Cancelling", RideStatus.CANCELLED, reason = reason)
    }

    private fun rideAction(label: String, status: RideStatus, reason: String? = null, otp: String? = null) {
        val ride = _trip.value.ride ?: return
        action(label) {
            val updated = repo.updateRideStatus(ride.id, status, reason, otp)
            when {
                status == RideStatus.COMPLETED -> {
                    _trip.value = TripState(finished = updated)
                    _screen.value = Screen.Dashboard
                    refreshDashboard()
                }
                status == RideStatus.CANCELLED -> {
                    _trip.value = TripState()
                    _screen.value = Screen.Dashboard
                    toast("Trip cancelled. It has been offered to other drivers.")
                    refreshDashboard()
                }
                else -> _trip.update { it.copy(ride = updated) }
            }
        }
    }

    fun rateCustomer(stars: Int, comment: String) {
        val ride = _trip.value.finished ?: return
        action("Sending rating") {
            repo.rateCustomer(ride.id, ride.customerId, stars, comment)
            _trip.update { it.copy(finished = null) }
            toast("Thanks for the feedback")
        }
    }

    fun dismissTripSummary() {
        _trip.update { it.copy(finished = null) }
    }

    fun triggerSos() = action("Sending SOS") {
        val location = LocationProvider.current(app) ?: LocationProvider.lastLocation.value
        repo.triggerSos(_trip.value.ride?.id, location?.latitude, location?.longitude)
        toast("SOS sent. Our safety team has been alerted.")
    }

    private suspend fun onRidesChanged() {
        if (_profile.value?.onboardingStatus != OnboardingStatus.APPROVED) return
        val current = _trip.value.ride
        val active = runCatching { repo.activeRide() }.getOrElse { return }
        when {
            active != null -> {
                if (current?.id != active.id || current.status != active.status) {
                    if (_screen.value == Screen.Dashboard || _screen.value == Screen.ActiveTrip) openTrip(active)
                    else _trip.update { it.copy(ride = active) }
                }
            }
            current != null -> {
                // The trip left the active states without this device acting: usually a customer cancellation.
                val latest = runCatching { repo.ride(current.id) }.getOrNull()
                _trip.value = TripState()
                if (_screen.value == Screen.ActiveTrip) _screen.value = Screen.Dashboard
                when (latest?.status) {
                    RideStatus.CANCELLED -> toast("The customer cancelled this trip.")
                    RideStatus.COMPLETED -> _trip.value = TripState(finished = latest)
                    else -> toast("This trip is no longer assigned to you.")
                }
            }
        }
        if (_dashboard.value.status?.isOnline == true) refreshRequests()
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                when (_screen.value) {
                    Screen.ActiveTrip -> onRidesChanged()
                    Screen.Dashboard -> if (_dashboard.value.status?.isOnline == true) onRidesChanged()
                    else -> Unit
                }
            }
        }
    }

    // ---------- Earnings, wallet ----------

    fun loadEarnings() = launchQuiet {
        val days = repo.earnings(30)
        val history = repo.tripHistory()
        val summary = repo.ratingSummary()
        val ratings = repo.recentRatings()
        _earnings.value = EarningsState(days, history, summary, ratings)
    }

    fun loadWallet() = launchQuiet {
        _wallet.value = WalletState(repo.walletBalance(), repo.walletTransactions(), repo.withdrawals())
    }

    fun requestWithdrawal(amountText: String, account: String, confirmAccount: String, ifsc: String, onDone: () -> Unit) {
        val amount = amountText.toDoubleOrNull()
        val error = when {
            amount == null || amount <= 0 -> "Enter an amount"
            amount > _wallet.value.balance -> "Amount is more than your wallet balance"
            !Validators.isValidAccountNumber(account) -> "Account number must be 9 to 18 digits"
            account.trim() != confirmAccount.trim() -> "Account numbers do not match"
            !Validators.isValidIfsc(ifsc) -> "Enter a valid IFSC code, for example HDFC0001234"
            else -> null
        }
        if (error != null) {
            toast(error)
            return
        }
        action("Requesting payout") {
            repo.requestWithdrawal(amount!!, account, ifsc)
            loadWallet()
            toast("Withdrawal requested. You will be notified when it is processed.")
            onDone()
        }
    }

    // ---------- Incentives, complaints, notifications, profile ----------

    fun loadIncentives() = launchQuiet { _incentives.value = repo.incentives() }

    fun loadComplaints() = launchQuiet {
        _complaints.value = repo.complaints()
        if (_earnings.value.history.isEmpty()) _earnings.update { it.copy(history = repo.tripHistory()) }
    }

    fun raiseComplaint(ride: Ride?, category: String, description: String, onDone: () -> Unit) {
        if (description.trim().length < 10) {
            toast("Describe the issue in a few words (at least 10 characters)")
            return
        }
        action("Sending") {
            repo.raiseComplaint(ride?.id, ride?.customerId, category, description)
            _complaints.value = repo.complaints()
            toast("Complaint submitted. Our team will respond soon.")
            onDone()
        }
    }

    fun loadNotifications() = launchQuiet { _notifications.value = repo.notifications() }

    fun markNotificationsRead() {
        if (_notifications.value.none { !it.isRead }) return
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        launchQuiet { repo.markNotificationsRead() }
    }

    private suspend fun onNotification() {
        _notifications.value = runCatching { repo.notifications() }.getOrDefault(_notifications.value)
        // Application decisions and suspensions arrive as notifications; re-route when one lands.
        when (_screen.value) {
            Screen.UnderReview, Screen.Rejected, Screen.Suspended, Screen.Onboarding ->
                runCatching { repo.profile() }.getOrNull()?.let { routeFor(it) }
            else -> Unit
        }
    }

    fun updateName(name: String, onDone: () -> Unit) = saveName(name, onDone)

    fun requestAccountDeletion(reason: String, onDone: () -> Unit) = action("Sending request") {
        repo.requestAccountDeletion(reason)
        toast("Deletion requested. Your account will be removed after review.")
        onDone()
    }

    // ---------- Helpers ----------

    private fun action(label: String, block: suspend () -> Unit) {
        if (_busy.value != null) return
        viewModelScope.launch {
            _busy.value = label
            try {
                block()
            } catch (e: Exception) {
                report(e)
            } finally {
                _busy.value = null
            }
        }
    }

    private fun launchQuiet(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            report(e)
        }
    }

    private suspend fun report(e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        if (e is SessionExpiredException) return // handled by the sessionExpired collector
        messages.send(e.userMessage())
    }

    private fun toast(message: String) {
        messages.trySend(message)
    }

    private fun Exception.userMessage(): String =
        if (this is ApiException) message ?: "Something went wrong" else "Something went wrong. Please try again."

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val POLL_MS = 15_000L
    }
}

/** Build-version helper kept here so screens don't import android.os.Build everywhere. */
val needsNotificationPermission: Boolean get() = Build.VERSION.SDK_INT >= 33
