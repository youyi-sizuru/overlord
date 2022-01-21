package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class Ball(private val game: BricksGame) {

    /**
     * 球所在的点
     */
    private val rect = RectF(0f, 0f, 0f, 0f)
    private var offsetX = 0f
    private var offsetY = 0f

    /**
     * 移动速度,单位:像素/秒
     */
    private var speed = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastMoveTime: Long? = null

    /**
     * 小球移动的角度
     */
    var angle = 0f
        private set
    val left
        get() = rect.left + offsetX
    val right
        get() = rect.right + offsetX
    val top
        get() = rect.top + offsetY
    val bottom
        get() = rect.bottom + offsetY

    val isStart
        get() = speed > 0

    init {
        paint.style = Paint.Style.FILL
        paint.color = Color.RED
    }

    fun reset() {
        offsetX = 0f
        offsetY = 0f
        val size = game.width * max(1, 10 - game.level) / 100f
        rect.bottom = game.board.top + 1f
        rect.top = rect.bottom - size
        rect.left = game.board.centerX - size / 2
        rect.right = rect.left + size
        speed = 0f
        lastMoveTime = null
        angle = 0f
    }

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.drawOval(rect, paint)
        canvas.restore()
    }

    fun start() {
        speed = game.width * (game.level / 2 + 1) / 5f
        angle = 315f
    }

    fun move() {
        if (isStart.not()) {
            return
        }
        val now = System.currentTimeMillis()
        val preTime = lastMoveTime ?: now
        lastMoveTime = now
        var moved = (now - preTime) * speed / 1000
        while (true) {
            val collisionMove = game.calculateCollisionMove(this)
            if (collisionMove.move > moved) {
                offsetX += moved * cos(Math.toRadians(angle.toDouble())).toFloat()
                offsetY += moved * sin(Math.toRadians(angle.toDouble())).toFloat()
                break
            }
            offsetX += collisionMove.move * cos(Math.toRadians(angle.toDouble())).toFloat()
            offsetY += collisionMove.move * sin(Math.toRadians(angle.toDouble())).toFloat()
            angle = if (collisionMove.vertical) {
                (180 - angle + 360) % 360
            } else {
                360 - angle
            }
            moved -= collisionMove.move
        }
    }
}