package com.example.data.api

data class DriverProfile(
    val id: String = "",
    val full_name: String = "",
    val phone: String = "",
    val role: String = "driver",
    val onboarding_status: String = "incomplete", // incomplete, submitted, rejected, approved
    val is_suspended: Boolean = false,
    val suspension_reason: String? = null,
    val suspended_until: String? = null,
    val rejection_reason: String? = null,
    val referral_code: String? = null,
    val is_online: Boolean = false,
    val current_lat: Double? = null,
    val current_lng: Double? = null,
    val rating: Double = 5.0,
    val total_trips: Int = 0
)

data class VehicleType(
    val id: String = "",
    val name: String = "",
    val category: String = "passenger", // passenger or goods
    val icon_emoji: String = "🚗",
    val capacity_kg: Int? = null,
    val sort_order: Int = 0,
    val is_active: Boolean = true
)

data class DriverVehicle(
    val id: String = "",
    val owner_id: String = "",
    val vehicle_type_id: String = "",
    val plate_number: String = "",
    val make: String = "",
    val model: String = "",
    val capacity_kg: Int? = null,
    val has_ac: Boolean = false,
    val is_default: Boolean = false,
    val vehicle_type_name: String? = null,
    val icon_emoji: String? = null
)

data class DriverDocument(
    val id: String = "",
    val driver_id: String = "",
    val doc_type: String = "", // driving_license, rc_book, insurance, vehicle_photo, id_proof, profile_photo
    val file_path: String = "",
    val expiry_date: String? = null,
    val status: String = "submitted",
    val created_at: String? = null
)

data class RideRequest(
    val id: String = "",
    val customer_id: String = "",
    val customer_name: String = "Rider Customer",
    val customer_phone: String = "+91 98765 43210",
    val status: String = "requested", // requested, accepted, arrived, ongoing, completed, cancelled
    val pickup_address: String = "",
    val drop_address: String = "",
    val pickup_lat: Double = 0.0,
    val pickup_lng: Double = 0.0,
    val drop_lat: Double = 0.0,
    val drop_lng: Double = 0.0,
    val fare_amount: Double = 0.0,
    val distance_km: Double = 0.0,
    val requested_vehicle_type_id: String = "",
    val vehicle_type_name: String = "Sedan",
    val vehicle_emoji: String = "🚗",
    val is_outstation: Boolean = false,
    val created_at: String = ""
)

data class ParticipantInfo(
    val customer_name: String = "Customer",
    val customer_phone: String = "",
    val vehicle_plate: String = "",
    val vehicle_make: String = "",
    val vehicle_model: String = ""
)

data class DailyEarnings(
    val id: String = "",
    val driver_id: String = "",
    val day: String = "",
    val rides_count: Int = 0,
    val gross_earnings: Double = 0.0,
    val net_earnings: Double = 0.0,
    val commission_deducted: Double = 0.0
)

data class WalletData(
    val balance: Double = 0.0,
    val currency: String = "₹"
)

data class WalletTransaction(
    val id: String = "",
    val user_id: String = "",
    val amount: Double = 0.0,
    val type: String = "credit", // credit, debit
    val description: String = "",
    val created_at: String = ""
)

data class WithdrawalRequest(
    val id: String = "",
    val driver_id: String = "",
    val amount: Double = 0.0,
    val bank_account: String = "",
    val ifsc: String = "",
    val status: String = "pending", // pending, processed, rejected
    val created_at: String = ""
)

data class AppNotification(
    val id: String = "",
    val user_id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "system",
    val is_read: Boolean = false,
    val created_at: String = ""
)

data class ReferralItem(
    val id: String = "",
    val referrer_id: String = "",
    val referred_name: String = "Driver Friend",
    val referred_phone: String = "",
    val status: String = "completed",
    val reward_amount: Double = 250.0,
    val created_at: String = ""
)

data class IncentiveProgram(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val target_trips: Int = 10,
    val reward_amount: Double = 500.0,
    val is_active: Boolean = true,
    val expires_at: String = ""
)

data class DriverIncentiveProgress(
    val id: String = "",
    val driver_id: String = "",
    val program_id: String = "",
    val program_title: String = "",
    val completed_trips: Int = 0,
    val target_trips: Int = 10,
    val reward_amount: Double = 500.0,
    val is_completed: Boolean = false,
    val reward_claimed: Boolean = false
)

data class ReturnTripOffer(
    val id: String = "",
    val driver_id: String = "",
    val to_lat: Double = 0.0,
    val to_lng: Double = 0.0,
    val to_address: String = "",
    val discount_percent: Int = 30,
    val valid_minutes: Int = 120,
    val status: String = "active", // active, matched, expired
    val created_at: String = ""
)

data class DriverRatingSummary(
    val driver_id: String = "",
    val avg_rating: Double = 4.88,
    val total_reviews: Int = 42,
    val five_star_pct: Int = 92
)

data class RatingReview(
    val id: String = "",
    val rated_user: String = "",
    val customer_name: String = "Passenger",
    val rating: Int = 5,
    val review_text: String = "Smooth driving and very polite!",
    val created_at: String = ""
)

data class ComplaintItem(
    val id: String = "",
    val raised_by: String = "",
    val ride_id: String = "",
    val against: String = "Customer",
    val category: String = "Fare Dispute",
    val description: String = "",
    val status: String = "open", // open, investigating, resolved
    val created_at: String = ""
)

data class LegalDocument(
    val key: String = "customer_tnc",
    val title: String = "Driver Partner Terms & Conditions",
    val content: String = ""
)
