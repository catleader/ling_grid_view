package com.catleader.ling_grid_view

import android.graphics.PointF

interface LingGridContract {
    fun getGridUiCenterPoint(): PointF

    fun getGridUiRotation(): Float

    fun onGridSizeSetterClicked()

    fun onGridControllerClicked()

    fun onGridMoverClicked()

    fun onGridMove(xOffset: Int, yOffset: Int)

    fun onGridRotate(gridUISize: GridUISize, rotated: Float)

    fun getGridSizeInMeters(): Pair<Int, Int>

    fun onMapMove()

    fun onMapIdle()

    fun getZoomLevelForGridLineDrawing(): Float

    fun getZoomLevelForToolingVisibilities(): Float

}

data class GridUISize(
    var width: Int = 0,
    var height: Int = 0,
    var leftMargin: Int = 0,
    var topMargin: Int = 0
) {

    fun set(
        width: Int,
        height: Int,
        leftMargin: Int,
        topMargin: Int
    ) {
        this.width = width
        this.height = height
        this.leftMargin = leftMargin
        this.topMargin = topMargin
    }

    fun set(newGridUISize: GridUISize) {
        this.width = newGridUISize.width
        this.height = newGridUISize.height
        this.leftMargin = newGridUISize.leftMargin
        this.topMargin = newGridUISize.topMargin
    }

}