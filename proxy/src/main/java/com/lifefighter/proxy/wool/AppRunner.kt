package com.lifefighter.proxy.wool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import kotlinx.coroutines.CoroutineScope

/**
 * @author xzp
 * @created on 2022/3/8.
 */
interface AppRunner {

    suspend fun start()
}


interface AppRunnerService : CoroutineScope {

    fun acquireScreenShot(): Bitmap?

    fun touchPoint(point: Point)

    fun scrollTo(from: Point, to: Point)

    fun sendGlobalAction(action: Int)

    fun stop()

    fun getContext(): Context

    fun getAccessibilityPackageName(): String?
}