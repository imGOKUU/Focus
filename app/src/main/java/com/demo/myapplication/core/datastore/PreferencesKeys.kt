package com.demo.myapplication.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    val USAGE_ACCESS_GRANTED = booleanPreferencesKey("usage_access_granted")
    // "LIGHT" | "DARK" | "SYSTEM" - spec section 4, Settings > Appearance
    val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
    // "NORMAL" | "MINIMAL" default for active sessions - spec section 2
    val DEFAULT_SESSION_MODE = stringPreferencesKey("default_session_mode")
}
