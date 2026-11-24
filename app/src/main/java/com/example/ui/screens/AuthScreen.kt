package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.GoogleGIcon
import com.example.ui.components.VahfBrandedDarkBackdrop
import com.example.ui.theme.VahfCyanBright
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfWhite

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var emailOrMobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showGooglePromptDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("Sign In with Google") }
    var dialogBody by remember { mutableStateOf("") }

    fun triggerGoogleSignIn() {
        errorMessage = null
        isSigningIn = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isSigningIn = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isSigningIn = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isSigningIn = false
            }
        )
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope
        )
    }

    VahfBrandedDarkBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Exact original VAHF logo with transparent background (zero black box)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle cyan side divider lines matching the screenshot tagline flanks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = VahfCyanPrimary.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.width(230.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = VahfCyanPrimary.copy(alpha = 0.45f)
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.vahf_logo_transparent),
                        contentDescription = "VAHF Official Logo - Prevention, Care and Awareness",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(194.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Welcome Back!",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 31.sp
                    ),
                    color = VahfWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Login to continue your wellness journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD6E4F0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Email or Mobile Number field matching screenshot styling
                DarkGlassTextField(
                    value = emailOrMobile,
                    onValueChange = {
                        emailOrMobile = it
                        errorMessage = null
                    },
                    placeholder = "Email or Mobile Number",
                    leadingIcon = Icons.Rounded.Person,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field with visibility toggle matching screenshot styling
                DarkGlassTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    placeholder = "Password",
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag("login_password_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                val isBusy = isCheckingAutoSignIn || isSigningIn

                // Cyan -> Sky Blue gradient "Login ->" pill button matching screenshot
                Button(
                    onClick = {
                        val idInput = emailOrMobile.trim()
                        val passInput = password.trim()
                        when {
                            idInput.isEmpty() -> {
                                errorMessage = "Please enter your Email or Mobile Number."
                            }
                            passInput.isEmpty() -> {
                                errorMessage = "Please enter your Password."
                            }
                            else -> {
                                errorMessage = null
                                dialogTitle = "Continue with Google Sign-In"
                                dialogBody =
                                    "VAHF uses your Google Account for secure authentication and syncing likes, comments, and student profiles. Tap Continue to sign in with Google."
                                showGooglePromptDialog = true
                            }
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00F2EA),
                                    Color(0xFF0AD0FA),
                                    Color(0xFF1FA6FF)
                                )
                            )
                        )
                        .testTag("login_submit_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF041322)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF041322),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "OR" circular divider matching screenshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0xFF476379)
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(34.dp)
                            .border(1.dp, Color(0xFF9EB4C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = VahfWhite
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0xFF476379)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Continue with Google" outlined pill button matching screenshot
                OutlinedButton(
                    onClick = { triggerGoogleSignIn() },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.2.dp,
                        color = Color(0xFFBFD2E2)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF03111E).copy(alpha = 0.55f),
                        contentColor = VahfWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VahfCyanBright,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCheckingAutoSignIn) "Checking session..." else "Signing in...",
                            style = MaterialTheme.typography.titleMedium,
                            color = VahfWhite
                        )
                    } else {
                        GoogleGIcon()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = VahfWhite
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Color(0xFFF87171).copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFECACA),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // "Forgot Password? | Create New Account" links matching screenshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = VahfCyanBright,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isBusy) {
                                dialogTitle = "Account Access & Recovery"
                                dialogBody =
                                    "Your VAHF account signs in securely via Google Sign-In, so you do not need a separate password. Tap Continue with Google to access or recover your account."
                                showGooglePromptDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("forgot_password_link")
                    )

                    Text(
                        text = "|",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7B93A8),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Text(
                        text = "Create New Account",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = VahfWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isBusy) {
                                dialogTitle = "Create New VAHF Account"
                                dialogBody =
                                    "Tap Continue with Google to create your VAHF student account instantly, then complete your student profile details inside the Profile tab."
                                showGooglePromptDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("create_account_link")
                    )
                }

                Spacer(modifier = Modifier.height(122.dp))
            }
        }
    }

    if (showGooglePromptDialog) {
        AlertDialog(
            onDismissRequest = { showGooglePromptDialog = false },
            containerColor = VahfWhite,
            titleContentColor = VahfNavyDeep,
            textContentColor = Color(0xFF334155),
            title = {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = dialogBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGooglePromptDialog = false
                        triggerGoogleSignIn()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VahfNavyDeep,
                        contentColor = VahfWhite
                    )
                ) {
                    Text("Continue with Google")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGooglePromptDialog = false }) {
                    Text("Cancel", color = VahfNavyDeep)
                }
            }
        )
    }
}

@Composable
private fun DarkGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val visualTransformation = if (isPassword && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF041422).copy(alpha = 0.72f))
            .border(1.dp, Color(0xFF476379), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = Color(0xFFD6E4F0),
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF9EB4C7)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = VahfWhite),
                cursorBrush = SolidColor(VahfCyanBright),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isPassword && onTogglePasswordVisibility != null) {
            IconButton(
                onClick = onTogglePasswordVisibility,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Outlined.Visibility
                    } else {
                        Icons.Outlined.VisibilityOff
                    },
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = Color(0xFFD6E4F0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
