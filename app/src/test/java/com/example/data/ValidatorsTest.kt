package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `phone numbers are normalised to E164`() {
        assertEquals("+919876543210", Validators.normalizePhone("9876543210"))
        assertEquals("+919876543210", Validators.normalizePhone("+91 98765 43210"))
        assertEquals("+919876543210", Validators.normalizePhone("09876543210"))
        assertNull(Validators.normalizePhone("12345"))
        assertNull(Validators.normalizePhone("5876543210")) // Indian mobiles start with 6-9
    }

    @Test
    fun `ifsc and account rules match the backend`() {
        assertTrue(Validators.isValidIfsc("HDFC0001234"))
        assertTrue(Validators.isValidIfsc("hdfc0001234"))
        assertFalse(Validators.isValidIfsc("HDFC1001234"))
        assertFalse(Validators.isValidIfsc("BAD"))
        assertTrue(Validators.isValidAccountNumber("123456789"))
        assertFalse(Validators.isValidAccountNumber("12345678"))
        assertFalse(Validators.isValidAccountNumber("1234567890123456789"))
    }

    @Test
    fun `plates ignore spaces and dashes`() {
        assertEquals("KA01AB1234", Validators.normalizePlate("ka-01 ab 1234"))
        assertTrue(Validators.isValidPlate("KA 01 AB 1234"))
        assertTrue(Validators.isValidPlate("22BH1234AA"))
        assertFalse(Validators.isValidPlate("ABCDEF"))
    }

    @Test
    fun `otp must be six digits`() {
        assertTrue(Validators.isValidOtp("123456"))
        assertFalse(Validators.isValidOtp("12345"))
        assertFalse(Validators.isValidOtp("12a456"))
    }
}
