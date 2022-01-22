package com.lifefighter.overlord.action.wallpaper.bricks

import android.graphics.Canvas

/**
 * 可绘制
 */
interface DrawAble {
    fun onDraw(canvas: Canvas)
}

/**
 * 可重置
 */
interface ResetAble {
    fun reset()
}

/**
 * 可碰撞体
 */
interface CollisionBlock {
    /**
     * 碰到这个物品的那个方向会死亡
     */
    fun isDead(direction: CollisionDirection): Boolean

    /**
     * 计算小球以当前角度运动碰到该物体需要多长的距离
     */
    fun calculateCollisionMove(ball: Ball): CollisionMove

    /**
     * 发生碰撞后
     */
    fun afterCollision() {}
}

/**
 * 碰撞方向
 */
enum class CollisionDirection {
    LEFT, TOP, RIGHT, BOTTOM;
}