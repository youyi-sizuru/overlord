package com.lifefighter.overlord.action.wallpaper.bricks

/**
 * 碰撞移动距离
 * [block] 撞击物
 * [move] 需要移动多少距离才能撞击到这个撞击物
 * [direction]撞击方向
 */
class CollisionMove(val block: CollisionBlock, val move: Float, val direction: CollisionDirection) :
    Comparable<CollisionMove> {
    override fun compareTo(other: CollisionMove): Int {
        return when {
            move > other.move -> {
                1
            }
            move < other.move -> {
                -1
            }
            else -> {
                direction.compareTo(other.direction)
            }
        }
    }

    fun isDead(): Boolean {
        return block.isDead(direction)
    }

}