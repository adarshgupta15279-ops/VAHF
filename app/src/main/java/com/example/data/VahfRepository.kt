package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DuplicateRegistrationException(message: String) : IllegalStateException(message)

class VahfRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google to continue.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(
        userId: String,
        fullName: String,
        email: String,
        mobileNumber: String,
        city: String,
        wellnessGoal: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch; refusing cross-user write." }

            val cleanName = fullName.trim()
            val cleanEmail = email.trim()
            val cleanMobile = mobileNumber.trim()
            val cleanCity = city.trim()
            val cleanGoal = wellnessGoal.trim()

            require(cleanName.length in 2..80) { "Full name must be between 2 and 80 characters." }
            require(cleanEmail.length in 3..120) { "Email must be between 3 and 120 characters." }
            require(cleanMobile.length <= 20) { "Mobile number must be 20 characters or less." }
            require(cleanCity.length <= 80) { "City must be 80 characters or less." }
            require(cleanGoal.length <= 120) { "Wellness goal must be 120 characters or less." }

            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()

            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to cleanName,
                    "email" to cleanEmail,
                    "mobileNumber" to cleanMobile,
                    "city" to cleanCity,
                    "wellnessGoal" to cleanGoal,
                    "registrationCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to cleanName,
                    "email" to cleanEmail,
                    "mobileNumber" to cleanMobile,
                    "city" to cleanCity,
                    "wellnessGoal" to cleanGoal,
                    "registrationCompleted" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserRegistrations(userId: String): Flow<List<RegistrationSubmission>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "registrations"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RegistrationSubmission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitRegistration(draft: RegistrationDraft): Result<RegistrationSubmissionResult> {
        val currentUid = auth.currentUser?.uid ?: "unauthenticated"
        var docPath = "registrations/${currentUid}_pending"
        return try {
            val uid = requireUserId()

            val cleanName = draft.fullName.trim()
            val cleanEmail = draft.email.trim()
            val cleanPhone = draft.phoneNumber.trim()
            val cleanOccupation = draft.occupation.trim()
            val cleanAddress = draft.address.trim()
            val cleanGender = draft.gender.trim()
            val cleanTermsSig = draft.termsSignature.trim()

            require(draft.profileImageBase64.length in 16..200_000) {
                "Please upload a valid applicant photo."
            }
            require(cleanName.length in 2..80) { "Name must be between 2 and 80 characters." }
            require(cleanEmail.length in 3..120) { "Please enter a valid email address." }
            require(cleanPhone.length in 7..20) { "Please enter a valid phone number." }
            require(draft.phoneVerified && draft.verifiedWhatsappNumber.length in 10..20) {
                "Please enter a valid 10-digit mobile number for instant verification."
            }
            require(draft.joiningMonths in 1..36) { "Please choose between 1 and 36 joining months." }
            require(draft.monthlyFeeInr in 100..500_000) { "Monthly fee must be between ₹100 and ₹5,00,000." }
            require(draft.totalFeeInr in 100..18_000_000) { "Total fee is out of valid bounds." }
            require(cleanOccupation.length in 2..80) { "Occupation must be between 2 and 80 characters." }
            require(cleanAddress.length in 3..250) { "Address must be between 3 and 250 characters." }
            require(cleanGender in VahfConfig.REGISTRATION_GENDER_OPTIONS) { "Please select Gender." }
            require(draft.termsAccepted && cleanTermsSig.length in 2..80) {
                "Please accept the Terms & Conditions and enter your confirmation."
            }
            require(draft.selectedClasses.size <= 8) { "Too many class selections." }
            require(draft.selectedPackages.size <= 4) { "Too many package selections." }
            require(draft.medicalReportNames.size <= 6 && draft.medicalReportPreviews.size <= 6) {
                "You can upload up to 6 medical report files."
            }

            val submissionHash = RegistrationNotificationHelper.computeSubmissionHash(draft)
            val registrationId = "${uid}_${submissionHash.take(24)}"
            val docRef = db.collection("registrations").document(registrationId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.failure(
                    DuplicateRegistrationException(
                        "You have already submitted a registration with these exact details."
                    )
                )
            }

            val sanitizedDraft = draft.copy(
                profileImageName = draft.profileImageName.trim().ifBlank { "applicant_photo.jpg" }.take(120),
                fullName = cleanName,
                email = cleanEmail,
                phoneNumber = cleanPhone,
                occupation = cleanOccupation,
                address = cleanAddress,
                ageYears = draft.ageYears.trim().take(10),
                dateOfBirth = draft.dateOfBirth.trim().take(30),
                membersCount = draft.membersCount.trim().take(10),
                healthIssues = draft.healthIssues.trim().take(1000),
                expectations = draft.expectations.trim().take(1000),
                gender = cleanGender,
                maritalStatus = draft.maritalStatus.trim().take(40),
                height = draft.height.trim().take(40),
                weight = draft.weight.trim().take(40),
                dateOfJoining = draft.dateOfJoining.trim().take(30),
                preferredTime = draft.preferredTime.trim().take(30),
                medicationDetails = draft.medicationDetails.trim().take(1000),
                referralSource = draft.referralSource.trim().take(500),
                termsSignature = cleanTermsSig
            )

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "submissionHash" to submissionHash,
                "profileImageBase64" to sanitizedDraft.profileImageBase64,
                "profileImageName" to sanitizedDraft.profileImageName,
                "fullName" to sanitizedDraft.fullName,
                "email" to sanitizedDraft.email,
                "phoneNumber" to sanitizedDraft.phoneNumber,
                "phoneVerified" to sanitizedDraft.phoneVerified,
                "verifiedWhatsappNumber" to sanitizedDraft.verifiedWhatsappNumber.take(20),
                "occupation" to sanitizedDraft.occupation,
                "address" to sanitizedDraft.address,
                "joiningMonths" to sanitizedDraft.joiningMonths,
                "monthlyFeeInr" to sanitizedDraft.monthlyFeeInr,
                "totalFeeInr" to sanitizedDraft.totalFeeInr,
                "feeAutomationEnabled" to true,
                "nextDueDateMillis" to sanitizedDraft.nextDueDateMillis.coerceAtLeast(0L),
                "lastNotifiedMonth" to 0,
                "selectedClasses" to sanitizedDraft.selectedClasses,
                "selectedPackages" to sanitizedDraft.selectedPackages,
                "gender" to sanitizedDraft.gender,
                "termsAccepted" to true,
                "termsSignature" to sanitizedDraft.termsSignature,
                "medicalReportNames" to sanitizedDraft.medicalReportNames,
                "medicalReportPreviews" to sanitizedDraft.medicalReportPreviews,
                "notificationChannel" to "email",
                "notificationDispatched" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (sanitizedDraft.ageYears.isNotEmpty()) payload["ageYears"] = sanitizedDraft.ageYears
            if (sanitizedDraft.dateOfBirth.isNotEmpty()) payload["dateOfBirth"] = sanitizedDraft.dateOfBirth
            if (sanitizedDraft.membersCount.isNotEmpty()) payload["membersCount"] = sanitizedDraft.membersCount
            if (sanitizedDraft.healthIssues.isNotEmpty()) payload["healthIssues"] = sanitizedDraft.healthIssues
            if (sanitizedDraft.expectations.isNotEmpty()) payload["expectations"] = sanitizedDraft.expectations
            if (sanitizedDraft.maritalStatus.isNotEmpty()) payload["maritalStatus"] = sanitizedDraft.maritalStatus
            if (sanitizedDraft.height.isNotEmpty()) payload["height"] = sanitizedDraft.height
            if (sanitizedDraft.weight.isNotEmpty()) payload["weight"] = sanitizedDraft.weight
            if (sanitizedDraft.dateOfJoining.isNotEmpty()) payload["dateOfJoining"] = sanitizedDraft.dateOfJoining
            if (sanitizedDraft.preferredTime.isNotEmpty()) payload["preferredTime"] = sanitizedDraft.preferredTime
            if (sanitizedDraft.medicationDetails.isNotEmpty()) payload["medicationDetails"] = sanitizedDraft.medicationDetails
            if (sanitizedDraft.referralSource.isNotEmpty()) payload["referralSource"] = sanitizedDraft.referralSource

            docRef.set(payload).await()

            // Mark student profile registration summary completed if needed (best effort)
            runCatching {
                val userRef = db.collection("users").document(uid)
                val userSnap = userRef.get().await()
                if (userSnap.exists()) {
                    userRef.update(
                        mapOf(
                            "fullName" to cleanName,
                            "email" to cleanEmail,
                            "mobileNumber" to cleanPhone,
                            "registrationCompleted" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    userRef.set(
                        mapOf(
                            "userId" to uid,
                            "fullName" to cleanName,
                            "email" to cleanEmail,
                            "mobileNumber" to cleanPhone,
                            "city" to cleanAddress.take(80),
                            "wellnessGoal" to sanitizedDraft.expectations.take(120),
                            "registrationCompleted" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val emailSubject = RegistrationNotificationHelper.buildProfessionalAdminEmailSubject(
                applicantName = sanitizedDraft.fullName,
                registrationId = registrationId
            )
            val emailBody = RegistrationNotificationHelper.buildProfessionalAdminEmailBody(
                registrationId = registrationId,
                draft = sanitizedDraft
            )
            val notificationMessage = RegistrationNotificationHelper.buildAdminNotificationMessage(
                registrationId = registrationId,
                draft = sanitizedDraft
            )

            val webhookSent = RegistrationNotificationHelper.postAdminWebhookNotification(
                registrationId = registrationId,
                draft = sanitizedDraft,
                emailSubject = emailSubject,
                emailBody = emailBody,
                formattedMessage = notificationMessage
            )

            if (webhookSent) {
                runCatching {
                    docRef.update(
                        mapOf(
                            "notificationChannel" to "webhook+email",
                            "notificationDispatched" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val nextDueDateFormatted = FeeAutomationHelper.formatDateDdMmmYyyy(sanitizedDraft.nextDueDateMillis)
            val phoneVerifyResult = FeeAutomationHelper.verifyPhoneNumber(sanitizedDraft.phoneNumber)
            val studentWelcomeMessage = FeeAutomationHelper.buildStudentWelcomeWhatsAppMessage(
                studentName = sanitizedDraft.fullName,
                verifiedDisplayPhone = phoneVerifyResult.formattedDisplay.ifBlank { sanitizedDraft.phoneNumber },
                joiningMonths = sanitizedDraft.joiningMonths,
                monthlyFeeInr = sanitizedDraft.monthlyFeeInr,
                totalFeeInr = sanitizedDraft.totalFeeInr,
                nextDueDateText = nextDueDateFormatted,
                selectedClasses = sanitizedDraft.selectedClasses
            )

            Result.success(
                RegistrationSubmissionResult(
                    registrationId = registrationId,
                    applicantName = sanitizedDraft.fullName,
                    verifiedWhatsappNumber = sanitizedDraft.verifiedWhatsappNumber,
                    joiningMonths = sanitizedDraft.joiningMonths,
                    monthlyFeeInr = sanitizedDraft.monthlyFeeInr,
                    totalFeeInr = sanitizedDraft.totalFeeInr,
                    nextDueDateMillis = sanitizedDraft.nextDueDateMillis,
                    nextDueDateFormatted = nextDueDateFormatted,
                    studentWelcomeWhatsAppMessage = studentWelcomeMessage,
                    emailSubject = emailSubject,
                    emailBody = emailBody,
                    notificationMessage = notificationMessage,
                    webhookDispatched = webhookSent
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markRegistrationNotificationDispatched(
        registrationId: String,
        channel: String
    ): Result<Unit> {
        val path = "registrations/$registrationId"
        return try {
            requireUserId()
            require(registrationId.isNotBlank()) { "Invalid registration ID." }
            db.collection("registrations").document(registrationId)
                .update(
                    mapOf(
                        "notificationChannel" to channel.take(40),
                        "notificationDispatched" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMonthlyFeeNotified(
        registration: RegistrationSubmission,
        notifiedMonth: Int
    ): Result<Unit> {
        val path = "registrations/${registration.id}"
        return try {
            requireUserId()
            require(registration.id.isNotBlank()) { "Invalid registration ID." }
            val safeMonth = notifiedMonth.coerceIn(0, registration.joiningMonths.coerceAtLeast(1))
            val baseMillis = FeeAutomationHelper.parseJoiningDateMillis(
                registration.dateOfJoining,
                registration.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            )
            val upcomingMonth = safeMonth + 1
            val nextDueMillis = if (upcomingMonth <= registration.joiningMonths) {
                FeeAutomationHelper.addMonthsAtMorningHour(baseMillis, upcomingMonth)
            } else {
                registration.nextDueDateMillis
            }
            db.collection("registrations").document(registration.id)
                .update(
                    mapOf(
                        "lastNotifiedMonth" to safeMonth,
                        "nextDueDateMillis" to nextDueMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePostLikes(postId: String): Flow<List<PostLike>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/likes"
        emitAll(
            db.collection("posts").document(postId).collection("likes")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PostLike::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setPostLiked(postId: String, liked: Boolean): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "posts/$postId/likes/$currentUid"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank() && postId.length <= 128) { "Invalid post ID." }
            val likeRef = db.collection("posts").document(postId)
                .collection("likes").document(uid)

            if (liked) {
                val payload = mapOf(
                    "userId" to uid,
                    "postId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                likeRef.set(payload).await()
            } else {
                likeRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (liked) OperationType.CREATE else OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observePostComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PostComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(postId: String, authorName: String, text: String): Result<Unit> {
        val path = "posts/$postId/comments"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank() && postId.length <= 128) { "Invalid post ID." }
            val cleanAuthor = authorName.trim().ifBlank { "VAHF Member" }.take(80)
            val cleanText = text.trim()
            require(cleanText.length in 1..500) { "Comment must be between 1 and 500 characters." }

            val commentRef = db.collection("posts").document(postId)
                .collection("comments").document()
            val payload = mapOf(
                "userId" to uid,
                "postId" to postId,
                "authorName" to cleanAuthor,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            commentRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val path = "posts/$postId/comments/$commentId"
        return try {
            requireUserId()
            require(postId.isNotBlank() && commentId.isNotBlank()) { "Invalid comment reference." }
            db.collection("posts").document(postId)
                .collection("comments").document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePublishedClasses(): Flow<List<VahfClassItem>> = flow {
        val uid = requireUserId()
        val path = "classes"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VahfClassItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublishedLiveSessions(): Flow<List<VahfLiveSession>> = flow {
        val uid = requireUserId()
        val path = "live_sessions"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VahfLiveSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublishedRemotePosts(): Flow<List<RemoteFeedPost>> = flow {
        val uid = requireUserId()
        val path = "feed_posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RemoteFeedPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sortOrder }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }
}
