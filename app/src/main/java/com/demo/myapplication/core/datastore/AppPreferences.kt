package com.demo.myapplication.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** User-facing settings (Settings screen). Defaults are what a new user gets. */
data class AppSettings(
    // Ongoing notification with the countdown while a session runs.
    val sessionNotification: Boolean = true,
    // A quiet "Done" notification when a session finishes in the background.
    val completionAlert: Boolean = true,
    // Open new sessions straight into minimal (black) mode.
    val startInMinimalMode: Boolean = false,
    // Keep the screen awake on the session screen (minimal mode always does).
    val keepScreenOn: Boolean = false
)

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[PreferencesKeys.ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETE] = true }
    }

    // Optional first name from onboarding, for a friendlier greeting. Never leaves the phone.
    val userName: Flow<String> = dataStore.data.map { it[USER_NAME].orEmpty() }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME] = name.trim() }
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            sessionNotification = prefs[SESSION_NOTIFICATION] ?: defaults.sessionNotification,
            completionAlert = prefs[COMPLETION_ALERT] ?: defaults.completionAlert,
            startInMinimalMode = prefs[PreferencesKeys.DEFAULT_SESSION_MODE] == MODE_MINIMAL,
            keepScreenOn = prefs[KEEP_SCREEN_ON] ?: defaults.keepScreenOn
        )
    }

    suspend fun setSessionNotification(enabled: Boolean) = set(SESSION_NOTIFICATION, enabled)
    suspend fun setCompletionAlert(enabled: Boolean) = set(COMPLETION_ALERT, enabled)
    suspend fun setKeepScreenOn(enabled: Boolean) = set(KEEP_SCREEN_ON, enabled)

    suspend fun setStartInMinimalMode(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_SESSION_MODE] = if (enabled) MODE_MINIMAL else MODE_NORMAL }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private companion object {
        val SESSION_NOTIFICATION = booleanPreferencesKey("session_notification")
        val COMPLETION_ALERT = booleanPreferencesKey("completion_alert")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val USER_NAME = stringPreferencesKey("user_name")
        // Values for PreferencesKeys.DEFAULT_SESSION_MODE - spec section 2.
        const val MODE_NORMAL = "NORMAL"
        const val MODE_MINIMAL = "MINIMAL"
    }
}
