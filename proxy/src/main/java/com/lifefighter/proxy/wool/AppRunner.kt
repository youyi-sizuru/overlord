package com.lifefighter.proxy.wool

import android.graphics.Bitmap
import android.media.Image
import com.lifefighter.widget.accessibility.ExAccessibilityService

/**
 * @author xzp
 * @created on 2022/3/8.
 */
interface AppRunner {

    fun startWith(service: AppRunnerAccessibilityService)


    fun destroy()
}


abstract class AppRunnerAccessibilityService : ExAccessibilityService() {

    abstract fun acquireScreenShot(): Bitmap?
}