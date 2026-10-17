package com.meltide.app.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meltide.app.core.datastore.AppPreferences
import com.meltide.app.core.datastore.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        preferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setSessionNotification(enabled: Boolean) = viewModelScope.launch { preferences.setSessionNotification(enabled) }
    fun setCompletionAlert(enabled: Boolean) = viewModelScope.launch { preferences.setCompletionAlert(enabled) }
    fun setStartInMinimalMode(enabled: Boolean) = viewModelScope.launch { preferences.setStartInMinimalMode(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { preferences.setKeepScreenOn(enabled) }
}
