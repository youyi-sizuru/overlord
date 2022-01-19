package com.lifefighter.overlord.action.wallpaper

import com.lifefighter.widget.wallpaper.CanvasPainter

/**
 * @author xzp
 * @created on 2022/1/19.
 */
interface CanvasGame : CanvasPainter {
    fun onStart()

    fun onEnd()

    fun getName(): String
}
