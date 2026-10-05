package com.example.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Every backend call the driver app makes. Business rules live in the database RPCs; this class only maps data. */
class DriverRepository(
    private val api: SupabaseClient,
    private val session: SessionStore,
) {
    val userId: String get() = session.userId ?: throw SessionExpiredException()
    val isLoggedIn: Boolean get() = session.isLoggedIn
    val sessionExpired = api.sessionExpired

    var selectedVehicleId: String?
        get() = session.selectedVehicleId
        set(value) { session.selectedVehicleId = value }

    // ---------- Auth ----------

    suspend fun sendOtp(phone: String, fullName: String?) = api.sendOtp(phone, fullName)

    suspend fun verifyOtp(phone: String, code: String) = api.verifyOtp(phone, code)

    suspend fun signOut() {
        session.pushToken?.let { token -> runCatching { unregisterDeviceToken(token) } }
        api.signOut()
    }

    suspend fun accessToken(): String = api.validAccessToken()

    // ---------- Profile ----------

    suspend fun profile(): Profile {
        val rows = api.select("profiles?id=eq.$userId&select=${Profile.COLUMNS}")
        val row = rows.optJSONObject(0) ?: throw ApiException("Your profile was not found. Please contact support.")
        return Profile.from(row)
    }

    suspend fun updateName(name: String) {
        api.update("profiles", "id=eq.$userId", JSONObject().put("full_name", name.trim()))
    }

    suspend fun submitApplication() { api.rpc("submit_driver_application") }

    suspend fun resubmitApplication() { api.rpc("resubmit_driver_application") }

    suspend fun requestAccountDeletion(reason: String?) {
        api.rpc("request_account_deletion", JSONObject().put("p_reason", reason.orNull()))
    }

    // ---------- Vehicles ----------

    suspend fun vehicleTypes(): List<VehicleType> =
        api.select("vehicle_types?is_active=eq.true&select=id,name,category,capacity_label,icon_emoji&order=sort_order")
            .mapObjects(VehicleType::from)

    suspend fun vehicles(): List<Vehicle> =
        api.select("vehicles?owner_id=eq.$userId&is_active=eq.true&select=${Vehicle.COLUMNS}&order=created_at")
            .mapObjects(Vehicle::from)

    suspend fun addVehicle(
        typeId: String, make: String, model: String, plate: String,
        capacity: Int, capacityKg: Double?, hasAc: Boolean, makeDefault: Boolean,
    ) {
        val body = JSONObject()
            .put("owner_id", userId)
            .put("vehicle_type_id", typeId)
            .put("make", make.trim())
            .put("model", model.trim())
            .put("plate_number", Validators.normalizePlate(plate))
            .put("capacity", capacity)
            .put("capacity_kg", capacityKg ?: JSONObject.NULL)
            .put("has_ac", hasAc)
            .put("is_default", makeDefault)
        val created = api.insert("vehicles", body).optJSONObject(0)
        if (makeDefault && created != null) setDefaultVehicle(created.optString("id"))
    }

    suspend fun setDefaultVehicle(vehicleId: String) {
        api.rpc("set_default_vehicle", JSONObject().put("p_vehicle_id", vehicleId))
    }

    // ---------- Documents ----------

    suspend fun documents(): List<DriverDocument> =
        api.select("driver_documents?driver_id=eq.$userId&select=${DriverDocument.COLUMNS}&order=uploaded_at.desc")
            .mapObjects(DriverDocument::from)

    /** Uploads the file and records it. Re-uploading a type replaces it and sends it for review again. */
    suspend fun uploadDocument(type: DocType, file: PickedFile, expiry: LocalDate?) {
        val path = "$userId/${type.apiValue}-${System.currentTimeMillis()}.${file.extension}"
        api.upload(SupabaseConfig.DOCUMENTS_BUCKET, path, file.bytes, file.mimeType)
        val body = JSONObject()
            .put("driver_id", userId)
            .put("doc_type", type.apiValue)
            .put("file_path", path)
            .put("expiry_date", expiry?.toString() ?: JSONObject.NULL)
        // The database turns a second upload of the same type into an update, so no row comes back.
        api.insert("driver_documents", body, returnRows = false)
    }

    suspend fun documentUrl(path: String): String = api.signedUrl(SupabaseConfig.DOCUMENTS_BUCKET, path)

    // ---------- Online status ----------

    suspend fun driverStatus(): DriverStatus? =
        api.select("driver_status?driver_id=eq.$userId&select=is_online,vehicle_id,current_lat,current_lng,updated_at")
            .optJSONObject(0)?.let(DriverStatus::from)

    suspend fun updateStatus(online: Boolean, lat: Double?, lng: Double?, vehicleId: String?): DriverStatus? {
        val params = JSONObject()
            .put("p_is_online", online)
            .put("p_lat", lat ?: JSONObject.NULL)
            .put("p_lng", lng ?: JSONObject.NULL)
            .put("p_vehicle_id", vehicleId ?: JSONObject.NULL)
        return api.rpcObject("update_driver_status", params)?.let(DriverStatus::from)
    }

    // ---------- Rides ----------

    /** Requests the backend lets this driver see: nearby, matching vehicle type, not already skipped. */
    suspend fun openRequests(): List<Ride> =
        api.select("rides?status=eq.requested&select=${Ride.COLUMNS}&order=requested_at.asc&limit=20")
            .mapObjects(Ride::from)

    suspend fun activeRide(): Ride? =
        api.select("rides?driver_id=eq.$userId&status=in.(accepted,arrived,ongoing)&select=${Ride.COLUMNS}&order=accepted_at.desc&limit=1")
            .optJSONObject(0)?.let(Ride::from)

    suspend fun ride(rideId: String): Ride? =
        api.select("rides?id=eq.$rideId&select=${Ride.COLUMNS}").optJSONObject(0)?.let(Ride::from)

    suspend fun tripHistory(limit: Int = 50): List<Ride> =
        api.select("rides?driver_id=eq.$userId&status=in.(completed,cancelled)&select=${Ride.COLUMNS}&order=requested_at.desc&limit=$limit")
            .mapObjects(Ride::from)

    suspend fun acceptRide(rideId: String): Ride =
        api.rpcObject("accept_ride", JSONObject().put("p_ride_id", rideId))?.let(Ride::from)
            ?: throw ApiException("Could not accept this ride.")

    suspend fun rejectRide(rideId: String, reason: String?) {
        api.rpc("reject_ride", JSONObject().put("p_ride_id", rideId).put("p_reason", reason.orNull()))
    }

    suspend fun updateRideStatus(rideId: String, status: RideStatus, cancelReason: String? = null, otp: String? = null): Ride {
        val params = JSONObject()
            .put("p_ride_id", rideId)
            .put("p_status", status.apiValue)
            .put("p_cancel_reason", cancelReason.orNull())
            .put("p_otp", otp.orNull())
        return api.rpcObject("update_ride_status", params)?.let(Ride::from)
            ?: throw ApiException("Could not update the trip.")
    }

    suspend fun participants(rideId: String): RideParticipants? =
        api.rpcObject("get_ride_participant_info", JSONObject().put("p_ride_id", rideId))?.let(RideParticipants::from)

    suspend fun rateCustomer(rideId: String, customerId: String, rating: Int, comment: String?) {
        val params = JSONObject()
            .put("p_ride_id", rideId)
            .put("p_rated_user", customerId)
            .put("p_rating", rating)
            .put("p_comment", comment.orNull())
        api.rpc("submit_rating", params)
    }

    suspend fun triggerSos(rideId: String?, lat: Double?, lng: Double?) {
        val params = JSONObject()
            .put("p_ride_id", rideId ?: JSONObject.NULL)
            .put("p_lat", lat ?: JSONObject.NULL)
            .put("p_lng", lng ?: JSONObject.NULL)
        api.rpc("trigger_sos", params)
    }

    // ---------- Return trips ----------

    suspend fun activeReturnOffer(): ReturnTripOffer? =
        api.select("return_trip_offers?driver_id=eq.$userId&status=eq.active&expires_at=gt.${isoNow()}&select=id,to_address,discount_percent,status,expires_at&limit=1")
            .optJSONObject(0)?.let(ReturnTripOffer::from)

    suspend fun offerReturnTrip(toLat: Double, toLng: Double, toAddress: String, discountPercent: Int, validMinutes: Int) {
        val params = JSONObject()
            .put("p_to_lat", toLat)
            .put("p_to_lng", toLng)
            .put("p_to_address", toAddress)
            .put("p_discount_percent", discountPercent)
            .put("p_valid_minutes", validMinutes)
        api.rpc("offer_return_trip", params)
    }

    suspend fun cancelReturnOffer(offerId: String) {
        api.update("return_trip_offers", "id=eq.$offerId&driver_id=eq.$userId", JSONObject().put("status", "cancelled"))
    }

    // ---------- Earnings, wallet ----------

    suspend fun earnings(days: Long = 30): List<EarningsDay> {
        val from = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(days - 1)
        return api.select("driver_earnings_daily?driver_id=eq.$userId&day=gte.$from&select=day,rides_count,gross_earnings,net_earnings&order=day.desc")
            .mapObjects(EarningsDay::from)
    }

    suspend fun walletBalance(): Double =
        api.select("wallets?user_id=eq.$userId&select=balance").optJSONObject(0)?.dbl("balance") ?: 0.0

    suspend fun walletTransactions(): List<WalletTransaction> =
        api.select("wallet_transactions?user_id=eq.$userId&select=id,amount,type,reason,ride_id,created_at&order=created_at.desc&limit=50")
            .mapObjects(WalletTransaction::from)

    suspend fun withdrawals(): List<Withdrawal> =
        api.select("withdrawal_requests?driver_id=eq.$userId&select=id,amount,bank_account,ifsc,status,admin_note,requested_at,processed_at&order=requested_at.desc&limit=30")
            .mapObjects(Withdrawal::from)

    suspend fun requestWithdrawal(amount: Double, account: String, ifsc: String) {
        val params = JSONObject()
            .put("p_amount", amount)
            .put("p_bank_account", account.trim())
            .put("p_ifsc", ifsc.trim().uppercase())
        api.rpc("request_withdrawal", params)
    }

    // ---------- Ratings, incentives, complaints ----------

    suspend fun ratingSummary(): RatingSummary? =
        api.select("driver_ratings_summary?driver_id=eq.$userId&select=avg_rating,total_reviews").optJSONObject(0)?.let {
            RatingSummary(it.dbl("avg_rating") ?: 0.0, it.int("total_reviews") ?: 0)
        }

    suspend fun recentRatings(): List<Rating> =
        api.select("ratings?rated_user=eq.$userId&select=rating,comment,created_at&order=created_at.desc&limit=20")
            .mapObjects(Rating::from)

    suspend fun incentives(): List<Incentive> {
        val programs = api.select("incentive_programs?is_active=eq.true&valid_until=gte.${isoNow()}&select=*&order=valid_until")
        val progress = api.select("driver_incentive_progress?driver_id=eq.$userId&select=program_id,rides_count,achieved")
        val byProgram = progress.mapObjects { it }.associateBy { it.optString("program_id") }
        return programs.mapObjects { Incentive.from(it, byProgram[it.optString("id")]) }
    }

    suspend fun complaints(): List<Complaint> =
        api.select("complaints?raised_by=eq.$userId&select=id,ride_id,category,description,status,admin_response,created_at&order=created_at.desc")
            .mapObjects(Complaint::from)

    /** [againstUserId] is the customer of the ride, when the complaint is about a trip. */
    suspend fun raiseComplaint(rideId: String?, againstUserId: String?, category: String, description: String) {
        val params = JSONObject()
            .put("p_ride_id", rideId ?: JSONObject.NULL)
            .put("p_against", againstUserId ?: JSONObject.NULL)
            .put("p_category", category)
            .put("p_description", description.trim())
        api.rpc("raise_complaint", params)
    }

    // ---------- Notifications and push ----------

    suspend fun notifications(): List<AppNotification> =
        api.select("notifications?user_id=eq.$userId&select=id,title,body,type,is_read,created_at&order=created_at.desc&limit=50")
            .mapObjects(AppNotification::from)

    suspend fun markNotificationsRead() {
        api.update("notifications", "user_id=eq.$userId&is_read=eq.false", JSONObject().put("is_read", true))
    }

    suspend fun registerDeviceToken(token: String) {
        api.rpc("register_device_token", JSONObject().put("p_token", token).put("p_platform", "android"))
        session.pushToken = token
    }

    private suspend fun unregisterDeviceToken(token: String) {
        api.rpc("unregister_device_token", JSONObject().put("p_token", token))
    }

    private fun String?.orNull(): Any = this?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL

    private fun isoNow(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}

/** A file chosen by the driver, already read and compressed. */
class PickedFile(val bytes: ByteArray, val mimeType: String, val extension: String)
