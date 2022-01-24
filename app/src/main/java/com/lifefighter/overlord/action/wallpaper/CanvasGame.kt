package com.lifefighter.overlord.action.wallpaper

import android.content.Context
import com.lifefighter.widget.wallpaper.CanvasPainter

/**
 * @author xzp
 * @created on 2022/1/19.
 */
interface CanvasGame : CanvasPainter {
    fun onStart(context: Context)

    fun onEnd(context: Context)

    fun getName(): String
}
