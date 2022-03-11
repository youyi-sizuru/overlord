package com.lifefighter.wool.library

import com.lifefighter.ocr.OcrLibrary
import com.lifefighter.proxy.wool.AppRunner
import com.lifefighter.proxy.wool.AppRunnerService
import com.lifefighter.utils.bg
import com.lifefighter.utils.logDebug
import com.lifefighter.utils.logError
import com.lifefighter.utils.ui
import kotlinx.coroutines.delay

/**
 * @author xzp
 * @created on 2022/3/11.
 */
abstract class WoolAppRunner(private val service: AppRunnerService, private val appName: String) :
    AppRunner {
    override suspend fun start() {
        while (checkNode()) {
            delay(1)
            continue
        }
    }

    private suspend fun checkNode(): Boolean = bg {
        var currentAppName = service.getAccessibilityPackageName()
        if (appName != currentAppName) {
            logDebug("current app $currentAppName is not $appName, try to start")
            startApp()
            delay(5000)
            currentAppName = service.getAccessibilityPackageName()
            if (appName != currentAppName) {
                logDebug("current app $currentAppName is not $appName, stop self")
                return@bg false
            }
        }
        val bitmap = service.acquireScreenShot()
        if (bitmap == null) {
            logError("can't acquireScreenShot")
            return@bg false
        }
        val results = OcrLibrary.detectBitmap(bitmap, true).orEmpty()
        logDebug("find screen text, size: ${results.size}")
        for (result in results) {
            logDebug(result.toString())
        }
        return@bg true
    }
    abstract class

    protected suspend fun startApp() = ui {
        try {
            val context = service.getContext()
            context.startActivity(context.packageManager.getLaunchIntentForPackage(appName))
        } catch (e: Exception) {
            logError("startApp error", e)
        }
    }
}