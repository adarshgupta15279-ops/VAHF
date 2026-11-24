package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.VahfConfig
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfOffWhite
import com.example.ui.theme.VahfTextMuted
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite

@Composable
fun VahfSupportContactCard(
    onCallNumber: (String) -> Unit,
    onEmailSupport: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Need Any Help?",
    subtitle: String = "If you need any help with registration, classes, or your account, contact VAHF support on call or email anytime."
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VahfWhite,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, VahfBorderSubtle, RoundedCornerShape(20.dp))
            .testTag("vahf_support_contact_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VahfCyanSoftContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        tint = VahfNavyDeep,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = VahfNavyDeep
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = VahfTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onCallNumber(VahfConfig.SUPPORT_PHONE_PRIMARY) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("support_call_primary")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Call ${VahfConfig.SUPPORT_PHONE_PRIMARY_DISPLAY}",
                        tint = VahfCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = VahfConfig.SUPPORT_PHONE_PRIMARY,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = VahfNavyDeep
                    )
                }

                OutlinedButton(
                    onClick = { onCallNumber(VahfConfig.SUPPORT_PHONE_SECONDARY) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("support_call_secondary")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Call ${VahfConfig.SUPPORT_PHONE_SECONDARY_DISPLAY}",
                        tint = VahfCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = VahfConfig.SUPPORT_PHONE_SECONDARY,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = VahfNavyDeep
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VahfOffWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onEmailSupport)
                    .border(1.dp, VahfBorderSubtle, RoundedCornerShape(12.dp))
                    .testTag("support_email_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = VahfCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Email VAHF Support (Tap to mail)",
                            style = MaterialTheme.typography.labelSmall,
                            color = VahfTextMuted
                        )
                        Text(
                            text = VahfConfig.ADMIN_EMAIL,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = VahfNavyDeep
                        )
                    }
                }
            }
        }
    }
}
