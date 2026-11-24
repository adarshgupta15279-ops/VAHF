package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.PostComment
import com.example.data.UiState
import com.example.data.VahfConfig
import com.example.data.VahfFeedPost
import com.example.ui.theme.VahfBorderSubtle
import com.example.ui.theme.VahfCyanPrimary
import com.example.ui.theme.VahfCyanSoftContainer
import com.example.ui.theme.VahfError
import com.example.ui.theme.VahfLikeActive
import com.example.ui.theme.VahfNavyDeep
import com.example.ui.theme.VahfNavyMid
import com.example.ui.theme.VahfOffWhite
import com.example.ui.theme.VahfTextMuted
import com.example.ui.theme.VahfTextSecondary
import com.example.ui.theme.VahfWhite
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    feedState: UiState<List<VahfFeedPost>>,
    currentUserId: String,
    likeInFlightIds: Set<String>,
    isSendingComment: Boolean,
    onToggleLike: (VahfFeedPost) -> Unit,
    onAddComment: (postId: String, text: String, onSuccess: () -> Unit) -> Unit,
    onDeleteComment: (postId: String, commentId: String) -> Unit
) {
    val context = LocalContext.current
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VahfWhite),
        contentAlignment = Alignment.TopCenter
    ) {
        when (feedState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                        text = feedState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VahfError
                    )
                }
            }

            is UiState.Success -> {
                val posts = feedState.data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("home_photo_feed"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(
                                text = "Studio & Community",
                                style = MaterialTheme.typography.headlineMedium,
                                color = VahfNavyDeep
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Real moments from VAHF Yoga & Wellness",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VahfTextSecondary
                            )
                        }
                    }

                    items(posts, key = { it.id }) { post ->
                        FeedPostCard(
                            post = post,
                            isLikePending = likeInFlightIds.contains(post.id),
                            onLikeClick = { onToggleLike(post) },
                            onCommentClick = { activeCommentPostId = post.id },
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "VAHF Yoga & Wellness"
                                    )
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${post.caption}\n\nVAHF – Prevention, Care and Awareness\n${VahfConfig.WEBSITE_URL}"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share VAHF Moment")
                                )
                            }
                        )
                    }
                }

                val selectedPost = posts.firstOrNull { it.id == activeCommentPostId }
                if (selectedPost != null) {
                    CommentsBottomSheet(
                        post = selectedPost,
                        currentUserId = currentUserId,
                        isSendingComment = isSendingComment,
                        onDismiss = { activeCommentPostId = null },
                        onSendComment = { text, clearInput ->
                            onAddComment(selectedPost.id, text, clearInput)
                        },
                        onDeleteComment = { commentId ->
                            onDeleteComment(selectedPost.id, commentId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPostCard(
    post: VahfFeedPost,
    isLikePending: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val heartScale by animateFloatAsState(
        targetValue = if (post.isLikedByCurrentUser) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "likeScale"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = VahfWhite,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = VahfNavyDeep.copy(alpha = 0.08f),
                spotColor = VahfNavyDeep.copy(alpha = 0.08f)
            )
            .border(1.dp, VahfBorderSubtle, RoundedCornerShape(22.dp))
            .testTag("feed_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Real uploaded photograph preserving natural high resolution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(VahfOffWhite)
            ) {
                when {
                    post.imageRes != null -> {
                        Image(
                            painter = painterResource(id = post.imageRes),
                            contentDescription = post.caption,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    !post.imageUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = post.caption,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = VahfNavyDeep.copy(alpha = 0.82f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = VahfWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = VahfNavyDeep
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = VahfBorderSubtle, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onLikeClick,
                            enabled = !isLikePending,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("like_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (post.isLikedByCurrentUser) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = if (post.isLikedByCurrentUser) "Unlike post" else "Like post",
                                tint = if (post.isLikedByCurrentUser) VahfLikeActive else VahfNavyMid,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(heartScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${post.likeCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (post.isLikedByCurrentUser) VahfLikeActive else VahfNavyDeep
                            )
                        }

                        TextButton(
                            onClick = onCommentClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("comment_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = VahfNavyMid,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${post.commentCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = VahfNavyDeep
                            )
                        }
                    }

                    TextButton(
                        onClick = onShareClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share post",
                            tint = VahfCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelLarge,
                            color = VahfNavyDeep
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    post: VahfFeedPost,
    currentUserId: String,
    isSendingComment: Boolean,
    onDismiss: () -> Unit,
    onSendComment: (String, onSuccess: () -> Unit) -> Unit,
    onDeleteComment: (commentId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentDraft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VahfWhite,
        contentColor = VahfNavyDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Comments (${post.commentCount})",
                style = MaterialTheme.typography.titleLarge,
                color = VahfNavyDeep
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Visible to registered VAHF members",
                style = MaterialTheme.typography.bodySmall,
                color = VahfTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = VahfBorderSubtle)

            if (post.comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Be the first to share a note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VahfTextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(post.comments, key = { it.id }) { comment ->
                        CommentRow(
                            comment = comment,
                            isOwnComment = comment.userId == currentUserId,
                            onDelete = { onDeleteComment(comment.id) }
                        )
                    }
                }
            }

            HorizontalDivider(color = VahfBorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { if (it.length <= 500) commentDraft = it },
                    placeholder = { Text("Write a comment...") },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VahfCyanPrimary,
                        unfocusedBorderColor = VahfBorderSubtle,
                        focusedContainerColor = VahfOffWhite,
                        unfocusedContainerColor = VahfOffWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = if (commentDraft.isNotBlank() && !isSendingComment) {
                        VahfNavyDeep
                    } else {
                        VahfBorderSubtle
                    }
                ) {
                    IconButton(
                        onClick = {
                            onSendComment(commentDraft) {
                                commentDraft = ""
                            }
                        },
                        enabled = commentDraft.isNotBlank() && !isSendingComment,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_comment_button")
                    ) {
                        if (isSendingComment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = VahfWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Post comment",
                                tint = VahfWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: PostComment,
    isOwnComment: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = VahfOffWhite,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VahfBorderSubtle, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VahfCyanSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.authorName.firstOrNull()?.uppercase() ?: "V",
                    style = MaterialTheme.typography.labelLarge,
                    color = VahfNavyDeep
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        color = VahfNavyDeep
                    )
                    Text(
                        text = formatCommentTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = VahfTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VahfTextSecondary
                )
            }
            if (isOwnComment && comment.id.isNotBlank()) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete comment",
                        tint = VahfTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatCommentTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(timestamp.toDate())
}
