package com.catleader.ling_grid_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat


class GridSizeSetter @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val tag = "GridSizeSetter"

    var lingGridContract: LingGridContract? = null

    init {
        background = ContextCompat.getDrawable(context, R.drawable.ic_grid_size_setter)
    }

    private var textWidth = 0f

    private var textHeight = 0f

    private var textBounds = Rect()

    private val textPaintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        textSize = context.resources.displayMetrics.density * 12f
        textAlign = Paint.Align.CENTER
    }

    private val textPaintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFFFF")
        textSize = context.resources.displayMetrics.density * 11f
        textAlign = Paint.Align.CENTER
    }

    var gridSize = 120

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return

        with(canvas) {
            val textPaint = if (gridSize < 10) textPaintNormal else textPaintSmall
            textWidth = textPaint.measureText("$gridSize")
            textPaint.getTextBounds(
                "$gridSize",
                0,
                "$gridSize".length,
                textBounds
            )
            textHeight = textBounds.height().toFloat()

            drawText(
                "$gridSize",
                width/2f,
                height/2f + textHeight/2f,
                textPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return true
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lingGridContract?.onGridSizeSetterClicked()
                true
            }
            else -> false
        }
    }


}