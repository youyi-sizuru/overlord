package com.lifefighter.overlord.action.wallpaper.bricks

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