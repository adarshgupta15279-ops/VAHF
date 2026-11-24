package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.VahfRepository
import com.example.ui.VahfViewModel
import com.example.ui.auth.authStateFlow
import com.example.ui.components.VahfBrandedDarkBackdrop
import com.example.ui.components.VahfOriginalLogoBadge
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.VahfMainScreen
import com.example.ui.theme.VahfCyanBright
import com.example.ui.theme.VahfTheme
import com.example.ui.theme.VahfWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            VahfTheme {
                VahfAppNavigation()
            }
        }
    }
}

@Composable
fun VahfAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    val activity = LocalContext.current as? ComponentActivity
    val view = LocalView.current
    var websiteNeedsDarkStatusBar by remember { mutableStateOf(false) }
    var isWebsiteFullScreen by remember { mutableStateOf(false) }
    var showAppLaunchSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (showAppLaunchSplash) {
            delay(1350L)
            showAppLaunchSplash = false
        }
    }

    val useDarkStatusBar = showAppLaunchSplash || user == null || websiteNeedsDarkStatusBar

    LaunchedEffect(useDarkStatusBar, showAppLaunchSplash, user, isWebsiteFullScreen) {
        if (useDarkStatusBar) {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = if (showAppLaunchSplash || user == null) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
            )
        } else {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            )
        }

        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            if (isWebsiteFullScreen && !showAppLaunchSplash && user != null) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (user == null) {
            if (!showAppLaunchSplash) {
                AuthScreen(onAuthSuccess = { })
            }
        } else {
            val uid = user.uid
            key(uid) {
                val viewModel: VahfViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            VahfViewModel(
                                repository = VahfRepository(db),
                                currentUserId = uid,
                                defaultDisplayName = user.displayName.orEmpty(),
                                defaultEmail = user.email.orEmpty()
                            )
                        }
                    }
                )
                VahfMainScreen(
                    viewModel = viewModel,
                    currentUserId = uid,
                    onUseDarkStatusBarIconsChanged = { websiteNeedsDarkStatusBar = it },
                    onWebsiteFullScreenChanged = { isWebsiteFullScreen = it }
                )
            }
        }

        AnimatedVisibility(
            visible = showAppLaunchSplash,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            AppMainLoadingScreen()
        }
    }
}

@Composable
private fun AppMainLoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "appSplashPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "appSplashAlpha"
    )

    VahfBrandedDarkBackdrop(
        modifier = Modifier.testTag("app_main_loading_screen"),
        showBottomLotus = true
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .widthIn(max = 400.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large original VAHF logo for the app's main startup loading screen
            VahfOriginalLogoBadge(size = 320.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(id = R.string.loading_app_please_wait),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = VahfWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.brand_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = VahfCyanBright,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                color = VahfCyanBright,
                trackColor = VahfWhite.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}
