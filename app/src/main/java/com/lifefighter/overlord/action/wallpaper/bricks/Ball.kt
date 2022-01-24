package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class Ball(private val game: BricksGame) : DrawAble, ResetAble {

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

    /**
     * 球半径
     */
    val radius
        get() = rect.height() / 2f

    /**
     * 球中心点
     */
    val centerPoint
        get() = PointF(rect.centerX() + offsetX, rect.centerY() + offsetY)

    /**
     * 球是否在运动
     */
    val isStart
        get() = speed > 0

    init {
        paint.style = Paint.Style.FILL
        paint.color = Color.RED
    }

    override fun reset() {
        offsetX = 0f
        offsetY = 0f
        val size = game.width * max(1, 10 - game.level) / 100f
        rect.bottom = game.board.top - 1f
        rect.top = rect.bottom - size
        rect.left = game.board.centerX - size / 2
        rect.right = rect.left + size
        speed = 0f
        lastMoveTime = null
        angle = 0f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.drawOval(rect, paint)
        canvas.restore()
    }

    fun start() {
        if (isStart.not()) {
            speed = game.width * (game.level / 2 + 1) / 5f
            angle = 315f
        }
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
            angle =
                if (collisionMove.direction == CollisionDirection.LEFT || collisionMove.direction == CollisionDirection.RIGHT) {
                    (180 - angle + 360) % 360
                } else {
                    360 - angle
                }
            moved -= collisionMove.move
            if (collisionMove.isDead()) {
                game.reset()
                break
            }
            collisionMove.block.afterCollision()
        }
    }

    /**
     * 计算小球会撞到一个矩形的方向+距离
     * TODO 矩形的4个角撞击判定
     */
    fun calculateCollisionMove(rect: RectF): Pair<CollisionDirection, Float> {
        val topRect = RectF(
            rect.left,
            rect.top - radius,
            rect.right,
            rect.top
        )
        var direction = CollisionDirection.TOP
        var minMove = calculateCollisionMove(topRect, CollisionDirection.TOP)
        val leftRect = RectF(
            rect.left - radius,
            rect.top,
            rect.left,
            rect.bottom
        )
        val bottomRect = RectF(
            rect.left,
            rect.bottom,
            rect.right,
            rect.bottom + radius
        )
        val bottomMove = calculateCollisionMove(bottomRect, CollisionDirection.BOTTOM)
        if (bottomMove < minMove) {
            minMove = bottomMove
            direction = CollisionDirection.BOTTOM
        }
        val leftMove = calculateCollisionMove(leftRect, CollisionDirection.LEFT)
        if (leftMove < minMove) {
            minMove = leftMove
            direction = CollisionDirection.LEFT
        }
        val rightRect = RectF(
            rect.right,
            rect.top,
            rect.right + radius,
            rect.bottom
        )
        val rightMove = calculateCollisionMove(rightRect, CollisionDirection.RIGHT)
        if (rightMove < minMove) {
            minMove = rightMove
            direction = CollisionDirection.RIGHT
        }
        return Pair(direction, minMove)
    }

    /**
     * 计算小球的中心以某一个方向进入一个矩形需要的距离
     */
    private fun calculateCollisionMove(rectF: RectF, direction: CollisionDirection): Float {
        val angle = Math.toRadians(angle.toDouble()).toFloat()
        //X轴方向移动系数
        val cos = cos(angle)
        //Y轴方向移动系数
        val sin = sin(angle)
        //要想撞到顶部区域，Y轴移动必须是正的，其他方向以此类推
        if (direction == CollisionDirection.TOP && sin <= 0) {
            return Float.MAX_VALUE
        }
        if (direction == CollisionDirection.BOTTOM && sin >= 0) {
            return Float.MAX_VALUE
        }
        if (direction == CollisionDirection.RIGHT && cos >= 0) {
            return Float.MAX_VALUE
        }
        if (direction == CollisionDirection.LEFT && cos <= 0) {
            return Float.MAX_VALUE
        }
        val centerPoint = this.centerPoint
        val toLeft = rectF.left - centerPoint.x
        val toRight = rectF.right - centerPoint.x
        val toTop = rectF.top - centerPoint.y
        val toBottom = rectF.bottom - centerPoint.y
        if (toLeft >= 0 && cos <= 0) {
            return Float.MAX_VALUE
        }
        if (toRight <= 0 && cos >= 0) {
            return Float.MAX_VALUE
        }
        if (toTop >= 0 && sin <= 0) {
            return Float.MAX_VALUE
        }
        if (toBottom <= 0 && sin >= 0) {
            return Float.MAX_VALUE
        }
        val distanceX: Pair<Float, Float> = when {
            cos == 0f -> Pair(0f, Float.MAX_VALUE)
            toLeft >= 0 -> Pair(toLeft / cos, toRight / cos)
            toRight <= 0 -> Pair(toRight / cos, toLeft / cos)
            cos > 0 -> Pair(0f, toRight / cos)
            else -> Pair(0f, toLeft / cos)
        }
        val distanceY: Pair<Float, Float> = when {
            sin == 0f -> Pair(0f, Float.MAX_VALUE)
            toTop >= 0 -> Pair(toTop / sin, toBottom / sin)
            toBottom <= 0 -> Pair(toBottom / sin, toTop / sin)
            sin > 0 -> Pair(0f, toBottom / sin)
            else -> Pair(0f, toTop / sin)
        }
        val min = max(distanceX.first, distanceY.first)
        val max = min(distanceX.second, distanceY.second)
        return if (max < min) Float.MAX_VALUE else min
    }
}