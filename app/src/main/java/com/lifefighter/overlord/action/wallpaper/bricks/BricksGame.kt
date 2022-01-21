package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import com.lifefighter.overlord.action.wallpaper.CanvasGame
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class BricksGame : CanvasGame {

    /**
     * 游戏面板适配宽度
     */
    val width = 1080

    /**
     * 游戏面板适配高度
     */
    val height = 1920

    /**
     * 游戏等级
     */
    var level: Int = 0
        private set

    /**
     * 打砖块用的球
     */
    val ball: Ball = Ball(this)


    /**
     * 打砖块下方的那个板子
     */
    val board: Board = Board(this)

    private var lastOffsetX: Float? = 0f

    init {
        reset()
    }

    private fun reset() {
        lastOffsetX = null
        board.reset()
        ball.reset()
    }

    /**
     * 重置关卡
     */
    fun updateLevel(level: Int) {
        this.level = level
        reset()
    }

    /**
     * 上一关
     */
    fun downLevel() {
        updateLevel(max(0, this.level - 1))
    }

    /**
     * 下一关
     */
    fun upLevel() {
        updateLevel(this.level + 1)
    }

    /**
     * 开始绘制
     */
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        canvas.save()
        canvas.scale(canvas.width / width.toFloat(), canvas.height / height.toFloat())
        board.draw(canvas)
        ball.draw(canvas)
        canvas.restore()
    }

    override fun onOffset(xOffset: Float, yOffset: Float) {
        val lastOffsetX = lastOffsetX ?: xOffset
        this.lastOffsetX = xOffset
        board.offset((xOffset - lastOffsetX) * -width)
        if (xOffset != lastOffsetX) {
            ball.start()
        }
    }

    override fun onStart() {
        reset()
    }

    override fun onEnd() {
    }

    override fun getName(): String {
        return "打砖块壁纸"
    }

    /**
     * 计算小球移动后撞击到其他物体上的需要多长的距离
     */
    fun calculateCollisionMove(ball: Ball): CollisionMove {
        var minMove = CollisionMove(Float.MAX_VALUE)
        val angle = Math.toRadians(ball.angle.toDouble()).toFloat()
        //X轴方向移动系数
        val cos = cos(angle)
        //Y轴方向移动系数
        val sin = sin(angle)
        if (cos > 0) {
            minMove = minOf(minMove, CollisionMove((width - ball.right) / cos, true))

        } else if (cos < 0) {
            minMove = minOf(minMove, CollisionMove((ball.left / -cos), true))
        }
        if (sin > 0) {
            minMove = minOf(minMove, CollisionMove((height - ball.bottom) / sin))
        } else if (sin < 0) {
            minMove = minOf(minMove, CollisionMove(ball.top / -sin))
        }
        return minMove
    }
}

/**
 * 碰撞移动距离
 * [move] 移动距离
 * [vertical]是否撞击到垂直方向
 */
class CollisionMove(val move: Float, val vertical: Boolean = false) : Comparable<CollisionMove> {
    override fun compareTo(other: CollisionMove): Int {
        return when {
            move > other.move -> {
                1
            }
            move < other.move -> {
                -1
            }
            else -> {
                vertical.compareTo(other.vertical)
            }
        }
    }

}