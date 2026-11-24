package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UiState
import com.example.data.VahfClassItem
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfError
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfOffWhite
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite

@Composable
fun ClassesScreen(
    classesState: UiState<List<VahfClassItem>>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite),
        contentAlignment = Alignment.TopCenter
    ) {
        when (classesState) {
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
                        text = classesState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VahfError
                    )
                }
            }

            is UiState.Success -> {
                val classes = classesState.data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("classes_screen"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(
                                text = "Online Classes",
                                style = MaterialTheme.typography.headlineMedium,
                                color = VahfNavyDeep
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Guided VAHF Yoga & Wellness programs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VahfTextSecondary
                            )
                        }
                    }

                    if (classes.isEmpty()) {
                        item {
                            ComingSoonSectionCard(
                                icon = Icons.Outlined.SelfImprovement,
                                sectionTitle = "Online Classes",
                                description = "Structured online yoga sessions will appear here once published."
                            )
                        }
                        item {
                            ComingSoonSectionCard(
                                icon = Icons.Outlined.CalendarMonth,
                                sectionTitle = "Class Schedule",
                                description = "Official weekly class timings and batch schedules will be shared here soon."
                            )
                        }
                    } else {
                        items(classes, key = { it.id }) { item ->
                            PublishedClassCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishedClassCard(item: VahfClassItem) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VahfWhite,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VahfBorderSubtle, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (item.category.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = VahfCyanSoftContainer
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = VahfNavyDeep,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = VahfNavyDeep
            )

            if (item.scheduleText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = VahfCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.scheduleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VahfTextSecondary
                    )
                }
            }

            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VahfTextSecondary
                )
            }
        }
    }
}

@Composable
internal fun ComingSoonSectionCard(
    icon: ImageVector,
    sectionTitle: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = VahfOffWhite,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VahfBorderSubtle, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VahfCyanSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VahfNavyDeep,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleLarge,
                color = VahfNavyDeep,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = VahfNavyDeep
            ) {
                Text(
                    text = stringResource(id = R.string.coming_soon_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = VahfWhite,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = VahfTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
