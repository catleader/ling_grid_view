package com.catleader.ling_grid_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class GridUI @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val tag = "GridUI"

    var extraPadding: Int = 0

    init {
        extraPadding = (resources.displayMetrics.density * 16).toInt()
    }

    var lingGridContract: LingGridContract? = null


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

    var showGridScaleLabel: Boolean = true
        set(value) {
            field = value
            extraPadding = if (field) {
                (resources.displayMetrics.density * 16).toInt()
            } else {
                0
            }
        }

    var gridScaleStep: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var mapZoomLevel = 19f

    private var pixelsPerMeter = 0f

    private var pixelsPerMeterV2 = 0f

    private var textWidth = 0f

    private var textHeight = 0f

    private var textBounds = Rect()

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
        strokeWidth = extraPadding.toFloat() * 2
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

            var extraPadding2Times = extraPadding * 2

            if (showGridScaleLabel) {
                rect.set(
                    extraPadding,
                    extraPadding,
                    width - extraPadding,
                    height - extraPadding
                )

            } else {

                rect.set(0, 0, width, height)

                extraPadding = 0
                extraPadding2Times = 0
            }

            drawRect(rect, gridLinePaint)

            drawRect(rect, gridBgPaint)

            if (mapZoomLevel >= contact.getZoomLevelForGridLineDrawing()) {

                rect.set(0, 0, width, height)

                if (showGridScaleLabel) drawRect(rect, rectBgPaint)

                val gridWidthSizeInMeters = contact.getGridSizeInMeters().first

                pixelsPerMeter = (width - extraPadding2Times) / gridWidthSizeInMeters.toFloat()

                val gridWidth = (width - extraPadding2Times).toFloat()

                val gridHeightSizeInMeters = contact.getGridSizeInMeters().second

                val gridHeight = (height - extraPadding2Times).toFloat()

                for (i in 1..gridWidthSizeInMeters) {
                    if (i < gridWidthSizeInMeters) {
                        if (i % gridScaleStep == 0) {
                            drawLine(
                                extraPadding + i * pixelsPerMeter,
                                extraPadding.toFloat(),
                                extraPadding + i * pixelsPerMeter,
                                extraPadding + gridHeight,
                                gridLinePaint
                            )
                        }
                    }

                    if (showGridScaleLabel) {
                        val textPaint = if (i < 10) textPaintNormal else textPaintSmall
                        textWidth = textPaint.measureText("$i")

                        textPaint.getTextBounds("$i", 0, "$i".length, textBounds)

                        textHeight = textBounds.height().toFloat()

                        if (i % gridScaleStep == 0) {
                            drawText(
                                "$i",
                                extraPadding + i * pixelsPerMeter,
                                extraPadding.toFloat() - (extraPadding - textHeight) / 2f,
                                textPaint
                            )
                        }

                        val label = gridWidthSizeInMeters - i + 1
                        if (label % gridScaleStep == 0) {
                            drawText(
                                "$label",
                                extraPadding + (i - 1) * pixelsPerMeter,
                                height - (extraPadding - textHeight) / 2f,
                                textPaint
                            )
                        }

                    }
                }

                for (i in 1..gridHeightSizeInMeters) {
                    if (i < gridHeightSizeInMeters) {

                        if (i % gridScaleStep == 0) {
                            drawLine(
                                extraPadding.toFloat(),
                                extraPadding + i * pixelsPerMeter,
                                gridWidth + extraPadding,
                                extraPadding + i * pixelsPerMeter,
                                gridLinePaint
                            )
                        }
                    }

                    if (showGridScaleLabel) {

                        val textPaint = if (i < 10) textPaintNormal else textPaintSmall

                        textWidth = textPaint.measureText("$i")

                        textPaint.getTextBounds("$i", 0, "$i".length, textBounds)

                        textHeight = textBounds.height().toFloat()


                        if (i % gridScaleStep == 0) {
                            drawText(
                                "$i",
                                extraPadding / 2f,
                                (extraPadding + i * pixelsPerMeter) + textHeight / 2,
                                textPaint
                            )
                        }

                        val label = gridHeightSizeInMeters - i + 1
                        if (label % gridScaleStep == 0) {
                            drawText(
                                "$label",
                                width - extraPadding / 2f,
                                (extraPadding + (i - 1) * pixelsPerMeter) + textHeight / 2,
                                textPaint
                            )
                        }

                    }
                }


            }
        }
    }

}