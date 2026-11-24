package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.VahfConfig
import com.example.ui.components.VahfBrandedDarkBackdrop
import com.example.ui.components.VahfOriginalLogoBadge
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanBright
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfOffWhite
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebsiteScreen(
    onDarkOverlayVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasCompletedInitialLoad by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorDescription by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var pendingFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val showLoadingOverlay = isLoading && !hasCompletedInitialLoad && !hasError

    val webFileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFilePathCallback
        pendingFilePathCallback = null
        if (callback == null) return@rememberLauncherForActivityResult

        val data = result.data
        val uris: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK && data != null) {
            val clipData = data.clipData
            when {
                clipData != null && clipData.itemCount > 0 -> {
                    Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
                }
                data.data != null -> arrayOf(data.data!!)
                else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            }
        } else {
            null
        }
        callback.onReceiveValue(uris)
    }

    LaunchedEffect(showLoadingOverlay) {
        onDarkOverlayVisibilityChanged(showLoadingOverlay)
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFilePathCallback?.onReceiveValue(null)
            pendingFilePathCallback = null
            onDarkOverlayVisibilityChanged(false)
        }
    }

    val webView = remember(context) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
    }

    // Safety guard so a slow/stalled page never leaves the loading screen stuck permanently
    LaunchedEffect(isLoading, hasCompletedInitialLoad) {
        if (isLoading && !hasCompletedInitialLoad) {
            delay(18000L)
            if (isLoading && !hasCompletedInitialLoad) {
                isLoading = false
                hasCompletedInitialLoad = true
                if (progress < 25) {
                    hasError = true
                    errorDescription = "The website took too long to respond. Please check your connection and retry."
                }
            }
        }
    }

    DisposableEffect(webView) {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress
                if (newProgress >= 90 && !hasError) {
                    isLoading = false
                    hasCompletedInitialLoad = true
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                pendingFilePathCallback?.onReceiveValue(null)
                pendingFilePathCallback = filePathCallback

                val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                val rawAcceptTokens = fileChooserParams?.acceptTypes
                    ?.flatMap { it.split(",") }
                    ?.map { it.trim().lowercase() }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()

                val mappedMimeTypes = rawAcceptTokens.mapNotNull { token ->
                    when {
                        token.contains("/") -> token
                        token == ".jpg" || token == ".jpeg" -> "image/jpeg"
                        token == ".png" -> "image/png"
                        token == ".webp" -> "image/webp"
                        token == ".heic" -> "image/heic"
                        token == ".pdf" -> "application/pdf"
                        token == ".doc" -> "application/msword"
                        token == ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        else -> null
                    }
                }.distinct()

                val hasUnmappedExtension = rawAcceptTokens.isNotEmpty() &&
                    mappedMimeTypes.size < rawAcceptTokens.size

                val chooserIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = if (!hasUnmappedExtension && mappedMimeTypes.size == 1) {
                        mappedMimeTypes.first()
                    } else {
                        "*/*"
                    }
                    if (!hasUnmappedExtension && mappedMimeTypes.size > 1) {
                        putExtra(Intent.EXTRA_MIME_TYPES, mappedMimeTypes.toTypedArray())
                    }
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
                }

                return try {
                    webFileChooserLauncher.launch(Intent.createChooser(chooserIntent, "Select File"))
                    true
                } catch (_: Exception) {
                    val fallbackIntent = runCatching { fileChooserParams?.createIntent() }.getOrNull()
                    if (fallbackIntent != null) {
                        try {
                            webFileChooserLauncher.launch(fallbackIntent)
                            true
                        } catch (_: Exception) {
                            pendingFilePathCallback?.onReceiveValue(null)
                            pendingFilePathCallback = null
                            false
                        }
                    } else {
                        pendingFilePathCallback?.onReceiveValue(null)
                        pendingFilePathCallback = null
                        false
                    }
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString().orEmpty()
                val isHttpUrl = url.startsWith("http://") || url.startsWith("https://")
                val isWhatsAppLink = url.startsWith("https://wa.me/") ||
                    url.startsWith("https://api.whatsapp.com/")
                val isInternalWebScheme = url.startsWith("about:") ||
                    url.startsWith("javascript:") ||
                    url.startsWith("data:") ||
                    url.startsWith("blob:")

                if (isWhatsAppLink || (!isHttpUrl && !isInternalWebScheme && url.isNotBlank())) {
                    return try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    } catch (_: Exception) {
                        !isHttpUrl
                    }
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                hasError = false
                errorDescription = null
                isLoading = true
                canGoBack = view?.canGoBack() ?: false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
                hasCompletedInitialLoad = true
                canGoBack = view?.canGoBack() ?: false
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    isLoading = false
                    hasError = true
                    errorDescription = "Unable to load the VAHF website right now. Please check your internet connection."
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                isLoading = false
                hasError = true
                errorDescription = "Secure connection to the VAHF website could not be established."
            }
        }

        if (webView.url == null) {
            webView.loadUrl(VahfConfig.WEBSITE_URL)
        }

        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite)
    ) {
        if (!hasError) {
            AndroidView(
                factory = { webView },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .testTag("vahf_webview")
            )
        }

        if (isLoading && hasCompletedInitialLoad && !hasError) {
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0.08f, 1f) },
                color = VahfCyanPrimary,
                trackColor = VahfCyanPrimary.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
            )
        }

        if (hasError) {
            WebsiteErrorState(
                message = errorDescription
                    ?: "Unable to load the VAHF website right now. Please try again.",
                onRetry = {
                    hasError = false
                    errorDescription = null
                    progress = 0
                    isLoading = true
                    hasCompletedInitialLoad = false
                    webView.loadUrl(VahfConfig.WEBSITE_URL)
                }
            )
        }

        // Full-screen branded VAHF Loading Overlay (automatically hides once loaded)
        androidx.compose.animation.AnimatedVisibility(
            visible = showLoadingOverlay,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(350))
        ) {
            WebsiteLoadingOverlay(progress = progress)
        }
    }
}

@Composable
private fun WebsiteLoadingOverlay(progress: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    VahfBrandedDarkBackdrop(
        modifier = Modifier.testTag("website_loading_overlay"),
        showBottomLotus = true
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .widthIn(max = 380.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VahfOriginalLogoBadge(size = 236.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.loading_please_wait),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = VahfWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.loading_website),
                style = MaterialTheme.typography.bodyLarge,
                color = VahfCyanBright,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (progress > 0) {
                LinearProgressIndicator(
                    progress = { (progress / 100f).coerceIn(0.05f, 1f) },
                    color = VahfCyanBright,
                    trackColor = VahfWhite.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            } else {
                LinearProgressIndicator(
                    color = VahfCyanBright,
                    trackColor = VahfWhite.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun WebsiteErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = VahfOffWhite,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .border(1.dp, VahfBorderSubtle, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = VahfCyanPrimary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Website Unavailable",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VahfNavyDeep,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VahfTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VahfNavyDeep,
                        contentColor = VahfWhite
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("website_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.retry_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
