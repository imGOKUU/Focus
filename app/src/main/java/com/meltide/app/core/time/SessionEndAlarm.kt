package com.meltide.app.core.time

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.meltide.app.core.notifications.SessionNotifier
import com.meltide.app.data.local.dao.SessionDao
import com.meltide.app.di.ApplicationScope
import com.meltide.app.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A system alarm at the running session's end time. Needed because Android
 * freezes cached app processes: once the user leaves the app (and blocking
 * is off), SessionCompletionWatcher's in-process timer simply stops, so a
 * session that ends in the background would otherwise only be recorded -
 * and its "Done" note shown - when the app is next opened.
 *
 * Exact when the system allows it (always below Android 12; with the
 * user-granted "Alarms & reminders" access above), otherwise an inexact
 * allow-while-idle alarm, which can land a few minutes late in Doze.
 */
@Singleton
class SessionEndAlarm @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Exact only on the branch where canExact says it's allowed: below Android
    // 12 (no permission needed), or when the user has granted alarm access.
    @SuppressLint("MissingPermission")
    fun schedule(atEpochMillis: Long) {
        val pending = pendingIntent()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pending)
        }
    }

    fun cancel() = alarmManager.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, SessionEndReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

/** Records the completion and posts the "Done" note while the alarm holds the process awake. */
@AndroidEntryPoint
class SessionEndReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var sessionNotifier: SessionNotifier
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        applicationScope.launch {
            try {
                val active = sessionDao.getActiveSession() ?: return@launch
                sessionRepository.completeIfExpired()
                sessionNotifier.notifyIfCompleted(active.id)
            } finally {
                pending.finish()
            }
        }
    }
}
