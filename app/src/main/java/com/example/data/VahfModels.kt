package com.example.data

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val city: String = "",
    val wellnessGoal: String = "",
    val registrationCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VahfClassItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val scheduleText: String = "",
    val category: String = "",
    val description: String = "",
    val published: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class VahfLiveSession(
    @DocumentId val id: String = "",
    val title: String = "",
    val scheduleText: String = "",
    val meetingUrl: String = "",
    val published: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class RemoteFeedPost(
    @DocumentId val id: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val sortOrder: Long = 0L,
    val published: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class RegistrationSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val submissionHash: String = "",
    val profileImageBase64: String = "",
    val profileImageName: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val verifiedWhatsappNumber: String = "",
    val occupation: String = "",
    val address: String = "",
    val ageYears: String = "",
    val dateOfBirth: String = "",
    val membersCount: String = "",
    val joiningMonths: Int = 1,
    val monthlyFeeInr: Int = VahfConfig.DEFAULT_MONTHLY_FEE_INR,
    val totalFeeInr: Int = VahfConfig.DEFAULT_MONTHLY_FEE_INR,
    val feeAutomationEnabled: Boolean = true,
    val nextDueDateMillis: Long = 0L,
    val lastNotifiedMonth: Int = 0,
    val selectedClasses: List<String> = emptyList(),
    val selectedPackages: List<String> = emptyList(),
    val healthIssues: String = "",
    val expectations: String = "",
    val gender: String = "",
    val maritalStatus: String = "",
    val height: String = "",
    val weight: String = "",
    val dateOfJoining: String = "",
    val preferredTime: String = "",
    val medicationDetails: String = "",
    val referralSource: String = "",
    val termsAccepted: Boolean = false,
    val termsSignature: String = "",
    val medicalReportNames: List<String> = emptyList(),
    val medicalReportPreviews: List<String> = emptyList(),
    val notificationChannel: String = "whatsapp",
    val notificationDispatched: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MedicalReportAttachment(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val compactPreviewBase64: String? = null
)

data class RegistrationDraft(
    val profileImageBase64: String,
    val profileImageName: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val phoneVerified: Boolean,
    val verifiedWhatsappNumber: String,
    val occupation: String,
    val address: String,
    val ageYears: String,
    val dateOfBirth: String,
    val membersCount: String,
    val joiningMonths: Int,
    val monthlyFeeInr: Int,
    val totalFeeInr: Int,
    val nextDueDateMillis: Long,
    val selectedClasses: List<String>,
    val selectedPackages: List<String>,
    val healthIssues: String,
    val expectations: String,
    val gender: String,
    val maritalStatus: String,
    val height: String,
    val weight: String,
    val dateOfJoining: String,
    val preferredTime: String,
    val medicationDetails: String,
    val referralSource: String,
    val termsAccepted: Boolean,
    val termsSignature: String,
    val medicalReportNames: List<String>,
    val medicalReportPreviews: List<String>
)

data class RegistrationSubmissionResult(
    val registrationId: String,
    val applicantName: String,
    val verifiedWhatsappNumber: String,
    val joiningMonths: Int,
    val monthlyFeeInr: Int,
    val totalFeeInr: Int,
    val nextDueDateMillis: Long,
    val nextDueDateFormatted: String,
    val studentWelcomeWhatsAppMessage: String,
    val emailSubject: String,
    val emailBody: String,
    val notificationMessage: String,
    val webhookDispatched: Boolean
)

sealed interface RegistrationSubmitState {
    data object Idle : RegistrationSubmitState
    data object Submitting : RegistrationSubmitState
    data class Success(val result: RegistrationSubmissionResult) : RegistrationSubmitState
    data class Error(val message: String) : RegistrationSubmitState
}

data class VahfFeedPost(
    val id: String,
    val caption: String,
    val category: String,
    @DrawableRes val imageRes: Int? = null,
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val comments: List<PostComment> = emptyList()
) {
    val commentCount: Int get() = comments.size
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
