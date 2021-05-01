package com.catleader.ling_grid.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.catleader.ling_grid.example.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val tag = "MapsActivity"

    private lateinit var map: GoogleMap

    private lateinit var binding: ActivityMainBinding

    private val lingGridView
        get() = binding.lingGridView

    private val gridSizeDialog: CustomGridSizeDialog by lazy {
        CustomGridSizeDialog.getInstance(this) { gw, gh ->
            gridSizeDialog.hide()
            lingGridView.gridSizeMeters = Pair(gw, gh)
        }
    }

    private val gridScaleStepDialog: CustomGridScaleStepDialog by lazy {
        CustomGridScaleStepDialog.getInstance(this) { gridScale ->
            gridScaleStepDialog.hide()
            lingGridView.gridScaleStep = gridScale
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnCustomGridSize.setOnClickListener {
            gridSizeDialog.show()
        }

        binding.btnCustomGridScaleStep.setOnClickListener {
            gridScaleStepDialog.show()
        }

        binding.btnToggleGridScaleLabel.setOnClickListener {
            if (lingGridView.showGridScaleLabel) {
                lingGridView.showGridScaleLabel = false
                binding.btnToggleGridScaleLabel.text = "Show grid scale"
            } else {
                lingGridView.showGridScaleLabel = true
                binding.btnToggleGridScaleLabel.text = "Hide grid scale"
            }
        }

        binding.btnToggleMapType.setOnClickListener {
            if (map.mapType == GoogleMap.MAP_TYPE_SATELLITE) {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
            } else {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
        }

        binding.btnCenterGrid.setOnClickListener {
            lingGridView.relocateGridToCenterOfMap()
        }

    }

    private var latestGridLatLng: LatLng? = null

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val latLng = savedInstanceState.getDoubleArray("latlng")
        if (latLng != null) latestGridLatLng = LatLng(latLng[0], latLng[1])
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putDoubleArray(
            "latlng",
            doubleArrayOf(lingGridView.centerLatLng.latitude, lingGridView.centerLatLng.longitude)
        )
        super.onSaveInstanceState(outState)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        map.uiSettings.apply {
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = true
        }

        val step = LatLng(18.7649001, 98.9362624)

        if (latestGridLatLng != null) map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latestGridLatLng,
                19f
            )
        ) else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(step, 19f))
        }

        map.setOnCameraIdleListener {
            lingGridView.onMapIdle()
            if (!isInitialized) {
                isInitialized = true
                lingGridView.initGrid(step, map)
            }
        }

        map.setOnCameraMoveListener {
            lingGridView.onMapMove()
        }

    }

    private var isInitialized = false


}