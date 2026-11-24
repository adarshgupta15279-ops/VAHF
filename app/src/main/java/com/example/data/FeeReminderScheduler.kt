package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log

object FeeReminderScheduler {
    private const val TAG = "VahfFeeScheduler"

    fun syncAllReminders(context: Context, registrations: List<RegistrationSubmission>) {
        registrations.forEach { reg ->
            if (reg.feeAutomationEnabled &&
                reg.phoneVerified &&
                reg.verifiedWhatsappNumber.isNotBlank() &&
                reg.lastNotifiedMonth < reg.joiningMonths
            ) {
                scheduleNextMonthlyReminder(context, reg)
            } else {
                cancelReminder(context, reg.id)
            }
        }
    }

    fun scheduleNextMonthlyReminder(context: Context, registration: RegistrationSubmission) {
        val appContext = context.applicationContext
        val nextMonth = (registration.lastNotifiedMonth + 1).coerceAtLeast(1)
        if (!registration.feeAutomationEnabled ||
            !registration.phoneVerified ||
            registration.verifiedWhatsappNumber.isBlank() ||
            nextMonth > registration.joiningMonths
        ) {
            return
        }

        val dueMillis = if (registration.nextDueDateMillis > 0L) {
            registration.nextDueDateMillis
        } else {
            val baseMillis = FeeAutomationHelper.parseJoiningDateMillis(
                registration.dateOfJoining,
                registration.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            )
            FeeAutomationHelper.addMonthsAtMorningHour(baseMillis, nextMonth)
        }

        val triggerMillis = dueMillis.coerceAtLeast(System.currentTimeMillis() + 5_000L)
        val dueDateText = FeeAutomationHelper.formatDateDdMmmYyyy(dueMillis)

        val pendingIntent = buildPendingIntent(
            context = appContext,
            registrationId = registration.id,
            studentName = registration.fullName,
            whatsappE164 = registration.verifiedWhatsappNumber,
            monthIndex = nextMonth,
            joiningMonths = registration.joiningMonths,
            monthlyFeeInr = registration.monthlyFeeInr,
            totalFeeInr = registration.totalFeeInr,
            dueDateText = dueDateText,
            classesSummary = registration.selectedClasses.joinToString(", ")
        )

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }.onFailure { err ->
            Log.w(TAG, "setAndAllowWhileIdle fallback to set()", err)
            runCatching {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }

    fun cancelReminder(context: Context, registrationId: String) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appContext, FeeReminderReceiver::class.java).apply {
            action = FeeReminderReceiver.ACTION_MONTHLY_FEE_REMINDER
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            registrationId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(
        context: Context,
        registrationId: String,
        studentName: String,
        whatsappE164: String,
        monthIndex: Int,
        joiningMonths: Int,
        monthlyFeeInr: Int,
        totalFeeInr: Int,
        dueDateText: String,
        classesSummary: String
    ): PendingIntent {
        val intent = Intent(context, FeeReminderReceiver::class.java).apply {
            action = FeeReminderReceiver.ACTION_MONTHLY_FEE_REMINDER
            putExtra(FeeReminderReceiver.EXTRA_REGISTRATION_ID, registrationId)
            putExtra(FeeReminderReceiver.EXTRA_STUDENT_NAME, studentName)
            putExtra(FeeReminderReceiver.EXTRA_WHATSAPP_E164, whatsappE164)
            putExtra(FeeReminderReceiver.EXTRA_MONTH_INDEX, monthIndex)
            putExtra(FeeReminderReceiver.EXTRA_JOINING_MONTHS, joiningMonths)
            putExtra(FeeReminderReceiver.EXTRA_MONTHLY_FEE_INR, monthlyFeeInr)
            putExtra(FeeReminderReceiver.EXTRA_TOTAL_FEE_INR, totalFeeInr)
            putExtra(FeeReminderReceiver.EXTRA_DUE_DATE_TEXT, dueDateText)
            putExtra(FeeReminderReceiver.EXTRA_CLASSES_SUMMARY, classesSummary)
        }
        return PendingIntent.getBroadcast(
            context,
            registrationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
