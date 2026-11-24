package com.example.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.FeeAutomationHelper
import com.example.data.RegistrationNotificationHelper
import com.example.data.RegistrationSubmission
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.auth.signOut
import com.example.ui.components.VahfSupportContactCard
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfError
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfOffWhite
import com.example.ui.theme.VahfSuccess
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite

@Composable
fun ProfileScreen(
    userProfileState: UiState<UserProfile?>,
    registrationsState: UiState<List<RegistrationSubmission>>,
    defaultName: String,
    defaultEmail: String,
    isSaving: Boolean,
    onMarkMonthlyFeeSent: (RegistrationSubmission, Int) -> Unit,
    onSaveProfile: (
        fullName: String,
        email: String,
        mobileNumber: String,
        city: String,
        wellnessGoal: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val loadedProfile = (userProfileState as? UiState.Success)?.data

    var fullName by remember { mutableStateOf(defaultName) }
    var email by remember { mutableStateOf(defaultEmail) }
    var mobileNumber by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var wellnessGoal by remember { mutableStateOf("") }
    var initializedFromProfile by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loadedProfile) {
        if (loadedProfile != null) {
            fullName = loadedProfile.fullName
            email = loadedProfile.email
            mobileNumber = loadedProfile.mobileNumber
            city = loadedProfile.city
            wellnessGoal = loadedProfile.wellnessGoal
            initializedFromProfile = true
        } else if (!initializedFromProfile) {
            if (defaultName.isNotBlank()) fullName = defaultName
            if (defaultEmail.isNotBlank()) email = defaultEmail
        }
    }

    fun validateAndSave() {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanMobile = mobileNumber.trim()

        nameError = when {
            cleanName.length < 2 -> "Please enter your full name (at least 2 characters)."
            cleanName.length > 80 -> "Full name must be 80 characters or fewer."
            else -> null
        }
        emailError = when {
            cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                "Please enter a valid email address."
            cleanEmail.length > 120 -> "Email must be 120 characters or fewer."
            else -> null
        }
        mobileError = when {
            cleanMobile.isNotEmpty() && !cleanMobile.matches(Regex("^[+]?[0-9\\s\\-]{7,18}$")) ->
                "Please enter a valid mobile number (7-18 digits)."
            else -> null
        }

        if (nameError == null && emailError == null && mobileError == null) {
            onSaveProfile(cleanName, cleanEmail, cleanMobile, city.trim(), wellnessGoal.trim())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .testTag("profile_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Summary Header
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = VahfNavyDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VahfCyanPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (fullName.ifBlank { defaultEmail }.firstOrNull() ?: 'V')
                                .uppercaseChar()
                                .toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = VahfWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank { "VAHF Student" },
                            style = MaterialTheme.typography.titleLarge,
                            color = VahfWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email.ifBlank { defaultEmail },
                            style = MaterialTheme.typography.bodySmall,
                            color = VahfWhite.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (loadedProfile?.registrationCompleted == true) {
                                VahfSuccess.copy(alpha = 0.25f)
                            } else {
                                VahfCyanPrimary.copy(alpha = 0.25f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = VahfCyanSoftContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (loadedProfile?.registrationCompleted == true) {
                                        "Registered VAHF Student"
                                    } else {
                                        "Complete Student Registration"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VahfWhite
                                )
                            }
                        }
                    }
                }
            }

            if (userProfileState is UiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = VahfCyanPrimary
                    )
                }
            }

            if (userProfileState is UiState.Error) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VahfError.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = userProfileState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = VahfError,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Student Registration & Profile Form
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = VahfWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VahfBorderSubtle, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Student Registration & Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = VahfNavyDeep
                    )
                    Text(
                        text = "Keep your VAHF student information accurate and up to date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VahfTextSecondary
                    )

                    ProfileTextField(
                        value = fullName,
                        onValueChange = {
                            if (it.length <= 80) {
                                fullName = it
                                nameError = null
                            }
                        },
                        label = "Full Name *",
                        leadingIcon = Icons.Outlined.Badge,
                        errorMessage = nameError,
                        modifier = Modifier.testTag("profile_name_input")
                    )

                    ProfileTextField(
                        value = email,
                        onValueChange = {
                            if (it.length <= 120) {
                                email = it
                                emailError = null
                            }
                        },
                        label = "Email Address *",
                        leadingIcon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                        errorMessage = emailError,
                        modifier = Modifier.testTag("profile_email_input")
                    )

                    ProfileTextField(
                        value = mobileNumber,
                        onValueChange = {
                            if (it.length <= 20) {
                                mobileNumber = it
                                mobileError = null
                            }
                        },
                        label = "Mobile Number",
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone,
                        errorMessage = mobileError,
                        modifier = Modifier.testTag("profile_mobile_input")
                    )

                    ProfileTextField(
                        value = city,
                        onValueChange = { if (it.length <= 80) city = it },
                        label = "City / Studio Center",
                        leadingIcon = Icons.Outlined.LocationCity,
                        modifier = Modifier.testTag("profile_city_input")
                    )

                    ProfileTextField(
                        value = wellnessGoal,
                        onValueChange = { if (it.length <= 120) wellnessGoal = it },
                        label = "Yoga & Wellness Goal",
                        leadingIcon = Icons.Outlined.FitnessCenter,
                        modifier = Modifier.testTag("profile_goal_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { validateAndSave() },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VahfNavyDeep,
                            contentColor = VahfWhite
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VahfWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Saving...")
                        } else {
                            Text(
                                text = if (loadedProfile?.registrationCompleted == true) {
                                    "Update Profile"
                                } else {
                                    "Complete Registration"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Active Registrations & Monthly WhatsApp Fee Automation Cards
            val activeRegistrations = (registrationsState as? UiState.Success)?.data.orEmpty()
            if (activeRegistrations.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = VahfWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VahfBorderSubtle, RoundedCornerShape(22.dp))
                        .testTag("profile_fee_automation_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = VahfCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Fee & WhatsApp Automation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VahfNavyDeep
                            )
                        }

                        activeRegistrations.forEach { reg ->
                            val targetWhatsapp = reg.verifiedWhatsappNumber.ifBlank {
                                FeeAutomationHelper.verifyPhoneNumber(reg.phoneNumber).whatsappE164
                            }
                            val nextMonth = (reg.lastNotifiedMonth + 1).coerceAtMost(reg.joiningMonths.coerceAtLeast(1))
                            val isPlanDone = reg.lastNotifiedMonth >= reg.joiningMonths
                            val dueDateLabel = FeeAutomationHelper.formatDateDdMmmYyyy(reg.nextDueDateMillis)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = VahfOffWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, VahfBorderSubtle, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = reg.fullName.ifBlank { "Student" },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = VahfNavyDeep
                                        )
                                        if (reg.phoneVerified) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Verified,
                                                    contentDescription = "Verified",
                                                    tint = VahfSuccess,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Verified",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = VahfSuccess
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Plan: ${reg.joiningMonths} Month(s) • ${FeeAutomationHelper.formatInr(reg.monthlyFeeInr)}/month (Total: ${FeeAutomationHelper.formatInr(reg.totalFeeInr)})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = VahfNavyDeep
                                    )

                                    if (targetWhatsapp.isNotBlank()) {
                                        Text(
                                            text = "WhatsApp: +$targetWhatsapp",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VahfTextSecondary
                                        )
                                    }

                                    Text(
                                        text = if (isPlanDone) {
                                            "All ${reg.joiningMonths} monthly WhatsApp fee reminders completed."
                                        } else {
                                            "Cycle: Month $nextMonth of ${reg.joiningMonths} • Next Due: $dueDateLabel"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPlanDone) VahfSuccess else VahfCyanPrimary
                                    )

                                    if (targetWhatsapp.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                val msg = FeeAutomationHelper.buildStudentMonthlyFeeWhatsAppMessage(
                                                    studentName = reg.fullName,
                                                    monthIndex = nextMonth,
                                                    joiningMonths = reg.joiningMonths,
                                                    monthlyFeeInr = reg.monthlyFeeInr,
                                                    totalFeeInr = reg.totalFeeInr,
                                                    dueDateText = dueDateLabel,
                                                    classesSummary = reg.selectedClasses.joinToString(", ")
                                                )
                                                val sent = RegistrationNotificationHelper.openWhatsAppToNumber(
                                                    context = context,
                                                    phoneE164 = targetWhatsapp,
                                                    message = msg
                                                )
                                                if (sent && !isPlanDone) {
                                                    onMarkMonthlyFeeSent(reg, nextMonth)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VahfNavyDeep,
                                                contentColor = VahfWhite
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("profile_send_whatsapp_fee_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isPlanDone) {
                                                    "Resend Fee Summary (${FeeAutomationHelper.formatInr(reg.monthlyFeeInr)})"
                                                } else {
                                                    "Send Month $nextMonth Fee (${FeeAutomationHelper.formatInr(reg.monthlyFeeInr)}) on WhatsApp"
                                                },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            VahfSupportContactCard(
                title = "Need Any Help?",
                subtitle = "For any help with your account, classes, or registration, contact VAHF support on call or email anytime.",
                onCallNumber = { phone ->
                    RegistrationNotificationHelper.dialSupportNumber(context, phone)
                },
                onEmailSupport = {
                    RegistrationNotificationHelper.openAdminEmailNotification(
                        context = context,
                        emailSubject = "VAHF Student Support Inquiry",
                        emailBody = "Hello VAHF Support Team,\n\nI need assistance regarding my VAHF profile or registration.\n\nStudent Name: ${fullName.ifBlank { "VAHF Student" }}\nEmail: ${email.ifBlank { defaultEmail }}\n"
                    )
                }
            )

            OutlinedButton(
                onClick = {
                    signOut(credentialManager = credentialManager, scope = scope)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VahfError),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.sign_out_label),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorMessage: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = VahfCyanPrimary
                )
            },
            isError = errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VahfCyanPrimary,
                unfocusedBorderColor = VahfBorderSubtle,
                focusedContainerColor = VahfOffWhite,
                unfocusedContainerColor = VahfOffWhite,
                focusedLabelColor = VahfNavyDeep
            ),
            modifier = modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = VahfError,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }
}
