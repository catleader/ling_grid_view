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

    private val gridLineBoldRepeatPeriod: Int = 5

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

    var gridScaleHorizontalStep: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var gridScaleVerticalStep: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var gridScaleHorizontalStepMultiplier: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var gridScaleVerticalStepMultiplier: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var mapZoomLevel = 19f

    private var pixelsPerMeter = 0f

    private var textWidth = 0f

    private var textHeight = 0f

    private var textBounds = Rect()

    private val rect = Rect()

    private var gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80f0c929")
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density
    }

    private var gridLinePaintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80d8c83e")
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 2
    }

    private var gridBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80f0c929")
        style = Paint.Style.FILL
    }

    private var scaleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A000000")
        style = Paint.Style.STROKE
        strokeWidth = extraPadding.toFloat() * 2
    }

    private val textPaintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6ffffff")
        textSize = context.resources.displayMetrics.density * 10f
        textAlign = Paint.Align.CENTER
    }

    private val textPaintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6ffffff")
        textSize = context.resources.displayMetrics.density * 9f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return

        val contact = lingGridContract ?: return

        with(canvas) {
            var extraPadding2Times = extraPadding * 2

            val gridScaleHorizontalStep =
                gridScaleHorizontalStep * gridScaleHorizontalStepMultiplier

            val gridScaleVerticalStep = gridScaleVerticalStep * gridScaleVerticalStepMultiplier

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

            // drawRect(rect, gridLinePaint)

            drawRect(rect, gridBgPaint)


            rect.set(0, 0, width, height)

            if (showGridScaleLabel) drawRect(rect, scaleBgPaint)

            val gridWidthSizeInMeters = contact.getGridSizeInMeters().first

            pixelsPerMeter = (width - extraPadding2Times) / gridWidthSizeInMeters.toFloat()

            val gridWidth = (width - extraPadding2Times).toFloat()

            val gridHeightSizeInMeters = contact.getGridSizeInMeters().second

            val gridHeight = (height - extraPadding2Times).toFloat()

            val horizontalScaleSqr = this@GridUI.gridScaleHorizontalStep * gridLineBoldRepeatPeriod

            val verticalScaleSqr = this@GridUI.gridScaleVerticalStep * gridLineBoldRepeatPeriod

            for (i in 0..gridWidthSizeInMeters) {
                when {
                    i % gridScaleHorizontalStep == 0 -> {
                        drawLine(
                            extraPadding + i * pixelsPerMeter,
                            extraPadding.toFloat(),
                            extraPadding + i * pixelsPerMeter,
                            extraPadding + gridHeight,
                            if (i == 0 ||
                                i % (if (gridScaleHorizontalStep == gridLineBoldRepeatPeriod * gridScaleHorizontalStepMultiplier) {
                                    horizontalScaleSqr * gridScaleHorizontalStepMultiplier
                                } else {
                                    gridLineBoldRepeatPeriod
                                }) == 0
                            ) gridLinePaintBold else gridLinePaint
                        )
                    }
                    i == gridWidthSizeInMeters -> {
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

                    if (i == gridWidthSizeInMeters || (i != 0 && i % gridScaleHorizontalStep == 0)) {
                        drawText(
                            "$i",
                            extraPadding + i * pixelsPerMeter,
                            extraPadding.toFloat() - (extraPadding - textHeight) / 2f,
                            textPaint
                        )
                    }

                    val label = gridWidthSizeInMeters - i
                    if (label != 0 && i % gridScaleHorizontalStep == 0) {
                        drawText(
                            "$label",
                            extraPadding + i * pixelsPerMeter,
                            height - (extraPadding - textHeight) / 2f,
                            textPaint
                        )
                    }


                }
            }

            for (i in 0..gridHeightSizeInMeters) {
                when {
                    i % gridScaleVerticalStep == 0 -> {
                        drawLine(
                            extraPadding.toFloat(),
                            extraPadding + i * pixelsPerMeter,
                            gridWidth + extraPadding,
                            extraPadding + i * pixelsPerMeter,
                            if (i == 0 || i % (if (gridScaleVerticalStep == gridLineBoldRepeatPeriod * gridScaleVerticalStepMultiplier) {
                                    verticalScaleSqr * gridScaleVerticalStepMultiplier
                                } else {
                                    gridLineBoldRepeatPeriod
                                }) == 0
                            ) gridLinePaintBold else gridLinePaint
                        )
                    }
                    i == gridHeightSizeInMeters -> {
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

                    if (i == gridHeightSizeInMeters || (i != 0 && i % gridScaleVerticalStep == 0)) {
                        drawText(
                            "$i",
                            extraPadding / 2f,
                            (extraPadding + i * pixelsPerMeter) + textHeight / 2,
                            textPaint
                        )
                    }
                    val label = gridHeightSizeInMeters - i
                    if (label != 0 && i % gridScaleVerticalStep == 0) {
                        drawText(
                            "$label",
                            width - extraPadding / 2f,
                            (extraPadding + i * pixelsPerMeter) + textHeight / 2,
                            textPaint
                        )
                    }

                }
            }

        }
    }

}