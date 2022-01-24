package com.lifefighter.overlord.action.wallpaper.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.lifefighter.overlord.action.wallpaper.CanvasGame
import java.util.*
import kotlin.math.min

/**
 * @author xzp
 * @created on 2022/1/19.
 */
class ClockGame : CanvasGame {
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onStart(context: Context) {
    }

    override fun onEnd(context: Context) {
    }

    override fun getName(): String {
        return "时钟壁纸"
    }


    override fun onDraw(canvas: Canvas) {
        val minSize = min(canvas.width, canvas.height)
        if (minSize == 0) {
            return
        }
        canvas.drawColor(Color.BLACK)
        val clockRadius = minSize * 0.4f
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f
        clockPaint.style = Paint.Style.STROKE
        clockPaint.strokeWidth = minSize / 1080f * 10
        clockPaint.color = Color.WHITE
        canvas.save()
        for (i in 0 until 12) {
            canvas.drawLine(
                centerX,
                centerY - clockRadius,
                centerX,
                centerY - clockRadius * if (i % 3 == 0) 0.9f else 0.95f,
                clockPaint
            )
            canvas.rotate(30f, centerX, centerY)
        }
        canvas.restore()
        canvas.drawOval(
            centerX - clockRadius,
            centerY - clockRadius,
            centerX + clockRadius,
            centerY + clockRadius,
            clockPaint
        )
        clockPaint.style = Paint.Style.FILL
        clockPaint.textAlign = Paint.Align.CENTER
        clockPaint.textSize = clockRadius * 0.12f
        clockPaint.strokeWidth = minSize / 1080f * 3
        canvas.drawText("12", centerX, centerY - clockRadius * 1.09f, clockPaint)
        canvas.drawText(
            "3",
            centerX + clockRadius * 1.12f,
            centerY + clockRadius * 0.03f,
            clockPaint
        )
        canvas.drawText("6", centerX, centerY + clockRadius * 1.18f, clockPaint)
        canvas.drawText(
            "9",
            centerX - clockRadius * 1.12f,
            centerY + clockRadius * 0.03f,
            clockPaint
        )

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR) * 5
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        clockPaint.strokeWidth = minSize / 1080f * 10
        clockPaint.color = Color.WHITE
        canvas.save()
        canvas.rotate((hour + minute / 12f) * 6f, centerX, centerY)
        canvas.drawLine(centerX, centerY, centerX, centerY - clockRadius * 0.6f, clockPaint)
        canvas.restore()
        clockPaint.color = Color.rgb(215, 216, 96)
        canvas.save()
        canvas.rotate((minute + second / 60f) * 6f, centerX, centerY)
        canvas.drawLine(centerX, centerY, centerX, centerY - clockRadius * 0.7f, clockPaint)
        canvas.restore()
        clockPaint.color = Color.RED
        canvas.save()
        canvas.rotate(second * 6f, centerX, centerY)
        canvas.drawLine(centerX, centerY, centerX, centerY - clockRadius * 0.8f, clockPaint)
        canvas.restore()

        clockPaint.color = Color.WHITE
        canvas.drawOval(
            centerX - clockRadius * 0.05f,
            centerY - clockRadius * 0.05f,
            centerX + clockRadius * 0.05f,
            centerY + clockRadius * 0.05f,
            clockPaint
        )
    }

    override fun onOffset(xOffset: Float, yOffset: Float) {
    }
}