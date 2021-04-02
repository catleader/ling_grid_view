package com.catleader.ling_grid_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GridUI @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var lingGridContract: LingGridContract? = null

    var extraPadding = 0
        set(value) {
            field = value
            rectBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#40000000")
                style = Paint.Style.STROKE
                strokeWidth = value.toFloat() * 2f
            }
        }

    var gridLineColor: Int = Color.YELLOW
        set(value) {
            field = value
            gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = value
                style = Paint.Style.STROKE
                strokeWidth = context.resources.displayMetrics.density
            }
        }


    var gridBgColor: Int = Color.YELLOW
        set(value) {
            field = value
            gridBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = value
                style = Paint.Style.FILL
            }
        }

    var mapZoomLevel = 19f

    private val tag = "GridUI"

    private var pushPointX = 0f

    private var pushPointY = 0f

    private var gridWidthPixelGap = 0f

    private var gridHeightPixelGap = 0f

    private var textWidth = 0f

    private var textBounds = Rect()

    private val extraPadding2Times: Int by lazy {
        extraPadding * 2
    }

    private val rect = Rect()

    private var gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#fff0c929")
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density
    }

    private var gridBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80f0c929")
        style = Paint.Style.FILL
    }

    private var rectBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40000000")
        style = Paint.Style.STROKE
        strokeWidth = extraPadding.toFloat()
    }

    private val textPaintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = context.resources.displayMetrics.density * 10f
        textAlign = Paint.Align.CENTER
    }

    private val textPaintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = context.resources.displayMetrics.density * 9f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return

        val contact = lingGridContract ?: return
        with(canvas) {
            rect.set(
                extraPadding,
                extraPadding,
                width - extraPadding,
                height - extraPadding
            )

            drawRect(rect, gridLinePaint)

            drawRect(rect, gridBgPaint)

            if (mapZoomLevel >= 20f) {

                rect.set(0, 0, width, height)

                drawRect(rect, rectBgPaint)

                val gridWidthSizeInMeters = contact.getGridSizeInMeters().first

                gridWidthPixelGap = (width - extraPadding2Times) / gridWidthSizeInMeters.toFloat()

                val gridWidth = (width - extraPadding2Times).toFloat()

                val gridHeightSizeInMeters = contact.getGridSizeInMeters().second

                gridHeightPixelGap =
                    (height - extraPadding2Times) / gridHeightSizeInMeters.toFloat()

                val gridHeight = (height - extraPadding2Times).toFloat()

                for (i in 1..gridWidthSizeInMeters) {
                    val textPaint = if (i < 10) textPaintNormal else textPaintSmall
                    textWidth = textPaint.measureText("$i")

                    textPaint.getTextBounds("$i", 0, "$i".length, textBounds)

                    drawText(
                        "$i",
                        (extraPadding + i * gridWidthPixelGap) - gridWidthPixelGap / 2f,
                        extraPadding.toFloat() - (extraPadding - textBounds.height()) / 2f,
                        textPaint
                    )

                    drawText(
                        "${gridWidthSizeInMeters - i + 1}",
                        (extraPadding + i * gridWidthPixelGap) - gridWidthPixelGap / 2f,
                        height - (extraPadding - textBounds.height()) / 2f,
                        textPaint
                    )

                    if (i < gridWidthSizeInMeters) {
                        drawLine(
                            extraPadding + i * gridWidthPixelGap,
                            extraPadding.toFloat(),
                            extraPadding + i * gridWidthPixelGap,
                            extraPadding + gridHeight,
                            gridLinePaint
                        )
                    }
                }

                for (i in 1..gridHeightSizeInMeters) {
                    val textPaint = if (i < 10) textPaintNormal else textPaintSmall

                    textWidth = textPaint.measureText("$i")

                    textPaint.getTextBounds("$i", 0, "$i".length, textBounds)

                    drawText(
                        "$i",
                        extraPadding / 2f,
                        (extraPadding + i * gridHeightPixelGap) - (gridHeightPixelGap - textBounds.height()) / 2f,
                        textPaint
                    )

                    drawText(
                        "${gridHeightSizeInMeters - i + 1}",
                        width - extraPadding / 2f,
                        (extraPadding + i * gridHeightPixelGap) - (gridHeightPixelGap - textBounds.height()) / 2f,
                        textPaint
                    )

                    if (i < gridHeightSizeInMeters) {
                        drawLine(
                            extraPadding.toFloat(),
                            extraPadding + i * gridHeightPixelGap,
                            gridWidth + extraPadding,
                            extraPadding + i * gridHeightPixelGap,
                            gridLinePaint
                        )
                    }

                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return true
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pushPointX = event.rawX
                pushPointY = event.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val xOffset = (event.rawX - pushPointX).toInt()
                val yOffset = (event.rawY - pushPointY).toInt()
                pushPointX = event.rawX
                pushPointY = event.rawY
                lingGridContract?.onGridMove(xOffset, yOffset)
                true
            }
            else -> false
        }
    }

}