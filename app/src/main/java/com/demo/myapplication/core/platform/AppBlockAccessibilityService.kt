package com.demo.myapplication.core.platform

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.demo.myapplication.BlockedAppActivity
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.GoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.SessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Watches foreground-app changes (the only way to detect this without a VPN
 * or device-admin approach) and, while a goal's session is actively STARTED
 * (not PAUSED), launches BlockedAppActivity on top of any app the user chose
 * to block for that goal. Requires the user to manually flip this on in
 * Settings > Accessibility - it cannot be requested at runtime - so the rest
 * of the app must keep working with it off; see AccessibilityPermissionBanner
 * for the in-app nudge.
 */
@AndroidEntryPoint
class AppBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var dailyGoalDao: DailyGoalDao
    @Inject lateinit var goalDao: GoalDao
    @Inject lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private data class ActiveBlock(
        val goalName: String,
        val remainingSeconds: Long,
        val blockedPackages: Set<String>
    )

    // onAccessibilityEvent runs on the main thread and must return immediately,
    // so the DB-backed state is collected continuously in the background and
    // cached here for synchronous reads.
    @Volatile
    private var activeBlock: ActiveBlock? = null

    private var lastBlockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceScope.launch {
            sessionDao.observeActiveSession()
                .flatMapLatest { session ->
                    if (session == null || session.status != SessionStatus.STARTED) {
                        flowOf(null)
                    } else {
                        flow<ActiveBlock?> {
                            val dailyGoal = dailyGoalDao.getById(session.dailyGoalId)
                            if (dailyGoal == null) {
                                emit(null)
                            } else {
                                val goal = goalDao.getGoalById(dailyGoal.goalId)
                                emitAll(
                                    goalDao.observeBlockedApps(dailyGoal.goalId).map { blocked ->
                                        ActiveBlock(
                                            goalName = goal?.name.orEmpty(),
                                            remainingSeconds = timeProvider.remainingSeconds(
                                                session,
                                                timeProvider.now()
                                            ),
                                            blockedPackages = blocked.map { it.packageName }.toSet()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                .collect { snapshot -> activeBlock = snapshot }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Our own app (including the block screen itself) coming to the
        // front always clears the guard, so re-opening the same blocked app
        // later triggers a fresh block. Any other non-blocked foreground app
        // (home launcher included) already clears it too via the branch
        // below, since it won't be in blockedPackages - no separate "is this
        // the launcher" resolution needed (that lookup proved unreliable:
        // PackageManager.resolveActivity(CATEGORY_HOME) returned
        // com.android.settings's fallback-home activity instead of the real
        // launcher on-device, which silently defeated blocking entirely).
        if (packageName == applicationContext.packageName) {
            lastBlockedPackage = null
            return
        }

        val snapshot = activeBlock
        if (snapshot == null || packageName !in snapshot.blockedPackages) {
            lastBlockedPackage = null
            return
        }

        if (packageName == lastBlockedPackage) return
        lastBlockedPackage = packageName

        launchBlockScreen(packageName, snapshot)
    }

    private fun launchBlockScreen(packageName: String, snapshot: ActiveBlock) {
        val appName = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrDefault(packageName)

        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockedAppActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockedAppActivity.EXTRA_GOAL_NAME, snapshot.goalName)
            putExtra(BlockedAppActivity.EXTRA_REMAINING_SECONDS, snapshot.remainingSeconds)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
