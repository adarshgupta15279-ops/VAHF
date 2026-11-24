package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FeeAutomationHelper
import com.example.data.FeeReminderScheduler
import com.example.data.MedicalReportAttachment
import com.example.data.RegistrationDraft
import com.example.data.RegistrationNotificationHelper
import com.example.data.RegistrationSubmission
import com.example.data.RegistrationSubmitState
import com.example.data.UserProfile
import com.example.data.VahfConfig
import com.example.ui.components.VahfSupportContactCard
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfError
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfOffWhite
import com.example.ui.theme.VahfSuccess
import com.example.ui.theme.VahfTextMuted
import com.example.ui.theme.VahfTextPrimary
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val HeaderTintBackground = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationFormScreen(
    loadedProfile: UserProfile?,
    defaultName: String,
    defaultEmail: String,
    submitState: RegistrationSubmitState,
    onSubmitRegistration: (RegistrationDraft) -> Unit,
    onNotificationDispatched: (registrationId: String, channel: String) -> Unit,
    onResetSubmitState: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Photo & medical upload states
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageName by remember { mutableStateOf("") }
    var profileImageBase64 by remember { mutableStateOf("") }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    var medicalAttachments by remember { mutableStateOf<List<MedicalReportAttachment>>(emptyList()) }
    var isProcessingReports by remember { mutableStateOf(false) }
    var reportUploadHint by remember { mutableStateOf<String?>(null) }

    // Form fields matching every screenshot section sequentially
    var fullName by remember { mutableStateOf(loadedProfile?.fullName?.takeIf { it.isNotBlank() } ?: defaultName) }
    var email by remember { mutableStateOf(loadedProfile?.email?.takeIf { it.isNotBlank() } ?: defaultEmail) }
    var phoneNumber by remember { mutableStateOf(loadedProfile?.mobileNumber.orEmpty()) }
    var occupation by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(loadedProfile?.city.orEmpty()) }
    var ageYears by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var membersCount by remember { mutableStateOf("") }
    var joiningMonthsText by remember { mutableStateOf("1") }
    var customMonthlyFeeText by remember { mutableStateOf(VahfConfig.DEFAULT_MONTHLY_FEE_INR.toString()) }
    var hasEditedMonthlyFee by remember { mutableStateOf(false) }

    var selectedClasses by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    var healthIssues by remember { mutableStateOf("") }
    var expectations by remember { mutableStateOf(loadedProfile?.wellnessGoal.orEmpty()) }
    var gender by remember { mutableStateOf("") }

    var maritalStatus by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var dateOfJoining by remember { mutableStateOf("") }
    var preferredTime by remember { mutableStateOf("") }

    var medicationDetails by remember { mutableStateOf("") }
    var referralSource by remember { mutableStateOf("") }

    var termsAccepted by remember { mutableStateOf(false) }
    var termsSignature by remember { mutableStateOf("") }

    // Validation errors
    var photoError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var occupationError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var joiningMonthsError by remember { mutableStateOf<String?>(null) }
    var monthlyFeeError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }
    var validationBanner by remember { mutableStateOf<String?>(null) }

    // Instant live phone verification & fee automation calculation
    val phoneVerification = remember(phoneNumber) {
        FeeAutomationHelper.verifyPhoneNumber(phoneNumber)
    }
    val feeBreakdown = remember(
        selectedClasses,
        membersCount,
        joiningMonthsText,
        customMonthlyFeeText,
        dateOfJoining
    ) {
        FeeAutomationHelper.calculateFeeBreakdown(
            selectedClasses = selectedClasses,
            membersCountText = membersCount,
            joiningMonths = joiningMonthsText.toIntOrNull() ?: 1,
            customMonthlyFeePerMemberInr = customMonthlyFeeText.toIntOrNull(),
            dateOfJoiningText = dateOfJoining
        )
    }

    // Date & Time Pickers
    var showDobPicker by remember { mutableStateOf(false) }
    var showJoiningDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var autoNotifiedRegistrationId by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Schedule 1-month fee reminder & automatically open Gmail to vangmayyoga22@gmail.com on submit
    LaunchedEffect(submitState) {
        if (submitState is RegistrationSubmitState.Success) {
            val res = submitState.result
            if (autoNotifiedRegistrationId != res.registrationId) {
                autoNotifiedRegistrationId = res.registrationId
                FeeReminderScheduler.scheduleNextMonthlyReminder(
                    context = context,
                    registration = RegistrationSubmission(
                        id = res.registrationId,
                        fullName = res.applicantName,
                        phoneVerified = res.verifiedWhatsappNumber.isNotBlank(),
                        verifiedWhatsappNumber = res.verifiedWhatsappNumber,
                        joiningMonths = res.joiningMonths,
                        monthlyFeeInr = res.monthlyFeeInr,
                        totalFeeInr = res.totalFeeInr,
                        feeAutomationEnabled = true,
                        nextDueDateMillis = res.nextDueDateMillis,
                        lastNotifiedMonth = 0
                    )
                )
                val emailLaunched = RegistrationNotificationHelper.openAdminEmailNotification(
                    context = context,
                    emailSubject = res.emailSubject,
                    emailBody = res.emailBody
                )
                if (emailLaunched || res.webhookDispatched) {
                    val channel = if (res.webhookDispatched && emailLaunched) {
                        "webhook+email"
                    } else if (emailLaunched) {
                        "email"
                    } else {
                        "webhook"
                    }
                    onNotificationDispatched(res.registrationId, channel)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingPhoto = true
            photoError = null
            scope.launch {
                val encodedResult = RegistrationNotificationHelper.compressImageUriToBase64(context, uri)
                encodedResult.onSuccess { encoded ->
                    profilePhotoUri = uri
                    profileImageName = encoded.fileName
                    profileImageBase64 = encoded.base64
                }.onFailure { err ->
                    photoError = err.localizedMessage ?: "Failed to load selected photo."
                }
                isProcessingPhoto = false
            }
        }
    }

    val medicalReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = 6 - medicalAttachments.size
            if (remaining <= 0) {
                reportUploadHint = "Maximum 6 files allowed."
                return@rememberLauncherForActivityResult
            }
            val acceptedUris = uris.take(remaining)
            if (uris.size > remaining) {
                reportUploadHint = "Added first $remaining file(s). Limit is 6 files."
            } else {
                reportUploadHint = null
            }
            isProcessingReports = true
            scope.launch {
                val added = mutableListOf<MedicalReportAttachment>()
                for (uri in acceptedUris) {
                    RegistrationNotificationHelper.readMedicalReportAttachment(context, uri)
                        .onSuccess { added.add(it) }
                }
                medicalAttachments = (medicalAttachments + added).take(6)
                isProcessingReports = false
            }
        }
    }

    fun resetAllFields() {
        profilePhotoUri = null
        profileImageName = ""
        profileImageBase64 = ""
        medicalAttachments = emptyList()
        reportUploadHint = null
        occupation = ""
        address = ""
        ageYears = ""
        dateOfBirth = ""
        membersCount = ""
        joiningMonthsText = "1"
        customMonthlyFeeText = VahfConfig.DEFAULT_MONTHLY_FEE_INR.toString()
        hasEditedMonthlyFee = false
        selectedClasses = emptySet()
        selectedPackages = emptySet()
        healthIssues = ""
        expectations = ""
        gender = ""
        maritalStatus = ""
        height = ""
        weight = ""
        dateOfJoining = ""
        preferredTime = ""
        medicationDetails = ""
        referralSource = ""
        termsAccepted = false
        termsSignature = ""
        photoError = null
        nameError = null
        emailError = null
        phoneError = null
        occupationError = null
        addressError = null
        ageError = null
        joiningMonthsError = null
        monthlyFeeError = null
        genderError = null
        termsError = null
        validationBanner = null
        autoNotifiedRegistrationId = null
        onResetSubmitState()
    }

    fun validateAndSubmit() {
        if (submitState is RegistrationSubmitState.Submitting) return
        if (isProcessingPhoto || isProcessingReports) {
            validationBanner = "Please wait for file upload processing to finish."
            return
        }

        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phoneNumber.trim()
        val cleanOccupation = occupation.trim()
        val cleanAddress = address.trim()
        val cleanAge = ageYears.trim()
        val parsedMonths = joiningMonthsText.trim().toIntOrNull()
        val parsedFeePerMember = customMonthlyFeeText.trim().toIntOrNull()
        val cleanTermsSig = termsSignature.trim()

        photoError = if (profileImageBase64.length < 16) {
            "Please upload an applicant photo."
        } else null

        nameError = when {
            cleanName.length < 2 -> "Please enter your name (at least 2 characters)."
            cleanName.length > 80 -> "Name must be 80 characters or fewer."
            else -> null
        }

        emailError = when {
            cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                "Please enter a valid email address."
            cleanEmail.length > 120 -> "Email must be 120 characters or fewer."
            else -> null
        }

        phoneError = if (!phoneVerification.isValid) {
            phoneVerification.hintMessage
        } else null

        occupationError = when {
            cleanOccupation.length < 2 -> "Please enter your occupation."
            cleanOccupation.length > 80 -> "Occupation must be 80 characters or fewer."
            else -> null
        }

        addressError = when {
            cleanAddress.length < 3 -> "Please enter your complete address."
            cleanAddress.length > 250 -> "Address must be 250 characters or fewer."
            else -> null
        }

        ageError = if (cleanAge.isNotEmpty()) {
            val parsed = cleanAge.toIntOrNull()
            if (parsed == null || parsed !in 1..120) "Please enter a valid age (1–120)." else null
        } else null

        joiningMonthsError = if (parsedMonths == null || parsedMonths !in 1..36) {
            "Please enter how many months you are joining (1–36)."
        } else null

        monthlyFeeError = if (parsedFeePerMember == null || parsedFeePerMember !in 100..250_000) {
            "Please enter a valid monthly fee (₹100 – ₹2,50,000)."
        } else null

        genderError = if (gender !in VahfConfig.REGISTRATION_GENDER_OPTIONS) {
            "Please select your gender."
        } else null

        val effectiveAccepted = termsAccepted || cleanTermsSig.isNotEmpty()
        val effectiveSig = if (cleanTermsSig.isNotEmpty()) cleanTermsSig else if (termsAccepted) "I Agree" else ""
        termsError = if (!effectiveAccepted || effectiveSig.length < 2) {
            "Please accept the Terms & Conditions and enter your confirmation."
        } else null

        val hasAnyError = listOf(
            photoError,
            nameError,
            emailError,
            phoneError,
            occupationError,
            addressError,
            ageError,
            joiningMonthsError,
            monthlyFeeError,
            genderError,
            termsError
        ).any { it != null }

        if (hasAnyError) {
            validationBanner = "Please complete all required (*) fields before submitting."
            scope.launch {
                scrollState.animateScrollTo(0)
            }
            return
        }

        validationBanner = null
        termsAccepted = true
        termsSignature = effectiveSig

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val finalBreakdown = FeeAutomationHelper.calculateFeeBreakdown(
            selectedClasses = selectedClasses,
            membersCountText = membersCount,
            joiningMonths = parsedMonths ?: 1,
            customMonthlyFeePerMemberInr = parsedFeePerMember,
            dateOfJoiningText = dateOfJoining
        )

        onSubmitRegistration(
            RegistrationDraft(
                profileImageBase64 = profileImageBase64,
                profileImageName = profileImageName.ifBlank { "applicant_photo.jpg" },
                fullName = cleanName,
                email = cleanEmail,
                phoneNumber = cleanPhone,
                phoneVerified = phoneVerification.isValid,
                verifiedWhatsappNumber = phoneVerification.whatsappE164,
                occupation = cleanOccupation,
                address = cleanAddress,
                ageYears = cleanAge,
                dateOfBirth = dateOfBirth.trim(),
                membersCount = membersCount.trim(),
                joiningMonths = finalBreakdown.joiningMonths,
                monthlyFeeInr = finalBreakdown.monthlyFeeInr,
                totalFeeInr = finalBreakdown.totalFeeInr,
                nextDueDateMillis = finalBreakdown.nextDueDateMillis,
                selectedClasses = selectedClasses.toList(),
                selectedPackages = selectedPackages.toList(),
                healthIssues = healthIssues.trim(),
                expectations = expectations.trim(),
                gender = gender,
                maritalStatus = maritalStatus.trim(),
                height = height.trim(),
                weight = weight.trim(),
                dateOfJoining = dateOfJoining.trim(),
                preferredTime = preferredTime.trim(),
                medicationDetails = medicationDetails.trim(),
                referralSource = referralSource.trim(),
                termsAccepted = true,
                termsSignature = effectiveSig,
                medicalReportNames = medicalAttachments.map { it.displayName },
                medicalReportPreviews = medicalAttachments.mapNotNull { it.compactPreviewBase64 }.take(6)
            )
        )
    }

    if (showDobPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateOfBirth = formatUtcMillisToDate(millis)
                        }
                        showDobPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDobPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showJoiningDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showJoiningDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateOfJoining = formatUtcMillisToDate(millis)
                        }
                        showJoiningDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoiningDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Preferred Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferredTime = formatTime12Hour(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HeaderTintBackground)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(scrollState)
                .testTag("registration_form_screen")
        ) {
            // Top Registration Banner matching Screenshot 1 cleanly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderTintBackground)
                    .padding(horizontal = 16.dp, vertical = 22.dp)
            ) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .testTag("registration_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = VahfNavyDeep
                    )
                }

                Text(
                    text = stringResource(id = R.string.registration_form_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = VahfNavyDeep,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp)
                )
            }

            if (submitState is RegistrationSubmitState.Success) {
                RegistrationSuccessCard(
                    result = submitState.result,
                    onSendEmail = {
                        val ok = RegistrationNotificationHelper.openAdminEmailNotification(
                            context = context,
                            emailSubject = submitState.result.emailSubject,
                            emailBody = submitState.result.emailBody
                        )
                        if (ok) {
                            onNotificationDispatched(submitState.result.registrationId, "email")
                        }
                    },
                    onSendWhatsApp = {
                        val ok = RegistrationNotificationHelper.openWhatsAppNotification(
                            context = context,
                            message = submitState.result.notificationMessage
                        )
                        if (ok) {
                            onNotificationDispatched(submitState.result.registrationId, "whatsapp")
                        }
                    },
                    onSendStudentWhatsApp = {
                        RegistrationNotificationHelper.openWhatsAppToNumber(
                            context = context,
                            phoneE164 = submitState.result.verifiedWhatsappNumber,
                            message = submitState.result.studentWelcomeWhatsAppMessage
                        )
                    },
                    onCallSupport = { phone ->
                        RegistrationNotificationHelper.dialSupportNumber(context, phone)
                    },
                    onSubmitAnother = { resetAllFields() },
                    onBackHome = onBackToHome
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = VahfWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .border(1.dp, VahfBorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (validationBanner != null) {
                            StatusAlertBanner(
                                message = validationBanner!!,
                                isError = true
                            )
                        }

                        if (submitState is RegistrationSubmitState.Error) {
                            StatusAlertBanner(
                                message = submitState.message,
                                isError = true
                            )
                        }

                        // 1. Image Upload *
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormRequiredLabel(text = "Image Upload", required = true)

                            DashedUploadBox(
                                isProcessing = isProcessingPhoto,
                                subHint = "You can upload up to 1 files.",
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("reg_image_upload_box")
                            )

                            if (profilePhotoUri != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = VahfOffWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VahfBorderSubtle, RoundedCornerShape(14.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = profilePhotoUri,
                                            contentDescription = "Uploaded applicant photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profileImageName.ifBlank { "Selected Photo" },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = VahfNavyDeep,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Photo ready for upload",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VahfSuccess
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                profilePhotoUri = null
                                                profileImageName = ""
                                                profileImageBase64 = ""
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove photo",
                                                tint = VahfTextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            if (photoError != null) {
                                FieldErrorText(photoError!!)
                            }
                        }

                        // 2. Name *
                        RegistrationInputField(
                            label = "Name",
                            required = true,
                            value = fullName,
                            onValueChange = {
                                if (it.length <= 80) {
                                    fullName = it
                                    nameError = null
                                }
                            },
                            error = nameError,
                            testTag = "reg_name_input"
                        )

                        // 3. Email *
                        RegistrationInputField(
                            label = "Email",
                            required = true,
                            value = email,
                            onValueChange = {
                                if (it.length <= 120) {
                                    email = it
                                    emailError = null
                                }
                            },
                            keyboardType = KeyboardType.Email,
                            error = emailError,
                            testTag = "reg_email_input"
                        )

                        // 4. Phone Number * with Instant Verification & WhatsApp Automation
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RegistrationInputField(
                                label = "Phone Number",
                                required = true,
                                value = phoneNumber,
                                onValueChange = {
                                    if (it.length <= 20) {
                                        phoneNumber = it
                                        phoneError = null
                                    }
                                },
                                placeholder = "10-digit WhatsApp mobile number",
                                keyboardType = KeyboardType.Phone,
                                error = phoneError,
                                testTag = "reg_phone_input"
                            )

                            if (phoneVerification.isValid) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = VahfSuccess.copy(alpha = 0.09f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VahfSuccess.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .testTag("reg_phone_verified_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = "Phone number verified",
                                            tint = VahfSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Number Verified • Automation Active",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = VahfSuccess
                                            )
                                            Text(
                                                text = "WhatsApp: ${phoneVerification.formattedDisplay} • Monthly fee reminders enabled",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VahfNavyDeep
                                            )
                                        }
                                    }
                                }
                            } else if (phoneNumber.isNotBlank()) {
                                Text(
                                    text = phoneVerification.hintMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VahfCyanPrimary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // 5. Occupation : *
                        RegistrationInputField(
                            label = "Occupation :",
                            required = true,
                            value = occupation,
                            onValueChange = {
                                if (it.length <= 80) {
                                    occupation = it
                                    occupationError = null
                                }
                            },
                            error = occupationError,
                            testTag = "reg_occupation_input"
                        )

                        // 6. Address *
                        RegistrationInputField(
                            label = "Address",
                            required = true,
                            value = address,
                            onValueChange = {
                                if (it.length <= 250) {
                                    address = it
                                    addressError = null
                                }
                            },
                            error = addressError,
                            testTag = "reg_address_input"
                        )

                        // 7. Age (in year)
                        RegistrationInputField(
                            label = "Age (in year)",
                            required = false,
                            value = ageYears,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    ageYears = it
                                    ageError = null
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            error = ageError,
                            testTag = "reg_age_input"
                        )

                        // 8. Date of Birth
                        RegistrationInputField(
                            label = "Date of Birth",
                            required = false,
                            value = dateOfBirth,
                            onValueChange = { if (it.length <= 30) dateOfBirth = it },
                            placeholder = "DD/MM/YYYY",
                            trailingIcon = {
                                IconButton(
                                    onClick = { showDobPicker = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = "Pick Date of Birth",
                                        tint = VahfCyanPrimary
                                    )
                                }
                            },
                            testTag = "reg_dob_input"
                        )

                        // 9. How Many Members Want to Join?
                        RegistrationInputField(
                            label = "How Many Members Want to Join?",
                            required = false,
                            value = membersCount,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    membersCount = it
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            testTag = "reg_members_input"
                        )

                        // 10. Select (Classes Checkboxes)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FormRequiredLabel(text = "Select", required = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            VahfConfig.REGISTRATION_CLASS_OPTIONS.forEach { option ->
                                val isChecked = selectedClasses.contains(option)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .toggleable(
                                            value = isChecked,
                                            role = Role.Checkbox,
                                            onValueChange = { checked ->
                                                val updated = if (checked) {
                                                    selectedClasses + option
                                                } else {
                                                    selectedClasses - option
                                                }
                                                selectedClasses = updated
                                                if (!hasEditedMonthlyFee) {
                                                    customMonthlyFeeText = FeeAutomationHelper
                                                        .suggestedMonthlyFeePerMember(updated)
                                                        .toString()
                                                }
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = VahfNavyDeep,
                                            uncheckedColor = VahfTextMuted,
                                            checkmarkColor = VahfWhite
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = VahfTextPrimary
                                    )
                                }
                            }
                        }

                        // 11. Package (Checkboxes)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FormRequiredLabel(text = "Package", required = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            VahfConfig.REGISTRATION_PACKAGE_OPTIONS.forEach { pkg ->
                                val isChecked = selectedPackages.contains(pkg)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .toggleable(
                                            value = isChecked,
                                            role = Role.Checkbox,
                                            onValueChange = { checked ->
                                                selectedPackages = if (checked) {
                                                    selectedPackages + pkg
                                                } else {
                                                    selectedPackages - pkg
                                                }
                                                if (checked) {
                                                    val autoMonths = when (pkg) {
                                                        "First month" -> 1
                                                        "Third month" -> 3
                                                        "Six month" -> 6
                                                        "Nine month" -> 9
                                                        else -> null
                                                    }
                                                    if (autoMonths != null) {
                                                        joiningMonthsText = autoMonths.toString()
                                                        joiningMonthsError = null
                                                    }
                                                }
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = VahfNavyDeep,
                                            uncheckedColor = VahfTextMuted,
                                            checkmarkColor = VahfWhite
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = pkg,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = VahfTextPrimary
                                    )
                                }
                            }
                        }

                        // 11b. How Many Months Are You Joining? * + Fee Automation Preview
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FormRequiredLabel(text = "How Many Months Are You Joining?", required = true)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val selectedInt = joiningMonthsText.toIntOrNull()
                                VahfConfig.JOINING_MONTH_OPTIONS.forEach { monthOption ->
                                    val isSelected = selectedInt == monthOption
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) VahfNavyDeep else VahfOffWhite,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(50))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) VahfNavyDeep else VahfBorderSubtle,
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable {
                                                joiningMonthsText = monthOption.toString()
                                                joiningMonthsError = null
                                            }
                                            .testTag("reg_months_chip_$monthOption")
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (monthOption == 1) "1 Month" else "$monthOption Months",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) VahfWhite else VahfNavyDeep
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    RegistrationInputField(
                                        label = "Joining Months",
                                        required = true,
                                        value = joiningMonthsText,
                                        onValueChange = {
                                            if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                                                joiningMonthsText = it
                                                joiningMonthsError = null
                                            }
                                        },
                                        placeholder = "e.g. 1, 3, 6",
                                        keyboardType = KeyboardType.Number,
                                        error = joiningMonthsError,
                                        testTag = "reg_joining_months_input"
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    RegistrationInputField(
                                        label = "Monthly Fee (₹)",
                                        required = true,
                                        value = customMonthlyFeeText,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                                hasEditedMonthlyFee = true
                                                customMonthlyFeeText = it
                                                monthlyFeeError = null
                                            }
                                        },
                                        placeholder = "1500",
                                        keyboardType = KeyboardType.Number,
                                        error = monthlyFeeError,
                                        testTag = "reg_monthly_fee_input"
                                    )
                                }
                            }

                            // Live Automated Fee & WhatsApp Billing Schedule Preview Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = VahfCyanSoftContainer.copy(alpha = 0.42f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VahfCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .testTag("reg_fee_automation_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                tint = VahfNavyDeep,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Fee & WhatsApp Automation",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = VahfNavyDeep
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (phoneVerification.isValid) VahfSuccess else VahfNavyDeep
                                        ) {
                                            Text(
                                                text = if (phoneVerification.isValid) "WHATSAPP READY" else "ENTER PHONE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = VahfWhite,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Plan: ${feeBreakdown.joiningMonths} Month(s) × ${FeeAutomationHelper.formatInr(feeBreakdown.monthlyFeeInr)} = ${FeeAutomationHelper.formatInr(feeBreakdown.totalFeeInr)} total",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = VahfNavyDeep
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.NotificationsActive,
                                            contentDescription = null,
                                            tint = VahfCyanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "1st WhatsApp fee alert: ${feeBreakdown.nextDueDateFormatted} (${FeeAutomationHelper.formatInr(feeBreakdown.monthlyFeeInr)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VahfTextSecondary
                                        )
                                    }

                                    feeBreakdown.installments.take(3).forEach { inst ->
                                        Text(
                                            text = "• Month ${inst.monthNumber}: ${inst.dueDateFormatted} — ${FeeAutomationHelper.formatInr(inst.amountInr)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VahfNavyDeep
                                        )
                                    }
                                    if (feeBreakdown.installments.size > 3) {
                                        Text(
                                            text = "+ ${feeBreakdown.installments.size - 3} more monthly WhatsApp reminders scheduled automatically",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VahfTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 12. Do you have any health issues?
                        RegistrationInputField(
                            label = "Do you have any health issues?",
                            required = false,
                            value = healthIssues,
                            onValueChange = { if (it.length <= 1000) healthIssues = it },
                            singleLine = false,
                            minLines = 4,
                            testTag = "reg_health_issues_input"
                        )

                        // 13. What are the expectations from us?
                        RegistrationInputField(
                            label = "What are the expectations from us?",
                            required = false,
                            value = expectations,
                            onValueChange = { if (it.length <= 1000) expectations = it },
                            singleLine = false,
                            minLines = 4,
                            testTag = "reg_expectations_input"
                        )

                        // 14. Gender *
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FormRequiredLabel(text = "Gender", required = true)
                            VahfConfig.REGISTRATION_GENDER_OPTIONS.forEach { option ->
                                val selected = gender == option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = {
                                                gender = option
                                                genderError = null
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = VahfNavyDeep,
                                            unselectedColor = VahfTextMuted
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = VahfTextPrimary
                                    )
                                }
                            }
                            if (genderError != null) {
                                FieldErrorText(genderError!!)
                            }
                        }

                        // 15. Marital Status :
                        RegistrationInputField(
                            label = "Marital Status :",
                            required = false,
                            value = maritalStatus,
                            onValueChange = { if (it.length <= 40) maritalStatus = it },
                            testTag = "reg_marital_status_input"
                        )

                        // 16. Height
                        RegistrationInputField(
                            label = "Height",
                            required = false,
                            value = height,
                            onValueChange = { if (it.length <= 40) height = it },
                            testTag = "reg_height_input"
                        )

                        // 17. Weight
                        RegistrationInputField(
                            label = "Weight",
                            required = false,
                            value = weight,
                            onValueChange = { if (it.length <= 40) weight = it },
                            testTag = "reg_weight_input"
                        )

                        // 18. Date of Joining
                        RegistrationInputField(
                            label = "Date of Joining",
                            required = false,
                            value = dateOfJoining,
                            onValueChange = { if (it.length <= 30) dateOfJoining = it },
                            placeholder = "DD/MM/YYYY",
                            trailingIcon = {
                                IconButton(
                                    onClick = { showJoiningDatePicker = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = "Pick Date of Joining",
                                        tint = VahfCyanPrimary
                                    )
                                }
                            },
                            testTag = "reg_joining_date_input"
                        )

                        // 19. Time
                        RegistrationInputField(
                            label = "Time",
                            required = false,
                            value = preferredTime,
                            onValueChange = { if (it.length <= 30) preferredTime = it },
                            placeholder = "e.g. 07:00 AM",
                            trailingIcon = {
                                IconButton(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Pick Session Time",
                                        tint = VahfCyanPrimary
                                    )
                                }
                            },
                            testTag = "reg_time_input"
                        )

                        // 20. Are you taking any medication ? please share the details.
                        RegistrationInputField(
                            label = "Are you taking any medication ? please share the details.",
                            required = false,
                            value = medicationDetails,
                            onValueChange = { if (it.length <= 1000) medicationDetails = it },
                            singleLine = false,
                            minLines = 4,
                            testTag = "reg_medication_input"
                        )

                        // 21. How did you hear about VANGMAY YOGA FITNESS
                        RegistrationInputField(
                            label = "How did you hear about VANGMAY YOGA FITNESS",
                            required = false,
                            value = referralSource,
                            onValueChange = { if (it.length <= 500) referralSource = it },
                            singleLine = false,
                            minLines = 4,
                            testTag = "reg_referral_input"
                        )

                        // 22. Terms & Conditions *
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormRequiredLabel(text = "Terms & Conditions", required = true)

                            Text(
                                text = stringResource(id = R.string.registration_terms_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = VahfTextSecondary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .toggleable(
                                        value = termsAccepted,
                                        role = Role.Checkbox,
                                        onValueChange = { checked ->
                                            termsAccepted = checked
                                            if (checked && termsSignature.isBlank()) {
                                                termsSignature = "I Agree"
                                            }
                                            termsError = null
                                        }
                                    )
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = VahfNavyDeep,
                                        uncheckedColor = VahfTextMuted,
                                        checkmarkColor = VahfWhite
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I accept the Terms & Conditions",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = VahfNavyDeep
                                )
                            }

                            OutlinedTextField(
                                value = termsSignature,
                                onValueChange = {
                                    if (it.length <= 80) {
                                        termsSignature = it
                                        if (it.trim().length >= 2) termsAccepted = true
                                        termsError = null
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "Type 'I Agree' or your full name",
                                        color = VahfTextMuted
                                    )
                                },
                                singleLine = true,
                                isError = termsError != null,
                                shape = RoundedCornerShape(10.dp),
                                colors = vahfFormFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_terms_input")
                            )

                            if (termsError != null) {
                                FieldErrorText(termsError!!)
                            }
                        }

                        // 23. Upload Medical Report (up to 6 files)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormRequiredLabel(text = "Upload Medical Report", required = false)

                            DashedUploadBox(
                                isProcessing = isProcessingReports,
                                subHint = "You can upload up to 6 files.",
                                onClick = {
                                    medicalReportLauncher.launch(
                                        arrayOf("image/*", "application/pdf")
                                    )
                                },
                                modifier = Modifier.testTag("reg_medical_upload_box")
                            )

                            if (reportUploadHint != null) {
                                Text(
                                    text = reportUploadHint!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VahfCyanPrimary
                                )
                            }

                            if (medicalAttachments.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    medicalAttachments.forEachIndexed { index, attachment ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = VahfOffWhite,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, VahfBorderSubtle, RoundedCornerShape(12.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Description,
                                                    contentDescription = null,
                                                    tint = VahfCyanPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = attachment.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = VahfNavyDeep,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        medicalAttachments = medicalAttachments
                                                            .toMutableList()
                                                            .apply { removeAt(index) }
                                                        reportUploadHint = null
                                                    },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Remove ${attachment.displayName}",
                                                        tint = VahfTextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 24. Submit Button (aligned cleanly on left/full-responsive matching screenshot)
                        val isSubmitting = submitState is RegistrationSubmitState.Submitting
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { validateAndSubmit() },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VahfNavyDeep,
                                    contentColor = VahfWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("reg_submit_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = VahfWhite,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Submitting...")
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.registration_submit_button),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                VahfSupportContactCard(
                    onCallNumber = { phone ->
                        RegistrationNotificationHelper.dialSupportNumber(context, phone)
                    },
                    onEmailSupport = {
                        RegistrationNotificationHelper.openAdminEmailNotification(
                            context = context,
                            emailSubject = "VAHF Registration Help & Inquiry",
                            emailBody = "Hello VAHF Support Team,\n\nI need assistance regarding the VAHF Registration Form.\n"
                        )
                    },
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegistrationSuccessCard(
    result: com.example.data.RegistrationSubmissionResult,
    onSendEmail: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendStudentWhatsApp: () -> Unit,
    onCallSupport: (String) -> Unit,
    onSubmitAnother: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VahfWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .border(1.dp, VahfBorderSubtle, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("reg_success_card"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(VahfSuccess.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = VahfSuccess,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Registration & Fee Automation Active!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = VahfNavyDeep,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Thank you, ${result.applicantName}. Your form is saved, your mobile number is verified, and monthly WhatsApp fee reminders (${result.joiningMonths} Month(s)) are scheduled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VahfTextSecondary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VahfOffWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VahfBorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Reference ID: ${result.registrationId}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = VahfNavyDeep
                        )
                        Text(
                            text = "Verified WhatsApp: +${result.verifiedWhatsappNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VahfSuccess
                        )
                        Text(
                            text = "Fee Plan: ${result.joiningMonths} Month(s) • ${FeeAutomationHelper.formatInr(result.monthlyFeeInr)}/month (Total: ${FeeAutomationHelper.formatInr(result.totalFeeInr)})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = VahfNavyDeep
                        )
                        Text(
                            text = "Next Monthly Reminder: ${result.nextDueDateFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VahfTextSecondary
                        )
                        Text(
                            text = "Notification Recipient: ${VahfConfig.ADMIN_EMAIL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VahfTextSecondary
                        )
                        if (result.webhookDispatched) {
                            Text(
                                text = "Automated email/webhook alert delivered.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VahfSuccess
                            )
                        }
                    }
                }

                Button(
                    onClick = onSendEmail,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VahfNavyDeep,
                        contentColor = VahfWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reg_send_email_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Email to ${VahfConfig.ADMIN_EMAIL}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onSendStudentWhatsApp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reg_send_student_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = VahfSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Fee Plan on Student WhatsApp",
                        style = MaterialTheme.typography.labelLarge,
                        color = VahfNavyDeep
                    )
                }

                OutlinedButton(
                    onClick = onSendWhatsApp,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reg_send_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = VahfCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notify Studio on WhatsApp",
                        style = MaterialTheme.typography.labelLarge,
                        color = VahfNavyDeep
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onSubmitAnother,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("New Registration", color = VahfCyanPrimary)
                    }
                    TextButton(
                        onClick = onBackHome,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Back to Home", color = VahfNavyDeep)
                    }
                }
            }
        }

        VahfSupportContactCard(
            onCallNumber = onCallSupport,
            onEmailSupport = onSendEmail,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}

