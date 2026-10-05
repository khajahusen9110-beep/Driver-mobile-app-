package com.example.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ModelsTest {

    @Test
    fun `ride parses backend columns and prefers the final fare`() {
        val json = JSONObject(
            """
            {"id":"r1","customer_id":"c1","driver_id":null,"status":"accepted","pickup_lat":17.38,"pickup_lng":78.48,
             "pickup_address":"Ameerpet","drop_lat":17.45,"drop_lng":78.5,"drop_address":null,"distance_km":8.2,
             "fare_estimate":210,"fare_final":225.5,"requested_at":"2026-10-05T06:40:00.123456+00:00","scheduled_at":null}
            """
        )
        val ride = Ride.from(json)
        assertEquals(RideStatus.ACCEPTED, ride.status)
        assertNull(ride.driverId)
        assertNull(ride.dropAddress)
        assertEquals(225.5, ride.fare!!, 0.0)
        assertEquals(Instant.parse("2026-10-05T06:40:00.123456Z"), ride.requestedAt)
        assertFalse(ride.isScheduled)
        assertTrue(ride.status.isActiveTrip)
    }

    @Test
    fun `postgres style timestamps parse`() {
        assertEquals(Instant.parse("2026-10-05T06:40:00Z"), parseInstant("2026-10-05 06:40:00+00"))
    }

    @Test
    fun `timed suspension expires`() {
        val base = JSONObject().put("id", "d1").put("role", "driver").put("full_name", "Asha")
            .put("onboarding_status", "approved").put("is_suspended", true)
        val past = Profile.from(JSONObject(base.toString()).put("suspended_until", "2020-01-01T00:00:00+00:00"))
        val open = Profile.from(base)
        assertFalse(past.isCurrentlySuspended())
        assertTrue(open.isCurrentlySuspended())
        assertEquals(OnboardingStatus.APPROVED, open.onboardingStatus)
    }

    @Test
    fun `withdrawal masks the account number`() {
        val w = Withdrawal.from(JSONObject().put("id", "w1").put("amount", 500).put("bank_account", "123456789012")
            .put("ifsc", "HDFC0001234").put("status", "pending"))
        assertEquals("•••• 9012", w.maskedAccount)
        assertTrue(w.isOpen)
    }

    @Test
    fun `backend errors become readable messages`() {
        assertEquals(
            "Driver must be online to accept rides",
            SupabaseClient.errorMessage("""{"code":"P0001","message":"Driver must be online to accept rides"}""", 400),
        )
        assertEquals(
            "That code is wrong or has expired. Request a new one.",
            SupabaseClient.errorMessage("""{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}""", 403),
        )
        assertEquals(
            "This registration number is already registered.",
            SupabaseClient.errorMessage("""{"code":"23505","message":"duplicate key value violates unique constraint \"vehicles_plate_number_key\""}""", 409),
        )
        assertEquals("Server error. Please try again shortly.", SupabaseClient.errorMessage("", 502))
    }
}
