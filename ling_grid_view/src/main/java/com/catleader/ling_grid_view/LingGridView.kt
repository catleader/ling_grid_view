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
     * Grid's width and height in meters
     * @throws LingGridException.HorizontalGridSizeMustGreaterOrEqualItsScale
     * @throws LingGridException.VerticalGridSizeMustGreaterOrEqualItsScale
     */
    var gridSizeMeters = Pair(72, 72)
        set(value) {
            if (
                value.first > 0 &&
                value.second > 0 &&
                value.first <= maxGridSizePossibleInMeter &&
                value.second <= maxGridSizePossibleInMeter
            ) {
                when {
                    value.first < (gridScaleHorizontalStep * gridScaleHorizontalStepMultiplier) -> {
                        throw LingGridException.HorizontalGridSizeMustGreaterOrEqualItsScale
                    }
                    value.second < (gridScaleVerticalStep * gridScaleVerticalStepMultiplier) -> {
                        throw LingGridException.VerticalGridSizeMustGreaterOrEqualItsScale
                    }
                    else -> {
                        field = value
                        gridSizeSetter.gridSize = value.first
                        handleMapChange(gridUi.rotation)
                    }
                }
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
     * Callback function that will be called when [GridSizeSetter] view is clicked
     */
    var  gridSizeSetterClickListener: ((currentSize: Pair<Int, Int>) -> Unit)? = null


    /**
     * Callback function that will be called when [GridSizeSetter] view is clicked
     */
    var  gridSizeSetterVisibilityListener: ((isShowing: Boolean) -> Unit)? = null


    /**
     * Set grid scale horizontal step
     *
     * @throws LingGridException.HorizontalScaleValueExceed
     * the given scale is exceed its grid's size or larger than max possible value
     *
     * @throws LingGridException.HorizontalScaleTooSmall
     * scale is too small, it's pointless to draw and violating rules
     *
     * @throws LingGridException.HorizontalScaleMustBeEvenWhenZoomedOut
     * scale must be an even value, so it can be divided by 2 when zooming-in
     */
    var gridScaleHorizontalStep: Int = 6
        set(value) {
            if (value !in 1..maxGridSizePossibleInMeter || value > gridSizeMeters.first) {
                throw LingGridException.HorizontalScaleValueExceed
            }

            when {
                gridScaleHorizontalStepMultiplier == 1 -> {
                    // not zoom-out yet, user could set whatever value they want.
                    field = value
                    gridUi.gridScaleHorizontalStep = value
                }
                value % 2 == 0 -> {
                    // user change scale when map is zoom-out, also we're making sure the given scale is even
                    // then we need to find the correct value for scale & its multiplier.
                    val (scale, multiplier, blockPixelSize) = getCorrectValueForScale(value)

                    if (blockPixelSize <= minimumPixelSizeThreshold) throw LingGridException.HorizontalScaleTooSmall

                    field = scale
                    gridScaleHorizontalStepMultiplier = multiplier
                    gridUi.gridScaleHorizontalStep = field
                }
                value == 1 -> {
                    // map is zoom-out but user still want to set to 1 , smh.
                    throw LingGridException.HorizontalScaleTooSmall
                }
                else -> {
                    // Given scale is not even value.
                    throw LingGridException.HorizontalScaleMustBeEvenWhenZoomedOut
                }
            }
        }

    private var gridScaleHorizontalStepMultiplier: Int = 1
        set(value) {
            val sumScale = value * gridScaleHorizontalStep
            if (sumScale in 1..maxGridSizePossibleInMeter && sumScale <= gridSizeMeters.first ) {
                field = value
                gridUi.gridScaleHorizontalStepMultiplier = value
            }
        }

    /**
     * Set grid scale vertical step
     *
     * @throws LingGridException.VerticalScaleValueExceed
     * the given scale is exceed its grid's size or larger than max possible value
     *
     * @throws LingGridException.VerticalScaleTooSmall
     * scale is too small, it's pointless to draw and violating rules
     *
     * @throws LingGridException.VerticalScaleMustBeEvenValueZoomedOut
     * scale must be an even value, so it can be divided by 2 when zooming-in
     */
    var gridScaleVerticalStep: Int = 6
        set(value) {
            if (value !in 1..maxGridSizePossibleInMeter || value > gridSizeMeters.second) {
                throw LingGridException.VerticalScaleValueExceed
            }

            when {
                gridScaleVerticalStepMultiplier == 1 -> {
                    // not zoom-out yet, user could set whatever value they want.
                    field = value
                    gridUi.gridScaleVerticalStep = value
                }
                value % 2 == 0 -> {
                    // user change scale when map is zoom-out, also we're making sure the given scale is even
                    // then we need to find the correct value for scale & its multiplier.
                    val (scale, multiplier, blockPixelSize) = getCorrectValueForScale(value)

                    if (blockPixelSize <= minimumPixelSizeThreshold) throw LingGridException.VerticalScaleValueExceed

                    field = scale
                    gridScaleVerticalStepMultiplier = multiplier
                    gridUi.gridScaleVerticalStep = field
                }
                value == 1 -> {
                    // map is zoom-out but user still want to set to 1 , smh.
                    throw LingGridException.VerticalScaleTooSmall
                }
                else -> {
                    // Given scale is not even value.
                    throw LingGridException.VerticalScaleMustBeEvenValueZoomedOut
                }
            }
        }

    private var gridScaleVerticalStepMultiplier: Int = 1
        set(value) {
            val sumScale = value * gridScaleVerticalStep
            if (sumScale in 1..maxGridSizePossibleInMeter && sumScale <= gridSizeMeters.second) {
                field = value
                gridUi.gridScaleVerticalStepMultiplier = value
            }
        }

    private fun getCorrectValueForScale(value: Int): Triple<Int, Int, Float> {
        val pgw = gridUi.width - gridUi.extraPadding * 2f

        var count = 0
        var scale = value
        while (scale % 2 == 0) {
            scale /= 2
            count++
        }

        val multiplier = 2.toDouble().pow(count).toInt()

        val blockPixelSize = (pgw / gridSizeMeters.first) * scale * multiplier

        return Triple(scale, multiplier, blockPixelSize)
    }

    private val tag = "LingGridView"

    private var binding: LingGridViewBinding =
        LingGridViewBinding.inflate(LayoutInflater.from(context), this)

    private val gridController: GridController
        get() = binding.gridController

    private val gridMover: GridMover
        get() = binding.gridMover

    private val gridSizeSetter: GridSizeSetter
        get() = binding.gridSizeSetter

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

    private val minimumPixelSizeThreshold = 30

    init {
        elevation = 1 * resources.displayMetrics.density
        gridUi.lingGridContract = this
        gridController.visibility = View.GONE
        gridMover.visibility = View.GONE
        gridSizeSetter.visibility = View.GONE

        gridMover.lingGridContract = this
        gridController.lingGridContract = this
        gridSizeSetter.lingGridContract = this

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
                    getInteger(R.styleable.LingGridView_gridColumnCount, gridSizeMeters.first),
                    getInteger(R.styleable.LingGridView_gridRowCount, gridSizeMeters.second)
                )

                gridScaleHorizontalStep =
                    getInteger(R.styleable.LingGridView_gridScaleHorizontalStep, gridScaleHorizontalStep)

                gridScaleVerticalStep =
                    getInteger(R.styleable.LingGridView_gridScaleVerticalStep, gridScaleVerticalStep)

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

        currentZoomLevel = map.cameraPosition.zoom

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

    override fun onGridSizeSetterClicked() {
        gridSizeSetterClickListener?.invoke(gridSizeMeters)
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
        val gslp = gridSizeSetter.layoutParams as? LayoutParams? ?: return

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

        val gs = getRotatedPoint(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            targetX = glp.leftMargin + glp.width + controllerPadding,
            targetY = glp.topMargin + glp.height - gmlp.height - gslp.height - (2 * controllerPadding),
            angle = gridUi.rotation
        )

        val cGS = getRotatedPoint(
            gridCenterX = glp.leftMargin + glp.width / 2,
            gridCenterY = glp.topMargin + glp.height / 2,
            targetX = glp.leftMargin + glp.width + gmlp.width / 2 + controllerPadding,
            targetY = glp.topMargin + glp.height - gmlp.height - gslp.height / 2 - (2 * controllerPadding),
            angle = gridUi.rotation
        )

        val gcPoint = floatArrayOf(gc.x.toFloat(), gc.y.toFloat())

        val gmPoint = floatArrayOf(gm.x.toFloat(), gm.y.toFloat())

        val gsPoint = floatArrayOf(gs.x.toFloat(), gs.y.toFloat())

        mapRotationPoint(gcPoint, cGC.x.toFloat(), cGC.y.toFloat())

        mapRotationPoint(gmPoint, cGM.x.toFloat(), cGM.y.toFloat())

        mapRotationPoint(gsPoint, cGS.x.toFloat(), cGS.y.toFloat())

        gclp.leftMargin = gcPoint[0].toInt()
        gclp.topMargin = gcPoint[1].toInt()
        gridController.layoutParams = gclp

        gmlp.leftMargin = gmPoint[0].toInt()
        gmlp.topMargin = gmPoint[1].toInt()
        gridMover.layoutParams = gmlp
        gridMover.rotation = gridUi.rotation

        gslp.leftMargin = gsPoint[0].toInt()
        gslp.topMargin = gsPoint[1].toInt()
        gridSizeSetter.layoutParams = gslp

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
        return 15f
    }

    override fun getZoomLevelForToolingVisibilities(): Float {
        val gw = gridSizeMeters.first
        val gh = gridSizeMeters.second

        return when {
            gw >= 250 || gh >= 250 -> 13.8f
            gw >= 100 || gh >= 100 -> 14.8f
            gw >= 50 || gh >= 50 -> 15.8f
            gw >= 25 || gh >= 25 -> 16.8f
            else -> 17.8f
        }
    }

    fun getZoomLevelJustSizeSetterView(): Float {
        val gw = gridSizeMeters.first
        val gh = gridSizeMeters.second

        return when {
            gw >= 250 || gh >= 250 -> 15.8f
            gw >= 100 || gh >= 100 -> 16.8f
            gw >= 50 || gh >= 50 -> 17.8f
            gw >= 25 || gh >= 25 -> 18.8f
            else -> 17.8f
        }
    }

    private var currentZoomLevel = 0f

    private fun handleGridScaleMultipliers(zoomLevel: Float) {
        val horizontalScaleInPixel =
            (gridUi.width - gridUi.extraPadding * 2f) / gridSizeMeters.first * gridScaleHorizontalStep * gridScaleHorizontalStepMultiplier
        val gMap = map ?: return

        val isZoomingIn = when {
            zoomLevel - currentZoomLevel > 0f -> true
            zoomLevel - currentZoomLevel < 0f -> false
            else -> return
        }

        if (gMap.cameraPosition.zoom == gMap.maxZoomLevel) {
            gridScaleHorizontalStepMultiplier = 1
        } else if (
            horizontalScaleInPixel <= minimumPixelSizeThreshold
            && !isZoomingIn
        ) {
            val newHorizontalScaleStep =
                gridScaleHorizontalStep * gridScaleHorizontalStepMultiplier * 2

            if (newHorizontalScaleStep <= gridSizeMeters.first) {
                gridScaleHorizontalStepMultiplier *= 2
            }
        } else if (
            horizontalScaleInPixel > minimumPixelSizeThreshold * 2
            && isZoomingIn
            && gridScaleHorizontalStepMultiplier > 1
        ) {
            gridScaleHorizontalStepMultiplier /= 2
        }

        val verticalScaleInPixel =
            (gridUi.height - gridUi.extraPadding * 2f) / gridSizeMeters.second * gridScaleVerticalStep * gridScaleVerticalStepMultiplier

        if (gMap.cameraPosition.zoom == gMap.maxZoomLevel) {
            gridScaleVerticalStepMultiplier = 1
        } else if (verticalScaleInPixel <= minimumPixelSizeThreshold && !isZoomingIn) {
            val newVerticalScaleStep = gridScaleVerticalStep * gridScaleVerticalStepMultiplier * 2
            if (newVerticalScaleStep <= gridSizeMeters.second) {
                gridScaleVerticalStepMultiplier *= 2
            }
        } else if (
            verticalScaleInPixel > minimumPixelSizeThreshold * 2
            && isZoomingIn
            && gridScaleVerticalStepMultiplier > 1
        ) {
            gridScaleVerticalStepMultiplier /= 2
        }

        currentZoomLevel = zoomLevel

    }


    override fun onMapMove() {
        isMapMoving = true
        val gMap = map ?: return
        val diff = gMap.cameraPosition.bearing - externalDegrees
        externalDegrees = gMap.cameraPosition.bearing

        val newDegrees = (gridUi.rotation - diff) % 360

        val zoomLevel = gMap.cameraPosition.zoom

        handleGridScaleMultipliers(zoomLevel)

        gridUi.mapZoomLevel = zoomLevel

        if (zoomLevel >= getZoomLevelForToolingVisibilities()) {
            gridController.visibility = View.VISIBLE
            gridMover.visibility = View.VISIBLE
            gridSizeSetter.visibility = View.VISIBLE
        } else {
            gridController.visibility = View.GONE
            gridMover.visibility = View.GONE
            gridSizeSetter.visibility = View.GONE
        }

        if (zoomLevel >= getZoomLevelJustSizeSetterView()) {
            gridSizeSetterVisibilityListener?.invoke(true)
            gridSizeSetter.visibility = View.VISIBLE
        } else {
            gridSizeSetterVisibilityListener?.invoke(false)
            gridSizeSetter.visibility = View.GONE
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

        val gslp = gridSizeSetter.layoutParams as? LayoutParams? ?: return
        gslp.leftMargin += xOffset
        gslp.topMargin += yOffset
        gridSizeSetter.layoutParams = gslp

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
        const val maxGridSizePossibleInMeter = 500

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