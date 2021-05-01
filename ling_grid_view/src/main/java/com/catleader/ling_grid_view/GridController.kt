package com.catleader.ling_grid_view

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.catleader.ling_grid_view.LingGridView.Companion.getVectorAmplitude
import kotlin.math.acos

class GridController @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val tag = "GridController"

    init {
        background = ContextCompat.getDrawable(context, R.drawable.ic_grid_rotate)
    }

    lateinit var lingGridContract: LingGridContract

    private var startedGridUISize = GridUISize()

    private var startedRotatedDegrees: Float = 0f

    private var centerPoint = PointF()

    private var startedPoint = PointF()

    private var endedPoint = PointF()

    private val newGridUISize = GridUISize()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return true

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!::lingGridContract.isInitialized) return false

                startedRotatedDegrees = lingGridContract.getStartedRotatedDegree()

                centerPoint.set(lingGridContract.getGridUICenter())

                startedGridUISize = lingGridContract.getStatedGridUISize()

                val gclp = layoutParams as FrameLayout.LayoutParams

                startedPoint.set(
                    gclp.leftMargin + event.x,
                    gclp.topMargin + event.y
                )

                true
            }
            MotionEvent.ACTION_MOVE -> {
                val gclp = layoutParams as FrameLayout.LayoutParams

                endedPoint.set(
                    gclp.leftMargin + event.x,
                    gclp.topMargin + event.y
                )

                handleSizeAndRotation(centerPoint, startedPoint, endedPoint)
                true
            }
            else -> false
        }
    }

    /**
     * Calculate rotation using vector equation
     */
    private fun handleSizeAndRotation(center: PointF, started: PointF, ended: PointF) {
        val centerStartedLength = getVectorAmplitude(center, started)
        val centerEndedLength = getVectorAmplitude(center, ended)

        val aDotB =
            (started.x - center.x) * (ended.x - center.x) + (started.y - center.y) * (ended.y - center.y)
        val abProduct = centerStartedLength * centerEndedLength

        val ratio = (aDotB / abProduct).coerceIn(-1f, 1f)
        val rad = acos(ratio)

        var degrees = Math.toDegrees(rad.toDouble()).toFloat()

        if ((ended.y - center.y) * (started.x - center.x) < (started.y - center.y) * (ended.x - center.x)) {
            degrees = -degrees
        }

        var angle = startedRotatedDegrees + degrees
        angle %= 360

        lingGridContract.onGridRotate(newGridUISize, angle)

    }

}