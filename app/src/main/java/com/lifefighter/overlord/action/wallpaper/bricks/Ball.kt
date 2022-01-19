package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Point

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class Ball(private val game: BricksGame) {
    /**
     * 球大小
     */
    private var size: Int = 0

    /**
    * 球所在的点
    */
    private val point: Point = Point()
    fun reset() {

    }
}