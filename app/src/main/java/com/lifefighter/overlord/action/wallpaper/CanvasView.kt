package com.lifefighter.overlord.action.wallpaper

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.lifefighter.widget.wallpaper.CanvasPainter

/**
 * @author xzp
 * @created on 2021/12/25.
 */
class CanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var canvasPainter: CanvasPainter? = null

    override fun onDraw(canvas: Canvas) {
        canvasPainter?.onDraw(canvas)
        this.postInvalidateDelayed(20)
    }
}