@Composable
private fun FormRequiredLabel(
    text: String,
    required: Boolean
) {
    Text(
        text = buildAnnotatedString {
            append(text)
            if (required) {
                append(" ")
                withStyle(SpanStyle(color = VahfError, fontWeight = FontWeight.Bold)) {
                    append("*")
                }
            }
        },
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        color = VahfTextPrimary
    )
}

@Composable
private fun RegistrationInputField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormRequiredLabel(text = label, required = required)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            placeholder = placeholder?.let { hint ->
                { Text(text = hint, color = VahfTextMuted) }
            },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(10.dp),
            colors = vahfFormFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (error != null) {
            FieldErrorText(error)
        }
    }
}

@Composable
private fun DashedUploadBox(
    isProcessing: Boolean,
    subHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashColor = VahfTextMuted.copy(alpha = 0.55f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VahfWhite)
            .clickable(enabled = !isProcessing, onClick = onClick)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
            drawRoundRect(
                color = dashColor,
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = stroke
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HeaderTintBackground),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = VahfCyanPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = "Upload file",
                        tint = VahfTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.registration_drop_hint),
                style = MaterialTheme.typography.titleMedium,
                color = VahfTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = subHint,
                style = MaterialTheme.typography.bodyMedium,
                color = VahfTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusAlertBanner(
    message: String,
    isError: Boolean
) {
    val tint = if (isError) VahfError else VahfSuccess
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.09f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = tint
            )
        }
    }
}

@Composable
private fun FieldErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = VahfError,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun vahfFormFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VahfCyanPrimary,
    unfocusedBorderColor = VahfBorderSubtle,
    errorBorderColor = VahfError,
    focusedContainerColor = VahfWhite,
    unfocusedContainerColor = VahfWhite,
    cursorColor = VahfNavyDeep
)

private fun formatUtcMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(millis))
}

private fun formatTime12Hour(hour24: Int, minute: Int): String {
    val period = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, period)
}
