package com.meltide.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meltide.app.data.local.dao.SessionDao
import com.meltide.app.di.ApplicationScope
import com.meltide.app.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pause / Resume from the session notification. Ending isn't offered here on
 * purpose - the spec wants ending early to be a two-step, in-app decision.
 */
@AndroidEntryPoint
class SessionActionReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        applicationScope.launch {
            try {
                val active = sessionDao.getActiveSession() ?: return@launch
                when (intent.action) {
                    ACTION_PAUSE -> sessionRepository.pauseSession(active)
                    ACTION_RESUME -> sessionRepository.resumeSession(active)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.meltide.app.action.PAUSE_SESSION"
        const val ACTION_RESUME = "com.meltide.app.action.RESUME_SESSION"
    }
}
