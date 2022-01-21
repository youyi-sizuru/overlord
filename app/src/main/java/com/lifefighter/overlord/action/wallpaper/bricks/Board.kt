package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class Board(private val game: BricksGame) {
    /**
     * 板子大小
     */
    private val rect = Rect(0, 0, 0, 0)
    private var offsetX = 0f
    val centerX
        get() = rect.centerX()
    val top
        get() = rect.top
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
    }

    fun reset() {
        val width = game.width * max(1, (10 - game.level)) / 20
        val center = game.width / 2
        rect.left = center - width / 2
        rect.right = center + width / 2
        rect.bottom = game.height - game.height * (min(9, game.level) + 1) / 30
        rect.top = rect.bottom - game.width / 80
        offsetX = 0f
    }

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(offsetX, 0f)
        canvas.drawRect(rect, paint)
        canvas.restore()
    }

    fun offset(offsetX: Float) {
        val max = (game.width - rect.width()) / 2f
        this.offsetX = max(-max, min(max, this.offsetX + offsetX))
    }
}