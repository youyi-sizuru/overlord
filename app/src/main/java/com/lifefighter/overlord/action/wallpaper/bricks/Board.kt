package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.*
import kotlin.math.max
import kotlin.math.min

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class Board(private val game: BricksGame) : DrawAble, ResetAble, CollisionBlock {
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

    override fun reset() {
        val width = game.width * max(1, (10 - game.level / 2)) / 20
        val center = game.width / 2
        rect.left = center - width / 2
        rect.right = center + width / 2
        rect.bottom = game.height - game.height * (min(9, game.level / 2) + 1) / 30
        rect.top = rect.bottom - game.width / 80
        offsetX = 0f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(offsetX, 0f)
        canvas.drawRect(rect, paint)
        canvas.restore()
    }

    fun offset(offsetX: Float) {
        val max = (game.width - rect.width()) / 2f
        this.offsetX = max(-max, min(max, this.offsetX + offsetX))
    }

    override fun isDead(direction: CollisionDirection): Boolean {
        return false
    }

    override fun calculateCollisionMove(ball: Ball): CollisionMove {
        val offsetRect = RectF(
            rect.left + offsetX,
            rect.top.toFloat(),
            rect.right + offsetX,
            rect.bottom.toFloat()
        )
        val move = ball.calculateCollisionMove(offsetRect)
        return CollisionMove(this, move.second, move.first)
    }
}