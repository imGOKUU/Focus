package com.demo.myapplication.presentation.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.myapplication.core.platform.InstalledAppsProvider
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.domain.usecase.AddGoalUseCase
import com.demo.myapplication.domain.usecase.GetGoalByIdUseCase
import com.demo.myapplication.domain.usecase.GetGoalsUseCase
import com.demo.myapplication.domain.usecase.SessionUsecase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val blockedApps:Int=0
)
@HiltViewModel
class FocusViewModel @Inject constructor (
    private val getGoalsUseCase: GetGoalsUseCase,
    private val getGoalByIdUseCase: GetGoalByIdUseCase,
    private val sessionUsecase: SessionUsecase,
    private val timeProvider: TimeProvider
): ViewModel() {
    private val  _uiState = MutableStateFlow(UiState())
    val uiState= _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun start(goalId:Long){
        viewModelScope.launch {
            val goal = getGoalByIdUseCase(goalId)
            val session = goal?.dailyDurationSeconds?.let { sessionUsecase(goal.id , dailyAllocatedSeconds = it ) }
            if(session!=null){
                val endEpochSeconds = session.startedAt.epochSecond + session.allocatedSeconds
                _uiState.update {it.copy(totalSeconds = session.allocatedSeconds)}
                while(isActive){
                    val remainingSeconds =
                        (endEpochSeconds - timeProvider.now().epochSecond).coerceAtLeast(0)

                    _uiState.update {it.copy(remainingSeconds = remainingSeconds)}
                    if (remainingSeconds == 0L) {
                        sessionUsecase.endSession(session)
                        break
                    }
                    delay(1_000.milliseconds)
                }
            }
        }
        viewModelScope.launch {
            getGoalsUseCase.observeBlockedApps(goalId).collect { blockedApps ->
                _uiState.update {it.copy(blockedApps = blockedApps.size)}
            }
        }

    }


}