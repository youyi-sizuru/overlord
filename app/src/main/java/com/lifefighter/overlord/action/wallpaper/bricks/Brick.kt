package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

/**
 * 砖块们
 */
class Bricks(private val game: BricksGame) : ResetAble, DrawAble, CollisionBlock {
    private val brickList = mutableListOf<Brick>()
    override fun onDraw(canvas: Canvas) {
        for (brick in brickList) {
            brick.onDraw(canvas)
        }
    }

    override fun reset() {
        val row = 4 + min(game.level, 10)
        val column = 2 + min(game.level / 2, 5)
        val height = game.height / (4 * row)
        val space = game.width / 60f
        val width = ((game.width - (column + 1) * space) / column).toInt()
        brickList.clear()
        var offsetX = 0f
        var offsetY = 0f
        for (i in 0 until row) {
            offsetY += space
            for (j in 0 until column) {
                offsetX += space
                brickList.add(
                    Brick(
                        RectF(offsetX, offsetY, offsetX + width, offsetY + height),
                        if ((i + j) % 2 == 0) Color.BLUE else Color.WHITE
                    )
                )
                offsetX += width
            }
            offsetY += height
            offsetX = 0f
        }
    }

    override fun isDead(direction: CollisionDirection): Boolean {
        return false
    }

    override fun calculateCollisionMove(ball: Ball): CollisionMove {
        return brickList.map {
            it.calculateCollisionMove(ball)
        }.minOrNull() ?: CollisionMove(this, Float.MAX_VALUE, CollisionDirection.BOTTOM)
    }

    fun isWin(): Boolean {
        return brickList.isNotEmpty() && brickList.none {
            it.hasBreak.not()
        }
    }
}

class Brick(
    private val rect: RectF,
    color: Int
) : DrawAble,
    CollisionBlock {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.color = color
        paint.style = Paint.Style.FILL
    }

    /**
     * 被击毁
     */
    var hasBreak = false
    override fun onDraw(canvas: Canvas) {
        if (hasBreak.not()) {
            canvas.drawRect(rect, paint)
        }
    }

    override fun isDead(direction: CollisionDirection): Boolean {
        return false
    }

    override fun calculateCollisionMove(ball: Ball): CollisionMove {
        if (hasBreak) {
            return CollisionMove(this, Float.MAX_VALUE, CollisionDirection.BOTTOM)
        }
        val move = ball.calculateCollisionMove(rect)
        return CollisionMove(this, move.second, move.first)
    }

    override fun afterCollision() {
        hasBreak = true
    }
}