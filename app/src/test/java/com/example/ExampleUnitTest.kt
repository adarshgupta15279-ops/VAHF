package com.example

import com.example.data.PostComment
import com.example.data.VahfConfig
import com.example.data.VahfFeedPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun bundledPosts_containOnlyRealStudioPhotos() {
        assertEquals(3, VahfConfig.bundledPosts.size)
        assertTrue(VahfConfig.bundledPosts.all { it.imageRes != null && it.caption.isNotBlank() })
    }

    @Test
    fun websiteUrl_usesHttpsVangmayYoga() {
        assertTrue(VahfConfig.WEBSITE_URL.startsWith("https://"))
    }

    @Test
    fun feedPost_commentCountReflectsCommentsList() {
        val post = VahfFeedPost(
            id = "p1",
            caption = "Test",
            category = "Studio",
            comments = listOf(
                PostComment(id = "c1", userId = "u1", postId = "p1", authorName = "A", text = "Great!"),
                PostComment(id = "c2", userId = "u2", postId = "p1", authorName = "B", text = "Inspiring!")
            )
        )
        assertEquals(2, post.commentCount)
    }
}
