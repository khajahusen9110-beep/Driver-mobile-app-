package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

// ---------- JSON helpers (org.json returns "null" strings and 0 for missing values) ----------

fun JSONObject.str(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

fun JSONObject.dbl(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

fun JSONObject.int(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)

fun JSONObject.bool(key: String, default: Boolean = false): Boolean =
    if (!has(key) || isNull(key)) default else optBoolean(key, default)

fun JSONObject.instant(key: String): Instant? = str(key)?.let { parseInstant(it) }

fun JSONObject.date(key: String): LocalDate? = str(key)?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

fun parseInstant(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(value) }.getOrNull()
        // Postgres can send "2026-10-05 06:40:00+00"
        ?: runCatching { OffsetDateTime.parse(value.replace(' ', 'T').let { if (Regex("[+-]\\d\\d$").containsMatchIn(it)) "$it:00" else it }).toInstant() }.getOrNull()

inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(transform) }

// ---------- Profile ----------

enum class OnboardingStatus { INCOMPLETE, SUBMITTED, APPROVED, REJECTED;
    companion object {
        fun from(value: String?) = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: INCOMPLETE
    }
}

data class Profile(
    val id: String,
    val role: String,
    val fullName: String,
    val phone: String?,
    val onboardingStatus: OnboardingStatus,
    val rejectionReason: String?,
    val isActive: Boolean,
    val isSuspended: Boolean,
    val suspendedUntil: Instant?,
    val suspensionReason: String?,
    val referralCode: String?,
    val isDeleted: Boolean,
) {
    val isDriver get() = role == "driver"

    /** Suspension that is still in force (an expired timed suspension no longer counts). */
    fun isCurrentlySuspended(now: Instant = Instant.now()) =
        isSuspended && (suspendedUntil == null || suspendedUntil.isAfter(now))

    companion object {
        const val COLUMNS = "id,role,full_name,phone,onboarding_status,rejection_reason,is_active,is_suspended," +
            "suspended_until,suspension_reason,referral_code,is_deleted"

        fun from(j: JSONObject) = Profile(
            id = j.optString("id"),
            role = j.str("role") ?: "customer",
            fullName = j.str("full_name") ?: "",
            phone = j.str("phone"),
            onboardingStatus = OnboardingStatus.from(j.str("onboarding_status")),
            rejectionReason = j.str("rejection_reason"),
            isActive = j.bool("is_active", true),
            isSuspended = j.bool("is_suspended"),
            suspendedUntil = j.instant("suspended_until"),
            suspensionReason = j.str("suspension_reason"),
            referralCode = j.str("referral_code"),
            isDeleted = j.bool("is_deleted"),
        )
    }
}

// ---------- Rides ----------

enum class RideStatus(val label: String) {
    PENDING_PAYMENT("Awaiting payment"),
    REQUESTED("New request"),
    ACCEPTED("Heading to pickup"),
    ARRIVED("At pickup"),
    ONGOING("Trip in progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    val apiValue get() = name.lowercase()
    val isActiveTrip get() = this == ACCEPTED || this == ARRIVED || this == ONGOING

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.apiValue == value } ?: CANCELLED
    }
}

data class Ride(
    val id: String,
    val customerId: String,
    val driverId: String?,
    val status: RideStatus,
    val pickupLat: Double,
    val pickupLng: Double,
    val pickupAddress: String?,
    val dropLat: Double,
    val dropLng: Double,
    val dropAddress: String?,
    val distanceKm: Double?,
    val durationMin: Double?,
    val fareEstimate: Double?,
    val fareFinal: Double?,
    val discountAmount: Double?,
    val goodsDescription: String?,
    val goodsWeightKg: Double?,
    val passengerCount: Int?,
    val vehicleTypeId: String?,
    val requestedAt: Instant?,
    val scheduledAt: Instant?,
    val acceptedAt: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val cancelledAt: Instant?,
    val cancelReason: String?,
    val cancelledBy: String?,
) {
    /** The fare the driver should expect: the final fare once known, otherwise the estimate. */
    val fare: Double? get() = fareFinal ?: fareEstimate
    val isScheduled get() = scheduledAt != null

    companion object {
        // start_otp is deliberately not selected: the customer tells the driver the code at pickup.
        const val COLUMNS = "id,customer_id,driver_id,status,pickup_lat,pickup_lng,pickup_address,drop_lat,drop_lng," +
            "drop_address,distance_km,estimated_duration_min,fare_estimate,fare_final,discount_amount,goods_description," +
            "goods_weight_kg,passenger_count,requested_vehicle_type_id,requested_at,scheduled_at,accepted_at,started_at," +
            "completed_at,cancelled_at,cancel_reason,cancelled_by"

        fun from(j: JSONObject) = Ride(
            id = j.optString("id"),
            customerId = j.optString("customer_id"),
            driverId = j.str("driver_id"),
            status = RideStatus.from(j.str("status")),
            pickupLat = j.dbl("pickup_lat") ?: 0.0,
            pickupLng = j.dbl("pickup_lng") ?: 0.0,
            pickupAddress = j.str("pickup_address"),
            dropLat = j.dbl("drop_lat") ?: 0.0,
            dropLng = j.dbl("drop_lng") ?: 0.0,
            dropAddress = j.str("drop_address"),
            distanceKm = j.dbl("distance_km"),
            durationMin = j.dbl("estimated_duration_min"),
            fareEstimate = j.dbl("fare_estimate"),
            fareFinal = j.dbl("fare_final"),
            discountAmount = j.dbl("discount_amount"),
            goodsDescription = j.str("goods_description"),
            goodsWeightKg = j.dbl("goods_weight_kg"),
            passengerCount = j.int("passenger_count"),
            vehicleTypeId = j.str("requested_vehicle_type_id"),
            requestedAt = j.instant("requested_at"),
            scheduledAt = j.instant("scheduled_at"),
            acceptedAt = j.instant("accepted_at"),
            startedAt = j.instant("started_at"),
            completedAt = j.instant("completed_at"),
            cancelledAt = j.instant("cancelled_at"),
            cancelReason = j.str("cancel_reason"),
            cancelledBy = j.str("cancelled_by"),
        )
    }
}

