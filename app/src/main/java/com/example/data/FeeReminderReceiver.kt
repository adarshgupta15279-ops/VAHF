package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

class FeeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MONTHLY_FEE_REMINDER) return

        val registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID).orEmpty()
        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME).orEmpty().ifBlank { "Student" }
        val whatsappE164 = intent.getStringExtra(EXTRA_WHATSAPP_E164).orEmpty()
        val monthIndex = intent.getIntExtra(EXTRA_MONTH_INDEX, 1)
        val joiningMonths = intent.getIntExtra(EXTRA_JOINING_MONTHS, 1)
        val monthlyFeeInr = intent.getIntExtra(EXTRA_MONTHLY_FEE_INR, VahfConfig.DEFAULT_MONTHLY_FEE_INR)
        val totalFeeInr = intent.getIntExtra(EXTRA_TOTAL_FEE_INR, monthlyFeeInr)
        val dueDateText = intent.getStringExtra(EXTRA_DUE_DATE_TEXT).orEmpty()
        val classesSummary = intent.getStringExtra(EXTRA_CLASSES_SUMMARY).orEmpty()

        if (whatsappE164.isBlank()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VAHF Monthly Fee Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automated monthly fee due reminders with 1-tap WhatsApp messaging."
            }
            manager.createNotificationChannel(channel)
        }

        val whatsappMessage = FeeAutomationHelper.buildStudentMonthlyFeeWhatsAppMessage(
            studentName = studentName,
            monthIndex = monthIndex,
            joiningMonths = joiningMonths,
            monthlyFeeInr = monthlyFeeInr,
            totalFeeInr = totalFeeInr,
            dueDateText = dueDateText,
            classesSummary = classesSummary
        )
        val waUri = Uri.parse(
            "https://api.whatsapp.com/send?phone=$whatsappE164&text=${Uri.encode(whatsappMessage)}"
        )
        val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            (registrationId + "_" + monthIndex).hashCode(),
            waIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val feeDisplay = FeeAutomationHelper.formatInr(monthlyFeeInr)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("VAHF Fee Due: Month $monthIndex/$joiningMonths ($feeDisplay)")
            .setContentText("Tap to send WhatsApp fee update to $studentName (+$whatsappE164)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Month $monthIndex of $joiningMonths fee ($feeDisplay) is due for $studentName. Tap to send the automated WhatsApp fee message directly."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        manager.notify(registrationId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "vahf_fee_reminders"
        const val ACTION_MONTHLY_FEE_REMINDER = "com.example.ACTION_MONTHLY_FEE_REMINDER"
        const val EXTRA_REGISTRATION_ID = "extra_registration_id"
        const val EXTRA_STUDENT_NAME = "extra_student_name"
        const val EXTRA_WHATSAPP_E164 = "extra_whatsapp_e164"
        const val EXTRA_MONTH_INDEX = "extra_month_index"
        const val EXTRA_JOINING_MONTHS = "extra_joining_months"
        const val EXTRA_MONTHLY_FEE_INR = "extra_monthly_fee_inr"
        const val EXTRA_TOTAL_FEE_INR = "extra_total_fee_inr"
        const val EXTRA_DUE_DATE_TEXT = "extra_due_date_text"
        const val EXTRA_CLASSES_SUMMARY = "extra_classes_summary"
    }
}
