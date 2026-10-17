package com.demo.myapplication.core.notifications

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.demo.myapplication.MainActivity
import com.demo.myapplication.R
import com.demo.myapplication.core.datastore.AppPreferences
import com.demo.myapplication.core.platform.PermissionStatus
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.GoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.Session
import com.demo.myapplication.data.local.entity.SessionStatus
import com.demo.myapplication.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a notification in step with the active session, so a running
 * session is never invisible once the user leaves the app:
 * - running: ongoing notification with a live countdown and Pause,
 * - paused: time left and Resume,
 * - finished while the app is in the background: a quiet "Done" note.
 *
 * Not a foreground service: the countdown is drawn by the system chronometer
 * from the session's end time, and the session itself is clock-based (spec
 * section 7), so nothing needs to keep running for the time to stay right.
 * setTimeoutAfter clears the notification at the end time even if this
 * process has been killed by then.
 */
@Singleton
class SessionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao,
    private val dailyGoalDao: DailyGoalDao,
    private val goalDao: GoalDao,
    private val preferences: AppPreferences,
    private val timeProvider: TimeProvider,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private val manager = NotificationManagerCompat.from(context)
    private var lastSession: Session? = null
    // Bumped when the app comes to the foreground - permission may have just
    // been granted in system Settings, which no flow here would notice.
    private val refreshes = MutableStateFlow(0)

    fun start() {
        createChannels()
        applicationScope.launch {
            combine(sessionDao.observeActiveSession(), preferences.settings, refreshes) { session, settings, _ ->
                session to settings
            }
                .collect { (session, settings) ->
                    val previous = lastSession
                    lastSession = session
                    when {
                        session == null -> {
                            manager.cancel(ONGOING_ID)
                            if (previous != null) notifyIfCompleted(previous.id)
                        }
                        settings.sessionNotification -> postOngoing(session)
                        else -> manager.cancel(ONGOING_ID)
                    }
                }
        }
    }

    fun refresh() {
        refreshes.value++
    }

    @SuppressLint("MissingPermission") // checked by canPostNotifications
    private suspend fun postOngoing(session: Session) {
        if (!PermissionStatus.canPostNotifications(context)) return
        val goalId = dailyGoalDao.getById(session.dailyGoalId)?.goalId ?: return
        val goalName = goalDao.getGoalById(goalId)?.name ?: return
        val blockedCount = goalDao.observeBlockedApps(goalId).first().size
        val now = timeProvider.now()
        val remainingMillis = timeProvider.remainingSeconds(session, now) * 1000
        val isPaused = session.status == SessionStatus.PAUSED

        val builder = NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(if (isPaused) "$goalName · paused" else goalName)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(BrandColor)
            .setContentIntent(openAppIntent())

        if (isPaused) {
            builder
                .setContentText("${formatRemaining(remainingMillis / 1000)} left · your apps are available")
                .setShowWhen(false)
                .addAction(0, "Resume", actionIntent(SessionActionReceiver.ACTION_RESUME))
        } else {
            builder
                .setContentText(
                    when (blockedCount) {
                        0 -> "Focus session running"
                        1 -> "1 app blocked until it's done"
                        else -> "$blockedCount apps blocked until it's done"
                    }
                )
                .setWhen(now.toEpochMilli() + remainingMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setTimeoutAfter(remainingMillis)
                .addAction(0, "Pause", actionIntent(SessionActionReceiver.ACTION_PAUSE))
        }
        manager.notify(ONGOING_ID, builder.build())
    }

    /** Safe to call more than once for the same session - it replaces, never stacks. */
    @SuppressLint("MissingPermission") // checked by canPostNotifications
    suspend fun notifyIfCompleted(sessionId: Long) {
        if (!preferences.settings.first().completionAlert) return
        val session = sessionDao.getById(sessionId) ?: return
        // Ended early, or interrupted by another goal - only real completions get a note.
        if (session.status != SessionStatus.COMPLETED) return
        // In the app, the session result screen already says it.
        if (isAppInForeground() || !PermissionStatus.canPostNotifications(context)) return
        val goalName = dailyGoalDao.getById(session.dailyGoalId)
            ?.let { goalDao.getGoalById(it.goalId)?.name } ?: return

        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("$goalName · done")
            .setContentText("One less thing today. Your apps are available again.")
            .setAutoCancel(true)
            .setColor(BrandColor)
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(DONE_ID, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, "Active session", NotificationManager.IMPORTANCE_LOW).apply {
                description = "The countdown while a focus session runs"
                setShowBadge(false)
            }
        )
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Session finished", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When a session's time is up"
            }
        )
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, SessionActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun isAppInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun formatRemaining(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600 + 59) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private companion object {
        const val CHANNEL_SESSION = "session_active"
        const val CHANNEL_DONE = "session_done"
        const val ONGOING_ID = 1001
        const val DONE_ID = 1002
        const val BrandColor = 0xFF263640.toInt() // FocusPrimaryDark
    }
}
