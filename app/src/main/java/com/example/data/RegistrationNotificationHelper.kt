package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class EncodedImageResult(
    val fileName: String,
    val base64: String
)

object RegistrationNotificationHelper {
    private const val TAG = "VahfRegistrationNotify"

    suspend fun compressImageUriToBase64(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 480,
        maxBase64Length: Int = 140_000
    ): Result<EncodedImageResult> = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val resolver = appContext.contentResolver
            val fileName = queryDisplayName(appContext, uri).ifBlank { "applicant_photo.jpg" }.take(120)

            val bytes = readUriBytes(appContext, uri)
            var decoded: Bitmap? = null

            if (bytes != null && bytes.isNotEmpty()) {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                val srcW = boundsOptions.outWidth
                val srcH = boundsOptions.outHeight
                if (srcW > 0 && srcH > 0) {
                    var sampleSize = 1
                    while (max(srcW / sampleSize, srcH / sampleSize) > maxDimensionPx * 2) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                }
            }

            // Fallback for HEIC/AVIF or virtual/provider-rendered images on Android 9+
            if (decoded == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                decoded = runCatching {
                    val source = android.graphics.ImageDecoder.createSource(resolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        val w = info.size.width
                        val h = info.size.height
                        if (w > maxDimensionPx && h > maxDimensionPx) {
                            val ratio = maxDimensionPx.toFloat() / max(w, h).toFloat()
                            decoder.setTargetSize(
                                (w * ratio).toInt().coerceAtLeast(1),
                                (h * ratio).toInt().coerceAtLeast(1)
                            )
                        }
                    }
                }.getOrNull()
            }

