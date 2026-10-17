package com.demo.myapplication.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An installed app's launcher icon, loaded off the main thread; null while loading or if it's gone. */
@Composable
fun rememberAppIcon(packageName: String): State<ImageBitmap?> {
    val packageManager = LocalContext.current.packageManager
    return produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getApplicationIcon(packageName).toBitmap(width = 64, height = 64).asImageBitmap()
            }.getOrNull()
        }
    }
}
