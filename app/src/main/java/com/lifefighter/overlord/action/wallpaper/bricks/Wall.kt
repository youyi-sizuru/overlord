package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author xzp
 * @created on 2022/1/22.
 */
class Wall : DrawAble, ResetAble, CollisionBlock {
    var width: Int = 0
    var height: Int = 0
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.strokeWidth = 10f
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
    }

    override fun reset() {
        width = 600
        height = 800
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    override fun isDead(direction: CollisionDirection): Boolean {
        return direction == CollisionDirection.BOTTOM
    }

    override fun calculateCollisionMove(ball: Ball): CollisionMove {
        var minMove = CollisionMove(this, Float.MAX_VALUE, CollisionDirection.BOTTOM)
        val angle = Math.toRadians(ball.angle.toDouble()).toFloat()
        //X轴方向移动系数
        val cos = cos(angle)
        //Y轴方向移动系数
        val sin = sin(angle)
        if (cos > 0) {
            minMove = minOf(
                minMove,
                CollisionMove(this, (width - ball.right) / cos, CollisionDirection.RIGHT)
            )

        } else if (cos < 0) {
            minMove =
                minOf(minMove, CollisionMove(this, (ball.left / -cos), CollisionDirection.LEFT))
        }
        if (sin > 0) {
            minMove = minOf(
                minMove,
                CollisionMove(this, (height - ball.bottom) / sin, CollisionDirection.BOTTOM)
            )
        } else if (sin < 0) {
            minMove = minOf(minMove, CollisionMove(this, ball.top / -sin, CollisionDirection.TOP))
        }
        return minMove
    }
}