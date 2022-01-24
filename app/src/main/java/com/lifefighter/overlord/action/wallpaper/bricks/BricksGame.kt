package com.lifefighter.overlord.action.wallpaper.bricks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import com.lifefighter.overlord.action.wallpaper.CanvasGame
import kotlin.math.max

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class BricksGame : CanvasGame, DrawAble, ResetAble {

    /**
     * 游戏面板适配宽度
     */
    val width
        get() = wall.width

    /**
     * 游戏面板适配高度
     */
    val height
        get() = wall.height

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

    val wall: Wall = Wall()

    val bricks = Bricks(this)

    private var lastOffsetX: Float? = 0f

    /**
     * 用来计算的线程
     */
    private var ioThread: HandlerThread? = null

    /**
     * 消息发送机器
     */
    private var messageHandler: Handler? = null

    private var paused = false
    override fun reset() {
        messageHandler?.removeCallbacksAndMessages(null)
        messageHandler?.post {
            synchronized(this@BricksGame) {
                lastOffsetX = null
                wall.reset()
                board.reset()
                ball.reset()
                bricks.reset()
            }
        }
    }

    /**
     * 重置关卡
     */
    fun updateLevel(level: Int) {
        messageHandler?.post {
            synchronized(this@BricksGame) {
                this.level = level
                reset()
            }
        }

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
        synchronized(this@BricksGame) {
            if (paused) {
                return@synchronized
            }
            canvas.drawColor(Color.BLACK)
            canvas.save()
            if (canvas.width < width || canvas.height < height) {
                val scale =
                    1 / max(width.toFloat() / canvas.width, height.toFloat() / canvas.height)
                canvas.scale(scale, scale)
                canvas.translate(
                    (canvas.width - width.toFloat() * scale) / 2,
                    (canvas.height - height.toFloat() * scale) / 2
                )
            } else {
                canvas.translate(
                    (canvas.width - width.toFloat()) / 2,
                    (canvas.height - height.toFloat()) / 2
                )
            }
            wall.onDraw(canvas)
            board.onDraw(canvas)
            bricks.onDraw(canvas)
            ball.onDraw(canvas)
            canvas.restore()
            calculate()
        }
    }

    /**
     * 进行各种计算
     */
    private fun calculate() {
        messageHandler?.post {
            synchronized(this@BricksGame) {
                ball.move()
                if (bricks.isWin()) {
                    upLevel()
                }
            }
        }
    }

    override fun onOffset(xOffset: Float, yOffset: Float) {
        messageHandler?.post {
            synchronized(this@BricksGame) {
                val lastOffsetX = lastOffsetX ?: xOffset
                this.lastOffsetX = xOffset
                board.offset((xOffset - lastOffsetX) * -width)
                if (xOffset != lastOffsetX) {
                    ball.start()
                }
            }
        }
    }

    override fun onStart(context: Context) {
        ioThread = HandlerThread("bricks").also {
            it.start()
            messageHandler = Handler(it.looper)
        }
        reset()
    }

    override fun onResume() {
        messageHandler?.post {
            synchronized(this@BricksGame) {
                ball.resume()
                paused = false
            }
        }
    }

    override fun onPause() {
        messageHandler?.post {
            synchronized(this@BricksGame) {
                paused = true
                ball.pause()
            }
        }
    }

    override fun onEnd(context: Context) {
        messageHandler?.removeCallbacksAndMessages(null)
        messageHandler = null
        ioThread?.quit()
        ioThread = null
    }

    override fun getName(): String {
        return "打砖块壁纸"
    }

    /**
     * 计算小球移动后撞击到其他物体上的需要多长的距离
     */
    fun calculateCollisionMove(ball: Ball): CollisionMove {
        var minMove = wall.calculateCollisionMove(ball)
        minMove = minOf(minMove, board.calculateCollisionMove(ball))
        minMove = minOf(minMove, bricks.calculateCollisionMove(ball))
        return minMove
    }
}