            val bitmap = decoded
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Unable to read selected photo. Please try a JPG or PNG image.")
                )

            val scale = maxDimensionPx.toFloat() / max(bitmap.width, bitmap.height).toFloat()
            val scaled = if (scale < 1f) {
                val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also {
                    if (it !== bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            var encoded = ""
            for (quality in listOf(78, 60, 44, 30)) {
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val candidate = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                encoded = candidate
                if (candidate.length <= maxBase64Length) break
            }
            scaled.recycle()

            if (encoded.length < 16 || encoded.length > 195_000) {
                return@withContext Result.failure(
                    IllegalArgumentException("Photo is too large to process. Please select a smaller image.")
                )
            }

            Result.success(EncodedImageResult(fileName = fileName, base64 = encoded))
        } catch (e: Exception) {
            Log.w(TAG, "Image encoding failed", e)
            Result.failure(
                IllegalArgumentException(e.localizedMessage ?: "Unable to read selected photo.")
            )
        }
    }

    private fun readUriBytes(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }

        runCatching {
            resolver.openTypedAssetFileDescriptor(uri, "image/*", null)?.use { afd ->
                afd.createInputStream().use { it.readBytes() }
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }

        return runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.createInputStream().use { it.readBytes() }
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    suspend fun readMedicalReportAttachment(
        context: Context,
        uri: Uri
    ): Result<MedicalReportAttachment> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.applicationContext.contentResolver
            val displayName = queryDisplayName(context, uri).ifBlank { "medical_report" }.take(80)
            val mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }

            val compactPreview = if (mimeType.startsWith("image/")) {
                compressImageUriToBase64(
                    context = context,
                    uri = uri,
                    maxDimensionPx = 220,
                    maxBase64Length = 28_000
                ).getOrNull()?.base64
            } else {
                null
            }

            Result.success(
                MedicalReportAttachment(
                    uriString = uri.toString(),
                    displayName = displayName,
                    mimeType = mimeType,
                    compactPreviewBase64 = compactPreview
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Medical report read failed", e)
            Result.failure(e)
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        val resolver = context.applicationContext.contentResolver
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx).orEmpty() else ""
                } else ""
            }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    fun computeSubmissionHash(draft: RegistrationDraft): String {
        val normalized = buildString {
            append(draft.fullName.trim().lowercase())
            append('|')
            append(draft.email.trim().lowercase())
            append('|')
            append(draft.phoneNumber.replace(Regex("[^0-9+]"), ""))
            append('|')
            append(draft.occupation.trim().lowercase())
            append('|')
            append(draft.address.trim().lowercase())
            append('|')
            append(draft.gender.trim().lowercase())
            append('|')
            append(draft.dateOfBirth.trim().lowercase())
            append('|')
            append(draft.dateOfJoining.trim().lowercase())
            append('|')
            append(draft.selectedClasses.sorted().joinToString(",").lowercase())
            append('|')
            append(draft.selectedPackages.sorted().joinToString(",").lowercase())
            append('|')
            append(draft.joiningMonths)
            append('|')
            append(draft.monthlyFeeInr)
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildProfessionalAdminEmailSubject(
        applicantName: String,
        registrationId: String
    ): String {
        val shortRef = registrationId.takeLast(8).uppercase(Locale.getDefault())
        return "VAHF Registration Form Submitted: ${applicantName.trim()} [Ref: $shortRef]"
    }

    fun buildProfessionalAdminEmailBody(
        registrationId: String,
        draft: RegistrationDraft
    ): String {
        val submittedTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("Dear VAHF Admin Team,")
            appendLine()
            appendLine("A new student registration form has been completed and submitted via the VAHF Yoga & Wellness mobile application.")
            appendLine()
            appendLine("==================================================")
            appendLine("VAHF — NEW REGISTRATION CONFIRMATION")
            appendLine("==================================================")
            appendLine("Registration Reference ID : $registrationId")
            appendLine("Submitted On              : $submittedTimestamp")
            appendLine("Applicant Name            : ${draft.fullName.trim()}")
            appendLine("Contact Number            : ${draft.phoneNumber.trim()}")
            appendLine(
                "Phone & WhatsApp Status   : ${
                    if (draft.phoneVerified) "Verified (+${draft.verifiedWhatsappNumber})" else "Pending"
                }"
            )
            appendLine("Applicant Email           : ${draft.email.trim()}")
            appendLine()
            appendLine("1. APPLICANT & CONTACT DETAILS")
            appendLine("--------------------------------------------------")
            appendLine("Full Name                 : ${draft.fullName.trim()}")
            appendLine("Email Address             : ${draft.email.trim()}")
            appendLine("Phone Number              : ${draft.phoneNumber.trim()}")
            appendLine("Occupation                : ${draft.occupation.trim()}")
            appendLine("Address                   : ${draft.address.trim()}")
            if (draft.ageYears.isNotBlank()) {
                appendLine("Age (in year)             : ${draft.ageYears.trim()}")
            }
            if (draft.dateOfBirth.isNotBlank()) {
                appendLine("Date of Birth             : ${draft.dateOfBirth.trim()}")
            }
            appendLine("Gender                    : ${draft.gender.trim()}")
            if (draft.maritalStatus.isNotBlank()) {
                appendLine("Marital Status            : ${draft.maritalStatus.trim()}")
            }
            if (draft.membersCount.isNotBlank()) {
                appendLine("Members Joining           : ${draft.membersCount.trim()}")
            }
            appendLine()
            appendLine("2. CLASS, JOINING MONTHS & FEE AUTOMATION")
            appendLine("--------------------------------------------------")
            appendLine("Joining Duration          : ${draft.joiningMonths} Month(s)")
            appendLine("Monthly Fee               : ${FeeAutomationHelper.formatInr(draft.monthlyFeeInr)} / month")
            appendLine("Total Plan Fee            : ${FeeAutomationHelper.formatInr(draft.totalFeeInr)}")
            appendLine("Next Monthly WhatsApp Due : ${FeeAutomationHelper.formatDateDdMmmYyyy(draft.nextDueDateMillis)}")
            appendLine(
                "Selected Yoga Classes     : ${
                    if (draft.selectedClasses.isNotEmpty()) draft.selectedClasses.joinToString(", ")
                    else "Not specified"
                }"
            )
            appendLine(
                "Selected Package          : ${
                    if (draft.selectedPackages.isNotEmpty()) draft.selectedPackages.joinToString(", ")
                    else "Not specified"
                }"
            )
            if (draft.dateOfJoining.isNotBlank()) {
                appendLine("Date of Joining           : ${draft.dateOfJoining.trim()}")
            }
            if (draft.preferredTime.isNotBlank()) {
                appendLine("Preferred Time            : ${draft.preferredTime.trim()}")
            }
            appendLine()
            appendLine("3. HEALTH, WELLNESS & BACKGROUND")
            appendLine("--------------------------------------------------")
            if (draft.height.isNotBlank()) appendLine("Height                    : ${draft.height.trim()}")
            if (draft.weight.isNotBlank()) appendLine("Weight                    : ${draft.weight.trim()}")
            appendLine("Health Issues             : ${draft.healthIssues.trim().ifBlank { "None reported" }}")
            appendLine("Current Medications       : ${draft.medicationDetails.trim().ifBlank { "None reported" }}")
            appendLine("Expectations from VAHF    : ${draft.expectations.trim().ifBlank { "Not specified" }}")
            appendLine("Heard About VAHF Via      : ${draft.referralSource.trim().ifBlank { "Not specified" }}")
            appendLine()
            appendLine("4. ATTACHMENTS & DECLARATION")
            appendLine("--------------------------------------------------")
            appendLine("Applicant Photo           : ${draft.profileImageName.ifBlank { "Attached" }} (Saved in VAHF Database)")
            appendLine(
                "Medical Reports           : ${
                    if (draft.medicalReportNames.isNotEmpty()) {
                        "${draft.medicalReportNames.size} file(s) — ${draft.medicalReportNames.joinToString(", ")}"
                    } else {
                        "None uploaded"
                    }
                }"
            )
            appendLine("Terms & Conditions        : Accepted (${draft.termsSignature.trim()})")
            appendLine()
            appendLine("==================================================")
            appendLine("VAHF HELP & SUPPORT CONTACT INFORMATION")
            appendLine("==================================================")
            appendLine("For any query, assistance, or immediate coordination:")
            appendLine("Helpline / Contact 1      : ${VahfConfig.SUPPORT_PHONE_PRIMARY_DISPLAY}")
            appendLine("Helpline / Contact 2      : ${VahfConfig.SUPPORT_PHONE_SECONDARY_DISPLAY}")
            appendLine("Official Admin Email      : ${VahfConfig.ADMIN_EMAIL}")
            appendLine("Official Website          : ${VahfConfig.WEBSITE_URL}")
            appendLine()
            appendLine("Warm regards,")
            appendLine("VAHF — Vangmay Ayush Health Foundation")
        }.trim()
    }

    fun buildAdminNotificationMessage(
        registrationId: String,
        draft: RegistrationDraft
    ): String = buildString {
        appendLine("*VAHF - New Registration Form Submitted*")
        appendLine("Ref ID: $registrationId")
        appendLine("--------------------------------")
        appendLine("*Name:* ${draft.fullName.trim()}")
        appendLine("*Email:* ${draft.email.trim()}")
        appendLine("*Phone Number:* ${draft.phoneNumber.trim()} (${if (draft.phoneVerified) "Verified" else "Unverified"})")
        appendLine("*Joining Duration:* ${draft.joiningMonths} Month(s)")
        appendLine("*Monthly Fee:* ${FeeAutomationHelper.formatInr(draft.monthlyFeeInr)} / month")
        appendLine("*Total Fee:* ${FeeAutomationHelper.formatInr(draft.totalFeeInr)}")
        appendLine("*Next WhatsApp Fee Reminder:* ${FeeAutomationHelper.formatDateDdMmmYyyy(draft.nextDueDateMillis)}")
        appendLine("*Occupation:* ${draft.occupation.trim()}")
        appendLine("*Address:* ${draft.address.trim()}")
        if (draft.ageYears.isNotBlank()) appendLine("*Age (in year):* ${draft.ageYears.trim()}")
        if (draft.dateOfBirth.isNotBlank()) appendLine("*Date of Birth:* ${draft.dateOfBirth.trim()}")
        if (draft.membersCount.isNotBlank()) appendLine("*Members Joining:* ${draft.membersCount.trim()}")
        if (draft.selectedClasses.isNotEmpty()) {
            appendLine("*Classes Selected:* ${draft.selectedClasses.joinToString(", ")}")
        }
        if (draft.selectedPackages.isNotEmpty()) {
            appendLine("*Package:* ${draft.selectedPackages.joinToString(", ")}")
        }
        appendLine("*Gender:* ${draft.gender.trim()}")
        if (draft.maritalStatus.isNotBlank()) appendLine("*Marital Status:* ${draft.maritalStatus.trim()}")
        if (draft.height.isNotBlank()) appendLine("*Height:* ${draft.height.trim()}")
        if (draft.weight.isNotBlank()) appendLine("*Weight:* ${draft.weight.trim()}")
        if (draft.dateOfJoining.isNotBlank()) appendLine("*Date of Joining:* ${draft.dateOfJoining.trim()}")
        if (draft.preferredTime.isNotBlank()) appendLine("*Preferred Time:* ${draft.preferredTime.trim()}")
        if (draft.healthIssues.isNotBlank()) appendLine("*Health Issues:* ${draft.healthIssues.trim()}")
        if (draft.expectations.isNotBlank()) appendLine("*Expectations:* ${draft.expectations.trim()}")
        if (draft.medicationDetails.isNotBlank()) appendLine("*Medications:* ${draft.medicationDetails.trim()}")
        if (draft.referralSource.isNotBlank()) appendLine("*Heard About VAHF Via:* ${draft.referralSource.trim()}")
        appendLine("*Photo Attached:* ${draft.profileImageName.ifBlank { "Yes" }}")
        if (draft.medicalReportNames.isNotEmpty()) {
            appendLine("*Medical Reports (${draft.medicalReportNames.size}):* ${draft.medicalReportNames.joinToString(", ")}")
        }
        appendLine("*Terms Accepted:* Yes (${draft.termsSignature.trim()})")
        appendLine("--------------------------------")
        appendLine("*VAHF Help & Support:*")
        appendLine("Contact 1: ${VahfConfig.SUPPORT_PHONE_PRIMARY_DISPLAY}")
        appendLine("Contact 2: ${VahfConfig.SUPPORT_PHONE_SECONDARY_DISPLAY}")
        appendLine("Email: ${VahfConfig.ADMIN_EMAIL}")
    }.trim()

    suspend fun postAdminWebhookNotification(
        registrationId: String,
        draft: RegistrationDraft,
        emailSubject: String,
        emailBody: String,
        formattedMessage: String
    ): Boolean = withContext(Dispatchers.IO) {
        val webhookUrl = VahfConfig.ADMIN_NOTIFICATION_WEBHOOK_URL.trim()
        if (webhookUrl.isBlank()) return@withContext false

        var connection: HttpURLConnection? = null
        try {
            val payload = JSONObject().apply {
                put("event", "vahf_registration_submitted")
                put("registrationId", registrationId)
                put("recipientEmail", VahfConfig.ADMIN_EMAIL)
                put("emailSubject", emailSubject)
                put("emailBody", emailBody)
                put("supportPhonePrimary", VahfConfig.SUPPORT_PHONE_PRIMARY)
                put("supportPhoneSecondary", VahfConfig.SUPPORT_PHONE_SECONDARY)
                put("adminWhatsapp", VahfConfig.ADMIN_WHATSAPP_NUMBER)
                put("formattedMessage", formattedMessage)
                put("fullName", draft.fullName.trim())
                put("email", draft.email.trim())
                put("phoneNumber", draft.phoneNumber.trim())
                put("phoneVerified", draft.phoneVerified)
                put("verifiedWhatsappNumber", draft.verifiedWhatsappNumber)
                put("joiningMonths", draft.joiningMonths)
                put("monthlyFeeInr", draft.monthlyFeeInr)
                put("totalFeeInr", draft.totalFeeInr)
                put("nextDueDateMillis", draft.nextDueDateMillis)
                put("nextDueDateFormatted", FeeAutomationHelper.formatDateDdMmmYyyy(draft.nextDueDateMillis))
                put("occupation", draft.occupation.trim())
                put("address", draft.address.trim())
                put("ageYears", draft.ageYears.trim())
                put("dateOfBirth", draft.dateOfBirth.trim())
                put("membersCount", draft.membersCount.trim())
                put("selectedClasses", JSONArray(draft.selectedClasses))
                put("selectedPackages", JSONArray(draft.selectedPackages))
                put("healthIssues", draft.healthIssues.trim())
                put("expectations", draft.expectations.trim())
                put("gender", draft.gender.trim())
                put("maritalStatus", draft.maritalStatus.trim())
                put("height", draft.height.trim())
                put("weight", draft.weight.trim())
                put("dateOfJoining", draft.dateOfJoining.trim())
                put("preferredTime", draft.preferredTime.trim())
                put("medicationDetails", draft.medicationDetails.trim())
                put("referralSource", draft.referralSource.trim())
                put("termsAccepted", draft.termsAccepted)
                put("termsSignature", draft.termsSignature.trim())
                put("medicalReportNames", JSONArray(draft.medicalReportNames))
            }

            connection = (URL(webhookUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Admin webhook notification failed", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    fun openAdminEmailNotification(
        context: Context,
        emailSubject: String,
        emailBody: String
    ): Boolean {
        val fullMailtoUri = Uri.parse(
            "mailto:${VahfConfig.ADMIN_EMAIL}?subject=${Uri.encode(emailSubject)}&body=${Uri.encode(emailBody)}"
        )

        // 1. Direct Gmail Compose via ACTION_SEND reliably auto-fills To, Subject, and complete multi-line Body
        val gmailDirectSend = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(VahfConfig.ADMIN_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
            setPackage("com.google.android.gm")
        }
        if (tryStartActivity(context, gmailDirectSend)) return true

        // 2. Gmail ACTION_SENDTO with full RFC-6068 mailto URI (subject & body query params)
        val gmailSendTo = Intent(Intent.ACTION_SENDTO).apply {
            data = fullMailtoUri
            putExtra(Intent.EXTRA_EMAIL, arrayOf(VahfConfig.ADMIN_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
            setPackage("com.google.android.gm")
        }
        if (tryStartActivity(context, gmailSendTo)) return true

        // 3. Any default email client via full mailto URI
        val defaultMailSendTo = Intent(Intent.ACTION_SENDTO).apply {
            data = fullMailtoUri
            putExtra(Intent.EXTRA_EMAIL, arrayOf(VahfConfig.ADMIN_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }
        if (tryStartActivity(context, defaultMailSendTo)) return true

        // 4. Chooser fallback with full email extras
        val fallbackShare = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(VahfConfig.ADMIN_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
            },
            "Send Registration Email to ${VahfConfig.ADMIN_EMAIL}"
        )
        return tryStartActivity(context, fallbackShare)
    }

    fun openWhatsAppNotification(context: Context, message: String): Boolean {
        return openWhatsAppToNumber(context, VahfConfig.ADMIN_WHATSAPP_NUMBER, message)
    }

    fun openWhatsAppToNumber(context: Context, phoneE164: String, message: String): Boolean {
        val phone = phoneE164.replace(Regex("[^0-9]"), "")
        val encodedText = Uri.encode(message)

        if (phone.isNotBlank()) {
            val waUrlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encodedText")
            )
            if (tryStartActivity(context, waUrlIntent)) return true
        }

        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val pkgIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(pkg)
            }
            if (tryStartActivity(context, pkgIntent)) return true
        }

        val browserFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
        )
        if (tryStartActivity(context, browserFallback)) return true

        val shareFallback = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "VAHF Registration Submission")
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "Send Registration Details"
        )
        return tryStartActivity(context, shareFallback)
    }

    fun dialSupportNumber(context: Context, phoneNumber: String): Boolean {
        val cleanDigits = phoneNumber.replace(Regex("[^0-9+]"), "")
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanDigits"))
        return tryStartActivity(context, dialIntent)
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
