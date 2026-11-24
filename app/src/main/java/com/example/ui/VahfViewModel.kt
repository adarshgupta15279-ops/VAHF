package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RegistrationDraft
import com.example.data.RegistrationSubmission
import com.example.data.RegistrationSubmitState
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VahfClassItem
import com.example.data.VahfConfig
import com.example.data.VahfFeedPost
import com.example.data.VahfLiveSession
import com.example.data.VahfRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VahfViewModel(
    private val repository: VahfRepository,
    private val currentUserId: String,
    val defaultDisplayName: String,
    val defaultEmail: String
) : ViewModel() {

    private val _likeInFlightIds = MutableStateFlow<Set<String>>(emptySet())
    val likeInFlightIds: StateFlow<Set<String>> = _likeInFlightIds.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _isSendingComment = MutableStateFlow(false)
    val isSendingComment: StateFlow<Boolean> = _isSendingComment.asStateFlow()

    private val _registrationSubmitState = MutableStateFlow<RegistrationSubmitState>(RegistrationSubmitState.Idle)
    val registrationSubmitState: StateFlow<RegistrationSubmitState> = _registrationSubmitState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load student profile."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val registrationsState: StateFlow<UiState<List<RegistrationSubmission>>> = repository
        .observeUserRegistrations(currentUserId)
        .map<List<RegistrationSubmission>, UiState<List<RegistrationSubmission>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing registrations", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load registrations."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val feedState: StateFlow<UiState<List<VahfFeedPost>>> = repository.observePublishedRemotePosts()
        .onStart { emit(emptyList()) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Remote feed posts unavailable, using bundled studio photos", error)
            emit(emptyList())
        }
        .flatMapLatest { remotePosts ->
            val basePosts = VahfConfig.bundledPosts + remotePosts.map { remote ->
                VahfFeedPost(
                    id = remote.id,
                    category = remote.category.ifBlank { "Studio" },
                    caption = remote.caption,
                    imageUrl = remote.imageUrl
                )
            }
            val postFlows = basePosts.map { base ->
                combine(
                    repository.observePostLikes(base.id)
                        .catch { error ->
                            val fe = unwrapFirestoreError(error)
                            if (fe == null && error is CancellationException) throw error
                            Log.w(TAG, "Likes stream fallback for ${base.id}", error)
                            emit(emptyList())
                        },
                    repository.observePostComments(base.id)
                        .catch { error ->
                            val fe = unwrapFirestoreError(error)
                            if (fe == null && error is CancellationException) throw error
                            Log.w(TAG, "Comments stream fallback for ${base.id}", error)
                            emit(emptyList())
                        }
                ) { likes, comments ->
                    base.copy(
                        likeCount = likes.size,
                        isLikedByCurrentUser = likes.any { it.userId == currentUserId },
                        comments = comments
                    )
                }
            }
            combine(postFlows) { it.toList() }
        }
        .map<List<VahfFeedPost>, UiState<List<VahfFeedPost>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing feed", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load VAHF photo feed."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val classesState: StateFlow<UiState<List<VahfClassItem>>> = repository.observePublishedClasses()
        .map<List<VahfClassItem>, UiState<List<VahfClassItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing classes", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load classes."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val liveSessionsState: StateFlow<UiState<List<VahfLiveSession>>> = repository.observePublishedLiveSessions()
        .map<List<VahfLiveSession>, UiState<List<VahfLiveSession>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing live sessions", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load live sessions."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun toggleLike(post: VahfFeedPost) {
        if (_likeInFlightIds.value.contains(post.id)) return
        viewModelScope.launch {
            _likeInFlightIds.update { it + post.id }
            val result = repository.setPostLiked(
                postId = post.id,
                liked = !post.isLikedByCurrentUser
            )
            result.onFailure {
                _statusBanner.value = "Could not update like. Please try again."
            }
            _likeInFlightIds.update { it - post.id }
        }
    }

    fun addComment(postId: String, commentText: String, onSuccess: () -> Unit = {}) {
        val trimmed = commentText.trim()
        if (trimmed.isEmpty()) {
            _statusBanner.value = "Please enter a comment before posting."
            return
        }
        if (trimmed.length > 500) {
            _statusBanner.value = "Comment cannot exceed 500 characters."
            return
        }
        val profileName = (userProfileState.value as? UiState.Success)?.data?.fullName
            ?.takeIf { it.isNotBlank() }
            ?: defaultDisplayName.takeIf { it.isNotBlank() }
            ?: "VAHF Student"

        viewModelScope.launch {
            _isSendingComment.value = true
            val result = repository.addComment(postId, profileName, trimmed)
            result.onSuccess {
                onSuccess()
            }.onFailure {
                _statusBanner.value = "Could not post comment. Please try again."
            }
            _isSendingComment.value = false
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            val result = repository.deleteComment(postId, commentId)
            result.onFailure {
                _statusBanner.value = "Could not remove comment."
            }
        }
    }

    fun saveStudentProfile(
        fullName: String,
        email: String,
        mobileNumber: String,
        city: String,
        wellnessGoal: String
    ) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            val result = repository.saveUserProfile(
                userId = currentUserId,
                fullName = fullName,
                email = email,
                mobileNumber = mobileNumber,
                city = city,
                wellnessGoal = wellnessGoal
            )
            result.onSuccess {
                _statusBanner.value = "Student profile saved successfully."
            }.onFailure { error ->
                _statusBanner.value = error.localizedMessage ?: "Failed to save profile."
            }
            _isSavingProfile.value = false
        }
    }

    fun submitRegistration(draft: RegistrationDraft) {
        if (_registrationSubmitState.value is RegistrationSubmitState.Submitting) return
        viewModelScope.launch {
            _registrationSubmitState.value = RegistrationSubmitState.Submitting
            val result = repository.submitRegistration(draft)
            result.onSuccess { submissionResult ->
                _registrationSubmitState.value = RegistrationSubmitState.Success(submissionResult)
                _statusBanner.value = "Registration submitted! Sending details to VAHF Admin."
            }.onFailure { error ->
                val message = error.localizedMessage ?: "Could not submit registration. Please try again."
                _registrationSubmitState.value = RegistrationSubmitState.Error(message)
                _statusBanner.value = message
            }
        }
    }

    fun markRegistrationNotificationSent(registrationId: String, channel: String) {
        viewModelScope.launch {
            repository.markRegistrationNotificationDispatched(registrationId, channel)
        }
    }

    fun markMonthlyFeeSent(registration: RegistrationSubmission, monthIndex: Int) {
        viewModelScope.launch {
            repository.markMonthlyFeeNotified(registration, monthIndex)
                .onSuccess {
                    _statusBanner.value = "Month $monthIndex fee reminder recorded for ${registration.fullName}."
                }
                .onFailure {
                    _statusBanner.value = "Could not update monthly fee reminder status."
                }
        }
    }

    fun resetRegistrationSubmitState() {
        _registrationSubmitState.value = RegistrationSubmitState.Idle
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "VahfViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
