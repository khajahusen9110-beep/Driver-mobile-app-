package com.example.data

/** Input rules shared by the screens. They mirror the checks the backend RPCs enforce. */
object Validators {
    private val IFSC = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    private val ACCOUNT = Regex("^\\d{9,18}$")
    private val INDIAN_MOBILE = Regex("^[6-9]\\d{9}$")

    /** Returns the number in E.164 form (+91XXXXXXXXXX), or null if it is not a valid Indian mobile number. */
    fun normalizePhone(input: String): String? {
        var digits = input.filter { it.isDigit() }
        if (digits.length == 12 && digits.startsWith("91")) digits = digits.drop(2)
        if (digits.length == 11 && digits.startsWith("0")) digits = digits.drop(1)
        return if (INDIAN_MOBILE.matches(digits)) "+91$digits" else null
    }

    fun isValidOtp(code: String) = code.length == 6 && code.all { it.isDigit() }

    fun isValidIfsc(ifsc: String) = IFSC.matches(ifsc.trim().uppercase())

    fun isValidAccountNumber(account: String) = ACCOUNT.matches(account.trim())

    /** Indian registration plates such as KA01AB1234 or 22BH1234AA; spaces and dashes are ignored. */
    fun normalizePlate(plate: String): String = plate.uppercase().filter { it.isLetterOrDigit() }

    fun isValidPlate(plate: String): Boolean {
        val p = normalizePlate(plate)
        return p.length in 6..11 && p.any { it.isLetter() } && p.any { it.isDigit() }
    }
}
