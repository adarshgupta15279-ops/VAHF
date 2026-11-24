package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.data.VahfLiveSession
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfError
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite

@Composable
fun LiveScreen(
    liveSessionsState: UiState<List<VahfLiveSession>>
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite),
        contentAlignment = Alignment.TopCenter
    ) {
        when (liveSessionsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VahfCyanPrimary)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = liveSessionsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VahfError
                    )
                }
            }

            is UiState.Success -> {
                val sessions = liveSessionsState.data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("live_screen"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(
                                text = "Upcoming Live Classes",
                                style = MaterialTheme.typography.headlineMedium,
                                color = VahfNavyDeep
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Interactive live yoga & wellness sessions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VahfTextSecondary
                            )
                        }
                    }

                    if (sessions.isEmpty()) {
                        item {
                            ComingSoonSectionCard(
                                icon = Icons.Outlined.LiveTv,
                                sectionTitle = "Upcoming Live Classes",
                                description = "When a live VAHF session is scheduled with a real meeting link, a Join Live button will appear right here."
                            )
                        }
                    } else {
                        items(sessions, key = { it.id }) { session ->
                            LiveSessionCard(
                                session = session,
                                onJoinLive = { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSessionCard(
    session: VahfLiveSession,
    onJoinLive: (String) -> Unit
) {
    val validUrl = session.meetingUrl.trim().takeIf {
        it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VahfWhite,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VahfBorderSubtle, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = VahfCyanSoftContainer
            ) {
                Text(
                    text = if (validUrl != null) "Live Access Ready" else "Scheduled",
                    style = MaterialTheme.typography.labelMedium,
                    color = VahfNavyDeep,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleLarge,
                color = VahfNavyDeep
            )

            if (session.scheduleText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = VahfCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.scheduleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VahfTextSecondary
                    )
                }
            }

            if (validUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onJoinLive(validUrl) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VahfNavyDeep,
                        contentColor = VahfWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("join_live_button_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join Live",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
