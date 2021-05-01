package com.catleader.ling_grid_view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class GridMover @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val tag = "GridMover"

    init {
        background = ContextCompat.getDrawable(context, R.drawable.ic_grid_move)
    }

    var lingGridContract: LingGridContract? = null

    private var pushPointX = 0f

    private var pushPointY = 0f

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