data class RideParticipants(
    val customerName: String?,
    val customerPhone: String?,
    val vehiclePlate: String?,
    val vehicleTypeName: String?,
) {
    companion object {
        fun from(j: JSONObject) = RideParticipants(
            customerName = j.str("customer_name"),
            customerPhone = j.str("customer_phone"),
            vehiclePlate = j.str("vehicle_plate"),
            vehicleTypeName = j.str("vehicle_type_name"),
        )
    }
}

// ---------- Vehicles ----------

data class VehicleType(
    val id: String,
    val name: String,
    val category: String,
    val capacityLabel: String?,
    val iconEmoji: String?,
) {
    val isGoods get() = category == "goods"

    companion object {
        fun from(j: JSONObject) = VehicleType(
            id = j.optString("id"),
            name = j.str("name") ?: "Vehicle",
            category = j.str("category") ?: "passenger",
            capacityLabel = j.str("capacity_label"),
            iconEmoji = j.str("icon_emoji"),
        )
    }
}

data class Vehicle(
    val id: String,
    val make: String?,
    val model: String?,
    val plateNumber: String,
    val capacity: Int,
    val capacityKg: Double?,
    val hasAc: Boolean,
    val vehicleTypeId: String,
    val typeName: String?,
    val isVerified: Boolean,
    val isActive: Boolean,
    val isDefault: Boolean,
) {
    val title get() = listOfNotNull(make, model).joinToString(" ").ifBlank { typeName ?: "Vehicle" }

    companion object {
        const val COLUMNS = "id,make,model,plate_number,capacity,capacity_kg,has_ac,vehicle_type_id,is_verified," +
            "is_active,is_default,vehicle_types(name)"

        fun from(j: JSONObject) = Vehicle(
            id = j.optString("id"),
            make = j.str("make"),
            model = j.str("model"),
            plateNumber = j.str("plate_number") ?: "",
            capacity = j.int("capacity") ?: 0,
            capacityKg = j.dbl("capacity_kg"),
            hasAc = j.bool("has_ac"),
            vehicleTypeId = j.optString("vehicle_type_id"),
            typeName = j.optJSONObject("vehicle_types")?.str("name"),
            isVerified = j.bool("is_verified"),
            isActive = j.bool("is_active", true),
            isDefault = j.bool("is_default"),
        )
    }
}

// ---------- Documents ----------

enum class DocType(val apiValue: String, val label: String, val required: Boolean, val hasExpiry: Boolean) {
    DRIVING_LICENSE("driving_license", "Driving licence", true, true),
    RC_BOOK("rc_book", "Vehicle RC", true, false),
    INSURANCE("insurance", "Vehicle insurance", true, true),
    VEHICLE_PHOTO("vehicle_photo", "Vehicle photo", true, false),
    ID_PROOF("id_proof", "ID proof (Aadhaar / PAN)", false, false),
    PROFILE_PHOTO("profile_photo", "Profile photo", false, false);

    companion object {
        fun from(value: String) = entries.firstOrNull { it.apiValue == value }
    }
}

data class DriverDocument(
    val id: String,
    val docType: String,
    val filePath: String,
    val uploadedAt: Instant?,
    val expiryDate: LocalDate?,
    val status: String,
    val rejectionReason: String?,
) {
    val isApproved get() = status == "approved"
    val isRejected get() = status == "rejected"

    companion object {
        const val COLUMNS = "id,doc_type,file_path,uploaded_at,expiry_date,status,rejection_reason"

        fun from(j: JSONObject) = DriverDocument(
            id = j.optString("id"),
            docType = j.optString("doc_type"),
            filePath = j.optString("file_path"),
            uploadedAt = j.instant("uploaded_at"),
            expiryDate = j.date("expiry_date"),
            status = j.str("status") ?: "submitted",
            rejectionReason = j.str("rejection_reason"),
        )
    }
}

// ---------- Live status ----------

