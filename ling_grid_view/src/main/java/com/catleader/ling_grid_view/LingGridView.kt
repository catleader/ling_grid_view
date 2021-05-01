package com.catleader.ling_grid_view

import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.catleader.ling_grid_view.databinding.LingGridViewBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

class LingGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LingGridContract {

    /**
     * width and height in meters
     */
    var gridSizeMeters = Pair(16, 16)
        set(value) {
            if (
                value.first > 0 &&
                value.second > 0 &&
                value.first <= maxGridSizePossibleInMeter &&
                value.second <= maxGridSizePossibleInMeter
            ) {
                field = value
                if (value.first % gridScaleHorizontalStep != 0) {
                    gridScaleHorizontalStep = 1
                }

                if (value.second % gridScaleVerticalStep != 0) {
                    gridScaleVerticalStep = 1
                }

                handleMapChange(gridUi.rotation)
            }
        }

    /**
     * Grid background color
     */
    var gridBackgroundColor: Int = Color.GRAY
        set(value) {
            field = value
            gridUi.gridBgColor = value
        }

    /**
     * Grid line color
     */
    var gridLineColor: Int = Color.WHITE
        set(value) {
            field = value
            gridUi.gridLineColor = value
        }

    /**
     * Control grid scale label visibility
     */
    var showGridScaleLabel: Boolean = true
        set(value) {
            field = value
            gridUi.showGridScaleLabel = value
            onMapMove()
        }

    /**
     * Set grid scale horizontal step
     */
    var gridScaleHorizontalStep: Int = 1
        set(value) {
            if (value in 1..maxGridSizePossibleInMeter
                && (gridSizeMeters.first % value == 0)
            ) {
                field = value
                gridUi.gridScaleHorizontalStep = value
            }
        }


    /**
     * Set grid scale vertical step
     */
    var gridScaleVerticalStep: Int = 1
        set(value) {
            if (value in 1..maxGridSizePossibleInMeter
                && (gridSizeMeters.second % value == 0)
            ) {
                field = value
                gridUi.gridScaleVerticalStep = value
            }
        }

    private val tag = "LingGridView"

    private var binding: LingGridViewBinding =
        LingGridViewBinding.inflate(LayoutInflater.from(context), this)

    private val gridController: GridController
        get() = binding.gridController

    private val gridMover: GridMover
        get() = binding.gridMover

    private val gridUi: GridUI
        get() = binding.gridUi


    private val rect = Rect()

    private var map: GoogleMap? = null

    private val tempPointA = PointF()

    private val tempPointB = PointF()

    private var externalDegrees = 0f

    private var isMapMoving = false

    private var gridMovedPoint = Point()

    private val controllerPadding: Int
            by lazy { (resources.displayMetrics.density * 4).toInt() }

    /**
     *  default center @Democracy Monument, Thailand
     */
    var centerLatLng: LatLng = LatLng(13.7567046, 100.5018897)

    private var stateToBeRestored: SavedState? = null

