package com.lifefighter.overlord.action.wallpaper

import android.graphics.Canvas
import com.lifefighter.widget.wallpaper.CanvasWallpaperService

/**
 * @author xzp
 * @created on 2021/12/25.
 */
class ClockWallpaperService : CanvasWallpaperService() {

    override fun onDraw(canvas: Canvas) {
        ClockUtils.draw(canvas)
    }

}