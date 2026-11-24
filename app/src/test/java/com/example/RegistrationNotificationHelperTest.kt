package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.RegistrationDraft
import com.example.data.RegistrationNotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RegistrationNotificationHelperTest {

    @Test
    fun `compressImageUriToBase64 encodes valid jpeg and png files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File.createTempFile("passport_photo", ".jpg", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(640, 800, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.CYAN)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()

            val result = RegistrationNotificationHelper.compressImageUriToBase64(
                context = context,
                uri = Uri.fromFile(tempFile)
            )
            assertTrue(
                "Expected photo compression to succeed but got: ${result.exceptionOrNull()?.message}",
                result.isSuccess
            )
            val encoded = result.getOrThrow()
            assertTrue("Expected non-empty Base64 output", encoded.base64.length >= 16)
            assertTrue("Expected Base64 within Firestore budget", encoded.base64.length <= 195_000)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `compressImageUriToBase64 encodes valid png files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File.createTempFile("passport_photo_png", ".png", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(520, 520, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLUE)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            val result = RegistrationNotificationHelper.compressImageUriToBase64(
                context = context,
                uri = Uri.fromFile(tempFile)
            )
            assertTrue(
                "Expected PNG photo compression to succeed but got: ${result.exceptionOrNull()?.message}",
                result.isSuccess
            )
            val encoded = result.getOrThrow()
            assertTrue("Expected non-empty Base64 output", encoded.base64.length >= 16)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `buildProfessionalAdminEmailBody includes support contacts and applicant details`() {
        val draft = RegistrationDraft(
            profileImageBase64 = "abc123456789012345",
            profileImageName = "passport.jpg",
            fullName = "Adarsh Gupta",
            email = "adarsh@example.com",
            phoneNumber = "9876543210",
            occupation = "Engineer",
            address = "Bengaluru",
            ageYears = "28",
            dateOfBirth = "01/01/1998",
            membersCount = "1",
            selectedClasses = listOf("Weight Loss"),
            selectedPackages = listOf("Monthly"),
            healthIssues = "None",
            expectations = "Daily yoga",
            gender = "Male",
            maritalStatus = "Single",
            height = "175 cm",
            weight = "70 kg",
            dateOfJoining = "20/11/2026",
            preferredTime = "07:00 AM",
            medicationDetails = "None",
            referralSource = "Website",
            termsAccepted = true,
            termsSignature = "Adarsh Gupta",
            medicalReportNames = emptyList(),
            medicalReportPreviews = emptyList()
        )
        val body = RegistrationNotificationHelper.buildProfessionalAdminEmailBody("reg_12345678", draft)
        assertTrue(body.contains("Adarsh Gupta"))
        assertTrue(body.contains("9876543210"))
        assertTrue(body.contains("adarsh@example.com"))
        assertTrue(body.contains("+91 88921 59095"))
        assertTrue(body.contains("+91 76764 95076"))
        assertTrue(body.contains("vangmayyoga22@gmail.com"))
    }
}
