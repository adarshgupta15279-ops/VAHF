package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.AppRegistration
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.FeeReminderScheduler
import com.example.data.UiState
import com.example.ui.VahfViewModel
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfLoadingBlack
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfTextMuted
import com.example.ui.theme.VahfWhite

enum class VahfTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    WEBSITE(R.string.nav_website, Icons.Filled.Language, Icons.Outlined.Language, "nav_website"),
    CLASSES(R.string.nav_classes, Icons.Filled.SelfImprovement, Icons.Outlined.SelfImprovement, "nav_classes"),
    LIVE(R.string.nav_live, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_live"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun VahfMainScreen(
    viewModel: VahfViewModel,
    currentUserId: String,
    onUseDarkStatusBarIconsChanged: (Boolean) -> Unit = {},
    onWebsiteFullScreenChanged: (Boolean) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(VahfTab.HOME) }
    var isRegistrationOpen by rememberSaveable { mutableStateOf(false) }
    var isWebsiteDarkLoadingVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val registrationsState by viewModel.registrationsState.collectAsStateWithLifecycle()
    val classesState by viewModel.classesState.collectAsStateWithLifecycle()
    val liveSessionsState by viewModel.liveSessionsState.collectAsStateWithLifecycle()
    val registrationSubmitState by viewModel.registrationSubmitState.collectAsStateWithLifecycle()
    val likeInFlightIds by viewModel.likeInFlightIds.collectAsStateWithLifecycle()
    val isSendingComment by viewModel.isSendingComment.collectAsStateWithLifecycle()
    val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    LaunchedEffect(registrationsState) {
        val registrations = (registrationsState as? UiState.Success)?.data ?: return@LaunchedEffect
        FeeReminderScheduler.syncAllReminders(context, registrations)
    }

    val isWebsiteFullScreen = !isRegistrationOpen && selectedTab == VahfTab.WEBSITE

    LaunchedEffect(isWebsiteFullScreen) {
        onWebsiteFullScreenChanged(isWebsiteFullScreen)
    }

    DisposableEffect(Unit) {
        onDispose {
            onWebsiteFullScreenChanged(false)
        }
    }

    LaunchedEffect(selectedTab, isWebsiteDarkLoadingVisible, isRegistrationOpen) {
        onUseDarkStatusBarIconsChanged(
            !isRegistrationOpen && selectedTab == VahfTab.WEBSITE && isWebsiteDarkLoadingVisible
        )
    }

    LaunchedEffect(statusBanner) {
        val message = statusBanner
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusBanner()
        }
    }

    // Back press closes Registration Form first, or returns secondary tabs to Home cleanly
    BackHandler(enabled = isRegistrationOpen || selectedTab != VahfTab.HOME) {
        if (isRegistrationOpen) {
            isRegistrationOpen = false
        } else {
            selectedTab = VahfTab.HOME
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = VahfWhite,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isWebsiteFullScreen) {
                    VahfTopBar(
                        isRegistrationOpen = isRegistrationOpen,
                        onOpenRegistration = {
                            if (isRegistrationOpen) {
                                isRegistrationOpen = false
                            } else {
                                viewModel.resetRegistrationSubmitState()
                                isRegistrationOpen = true
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useRail && !isWebsiteFullScreen) {
                    VahfBottomBar(
                        selectedTab = selectedTab,
                        isRegistrationOpen = isRegistrationOpen,
                        onSelectTab = { tab ->
                            isRegistrationOpen = false
                            selectedTab = tab
                        }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isWebsiteFullScreen) {
                    NavigationRail(
                        containerColor = VahfWhite,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        VahfTab.entries.forEach { tab ->
                            val isSelected = !isRegistrationOpen && tab == selectedTab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    isRegistrationOpen = false
                                    selectedTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.titleRes)
                                    )
                                },
                                label = { Text(stringResource(id = tab.titleRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = VahfNavyDeep,
                                    selectedTextColor = VahfNavyDeep,
                                    indicatorColor = VahfCyanSoftContainer,
                                    unselectedIconColor = VahfTextMuted,
                                    unselectedTextColor = VahfTextMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isRegistrationOpen) {
                        val loadedProfile = (userProfileState as? UiState.Success)?.data
                        RegistrationFormScreen(
                            loadedProfile = loadedProfile,
                            defaultName = viewModel.defaultDisplayName,
                            defaultEmail = viewModel.defaultEmail,
                            submitState = registrationSubmitState,
                            onSubmitRegistration = viewModel::submitRegistration,
                            onNotificationDispatched = viewModel::markRegistrationNotificationSent,
                            onResetSubmitState = viewModel::resetRegistrationSubmitState,
                            onBackToHome = {
                                isRegistrationOpen = false
                                selectedTab = VahfTab.HOME
                            }
                        )
                    } else {
                        when (selectedTab) {
                            VahfTab.HOME -> HomeScreen(
                                feedState = feedState,
                                currentUserId = currentUserId,
                                likeInFlightIds = likeInFlightIds,
                                isSendingComment = isSendingComment,
                                onToggleLike = viewModel::toggleLike,
                                onAddComment = viewModel::addComment,
                                onDeleteComment = viewModel::deleteComment
                            )

                            VahfTab.WEBSITE -> WebsiteScreen(
                                onDarkOverlayVisibilityChanged = { isVisible ->
                                    isWebsiteDarkLoadingVisible = isVisible
                                }
                            )

                            VahfTab.CLASSES -> ClassesScreen(classesState = classesState)

                            VahfTab.LIVE -> LiveScreen(liveSessionsState = liveSessionsState)

                            VahfTab.PROFILE -> ProfileScreen(
                                userProfileState = userProfileState,
                                registrationsState = registrationsState,
                                defaultName = viewModel.defaultDisplayName,
                                defaultEmail = viewModel.defaultEmail,
                                isSaving = isSavingProfile,
                                onMarkMonthlyFeeSent = viewModel::markMonthlyFeeSent,
                                onSaveProfile = viewModel::saveStudentProfile
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VahfTopBar(
    isRegistrationOpen: Boolean,
    onOpenRegistration: () -> Unit
) {
    Surface(
        color = VahfWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Original VAHF Logo badge preserved without alteration
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(VahfLoadingBlack)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_logo),
                        contentDescription = "VAHF Official Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = VahfNavyDeep
                    )
                    Text(
                        text = stringResource(id = R.string.brand_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = VahfCyanPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onOpenRegistration,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isRegistrationOpen) VahfNavyDeep else VahfCyanPrimary.copy(alpha = 0.7f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRegistrationOpen) VahfNavyDeep else VahfCyanSoftContainer.copy(alpha = 0.65f),
                        contentColor = if (isRegistrationOpen) VahfWhite else VahfNavyDeep
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("top_bar_registration_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AppRegistration,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.registration_nav_button),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider(color = VahfBorderSubtle, thickness = 0.8.dp)
        }
    }
}

@Composable
private fun VahfBottomBar(
    selectedTab: VahfTab,
    isRegistrationOpen: Boolean,
    onSelectTab: (VahfTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = VahfBorderSubtle, thickness = 0.8.dp)
        NavigationBar(
            containerColor = VahfWhite,
            tonalElevation = 0.dp
        ) {
            VahfTab.entries.forEach { tab ->
                val isSelected = !isRegistrationOpen && tab == selectedTab
                val label = stringResource(id = tab.titleRes)
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = label
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VahfNavyDeep,
                        selectedTextColor = VahfNavyDeep,
                        indicatorColor = VahfCyanSoftContainer,
                        unselectedIconColor = VahfTextMuted,
                        unselectedTextColor = VahfTextMuted
                    ),
                    modifier = Modifier.testTag(tab.testTag)
                )
            }
        }
    }
}
