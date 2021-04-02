package com.catleader.ling_grid_view

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.catleader.ling_grid_view.databinding.LingGridViewBinding
import kotlin.math.*

class LingGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LingGridContract {

    /**
     * width and height in meters
     */
    var gridSizeMeters = Pair(16, 9)
        set(value) {
            if (value.first > 0 && value.second > 0) {
                field = value
                handleMapChanges(gridUi.rotation)
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


    private val tag = "LingGridView"

    private var listeners = mutableListOf<Listener>()

    private var binding: LingGridViewBinding =
        LingGridViewBinding.inflate(LayoutInflater.from(context), this)

    private val gridController: GridController
        get() = binding.gridController

    private val gridUi: GridUI
        get() = binding.gridUi

    private val rect = Rect()

    private var map: GoogleMap? = null

    private val tempPointA = PointF()

    private val tempPointB = PointF()

    private val gridUISize = GridUISize()

    private var externalDegrees = 0f

    private val invalidPointerID = -1

    private var activePointerId = invalidPointerID

    private var isMapMoving = false

    private var gridMovedPoint = Point()

    private val extraPadding: Float
        get() = resources.displayMetrics.density * 16

    // default center @Democracy Monument, Thailand
    private var centerLatLng: LatLng = LatLng(13.7567046, 100.5018897)

    private var stateToBeRestored: SavedState? = null

    init {
        gridUi.lingGridContract = this
        gridUi.extraPadding = extraPadding.toInt()
        gridController.visibility = View.GONE
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
                    Color.parseColor("#80f0c929")
                )

                gridLineColor = getColor(
                    R.styleable.LingGridView_gridLineColor,
                    Color.parseColor("#fff0c929")
                )

                gridSizeMeters = Pair(
                    getInteger(
                        R.styleable.LingGridView_gridColumnCount,
                        10
                    ),
                    getInteger(
                        R.styleable.LingGridView_gridRowCount,
                        10
                    )
                )
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
        return (gridSizeInMeters / metersPerPixels + 2 * extraPadding).toInt()
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
            handleMapChanges(state.gridRotation)
            stateToBeRestored = null
        } else {
            handleMapChanges(0f)
        }
    }

    override fun getGridUICenter(): PointF {
        tempPointA.set(
            gridUi.left + gridUi.width / 2f,
            gridUi.top + gridUi.height / 2f
        )
        return tempPointA
    }

    override fun getStartedRotatedDegree(): Float {
        return gridUi.rotation
    }

    override fun getStatedGridUISize(): GridUISize {
        val glp = gridUi.layoutParams as? LayoutParams? ?: return gridUISize

        return gridUISize.apply {
            set(
                glp.width,
                glp.height,
                glp.leftMargin,
                glp.topMargin
            )
        }
    }

    override fun getGridRect(): Pair<Rect, Float> {
        return Pair(rect, gridUi.rotation)
    }

    override fun getGridSizeInMeters(): Pair<Int, Int> {
        return gridSizeMeters
    }

    private fun handleMapChanges(newDegrees: Float) {
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

        repositionGridController()
    }

    private fun repositionGridController() {
        val glp = gridUi.layoutParams as? LayoutParams? ?: return

        val gclPoint = getRotatedPointForGridController(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            gridControllerX = glp.leftMargin + glp.width,
            gridControllerY = glp.topMargin + glp.height,
            angle = gridUi.rotation
        )

        val gclp = gridController.layoutParams as? LayoutParams? ?: return
        gclp.leftMargin = gclPoint.x - gclp.width / 2
        gclp.topMargin = gclPoint.y - gclp.height / 2
        gridController.layoutParams = gclp
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (isMapMoving || ev == null) return true

        val action = ev.actionMasked

        val pointerId = ev.getPointerId(ev.actionIndex)

        return when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                when {
                    activePointerId == invalidPointerID -> {
                        activePointerId = pointerId
                        false
                    }
                    pointerId != activePointerId -> {
                        activePointerId = invalidPointerID
                        true
                    }
                    else -> {
                        activePointerId = pointerId
                        false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                activePointerId = invalidPointerID
                true
            }
            else -> false

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    override fun onMapMove() {
        isMapMoving = true
        val gMap = map ?: return
        val diff = gMap.cameraPosition.bearing - externalDegrees
        externalDegrees = gMap.cameraPosition.bearing

        val newDegrees = (gridUi.rotation - diff) % 360
        val zoomLevel = gMap.cameraPosition.zoom
        gridUi.mapZoomLevel = zoomLevel
        gridController.visibility = if (zoomLevel >= 20f) View.VISIBLE else View.GONE

        handleMapChanges(newDegrees)
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
        repositionGridController()
    }

    private fun notifyListeners() {
        val glp = gridUi.layoutParams as? LayoutParams? ?: return

        listeners.forEach { each ->
            each.onGridSizeChanged(glp.width, glp.height)
        }
    }

    override fun getGridSize(): Int {
        return (gridUi.layoutParams as? LayoutParams?)?.width ?: 0
    }

    private fun getRotatedPointForGridController(
        gridCenterX: Int,
        gridCenterY: Int,
        gridControllerX: Int,
        gridControllerY: Int,
        angle: Float
    ): Point {
        tempPointA.set(gridCenterX.toFloat(), gridCenterY.toFloat())
        tempPointB.set(gridControllerX.toFloat(), gridControllerY.toFloat())
        val dOA: Float = getVectorAmplitude(tempPointA, tempPointB)
        val p1: Double = Math.toRadians(angle.toDouble())
        val cons = acos(((gridControllerX - gridCenterX) / dOA).toDouble())
        val x = (gridCenterX + dOA * cos(p1 + cons)).toInt()
        val y = (gridCenterY + dOA * sin(p1 + cons)).toInt()
        return Point(x, y)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun interface Listener {
        fun onGridSizeChanged(gridWidth: Int, gridHeight: Int)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.gridLatitude = centerLatLng.latitude
        state.gridLongitude = centerLatLng.longitude
        state.gridRotation = gridUi.rotation
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

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            gridLatitude = parcel.readDouble()
            gridLongitude = parcel.readDouble()
            gridRotation = parcel.readFloat()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeDouble(gridLatitude)
            parcel.writeDouble(gridLongitude)
            parcel.writeFloat(gridRotation)
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