package com.lifefighter.wool.library

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Point
import com.lifefighter.ocr.OcrLibrary
import com.lifefighter.ocr.OcrResult
import com.lifefighter.proxy.wool.AppRunner
import com.lifefighter.proxy.wool.AppRunnerService
import com.lifefighter.utils.*
import kotlinx.coroutines.delay
import java.io.File


/**
 * @author xzp
 * @created on 2022/3/11.
 */
abstract class WoolAppRunner(protected val service: AppRunnerService, private val appName: String) :
    AppRunner {
    override suspend fun start() {
        while (checkNode()) {
            delay(100)
            continue
        }
    }

    private suspend fun checkNode(): Boolean = bg {
        var currentAppName = service.getAccessibilityPackageName()
        if (appName != currentAppName) {
            logDebug("current app $currentAppName is not $appName, try to start")
            startApp()
            currentAppName = service.getAccessibilityPackageName()
            if (appName != currentAppName) {
                logDebug("current app $currentAppName is not $appName, stop self")
                return@bg false
            }
        }
        if (!gotoEventPage()) {
            return@bg false
        }
        if (!touchEventAndBack()) {
            return@bg false
        }
        return@bg true
    }

    abstract suspend fun gotoEventPage(): Boolean

    abstract suspend fun touchEventAndBack(): Boolean

    suspend fun startApp() = ui {
        tryOrNothing {
            home()
            delay(1000)
            val context = service.getContext()
            context.startActivity(context.packageManager.getLaunchIntentForPackage(appName))
            delay(5000)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun restartApp() = ui {
        tryOrNothing {
            while (appName == service.getAccessibilityPackageName()) {
                back()
                delay(1000)
            }
            startApp()
        }

    }

    fun back() {
        service.sendGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun home() {
        service.sendGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    suspend fun findMaxMatchTextByPattern(patternStr: String): OcrResult? =
        findTextByPattern(patternStr).maxByOrNull {
            it.score
        }

    suspend fun findTextByPattern(patternStr: String): List<OcrResult> = bg {
        val bitmap = service.acquireScreenShot() ?: return@bg emptyList()
        val results = OcrLibrary.detectBitmap(bitmap, false).orEmpty()
        val pattern = patternStr.toPattern()
        results.filter {
            val text = it.text.orEmpty().replace("\n", "").replace("\r", "")
            pattern.matcher(text).matches()
        }.toList()
    }

    fun touchResult(ocrResult: OcrResult, offsetX: Int = 0, offsetY: Int = 0) {
        val touchX = ((ocrResult.x0 + ocrResult.x1) / 2).toInt()
        val touchY = ((ocrResult.y0 + ocrResult.y3) / 2).toInt()
        val point = Point(touchX + offsetX, touchY + offsetY)
        service.touchPoint(point)
    }
}