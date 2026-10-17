package com.demo.myapplication.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.myapplication.core.datastore.AppPreferences
import com.demo.myapplication.core.datastore.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

object Routes {
    const val Welcome = "landing_page"
    const val Setup = "setup"
    const val Today = "today"
    const val Settings = "settings"
}

/** Decides where the app opens: the intro once, Today every time after. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        preferences.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val userName: StateFlow<String> =
        preferences.userName.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setUserName(name: String) {
        viewModelScope.launch { preferences.setUserName(name) }
    }

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            _startDestination.value =
                if (preferences.onboardingComplete.first()) Routes.Today else Routes.Welcome
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { preferences.setOnboardingComplete() }
    }
}