data class DriverStatus(
    val isOnline: Boolean,
    val vehicleId: String?,
    val lat: Double?,
    val lng: Double?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(j: JSONObject) = DriverStatus(
            isOnline = j.bool("is_online"),
            vehicleId = j.str("vehicle_id"),
            lat = j.dbl("current_lat"),
            lng = j.dbl("current_lng"),
            updatedAt = j.instant("updated_at"),
        )
    }
}

// ---------- Money ----------

data class WalletTransaction(
    val id: String,
    val amount: Double,
    val isCredit: Boolean,
    val reason: String,
    val rideId: String?,
    val createdAt: Instant?,
) {
    companion object {
        fun from(j: JSONObject) = WalletTransaction(
            id = j.optString("id"),
            amount = j.dbl("amount") ?: 0.0,
            isCredit = j.str("type") == "credit",
            reason = j.str("reason") ?: "",
            rideId = j.str("ride_id"),
            createdAt = j.instant("created_at"),
        )
    }
}

data class Withdrawal(
    val id: String,
    val amount: Double,
    val bankAccount: String,
    val ifsc: String,
    val status: String,
    val adminNote: String?,
    val requestedAt: Instant?,
    val processedAt: Instant?,
) {
    val maskedAccount get() = if (bankAccount.length > 4) "•••• " + bankAccount.takeLast(4) else bankAccount
    val isOpen get() = status == "pending" || status == "approved"

    companion object {
        fun from(j: JSONObject) = Withdrawal(
            id = j.optString("id"),
            amount = j.dbl("amount") ?: 0.0,
            bankAccount = j.str("bank_account") ?: "",
            ifsc = j.str("ifsc") ?: "",
            status = j.str("status") ?: "pending",
            adminNote = j.str("admin_note"),
            requestedAt = j.instant("requested_at"),
            processedAt = j.instant("processed_at"),
        )
    }
}

data class EarningsDay(val day: LocalDate, val rides: Int, val gross: Double, val net: Double) {
    companion object {
        fun from(j: JSONObject) = EarningsDay(
            day = j.date("day") ?: LocalDate.MIN,
            rides = j.int("rides_count") ?: 0,
            gross = j.dbl("gross_earnings") ?: 0.0,
            net = j.dbl("net_earnings") ?: 0.0,
        )
    }
}

// ---------- Ratings, incentives, complaints, notifications ----------

data class RatingSummary(val average: Double, val total: Int)

data class Rating(val rating: Int, val comment: String?, val createdAt: Instant?) {
    companion object {
        fun from(j: JSONObject) = Rating(j.int("rating") ?: 0, j.str("comment"), j.instant("created_at"))
    }
}

data class Incentive(
    val id: String,
    val title: String,
    val description: String?,
    val targetRides: Int,
    val rewardAmount: Double,
    val validFrom: Instant?,
    val validUntil: Instant?,
    val completedRides: Int,
    val achieved: Boolean,
) {
    val progress get() = if (targetRides <= 0) 0f else (completedRides.toFloat() / targetRides).coerceIn(0f, 1f)

    companion object {
        fun from(program: JSONObject, progress: JSONObject?) = Incentive(
            id = program.optString("id"),
            title = program.str("title") ?: "Incentive",
            description = program.str("description"),
            targetRides = program.int("target_rides") ?: 0,
            rewardAmount = program.dbl("reward_amount") ?: 0.0,
            validFrom = program.instant("valid_from"),
            validUntil = program.instant("valid_until"),
            completedRides = progress?.int("rides_count") ?: 0,
            achieved = progress?.bool("achieved") ?: false,
        )
    }
}

data class Complaint(
    val id: String,
    val rideId: String?,
    val category: String,
    val description: String,
    val status: String,
    val adminResponse: String?,
    val createdAt: Instant?,
) {
    companion object {
        fun from(j: JSONObject) = Complaint(
            id = j.optString("id"),
            rideId = j.str("ride_id"),
            category = j.str("category") ?: "",
            description = j.str("description") ?: "",
            status = j.str("status") ?: "open",
            adminResponse = j.str("admin_response"),
            createdAt = j.instant("created_at"),
        )
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val body: String?,
    val type: String,
    val isRead: Boolean,
    val createdAt: Instant?,
) {
    companion object {
        fun from(j: JSONObject) = AppNotification(
            id = j.optString("id"),
            title = j.str("title") ?: "",
            body = j.str("body"),
            type = j.str("type") ?: "general",
            isRead = j.bool("is_read"),
            createdAt = j.instant("created_at"),
        )
    }
}

data class ReturnTripOffer(
    val id: String,
    val toAddress: String?,
    val discountPercent: Double,
    val status: String,
    val expiresAt: Instant?,
) {
    companion object {
        fun from(j: JSONObject) = ReturnTripOffer(
            id = j.optString("id"),
            toAddress = j.str("to_address"),
            discountPercent = j.dbl("discount_percent") ?: 0.0,
            status = j.str("status") ?: "active",
            expiresAt = j.instant("expires_at"),
        )
    }
}