    init {
        elevation = 1 * resources.displayMetrics.density
        gridUi.lingGridContract = this
        gridController.visibility = View.GONE
        gridMover.visibility = View.GONE
        gridMover.lingGridContract = this

        gridController.lingGridContract = this

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LingGridView,
            0,
            0
        ).apply {
            try {
                gridBackgroundColor = getColor(
                    R.styleable.LingGridView_gridBgColor,
                    Color.parseColor("#3200BAF1")
                )

                gridLineColor = getColor(
                    R.styleable.LingGridView_gridLineColor,
                    Color.parseColor("#ffffffff")
                )

                gridSizeMeters = Pair(
                    getInteger(R.styleable.LingGridView_gridColumnCount, 16),
                    getInteger(R.styleable.LingGridView_gridRowCount, 16)
                )

                gridScaleHorizontalStep =
                    getInteger(R.styleable.LingGridView_gridScaleHorizontalStep, 1)

                gridScaleVerticalStep =
                    getInteger(R.styleable.LingGridView_gridScaleVerticalStep, 1)

                showGridScaleLabel = getBoolean(R.styleable.LingGridView_showGridScaleLabel, true)


            } finally {
                recycle()
            }
        }
    }

    private fun getGridPixelSizeIncludePadding(gridSizeInMeters: Int): Int {
        val gMap = map ?: return 0
        val metersPerPixels = calculateMetersPerPx(
            latitude = centerLatLng.latitude,
            zoomLevel = gMap.cameraPosition.zoom,
            displayDensity = resources.displayMetrics.density
        )
        return (gridSizeInMeters / metersPerPixels + 2 * gridUi.extraPadding).toInt()
    }

    fun initGrid(
        latLng: LatLng,
        map: GoogleMap
    ) {
        this.map = map

        centerLatLng = latLng

        val state = stateToBeRestored

        if (state != null) {
            centerLatLng = LatLng(state.gridLatitude, state.gridLongitude)
            gridSizeMeters = Pair(state.gridWidth, state.gridHeight)
            gridBackgroundColor = state.gridBackgroundColor
            gridLineColor = state.gridLineColor
            showGridScaleLabel = state.showGridScaleLabel == 1
            gridScaleHorizontalStep = state.gridScaleHorizontalStep
            gridScaleVerticalStep = state.gridScaleVerticalStep
            handleMapChange(state.gridRotation)
            stateToBeRestored = null
        } else {
            handleMapChange(0f)
        }
    }

    fun relocateGridToCenterOfMap() {
        val gMap = map ?: return
        if (centerLatLng == gMap.cameraPosition.target) {
            gridUi.rotation = 0f
        } else {
            centerLatLng = gMap.cameraPosition.target
        }
        onMapMove()
    }

    override fun getGridUiCenterPoint(): PointF {
        tempPointA.set(
            gridUi.left + gridUi.width / 2f,
            gridUi.top + gridUi.height / 2f
        )
        return tempPointA
    }

    override fun getGridUiRotation(): Float {
        return gridUi.rotation
    }

    override fun getGridSizeInMeters(): Pair<Int, Int> {
        return gridSizeMeters
    }

    private fun handleMapChange(newDegrees: Float) {
        gridUi.rotation = newDegrees

        val gMap = map ?: return

        val widthPixel = getGridPixelSizeIncludePadding(gridSizeMeters.first)

        val heightPixel = getGridPixelSizeIncludePadding(gridSizeMeters.second)

        val gridCenter = gMap.projection.toScreenLocation(centerLatLng)

        val left = gridCenter.x - widthPixel / 2
        val right = gridCenter.x + widthPixel / 2
        val top = gridCenter.y - heightPixel / 2
        val bottom = gridCenter.y + heightPixel / 2

        rect.set(left, top, right, bottom)

        val glp = gridUi.layoutParams as? LayoutParams? ?: return
        glp.width = widthPixel
        glp.height = heightPixel
        glp.leftMargin = left
        glp.topMargin = top
        gridUi.layoutParams = glp

        repositionGridTools()
    }

    private val m = Matrix()

    private val inM = Matrix()

    private fun repositionGridTools() {
        val glp = gridUi.layoutParams as? LayoutParams? ?: return
        val gclp = gridController.layoutParams as? LayoutParams? ?: return
        val gmlp = gridMover.layoutParams as? LayoutParams? ?: return

        val gc = getRotatedPoint(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            targetX = glp.leftMargin + glp.width + controllerPadding,
            targetY = glp.topMargin + glp.height + controllerPadding,
            angle = gridUi.rotation
        )

        val cGC = getRotatedPoint(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            targetX = glp.leftMargin + glp.width + gclp.width / 2 + controllerPadding,
            targetY = glp.topMargin + glp.height + gclp.height / 2 + controllerPadding,
            angle = gridUi.rotation
        )

        val gm = getRotatedPoint(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            targetX = glp.leftMargin + glp.width + controllerPadding,
            targetY = glp.topMargin + glp.height - gmlp.height - controllerPadding,
            angle = gridUi.rotation
        )

        val cGM = getRotatedPoint(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            targetX = glp.leftMargin + glp.width + gmlp.width / 2 + controllerPadding,
            targetY = glp.topMargin + glp.height - gmlp.height / 2 - controllerPadding,
            angle = gridUi.rotation
        )

        val gcPoint = floatArrayOf(gc.x.toFloat(), gc.y.toFloat())

        val gmPoint = floatArrayOf(gm.x.toFloat(), gm.y.toFloat())

        mapRotationPoint(gcPoint, cGC.x.toFloat(), cGC.y.toFloat())

        mapRotationPoint(gmPoint, cGM.x.toFloat(), cGM.y.toFloat())

        gclp.leftMargin = gcPoint[0].toInt()
        gclp.topMargin = gcPoint[1].toInt()
        gridController.layoutParams = gclp

        gmlp.leftMargin = gmPoint[0].toInt()
        gmlp.topMargin = gmPoint[1].toInt()
        gridMover.layoutParams = gmlp
        gridMover.rotation = gridUi.rotation

    }

    private fun mapRotationPoint(p: FloatArray, cx: Float, cy: Float) {
        m.reset()
        m.setRotate(gridUi.rotation, cx, cy)
        try {
            m.invert(inM)
        } catch (e: Exception) {
            Log.w(tag, "can't invert matrix: ${e.message}")
        }

        inM.mapPoints(p)
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun getZoomLevelForGridLineDrawing(): Float {
        val gw = gridSizeMeters.first
        val gh = gridSizeMeters.second

        return when {
            gw >= 100 || gh >= 100 -> 17.5f
            gw >= 50 || gh >= 50 -> 18.5f
            gw >= 25 || gh >= 25 -> 19.5f
            else -> 20f
        }
    }

    override fun getZoomLevelForToolingVisibilities(): Float {
        val gw = gridSizeMeters.first
        val gh = gridSizeMeters.second

        return when {
            gw >= 100 || gh >= 100 -> 15.8f
            gw >= 50 || gh >= 50 -> 16.8f
            gw >= 25 || gh >= 25 -> 17.8f
            else -> 18.8f
        }
    }

    override fun onMapMove() {
        isMapMoving = true
        val gMap = map ?: return
        val diff = gMap.cameraPosition.bearing - externalDegrees
        externalDegrees = gMap.cameraPosition.bearing

        val newDegrees = (gridUi.rotation - diff) % 360

        val zoomLevel = gMap.cameraPosition.zoom

        gridUi.mapZoomLevel = zoomLevel

        if (zoomLevel >= getZoomLevelForToolingVisibilities()) {
            gridController.visibility = View.VISIBLE
            gridMover.visibility = View.VISIBLE
        } else {
            gridController.visibility = View.GONE
            gridMover.visibility = View.GONE
        }

        handleMapChange(newDegrees)
    }

    override fun onMapIdle() {
        isMapMoving = false
    }

    override fun onGridMove(
        xOffset: Int,
        yOffset: Int
    ) {

        rect.offset(xOffset, yOffset)

        val glp = gridUi.layoutParams as? LayoutParams? ?: return
        glp.leftMargin += xOffset
        glp.topMargin += yOffset
        gridUi.layoutParams = glp

        val gclp = gridController.layoutParams as? LayoutParams? ?: return
        gclp.leftMargin += xOffset
        gclp.topMargin += yOffset
        gridController.layoutParams = gclp

        val gmlp = gridMover.layoutParams as? LayoutParams? ?: return
        gmlp.leftMargin += xOffset
        gmlp.topMargin += yOffset
        gridMover.layoutParams = gmlp

        val gMap = map ?: return

        val left = rect.left.toFloat()
        val top = rect.top.toFloat()
        val right = rect.right.toFloat()
        val bottom = rect.bottom.toFloat()

        gridMovedPoint.set(
            (left + (right - left) / 2).toInt(),
            (top + (bottom - top) / 2).toInt()
        )

        centerLatLng = gMap.projection.fromScreenLocation(gridMovedPoint)

    }

    override fun onGridRotate(
        gridUISize: GridUISize,
        rotated: Float
    ) {
        gridUi.rotation = rotated
        repositionGridTools()
    }


    private fun getRotatedPoint(
        gridCenterX: Int,
        gridCenterY: Int,
        targetX: Int,
        targetY: Int,
        angle: Float
    ): Point {
        tempPointA.set(gridCenterX.toFloat(), gridCenterY.toFloat())
        tempPointB.set(targetX.toFloat(), targetY.toFloat())
        val dOA: Float = getVectorAmplitude(tempPointA, tempPointB)
        val p1: Double = Math.toRadians(angle.toDouble())
        val cons = acos(((targetX - gridCenterX) / dOA).toDouble())
        val x = (gridCenterX + dOA * cos(p1 + cons)).toInt()
        val y = (gridCenterY + dOA * sin(p1 + cons)).toInt()
        return Point(x, y)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.gridLatitude = centerLatLng.latitude
        state.gridLongitude = centerLatLng.longitude
        state.gridRotation = gridUi.rotation
        state.gridWidth = gridSizeMeters.first
        state.gridHeight = gridSizeMeters.second
        state.gridBackgroundColor = gridBackgroundColor
        state.gridLineColor = gridLineColor
        state.showGridScaleLabel = if (showGridScaleLabel) 1 else 0
        state.gridScaleHorizontalStep = gridScaleHorizontalStep
        state.gridScaleVerticalStep = gridScaleVerticalStep
        return state
    }


    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is SavedState -> {
                super.onRestoreInstanceState(state.superState)
                stateToBeRestored = state
            }
            else -> {
                super.onRestoreInstanceState(state)
            }
        }
    }

    class SavedState : BaseSavedState {
        var gridLatitude: Double = 0.0
        var gridLongitude: Double = 0.0
        var gridRotation = 0f
        var gridWidth: Int = 0
        var gridHeight: Int = 0
        var gridBackgroundColor: Int = Color.GRAY
        var gridLineColor: Int = Color.WHITE
        var showGridScaleLabel: Int = 0
        var gridScaleHorizontalStep: Int = 1
        var gridScaleVerticalStep: Int = 1

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            gridLatitude = parcel.readDouble()
            gridLongitude = parcel.readDouble()
            gridRotation = parcel.readFloat()
            gridWidth = parcel.readInt()
            gridHeight = parcel.readInt()
            gridBackgroundColor = parcel.readInt()
            gridLineColor = parcel.readInt()
            showGridScaleLabel = parcel.readInt()
            gridScaleHorizontalStep = parcel.readInt()
            gridScaleVerticalStep = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeDouble(gridLatitude)
            parcel.writeDouble(gridLongitude)
            parcel.writeFloat(gridRotation)
            parcel.writeInt(gridWidth)
            parcel.writeInt(gridHeight)
            parcel.writeInt(gridBackgroundColor)
            parcel.writeInt(gridLineColor)
            parcel.writeInt(showGridScaleLabel)
            parcel.writeInt(gridScaleHorizontalStep)
            parcel.writeInt(gridScaleVerticalStep)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val maxGridSizePossibleInMeter = 200

        fun getVectorAmplitude(a: PointF, b: PointF): Float {
            return sqrt((b.x - a.x).pow(2) + (b.y - a.y).pow(2))
        }

        fun calculateMetersPerPx(
            latitude: Double,
            zoomLevel: Float,
            displayDensity: Float
        ): Float {
            return ((156543.03392 * cos(latitude * Math.PI / 180)).toFloat() / 2f.pow(zoomLevel)) / displayDensity
        }

    }

}