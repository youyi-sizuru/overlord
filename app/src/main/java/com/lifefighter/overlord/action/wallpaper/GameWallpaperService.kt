package com.lifefighter.overlord.action.wallpaper

import android.graphics.Canvas
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.action.wallpaper.clock.ClockGame
import com.lifefighter.utils.tryOrNull
import com.lifefighter.widget.wallpaper.CanvasWallpaperService
import com.tencent.mmkv.MMKV

/**
 * @author xzp
 * @created on 2021/12/25.
 */
class GameWallpaperService : CanvasWallpaperService() {
    private var game: CanvasGame? = null
    override fun onCreate() {
        super.onCreate()
        game = tryOrNull {
            Class.forName(
                MMKV.defaultMMKV().getString(AppConst.GAME_TYPE, ClockGame::class.java.name)!!
            ).newInstance() as? CanvasGame
        } ?: ClockGame()
    }

    override fun onDraw(canvas: Canvas) {
        game?.onDraw(canvas)
    }

    override fun onOffset(xOffset: Float, yOffset: Float) {
        game?.onOffset(xOffset, yOffset)
    }

    override fun onDestroy() {
        super.onDestroy()
        game?.onEnd()
    }
}