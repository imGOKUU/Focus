package com.demo.myapplication

import android.app.Application
import com.demo.myapplication.core.time.SessionCompletionWatcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FocusApplication : Application() {

    @Inject lateinit var sessionCompletionWatcher: SessionCompletionWatcher

    override fun onCreate() {
        super.onCreate()
        sessionCompletionWatcher.start()
    }
}
