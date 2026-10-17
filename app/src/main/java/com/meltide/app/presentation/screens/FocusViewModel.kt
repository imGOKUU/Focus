package com.meltide.app.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meltide.app.core.platform.InstalledAppsProvider
import com.meltide.app.core.time.TimeProvider
import com.meltide.app.data.local.entity.Session
import com.meltide.app.data.local.entity.SessionStatus
import com.meltide.app.domain.usecase.AddGoalUseCase
import com.meltide.app.domain.usecase.GetGoalByIdUseCase
import com.meltide.app.domain.usecase.GetGoalsUseCase
import com.meltide.app.domain.usecase.SessionUsecase
import com.meltide.app.di.ApplicationScope
import com.meltide.app.domain.repository.SessionOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class UiState(
    val totalSeconds :Long = 1L,
    val remainingSeconds:Long = 0L,
    val blockedApps:Int=0,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    // Set once the session is over (time ran out or ended early) - the
    // screen hands off to the session result screen.
    val outcome: SessionOutcome? = null,
    // Nothing left to run today for this goal - the screen just closes.
    val isUnavailable: Boolean = false
)
@HiltViewModel
class FocusViewModel @Inject constructor (
    private val getGoalsUseCase: GetGoalsUseCase,
    private val getGoalByIdUseCase: GetGoalByIdUseCase,
    private val sessionUsecase: SessionUsecase,
    private val timeProvider: TimeProvider,
    @ApplicationScope private val applicationScope: CoroutineScope
): ViewModel() {
    private val  _uiState = MutableStateFlow(UiState())
    val uiState= _uiState.asStateFlow()
    private var currentSession :Session?=null
    private var startedGoalId: Long? = null

    fun start(goalId:Long){
        // The screen's LaunchedEffect re-runs on rotation but this ViewModel
        // survives it - without this guard a second countdown loop and a
        // second blocked-apps collector would start.
        if (startedGoalId == goalId) return
        startedGoalId = goalId
        viewModelScope.launch {
            val goal = getGoalByIdUseCase(goalId)
            var session = goal?.dailyDurationSeconds?.let { sessionUsecase(goal.id , dailyAllocatedSeconds = it ) }
            if (session?.status == SessionStatus.PAUSED) {
                session = sessionUsecase.resumeSession(session)
            }
            currentSession=session
            if (session == null) {
                _uiState.update { it.copy(isUnavailable = true) }
            }
            if(session!=null){
                _uiState.update {it.copy(totalSeconds = session.allocatedSeconds)}
                while(isActive){
                    val activeSession = currentSession ?: break
                    val remainingSeconds = timeProvider.remainingSeconds(activeSession, timeProvider.now())

                    _uiState.update {
                        it.copy(
                            remainingSeconds = remainingSeconds,
                            isPaused = activeSession.status == SessionStatus.PAUSED
                        )
                    }
                    if (remainingSeconds == 0L) {
                        sessionUsecase.completeSession(activeSession)
                        _uiState.update {
                            it.copy(
                                isCompleted = true,
                                outcome = SessionOutcome(
                                    goalCompleted = true,
                                    focusedSeconds = goal?.dailyDurationSeconds ?: activeSession.allocatedSeconds,
                                    remainingTodaySeconds = 0L
                                )
                            )
                        }
                        break
                    }
                    // Ended early from this screen - stop counting.
                    if (_uiState.value.outcome != null) break
                    delay(1_000.milliseconds)
                }
            }
        }
        // Pause/Resume can also come from the notification - follow the
        // database so this screen never shows a stale running/paused state.
        viewModelScope.launch {
            sessionUsecase.observeActiveSession().collect { stored ->
                val current = currentSession
                if (stored != null && current != null && stored.id == current.id) {
                    currentSession = stored
                    _uiState.update { it.copy(isPaused = stored.status == SessionStatus.PAUSED) }
                }
            }
        }
        viewModelScope.launch {
            getGoalsUseCase.observeBlockedApps(goalId).collect { blockedApps ->
                _uiState.update {it.copy(blockedApps = blockedApps.size)}
            }
        }

    }
    // Pause/resume run in the application scope: the Focus screen's back
    // button pauses and leaves at the same time, which clears this ViewModel
    // and would cancel a viewModelScope write mid-flight. Main.immediate runs
    // each call up to its first suspension right away, so back-to-back
    // pause/resume taps reach the repository in the order they were made.
    fun pause() = updateSession { sessionUsecase.pauseSession(it) }

    fun resume() = updateSession { sessionUsecase.resumeSession(it) }

    // Application scope for the same reason as pause/resume: the write must
    // finish even though the screen navigates away right after.
    fun endSession() {
        applicationScope.launch(Dispatchers.Main.immediate) {
            val session = currentSession ?: return@launch
            val outcome = sessionUsecase.endSession(session) ?: return@launch
            currentSession = null
            _uiState.update { it.copy(outcome = outcome) }
        }
    }

    private fun updateSession(change: suspend (Session) -> Session) {
        applicationScope.launch(Dispatchers.Main.immediate) {
            val session = currentSession ?: return@launch
            val updated = change(session)
            currentSession = updated
            _uiState.update { it.copy(isPaused = updated.status == SessionStatus.PAUSED) }
        }
    }


}