package com.example.data

import com.example.R

/**
 * Single centralized configuration for VAHF content and admin notification channels.
 * Future real photographs, classes, live sessions, website updates, and admin
 * email/WhatsApp/webhook notification targets can be configured here without redesigning the UI.
 */
object VahfConfig {
    const val WEBSITE_URL = "https://vangmay-yoga.com"
    const val WEBSITE_DISPLAY_HOST = "vangmay-yoga.com"

    // Configured admin notification & support channels
    const val ADMIN_EMAIL = "vangmayyoga22@gmail.com"
    const val SUPPORT_PHONE_PRIMARY = "8892159095"
    const val SUPPORT_PHONE_SECONDARY = "7676495076"
    const val SUPPORT_PHONE_PRIMARY_DISPLAY = "+91 88921 59095"
    const val SUPPORT_PHONE_SECONDARY_DISPLAY = "+91 76764 95076"

    // Primary studio WhatsApp contact in E.164 format without '+'
    const val ADMIN_WHATSAPP_NUMBER = "918892159095"
    // Optional HTTPS webhook URL (e.g. Make / Zapier / Cloud Function / SMTP relay) for background HTTP POST delivery.
    const val ADMIN_NOTIFICATION_WEBHOOK_URL = ""

    val REGISTRATION_CLASS_OPTIONS: List<String> = listOf(
        "At Center Yoga Classes",
        "Online Yoga Class",
        "At Home Yoga Classes",
        "At Apartment Group Yoga Classes",
        "At Corporate Yoga Classes",
        "At Kids Yoga Classes",
        "Pregnancy and Postnatal Yoga",
        "Yoga Teacher Training Course"
    )

    val REGISTRATION_PACKAGE_OPTIONS: List<String> = listOf(
        "First month",
        "Third month",
        "Six month",
        "Nine month"
    )

    val REGISTRATION_GENDER_OPTIONS: List<String> = listOf(
        "Male",
        "Female",
        "Other"
    )

    const val DEFAULT_MONTHLY_FEE_INR: Int = 1500

    val JOINING_MONTH_OPTIONS: List<Int> = listOf(1, 2, 3, 6, 9, 12)

    val CLASS_MONTHLY_FEE_INR: Map<String, Int> = mapOf(
        "At Center Yoga Classes" to 1500,
        "Online Yoga Class" to 1200,
        "At Home Yoga Classes" to 4000,
        "At Apartment Group Yoga Classes" to 2000,
        "At Corporate Yoga Classes" to 3000,
        "At Kids Yoga Classes" to 1200,
        "Pregnancy and Postnatal Yoga" to 2000,
        "Yoga Teacher Training Course" to 5000
    )

    val bundledPosts: List<VahfFeedPost> = listOf(
        VahfFeedPost(
            id = "vahf_post_yoga_wheel",
            category = "Studio Practice",
            caption = "Yoga wheel balance and core strength practice at the VAHF studio.",
            imageRes = R.drawable.vahf_photo_yoga_wheel
        ),
        VahfFeedPost(
            id = "vahf_post_group_celebration",
            category = "Community",
            caption = "VAHF yoga students celebrating together with their certificates of excellence.",
            imageRes = R.drawable.vahf_photo_group_celebration
        ),
        VahfFeedPost(
            id = "vahf_post_certificate",
            category = "Recognition",
            caption = "Presenting the Best Student of the Year certificate at Vangmay Yoga.",
            imageRes = R.drawable.vahf_photo_certificate
        )
    )
}
