package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import com.lifefighter.overlord.action.wallpaper.CanvasGame

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
    private val ball: Ball = Ball(this)


    /**
     * 打砖块下方的那个板子
     */
    private val board: Board = Board(this)

    init {
        reset()
    }

    private fun reset() {
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
        updateLevel(this.level - 1)
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
        canvas.restore()
    }

    override fun onOffset(xOffset: Float, yOffset: Float) {
    }

    override fun onStart() {
        reset()
    }

    override fun onEnd() {
    }

    override fun getName(): String {
        return "打砖块壁纸"
    }
}