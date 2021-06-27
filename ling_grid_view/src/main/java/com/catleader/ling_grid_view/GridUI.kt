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

    enum class GridMode { GridCenterLabel, GridLeftRightLabel }

    private val gridMode = GridMode.GridCenterLabel

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

            val horizontalStep = gridScaleHorizontalStep * gridScaleHorizontalStepMultiplier

            val verticalStep = gridScaleVerticalStep * gridScaleVerticalStepMultiplier

            val boldHorizontalStep = horizontalStep * gridLineBoldRepeatPeriod

            val boldVerticalStep = verticalStep * gridLineBoldRepeatPeriod

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

            val gridHeightSizeInMeters = contact.getGridSizeInMeters().second

            pixelsPerMeter = (width - extraPadding2Times) / gridWidthSizeInMeters.toFloat()

            val gridWidth = (width - extraPadding2Times).toFloat()

            val gridHeight = (height - extraPadding2Times).toFloat()

            if (gridMode == GridMode.GridCenterLabel) {
                drawGridCenterMode(
                    gridWidthSizeInMeters,
                    gridHeightSizeInMeters,
                    gridWidth,
                    gridHeight,
                    verticalStep,
                    horizontalStep,
                    boldVerticalStep,
                    boldHorizontalStep
                )
            } else if (gridMode == GridMode.GridLeftRightLabel) {
                drawGridLeftToRightMode(
                    gridWidthSizeInMeters,
                    gridHeightSizeInMeters,
                    gridWidth,
                    gridHeight,
                    boldVerticalStep,
                    boldHorizontalStep
                )
            }

        }
    }

    private fun Canvas.drawGridCenterMode(
        gridWidthSizeInMeters: Int,
        gridHeightSizeInMeters: Int,
        gridWidth: Float,
        gridHeight: Float,
        verticalStep: Int,
        horizontalStep: Int,
        boldVerticalStep: Int,
        boldHorizontalStep: Int
    ) {
        for (i in 0..gridWidthSizeInMeters) {

            val centerNumber = gridWidthSizeInMeters / 2

            val drawingNumber = when {
                i < centerNumber -> centerNumber - i
                i > centerNumber -> i - centerNumber
                else -> 0
            }

            val isStartOrCenterOrEndNumber = i == 0
                    || drawingNumber == 0
                    || i == gridWidthSizeInMeters

            // check if we need to draw a line.
            // Always draw a line if it a start number, center number or ending number.
            if (isStartOrCenterOrEndNumber || drawingNumber % horizontalStep == 0) {
                val boldLine = drawingNumber % boldHorizontalStep == 0
                val paint = if (boldLine) gridLinePaintBold else gridLinePaint

                drawLine(
                    extraPadding + i * pixelsPerMeter,
                    extraPadding.toFloat(),
                    extraPadding + i * pixelsPerMeter,
                    extraPadding + gridHeight,
                    paint,
                )

                if (showGridScaleLabel) {
                    val textPaint = if (i < 10) textPaintNormal else textPaintSmall
                    textWidth = textPaint.measureText("$drawingNumber")
                    textPaint.getTextBounds(
                        "$drawingNumber",
                        0,
                        "$drawingNumber".length,
                        textBounds
                    )
                    textHeight = textBounds.height().toFloat()

                    drawText(
                        "$drawingNumber",
                        extraPadding + i * pixelsPerMeter,
                        extraPadding.toFloat() - (extraPadding - textHeight) / 2f,
                        textPaint
                    )
                }
            }
        }


        for (i in 0..gridHeightSizeInMeters) {
            val centerNumber = gridHeightSizeInMeters / 2

            val drawingNumber = when {
                i < centerNumber -> centerNumber - i
                i > centerNumber -> i - centerNumber
                else -> 0
            }

            val isStartOrCenterOrEndNumber = i == 0
                    || drawingNumber == 0
                    || i == gridHeightSizeInMeters

            // check if we need to draw a line.
            // Always draw a line if it a start number, center number or ending number.
            if (isStartOrCenterOrEndNumber || drawingNumber % verticalStep == 0) {
                val boldLine = drawingNumber % boldVerticalStep == 0
                val paint = if (boldLine) gridLinePaintBold else gridLinePaint

                drawLine(
                    extraPadding.toFloat(),
                    extraPadding + i * pixelsPerMeter,
                    gridWidth + extraPadding,
                    extraPadding + i * pixelsPerMeter,
                    paint
                )

                if (showGridScaleLabel) {
                    val textPaint = if (i < 10) textPaintNormal else textPaintSmall
                    textWidth = textPaint.measureText("$drawingNumber")
                    textPaint.getTextBounds(
                        "$drawingNumber",
                        0,
                        "$drawingNumber".length,
                        textBounds
                    )
                    textHeight = textBounds.height().toFloat()

                    drawText(
                        "$drawingNumber",
                        gridWidth + extraPadding + extraPadding/2,
                        (extraPadding + i * pixelsPerMeter) + textHeight / 2,
                        textPaint
                    )
                }
            }

        }
    }

    private fun Canvas.drawGridLeftToRightMode(
        gridWidthSizeInMeters: Int,
        gridHeightSizeInMeters: Int,
        gridWidth: Float,
        gridHeight: Float,
        boldVerticalStep: Int,
        boldHorizontalStep: Int
    ) {

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
                                boldHorizontalStep
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
                                boldVerticalStep
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