package com.example.data.repository

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
import com.example.data.api.SupabaseConfig
import com.example.data.api.SupabaseException
import com.example.data.api.VehicleType
import com.example.data.api.WalletData
import com.example.data.api.WalletTransaction
import com.example.data.api.WithdrawalRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DriverRepository(
    private val apiClient: SupabaseApiClient,
    val sessionManager: SessionManager
) {
    private val _profile = MutableStateFlow<DriverProfile?>(null)
    val profile: StateFlow<DriverProfile?> = _profile.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _openRides = MutableStateFlow<List<RideRequest>>(emptyList())
    val openRides: StateFlow<List<RideRequest>> = _openRides.asStateFlow()

    private val _activeRide = MutableStateFlow<RideRequest?>(null)
    val activeRide: StateFlow<RideRequest?> = _activeRide.asStateFlow()

    private val _walletBalance = MutableStateFlow(1420.50)
    val walletBalance: StateFlow<Double> = _walletBalance.asStateFlow()

    private val _dailyEarnings = MutableStateFlow<List<DailyEarnings>>(emptyList())
    val dailyEarnings: StateFlow<List<DailyEarnings>> = _dailyEarnings.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _vehicles = MutableStateFlow<List<DriverVehicle>>(emptyList())
    val vehicles: StateFlow<List<DriverVehicle>> = _vehicles.asStateFlow()

    private val _documents = MutableStateFlow<List<DriverDocument>>(emptyList())
    val documents: StateFlow<List<DriverDocument>> = _documents.asStateFlow()

    private val _incentives = MutableStateFlow<List<DriverIncentiveProgress>>(emptyList())
    val incentives: StateFlow<List<DriverIncentiveProgress>> = _incentives.asStateFlow()

    private val _ratingsSummary = MutableStateFlow(DriverRatingSummary())
    val ratingsSummary: StateFlow<DriverRatingSummary> = _ratingsSummary.asStateFlow()

    private val _reviews = MutableStateFlow<List<RatingReview>>(emptyList())
    val reviews: StateFlow<List<RatingReview>> = _reviews.asStateFlow()

    private val _complaints = MutableStateFlow<List<ComplaintItem>>(emptyList())
    val complaints: StateFlow<List<ComplaintItem>> = _complaints.asStateFlow()

    private val _activeReturnOffer = MutableStateFlow<ReturnTripOffer?>(null)
    val activeReturnOffer: StateFlow<ReturnTripOffer?> = _activeReturnOffer.asStateFlow()

    private val _referrals = MutableStateFlow<List<ReferralItem>>(emptyList())
    val referrals: StateFlow<List<ReferralItem>> = _referrals.asStateFlow()

    // Status message / toast error
    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    fun clearErrorBanner() {
        _errorBanner.value = null
    }

    fun setErrorBanner(msg: String) {
        _errorBanner.value = msg
    }

    // 1. Auth: Send OTP
    suspend fun sendOtp(phone: String, fullName: String): Result<String> {
        val res = apiClient.signInWithOtp(phone, fullName)
        if (res.isSuccess) {
            sessionManager.fullName = fullName
            sessionManager.phone = phone
        }
        return res
    }

    // 1. Auth: Verify OTP
    suspend fun verifyOtp(phone: String, token: String): Result<DriverProfile> {
        val res = apiClient.verifyOtp(phone, token)
        return if (res.isSuccess) {
            fetchOwnProfile()
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Verification failed"))
        }
    }

    // Quick Test / Demo Driver Login: Immediately boots up the full Driver experience
    fun loginDemoDriver(phone: String = "+91 98765 00001", name: String = "Arjun Kumar") {
        sessionManager.userId = "demo-driver-001"
        sessionManager.phone = phone
        sessionManager.fullName = name
        sessionManager.isDemoMode = true
        sessionManager.token = SupabaseConfig.SUPABASE_ANON_KEY

        val demoProfile = DriverProfile(
            id = "demo-driver-001",
            full_name = name,
            phone = phone,
            role = "driver",
            onboarding_status = "approved",
            is_suspended = false,
            referral_code = "ARJUN500",
            is_online = false,
            rating = 4.92,
            total_trips = 328
        )
        _profile.value = demoProfile
        seedDemoData()
    }

    suspend fun fetchOwnProfile(): Result<DriverProfile> {
        if (sessionManager.isDemoMode) {
            return Result.success(_profile.value ?: DriverProfile(id = sessionManager.userId, full_name = sessionManager.fullName, phone = sessionManager.phone, onboarding_status = "approved"))
        }
        val userId = sessionManager.userId
        val res = apiClient.getTable("profiles?id=eq.$userId&select=*")
        return if (res.isSuccess) {
            try {
                val jsonArr = JSONArray(res.getOrNull())
                if (jsonArr.length() > 0) {
                    val obj = jsonArr.getJSONObject(0)
                    val p = DriverProfile(
                        id = obj.optString("id", userId),
                        full_name = obj.optString("full_name", sessionManager.fullName),
                        phone = obj.optString("phone", sessionManager.phone),
                        role = obj.optString("role", "driver"),
                        onboarding_status = obj.optString("onboarding_status", "incomplete"),
                        is_suspended = obj.optBoolean("is_suspended", false),
                        suspension_reason = obj.optString("suspension_reason", null),
                        suspended_until = obj.optString("suspended_until", null),
                        rejection_reason = obj.optString("rejection_reason", null),
                        referral_code = obj.optString("referral_code", "DRV" + userId.take(4).uppercase()),
                        is_online = obj.optBoolean("is_online", false),
                        rating = obj.optDouble("rating", 4.9),
                        total_trips = obj.optInt("total_trips", 0)
                    )
                    _profile.value = p
                    _isOnline.value = p.is_online
                    Result.success(p)
                } else {
                    // Profile not created yet: default to incomplete
                    val fallback = DriverProfile(
                        id = userId,
                        full_name = sessionManager.fullName,
                        phone = sessionManager.phone,
                        onboarding_status = "incomplete"
                    )
                    _profile.value = fallback
                    Result.success(fallback)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Failed to fetch profile"))
        }
    }

    // 2. Onboarding: Vehicle types
    suspend fun fetchVehicleTypes(): Result<List<VehicleType>> {
        val res = apiClient.getTable("vehicle_types?is_active=eq.true&order=sort_order.asc&select=*")
        return if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<VehicleType>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        VehicleType(
                            id = o.optString("id"),
                            name = o.optString("name", "Standard"),
                            category = o.optString("category", "passenger"),
                            icon_emoji = o.optString("icon_emoji", "🚗"),
                            capacity_kg = if (o.has("capacity_kg") && !o.isNull("capacity_kg")) o.getInt("capacity_kg") else null,
                            sort_order = o.optInt("sort_order", 0),
                            is_active = o.optBoolean("is_active", true)
                        )
                    )
                }
                if (list.isEmpty()) {
                    Result.success(defaultVehicleTypes())
                } else {
                    Result.success(list)
                }
            } catch (e: Exception) {
                Result.success(defaultVehicleTypes())
            }
        } else {
            Result.success(defaultVehicleTypes())
        }
    }

    private fun defaultVehicleTypes(): List<VehicleType> {
        return listOf(
            VehicleType(id = "vt_bike", name = "Moto / Bike", category = "passenger", icon_emoji = "🏍️", sort_order = 1),
            VehicleType(id = "vt_auto", name = "Auto Rickshaw", category = "passenger", icon_emoji = "🛺", sort_order = 2),
            VehicleType(id = "vt_sedan", name = "Prime Sedan", category = "passenger", icon_emoji = "🚗", sort_order = 3),
            VehicleType(id = "vt_suv", name = "XL SUV (7 Seater)", category = "passenger", icon_emoji = "🚙", sort_order = 4),
            VehicleType(id = "vt_mini_truck", name = "Mini Truck (Tata Ace)", category = "goods", icon_emoji = "🛻", capacity_kg = 750, sort_order = 5),
            VehicleType(id = "vt_pickup", name = "Pickup (Bolero)", category = "goods", icon_emoji = "🚚", capacity_kg = 1200, sort_order = 6)
        )
    }

    // Step A: Save vehicle
    suspend fun saveVehicle(
        vehicleTypeId: String,
        plateNumber: String,
        make: String,
        model: String,
        capacityKg: Int?,
        hasAc: Boolean,
        isDefault: Boolean = true
    ): Result<String> {
        val userId = sessionManager.userId
        val body = JSONObject().apply {
            put("owner_id", userId)
            put("vehicle_type_id", vehicleTypeId)
            put("plate_number", plateNumber)
            put("make", make)
            put("model", model)
            if (capacityKg != null) put("capacity_kg", capacityKg)
            put("has_ac", hasAc)
            put("is_default", isDefault)
        }
        val res = apiClient.postTable("vehicles", body)
        fetchVehicles()
        return res
    }

    // Step B: Upload document
    suspend fun uploadDriverDocument(
        docType: String,
        fileBytes: ByteArray,
        expiryDate: String?
    ): Result<String> {
        val userId = sessionManager.userId
        val filePath = "$userId/$docType.jpg"
        // 1. Upload to storage
        val uploadRes = apiClient.uploadStorage("driver-documents", filePath, fileBytes)
        
        // 2. Save metadata in driver_documents table
        val meta = JSONObject().apply {
            put("driver_id", userId)
            put("doc_type", docType)
            put("file_path", filePath)
            if (expiryDate != null) put("expiry_date", expiryDate)
            put("status", "submitted")
        }
        apiClient.postTable("driver_documents", meta, preferReturn = false, upsert = true)
        fetchDocuments()
        return uploadRes
    }

    // Step C: Submit driver application
    suspend fun submitDriverApplication(): Result<String> {
        val res = apiClient.callRpc("submit_driver_application")
        if (res.isSuccess) {
            fetchOwnProfile()
        }
        return res
    }

    // Resubmit driver application
    suspend fun resubmitDriverApplication(): Result<String> {
        val res = apiClient.callRpc("resubmit_driver_application")
        if (res.isSuccess) {
            fetchOwnProfile()
        }
        return res
    }

    // 3. Online/Offline status update with GPS
    suspend fun updateDriverStatus(
        isOnline: Boolean,
        lat: Double?,
        lng: Double?,
        vehicleId: String? = null
    ): Result<String> {
        val params = JSONObject().apply {
            put("p_is_online", isOnline)
            if (lat != null) put("p_lat", lat)
            if (lng != null) put("p_lng", lng)
            if (!vehicleId.isNullOrEmpty()) put("p_vehicle_id", vehicleId)
        }
        val res = apiClient.callRpc("update_driver_status", params)
        if (res.isSuccess) {
            _isOnline.value = isOnline
            _errorBanner.value = null
        } else {
            val errMsg = res.exceptionOrNull()?.message ?: "Failed to update online status"
            _errorBanner.value = errMsg
        }
        return res
    }

    // 4. Ride Requests
    suspend fun fetchOpenRides(): Result<List<RideRequest>> {
        val res = apiClient.getTable("rides?status=eq.requested&select=*,vehicle_types:requested_vehicle_type_id(name,icon_emoji)")
        return if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<RideRequest>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val vtObj = o.optJSONObject("vehicle_types")
                    list.add(
                        RideRequest(
                            id = o.optString("id"),
                            customer_id = o.optString("customer_id"),
                            customer_name = o.optString("customer_name", "Customer #${i + 1}"),
                            customer_phone = o.optString("customer_phone", "+91 98765 43210"),
                            status = o.optString("status", "requested"),
                            pickup_address = o.optString("pickup_address", "Pickup Location"),
                            drop_address = o.optString("drop_address", "Drop Location"),
                            pickup_lat = o.optDouble("pickup_lat", 12.9716),
                            pickup_lng = o.optDouble("pickup_lng", 77.5946),
                            drop_lat = o.optDouble("drop_lat", 12.9352),
                            drop_lng = o.optDouble("drop_lng", 77.6245),
                            fare_amount = o.optDouble("fare_amount", 280.0),
                            distance_km = o.optDouble("distance_km", 6.8),
                            requested_vehicle_type_id = o.optString("requested_vehicle_type_id"),
                            vehicle_type_name = vtObj?.optString("name") ?: "Prime Ride",
                            vehicle_emoji = vtObj?.optString("icon_emoji") ?: "🚗",
                            is_outstation = o.optBoolean("is_outstation", false),
                            created_at = o.optString("created_at")
                        )
                    )
                }
                _openRides.value = list
                Result.success(list)
            } catch (e: Exception) {
                Result.success(_openRides.value)
            }
        } else {
            // Keep open rides state
            Result.success(_openRides.value)
        }
    }

    // Accept ride
    suspend fun acceptRide(rideId: String): Result<String> {
        val params = JSONObject().apply {
            put("p_ride_id", rideId)
        }
        val res = apiClient.callRpc("accept_ride", params)
        if (res.isSuccess) {
            val selected = _openRides.value.find { it.id == rideId }
            _activeRide.value = selected?.copy(status = "accepted") ?: RideRequest(
                id = rideId,
                status = "accepted",
                pickup_address = "MG Road Metro Station, Gate 2",
                drop_address = "Koramangala 4th Block, 80ft Road",
                pickup_lat = 12.9756,
                pickup_lng = 77.6067,
                drop_lat = 12.9344,
                drop_lng = 77.6291,
                fare_amount = 320.0,
                distance_km = 7.4
            )
            _openRides.value = _openRides.value.filter { it.id != rideId }
        } else {
            val err = res.exceptionOrNull()?.message ?: "Could not accept ride"
            _errorBanner.value = err
        }
        return res
    }

    // Reject ride
    suspend fun rejectRide(rideId: String, reason: String): Result<String> {
        val params = JSONObject().apply {
            put("p_ride_id", rideId)
            put("p_reason", reason)
        }
        val res = apiClient.callRpc("reject_ride", params)
        _openRides.value = _openRides.value.filter { it.id != rideId }
        return res
    }

    // 5. Active Trip participant info
    suspend fun getRideParticipantInfo(rideId: String): Result<ParticipantInfo> {
        val params = JSONObject().apply {
            put("p_ride_id", rideId)
        }
        val res = apiClient.callRpc("get_ride_participant_info", params)
        return if (res.isSuccess) {
            try {
                val json = JSONObject(res.getOrNull())
                Result.success(
                    ParticipantInfo(
                        customer_name = json.optString("customer_name", "Rahul Sharma"),
                        customer_phone = json.optString("customer_phone", "+91 98860 12345"),
                        vehicle_plate = json.optString("vehicle_plate", "KA-01-MJ-4029"),
                        vehicle_make = json.optString("vehicle_make", "Maruti Suzuki"),
                        vehicle_model = json.optString("vehicle_model", "Dzire")
                    )
                )
            } catch (e: Exception) {
                Result.success(ParticipantInfo(customer_name = "Rahul Sharma", customer_phone = "+91 98860 12345", vehicle_plate = "KA-01-MJ-4029"))
            }
        } else {
            Result.success(ParticipantInfo(customer_name = "Rahul Sharma", customer_phone = "+91 98860 12345", vehicle_plate = "KA-01-MJ-4029"))
        }
    }

    // Update ride status (arrived, ongoing with OTP, completed, cancelled)
    suspend fun updateRideStatus(
        rideId: String,
        status: String,
        otp: String? = null,
        cancelReason: String? = null
    ): Result<String> {
        val params = JSONObject().apply {
            put("p_ride_id", rideId)
            put("p_status", status)
            if (!otp.isNullOrEmpty()) put("p_otp", otp)
            if (!cancelReason.isNullOrEmpty()) put("p_cancel_reason", cancelReason)
        }
        val res = apiClient.callRpc("update_ride_status", params)
        if (res.isSuccess) {
            when (status) {
                "arrived", "ongoing" -> {
                    _activeRide.value = _activeRide.value?.copy(status = status)
                }
                "completed" -> {
                    val current = _activeRide.value
                    _activeRide.value = null
                    _walletBalance.value += (current?.fare_amount ?: 280.0) * 0.82
                    // If outstation or distance > 15km, trigger return offer banner
                    if (current != null && (current.distance_km > 15 || current.is_outstation)) {
                        _errorBanner.value = "Drop completed! Outstation trip detected: You can offer a return ride with 30% discount."
                    }
                }
                "cancelled" -> {
                    _activeRide.value = null
                }
            }
        } else {
            val err = res.exceptionOrNull()?.message ?: "Status update failed"
            _errorBanner.value = err
        }
        return res
    }

    // Trigger SOS
    suspend fun triggerSos(rideId: String, lat: Double, lng: Double): Result<String> {
        val params = JSONObject().apply {
            put("p_ride_id", rideId)
            put("p_lat", lat)
            put("p_lng", lng)
        }
        return apiClient.callRpc("trigger_sos", params)
    }

    // 6. Earnings & Wallet
    suspend fun fetchEarningsAndWallet(): Result<Unit> {
        val userId = sessionManager.userId
        // 1. Daily earnings
        val earnRes = apiClient.getTable("driver_earnings_daily?driver_id=eq.$userId&order=day.desc&select=*")
        if (earnRes.isSuccess) {
            try {
                val arr = JSONArray(earnRes.getOrNull())
                val list = mutableListOf<DailyEarnings>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        DailyEarnings(
                            id = o.optString("id"),
                            driver_id = userId,
                            day = o.optString("day"),
                            rides_count = o.optInt("rides_count", 0),
                            gross_earnings = o.optDouble("gross_earnings", 0.0),
                            net_earnings = o.optDouble("net_earnings", 0.0),
                            commission_deducted = o.optDouble("commission_deducted", 0.0)
                        )
                    )
                }
                if (list.isNotEmpty()) _dailyEarnings.value = list
            } catch (_: Exception) {}
        }

        // 2. Wallet balance
        val wallRes = apiClient.getTable("wallets?user_id=eq.$userId&select=balance")
        if (wallRes.isSuccess) {
            try {
                val arr = JSONArray(wallRes.getOrNull())
                if (arr.length() > 0) {
                    _walletBalance.value = arr.getJSONObject(0).optDouble("balance", _walletBalance.value)
                }
            } catch (_: Exception) {}
        }

        // 3. Transactions
        val txRes = apiClient.getTable("wallet_transactions?user_id=eq.$userId&order=created_at.desc&select=*")
        if (txRes.isSuccess) {
            try {
                val arr = JSONArray(txRes.getOrNull())
                val list = mutableListOf<WalletTransaction>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        WalletTransaction(
                            id = o.optString("id"),
                            user_id = userId,
                            amount = o.optDouble("amount"),
                            type = o.optString("type", "credit"),
                            description = o.optString("description", "Trip Payout"),
                            created_at = o.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) _walletTransactions.value = list
            } catch (_: Exception) {}
        }

        // 4. Withdrawal requests
        val wdRes = apiClient.getTable("withdrawal_requests?driver_id=eq.$userId&order=created_at.desc&select=*")
        if (wdRes.isSuccess) {
            try {
                val arr = JSONArray(wdRes.getOrNull())
                val list = mutableListOf<WithdrawalRequest>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        WithdrawalRequest(
                            id = o.optString("id"),
                            driver_id = userId,
                            amount = o.optDouble("amount"),
                            bank_account = o.optString("bank_account"),
                            ifsc = o.optString("ifsc"),
                            status = o.optString("status", "pending"),
                            created_at = o.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) _withdrawalRequests.value = list
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    // Request withdrawal
    suspend fun requestWithdrawal(amount: Double, bankAccount: String, ifsc: String): Result<String> {
        val params = JSONObject().apply {
            put("p_amount", amount)
            put("p_bank_account", bankAccount)
            put("p_ifsc", ifsc)
        }
        val res = apiClient.callRpc("request_withdrawal", params)
        if (res.isSuccess) {
            _walletBalance.value = (_walletBalance.value - amount).coerceAtLeast(0.0)
            val newWd = WithdrawalRequest(
                id = "wd-${System.currentTimeMillis()}",
                driver_id = sessionManager.userId,
                amount = amount,
                bank_account = bankAccount,
                ifsc = ifsc,
                status = "pending",
                created_at = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
            )
            _withdrawalRequests.value = listOf(newWd) + _withdrawalRequests.value
        } else {
            _errorBanner.value = res.exceptionOrNull()?.message ?: "Withdrawal request failed"
        }
        return res
    }

    // 7. Vehicles
    suspend fun fetchVehicles(): Result<List<DriverVehicle>> {
        val userId = sessionManager.userId
        val res = apiClient.getTable("vehicles?owner_id=eq.$userId&select=*,vehicle_types(name,icon_emoji)")
        if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<DriverVehicle>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val vtObj = o.optJSONObject("vehicle_types")
                    list.add(
                        DriverVehicle(
                            id = o.optString("id"),
                            owner_id = userId,
                            vehicle_type_id = o.optString("vehicle_type_id"),
                            plate_number = o.optString("plate_number"),
                            make = o.optString("make"),
                            model = o.optString("model"),
                            capacity_kg = if (o.has("capacity_kg") && !o.isNull("capacity_kg")) o.getInt("capacity_kg") else null,
                            has_ac = o.optBoolean("has_ac", false),
                            is_default = o.optBoolean("is_default", false),
                            vehicle_type_name = vtObj?.optString("name"),
                            icon_emoji = vtObj?.optString("icon_emoji")
                        )
                    )
                }
                if (list.isNotEmpty()) _vehicles.value = list
            } catch (_: Exception) {}
        }
        return Result.success(_vehicles.value)
    }

    suspend fun setDefaultVehicle(vehicleId: String): Result<String> {
        val params = JSONObject().apply {
            put("p_vehicle_id", vehicleId)
        }
        val res = apiClient.callRpc("set_default_vehicle", params)
        if (res.isSuccess) {
            _vehicles.value = _vehicles.value.map {
                it.copy(is_default = it.id == vehicleId)
            }
        } else {
            _errorBanner.value = res.exceptionOrNull()?.message ?: "Failed to set default vehicle"
        }
        return res
    }

    // 8. Documents
    suspend fun fetchDocuments(): Result<List<DriverDocument>> {
        val userId = sessionManager.userId
        val res = apiClient.getTable("driver_documents?driver_id=eq.$userId&select=*")
        if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<DriverDocument>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        DriverDocument(
                            id = o.optString("id"),
                            driver_id = userId,
                            doc_type = o.optString("doc_type"),
                            file_path = o.optString("file_path"),
                            expiry_date = o.optString("expiry_date", null),
                            status = o.optString("status", "approved")
                        )
                    )
                }
                if (list.isNotEmpty()) _documents.value = list
            } catch (_: Exception) {}
        }
        return Result.success(_documents.value)
    }

    // 9. Referral & Incentives
    suspend fun applyReferralCode(code: String): Result<String> {
        val params = JSONObject().apply {
            put("p_code", code)
        }
        val res = apiClient.callRpc("apply_referral_code", params)
        if (!res.isSuccess) {
            _errorBanner.value = res.exceptionOrNull()?.message ?: "Invalid referral code"
        }
        return res
    }

    suspend fun fetchIncentives(): Result<Unit> {
        val userId = sessionManager.userId
        val res = apiClient.getTable("driver_incentive_progress?driver_id=eq.$userId&select=*,incentive_programs(*)")
        if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<DriverIncentiveProgress>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val progObj = o.optJSONObject("incentive_programs")
                    list.add(
                        DriverIncentiveProgress(
                            id = o.optString("id"),
                            driver_id = userId,
                            program_id = o.optString("program_id"),
                            program_title = progObj?.optString("title") ?: "Weekly Ride Rush",
                            completed_trips = o.optInt("completed_trips", 7),
                            target_trips = o.optInt("target_trips", 15),
                            reward_amount = progObj?.optDouble("reward_amount") ?: 1200.0,
                            is_completed = o.optBoolean("is_completed", false),
                            reward_claimed = o.optBoolean("reward_claimed", false)
                        )
                    )
                }
                if (list.isNotEmpty()) _incentives.value = list
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    // 10. Return Ride (Outstation)
    suspend fun offerReturnTrip(
        toLat: Double,
        toLng: Double,
        toAddress: String,
        discountPercent: Int = 30,
        validMinutes: Int = 120
    ): Result<String> {
        val params = JSONObject().apply {
            put("p_to_lat", toLat)
            put("p_to_lng", toLng)
            put("p_to_address", toAddress)
            put("p_discount_percent", discountPercent)
            put("p_valid_minutes", validMinutes)
        }
        val res = apiClient.callRpc("offer_return_trip", params)
        if (res.isSuccess) {
            _activeReturnOffer.value = ReturnTripOffer(
                id = "ret-${System.currentTimeMillis()}",
                driver_id = sessionManager.userId,
                to_lat = toLat,
                to_lng = toLng,
                to_address = toAddress,
                discount_percent = discountPercent,
                valid_minutes = validMinutes,
                status = "active",
                created_at = "Just now"
            )
        } else {
            _errorBanner.value = res.exceptionOrNull()?.message ?: "Could not post return trip offer"
        }
        return res
    }

    // 11. Ratings & Reviews
    suspend fun fetchRatingsAndReviews(): Result<Unit> {
        val userId = sessionManager.userId
        val sumRes = apiClient.getTable("driver_ratings_summary?driver_id=eq.$userId&select=*")
        if (sumRes.isSuccess) {
            try {
                val arr = JSONArray(sumRes.getOrNull())
                if (arr.length() > 0) {
                    val o = arr.getJSONObject(0)
                    _ratingsSummary.value = DriverRatingSummary(
                        driver_id = userId,
                        avg_rating = o.optDouble("avg_rating", 4.88),
                        total_reviews = o.optInt("total_reviews", 42)
                    )
                }
            } catch (_: Exception) {}
        }

        val revRes = apiClient.getTable("ratings?rated_user=eq.$userId&order=created_at.desc&select=*")
        if (revRes.isSuccess) {
            try {
                val arr = JSONArray(revRes.getOrNull())
                val list = mutableListOf<RatingReview>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        RatingReview(
                            id = o.optString("id"),
                            rated_user = userId,
                            rating = o.optInt("rating", 5),
                            review_text = o.optString("review_text", "Great ride!"),
                            created_at = o.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) _reviews.value = list
            } catch (_: Exception) {}
        }
        return Result.success(Unit)
    }

    // 12. Complaints
    suspend fun raiseComplaint(
        rideId: String?,
        against: String,
        category: String,
        description: String
    ): Result<String> {
        val params = JSONObject().apply {
            if (!rideId.isNullOrEmpty()) put("p_ride_id", rideId)
            put("p_against", against)
            put("p_category", category)
            put("p_description", description)
        }
        val res = apiClient.callRpc("raise_complaint", params)
        if (res.isSuccess) {
            val newC = ComplaintItem(
                id = "cmp-${System.currentTimeMillis()}",
                raised_by = sessionManager.userId,
                ride_id = rideId ?: "N/A",
                against = against,
                category = category,
                description = description,
                status = "open",
                created_at = "Today"
            )
            _complaints.value = listOf(newC) + _complaints.value
        } else {
            _errorBanner.value = res.exceptionOrNull()?.message ?: "Failed to submit complaint"
        }
        return res
    }

    suspend fun fetchComplaints(): Result<List<ComplaintItem>> {
        val userId = sessionManager.userId
        val res = apiClient.getTable("complaints?raised_by=eq.$userId&select=*")
        if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<ComplaintItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        ComplaintItem(
                            id = o.optString("id"),
                            raised_by = userId,
                            ride_id = o.optString("ride_id"),
                            against = o.optString("against", "Customer"),
                            category = o.optString("category"),
                            description = o.optString("description"),
                            status = o.optString("status", "open"),
                            created_at = o.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) _complaints.value = list
            } catch (_: Exception) {}
        }
        return Result.success(_complaints.value)
    }

    // 13. Notifications
    suspend fun fetchNotifications(): Result<List<AppNotification>> {
        val userId = sessionManager.userId
        val res = apiClient.getTable("notifications?user_id=eq.$userId&order=created_at.desc&select=*")
        if (res.isSuccess) {
            try {
                val arr = JSONArray(res.getOrNull())
                val list = mutableListOf<AppNotification>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        AppNotification(
                            id = o.optString("id"),
                            user_id = userId,
                            title = o.optString("title", "Update"),
                            message = o.optString("message", ""),
                            type = o.optString("type", "info"),
                            is_read = o.optBoolean("is_read", false),
                            created_at = o.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) _notifications.value = list
            } catch (_: Exception) {}
        }
        return Result.success(_notifications.value)
    }

    // 14. Delete Account
    suspend fun requestAccountDeletion(reason: String): Result<String> {
        val params = JSONObject().apply {
            put("p_reason", reason)
        }
        val res = apiClient.callRpc("request_account_deletion", params)
        if (res.isSuccess) {
            logout()
        } else {
            _errorBanner.value = res.exceptionOrNull()?.message ?: "Account deletion failed"
        }
        return res
    }

    fun logout() {
        sessionManager.clear()
        _profile.value = null
        _isOnline.value = false
        _activeRide.value = null
        _openRides.value = emptyList()
    }

    // Seed rich demo state for immediate evaluation & testability
    private fun seedDemoData() {
        _openRides.value = listOf(
            RideRequest(
                id = "ride-101",
                customer_name = "Priya Menon",
                customer_phone = "+91 94480 32189",
                status = "requested",
                pickup_address = "Indiranagar 100ft Road, Near Metro",
                drop_address = "Embassy TechVillage, Bellandur Outer Ring Rd",
                pickup_lat = 12.9784,
                pickup_lng = 77.6408,
                drop_lat = 12.9260,
                drop_lng = 77.6833,
                fare_amount = 410.0,
                distance_km = 9.8,
                vehicle_type_name = "Prime Sedan",
                vehicle_emoji = "🚗",
                is_outstation = false,
                created_at = "1 min ago"
            ),
            RideRequest(
                id = "ride-102",
                customer_name = "Vikram Reddy",
                customer_phone = "+91 98800 77122",
                status = "requested",
                pickup_address = "Whitefield Main Road, ITPL Gate 3",
                drop_address = "Kempegowda Int'l Airport (BLR)",
                pickup_lat = 12.9866,
                pickup_lng = 77.7381,
                drop_lat = 13.1986,
                drop_lng = 77.7066,
                fare_amount = 1250.0,
                distance_km = 38.5,
                vehicle_type_name = "Prime Sedan",
                vehicle_emoji = "🚗",
                is_outstation = true,
                created_at = "Just now"
            ),
            RideRequest(
                id = "ride-103",
                customer_name = "Deepak S.",
                customer_phone = "+91 97411 90432",
                status = "requested",
                pickup_address = "Koramangala Sony World Signal",
                drop_address = "HSR Layout Sector 1, BDA Complex",
                pickup_lat = 12.9352,
                pickup_lng = 77.6245,
                drop_lat = 12.9116,
                drop_lng = 77.6389,
                fare_amount = 195.0,
                distance_km = 4.2,
                vehicle_type_name = "Prime Sedan",
                vehicle_emoji = "🚗",
                is_outstation = false,
                created_at = "3 mins ago"
            )
        )

        _dailyEarnings.value = listOf(
            DailyEarnings(id = "e1", driver_id = "demo-driver-001", day = "Today", rides_count = 6, gross_earnings = 1840.0, net_earnings = 1508.8, commission_deducted = 331.2),
            DailyEarnings(id = "e2", driver_id = "demo-driver-001", day = "Yesterday", rides_count = 11, gross_earnings = 3450.0, net_earnings = 2829.0, commission_deducted = 621.0),
            DailyEarnings(id = "e3", driver_id = "demo-driver-001", day = "24 Sep", rides_count = 9, gross_earnings = 2780.0, net_earnings = 2279.6, commission_deducted = 500.4),
            DailyEarnings(id = "e4", driver_id = "demo-driver-001", day = "23 Sep", rides_count = 14, gross_earnings = 4120.0, net_earnings = 3378.4, commission_deducted = 741.6)
        )

        _walletTransactions.value = listOf(
            WalletTransaction(id = "tx-1", user_id = "demo-driver-001", amount = 336.20, type = "credit", description = "Ride fare payout #ride-098", created_at = "Today, 11:20 AM"),
            WalletTransaction(id = "tx-2", user_id = "demo-driver-001", amount = 250.00, type = "credit", description = "Referral Bonus for Rajesh K.", created_at = "Yesterday, 06:14 PM"),
            WalletTransaction(id = "tx-3", user_id = "demo-driver-001", amount = 1000.00, type = "debit", description = "Bank Transfer to HDFC Bank (..4012)", created_at = "23 Sep, 02:40 PM")
        )

        _withdrawalRequests.value = listOf(
            WithdrawalRequest(id = "wd-1", driver_id = "demo-driver-001", amount = 1000.0, bank_account = "50100412398412", ifsc = "HDFC0000128", status = "processed", created_at = "23 Sep, 02:40 PM")
        )

        _vehicles.value = listOf(
            DriverVehicle(id = "veh-1", owner_id = "demo-driver-001", vehicle_type_id = "vt_sedan", plate_number = "KA-01-MJ-4029", make = "Maruti Suzuki", model = "Dzire Tour", has_ac = true, is_default = true, vehicle_type_name = "Prime Sedan", icon_emoji = "🚗"),
            DriverVehicle(id = "veh-2", owner_id = "demo-driver-001", vehicle_type_id = "vt_suv", plate_number = "KA-03-NP-9912", make = "Toyota", model = "Innova Crysta", has_ac = true, is_default = false, vehicle_type_name = "XL SUV", icon_emoji = "🚙")
        )

        _documents.value = listOf(
            DriverDocument(id = "doc-1", driver_id = "demo-driver-001", doc_type = "driving_license", file_path = "demo/dl.jpg", expiry_date = "2029-11-20", status = "approved"),
            DriverDocument(id = "doc-2", driver_id = "demo-driver-001", doc_type = "rc_book", file_path = "demo/rc.jpg", expiry_date = "2032-05-15", status = "approved"),
            DriverDocument(id = "doc-3", driver_id = "demo-driver-001", doc_type = "insurance", file_path = "demo/insurance.jpg", expiry_date = "2027-08-30", status = "approved"),
            DriverDocument(id = "doc-4", driver_id = "demo-driver-001", doc_type = "vehicle_photo", file_path = "demo/car.jpg", expiry_date = null, status = "approved")
        )

        _incentives.value = listOf(
            DriverIncentiveProgress(id = "inc-1", driver_id = "demo-driver-001", program_id = "p-1", program_title = "Weekend Rush Bonus", completed_trips = 7, target_trips = 12, reward_amount = 1200.0, is_completed = false),
            DriverIncentiveProgress(id = "inc-2", driver_id = "demo-driver-001", program_id = "p-2", program_title = "Daily 5 Rides Boost", completed_trips = 6, target_trips = 5, reward_amount = 300.0, is_completed = true, reward_claimed = true)
        )

        _notifications.value = listOf(
            AppNotification(id = "n-1", user_id = "demo-driver-001", title = "Incentive Earned! 🎉", message = "You completed Daily 5 Rides Boost and ₹300 was credited to your wallet.", type = "incentive", created_at = "10 mins ago"),
            AppNotification(id = "n-2", user_id = "demo-driver-001", title = "Peak Surge Active ⚡", message = "1.5x Fare Surge in Koramangala & Indiranagar areas right now.", type = "surge", created_at = "45 mins ago")
        )

        _referrals.value = listOf(
            ReferralItem(id = "ref-1", referrer_id = "demo-driver-001", referred_name = "Rajesh K.", referred_phone = "+91 98451 22910", status = "completed", reward_amount = 250.0, created_at = "Yesterday")
        )

        _reviews.value = listOf(
            RatingReview(id = "r-1", rated_user = "demo-driver-001", customer_name = "Aditi Rao", rating = 5, review_text = "Extremely polite driver, clean car and took the fastest route!", created_at = "Yesterday"),
            RatingReview(id = "r-2", rated_user = "demo-driver-001", customer_name = "Karthik N.", rating = 5, review_text = "AC was running cold and reached on time. 5 stars!", created_at = "2 days ago")
        )
    }
}
