package com.meltide.app

import android.app.Application
import com.meltide.app.core.notifications.SessionNotifier
import com.meltide.app.core.time.SessionCompletionWatcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FocusApplication : Application() {

    @Inject lateinit var sessionCompletionWatcher: SessionCompletionWatcher
    @Inject lateinit var sessionNotifier: SessionNotifier

    override fun onCreate() {
        super.onCreate()
        sessionCompletionWatcher.start()
        sessionNotifier.start()
    }
}
