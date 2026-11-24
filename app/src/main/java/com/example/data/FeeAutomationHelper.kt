package com.example.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PhoneVerificationResult(
    val isValid: Boolean,
    val formattedDisplay: String,
    val whatsappE164: String,
    val hintMessage: String
)

data class MonthlyInstallment(
    val monthNumber: Int,
    val dueDateMillis: Long,
    val dueDateFormatted: String,
    val amountInr: Int
)

data class FeePlanBreakdown(
    val membersCount: Int,
    val joiningMonths: Int,
    val monthlyFeeInr: Int,
    val totalFeeInr: Int,
    val nextDueDateMillis: Long,
    val nextDueDateFormatted: String,
    val installments: List<MonthlyInstallment>
)

object FeeAutomationHelper {

    fun verifyPhoneNumber(rawInput: String): PhoneVerificationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return PhoneVerificationResult(
                isValid = false,
                formattedDisplay = "",
                whatsappE164 = "",
                hintMessage = "Enter a 10-digit mobile number for instant verification & WhatsApp fee automation."
            )
        }

        val digitsOnly = trimmed.filter { it.isDigit() }
        val hasOnlyValidChars = trimmed.matches(Regex("^[+]?[0-9\\s\\-()]{1,20}$"))
        if (!hasOnlyValidChars) {
            return PhoneVerificationResult(
                isValid = false,
                formattedDisplay = "",
                whatsappE164 = "",
                hintMessage = "Only digits, spaces, hyphens, or leading + are allowed."
            )
        }

        if (digitsOnly.length < 10) {
            return PhoneVerificationResult(
                isValid = false,
                formattedDisplay = "",
                whatsappE164 = "",
                hintMessage = "Enter ${10 - digitsOnly.length} more digit(s) to auto-verify & activate WhatsApp automation."
            )
        }

        if (digitsOnly.toSet().size <= 1) {
            return PhoneVerificationResult(
                isValid = false,
                formattedDisplay = "",
                whatsappE164 = "",
                hintMessage = "Please enter a valid active mobile number."
            )
        }

        val e164Digits = when {
            trimmed.startsWith("+") && digitsOnly.length in 10..15 -> digitsOnly
            digitsOnly.length == 10 -> "91$digitsOnly"
            digitsOnly.length == 11 && digitsOnly.startsWith("0") -> "91${digitsOnly.drop(1)}"
            digitsOnly.length in 11..15 -> digitsOnly
            else -> ""
        }

        if (e164Digits.length !in 10..15) {
            return PhoneVerificationResult(
                isValid = false,
                formattedDisplay = "",
                whatsappE164 = "",
                hintMessage = "Enter a valid 10–15 digit phone number."
            )
        }

        val display = if (e164Digits.length == 12 && e164Digits.startsWith("91")) {
            val local = e164Digits.drop(2)
            "+91 ${local.take(5)} ${local.drop(5)}"
        } else {
            "+$e164Digits"
        }

        return PhoneVerificationResult(
            isValid = true,
            formattedDisplay = display,
            whatsappE164 = e164Digits,
            hintMessage = "Verified! Monthly WhatsApp fee reminders will be linked to $display."
        )
    }

    fun suggestedMonthlyFeePerMember(selectedClasses: Collection<String>): Int {
        if (selectedClasses.isEmpty()) return VahfConfig.DEFAULT_MONTHLY_FEE_INR
        return selectedClasses
            .mapNotNull { VahfConfig.CLASS_MONTHLY_FEE_INR[it] }
            .maxOrNull()
            ?: VahfConfig.DEFAULT_MONTHLY_FEE_INR
    }

    fun calculateFeeBreakdown(
        selectedClasses: Collection<String>,
        membersCountText: String,
        joiningMonths: Int,
        customMonthlyFeePerMemberInr: Int?,
        dateOfJoiningText: String,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): FeePlanBreakdown {
        val safeMonths = joiningMonths.coerceIn(1, 36)
        val members = membersCountText.trim().toIntOrNull()?.coerceIn(1, 50) ?: 1
        val perMemberMonthly = customMonthlyFeePerMemberInr?.takeIf { it >= 100 }
            ?: suggestedMonthlyFeePerMember(selectedClasses)
        val monthlyFee = (perMemberMonthly * members).coerceIn(100, 500_000)
        val totalFee = (monthlyFee.toLong() * safeMonths.toLong()).coerceIn(100L, 18_000_000L).toInt()

        val baseMillis = parseJoiningDateMillis(dateOfJoiningText, referenceTimeMillis)
        val installments = (1..safeMonths).map { monthNum ->
            val dueMillis = addMonthsAtMorningHour(baseMillis, monthNum)
            MonthlyInstallment(
                monthNumber = monthNum,
                dueDateMillis = dueMillis,
                dueDateFormatted = formatDateDdMmmYyyy(dueMillis),
                amountInr = monthlyFee
            )
        }

        val firstDue = installments.first()
        return FeePlanBreakdown(
            membersCount = members,
            joiningMonths = safeMonths,
            monthlyFeeInr = monthlyFee,
            totalFeeInr = totalFee,
            nextDueDateMillis = firstDue.dueDateMillis,
            nextDueDateFormatted = firstDue.dueDateFormatted,
            installments = installments
        )
    }

    fun parseJoiningDateMillis(dateText: String, fallbackMillis: Long): Long {
        val clean = dateText.trim()
        if (clean.isBlank()) return fallbackMillis
        for (pattern in listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }.parse(clean)
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return fallbackMillis
    }

    fun addMonthsAtMorningHour(baseMillis: Long, monthsToAdd: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.MONTH, monthsToAdd)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDateDdMmmYyyy(millis: Long): String {
        if (millis <= 0L) return "Not scheduled"
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
    }

    fun formatInr(amountInr: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
        return "₹${nf.format(amountInr)}"
    }

    fun buildStudentWelcomeWhatsAppMessage(
        studentName: String,
        verifiedDisplayPhone: String,
        joiningMonths: Int,
        monthlyFeeInr: Int,
        totalFeeInr: Int,
        nextDueDateText: String,
        selectedClasses: List<String>
    ): String = buildString {
        appendLine("Namaste ${studentName.trim()},")
        appendLine()
        appendLine("*VAHF Yoga & Wellness — Registration & Fee Automation Active*")
        appendLine("Your mobile number ($verifiedDisplayPhone) is verified and linked for monthly fee updates.")
        appendLine()
        if (selectedClasses.isNotEmpty()) {
            appendLine("*Program:* ${selectedClasses.joinToString(", ")}")
        }
        appendLine("*Joining Duration:* $joiningMonths Month(s)")
        appendLine("*Monthly Fee:* ${formatInr(monthlyFeeInr)} / month")
        appendLine("*Total Plan Fee:* ${formatInr(totalFeeInr)}")
        appendLine("*Next Monthly Fee Date:* $nextDueDateText (Month 1 of $joiningMonths)")
        appendLine()
        appendLine("Each month on your due date, you will receive your monthly fee reminder on WhatsApp.")
        appendLine("For help: ${VahfConfig.SUPPORT_PHONE_PRIMARY_DISPLAY} | ${VahfConfig.ADMIN_EMAIL}")
    }.trim()

    fun buildStudentMonthlyFeeWhatsAppMessage(
        studentName: String,
        monthIndex: Int,
        joiningMonths: Int,
        monthlyFeeInr: Int,
        totalFeeInr: Int,
        dueDateText: String,
        classesSummary: String = ""
    ): String = buildString {
        appendLine("Namaste ${studentName.trim()},")
        appendLine()
        appendLine("*VAHF Yoga & Wellness — Monthly Fee Reminder*")
        appendLine("--------------------------------")
        appendLine("*Billing Cycle:* Month $monthIndex of $joiningMonths")
        if (classesSummary.isNotBlank()) {
            appendLine("*Yoga Program:* $classesSummary")
        }
        appendLine("*This Month's Fee:* ${formatInr(monthlyFeeInr)}")
        appendLine("*Total Plan ($joiningMonths Months):* ${formatInr(totalFeeInr)}")
        appendLine("*Due Date:* $dueDateText")
        appendLine("--------------------------------")
        appendLine("Please share payment confirmation with VAHF Admin.")
        appendLine("Support: ${VahfConfig.SUPPORT_PHONE_PRIMARY_DISPLAY} | ${VahfConfig.ADMIN_EMAIL}")
    }.trim()
}
