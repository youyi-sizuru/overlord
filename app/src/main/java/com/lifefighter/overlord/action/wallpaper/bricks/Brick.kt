package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.min

/**
 * 砖块们
 */
class Bricks(private val game: BricksGame) : ResetAble, DrawAble, CollisionBlock {
    private val brickList = mutableListOf<List<Brick>>()
    private var space: Float = 0f
    override fun onDraw(canvas: Canvas) {
        canvas.save()
        for (list in brickList) {
            canvas.translate(0f, space)
            canvas.save()
            for (brick in list) {
                canvas.translate(space, 0f)
                brick.onDraw(canvas)
                canvas.translate(brick.width.toFloat(), 0f)
            }
            canvas.restore()
            canvas.translate(0f, space)
        }
        canvas.restore()
    }

    override fun reset() {
        val row = 4 + min(game.level, 10)
        val column = 2 + min(game.level / 2, 5)
        val height = game.height / (4 * row)
        space = height.toFloat()
        val width = ((game.width - (column + 1) * space) / column).toInt()
        brickList.clear()
        for (i in 0 until row) {
            val list = mutableListOf<Brick>()
            for (j in 0 until column) {
                list.add(Brick(width, height))
            }
            brickList.add(list)
        }
    }

    override fun isDead(direction: CollisionDirection): Boolean {
        return false
    }

    override fun calculateCollisionMove(ball: Ball): CollisionMove {
        return brickList.flatten().map {
            it.calculateCollisionMove(ball)
        }.minOrNull() ?: CollisionMove(this, Float.MAX_VALUE, CollisionDirection.BOTTOM)
    }

    fun isWin(): Boolean {
        return brickList.isNotEmpty() && brickList.flatten().none {
            it.hasBreak.not()
        }
    }
}

class Brick(val width: Int, val height: Int) : DrawAble, CollisionBlock {
    var hasBreak = false
    override fun onDraw(canvas: Canvas) {
        if (hasBreak.not()) {
            canvas.save()
            canvas.clipRect(0, 0, width, height)
            canvas.drawColor(Color.WHITE)
            canvas.restore()
        }
    }

    override fun isDead(direction: CollisionDirection): Boolean {
        return false
    }

    override fun calculateCollisionMove(ball: Ball): CollisionMove {
        if (hasBreak) {
            return CollisionMove(this, Float.MAX_VALUE, CollisionDirection.BOTTOM)
        }
        return CollisionMove(this, Float.MAX_VALUE, CollisionDirection.BOTTOM)
    }

    override fun afterCollision() {
        hasBreak = true
    }
}