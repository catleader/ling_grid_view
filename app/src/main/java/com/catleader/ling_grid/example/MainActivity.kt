package com.catleader.ling_grid.example

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.catleader.ling_grid.example.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.tbruyelle.rxpermissions3.RxPermissions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val tag = "MapsActivity"

    private lateinit var map: GoogleMap

    private lateinit var binding: ActivityMainBinding

    private val lingGridView
        get() = binding.lingGridView

    private val gridSizeDialog: CustomGridSizeDialog by lazy {
        CustomGridSizeDialog.getInstance(this) { gw, gh ->
            lingGridView.gridSizeMeters = Pair(gw, gh)
        }
    }

    private val gridScaleDialog: CustomGridScaleStepDialog by lazy {
        CustomGridScaleStepDialog.getInstance(
            this,
        ) { gridScaleHorizontal, gridScaleVertical ->
            lingGridView.gridScaleHorizontalStep = gridScaleHorizontal
            lingGridView.gridScaleVerticalStep = gridScaleVertical
        }
    }

    private val rxPermissions: RxPermissions by lazy {
        RxPermissions(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnCustomGridSize.setOnClickListener {
            gridSizeDialog.clearError()
            gridSizeDialog.setCurrentGridSize(gridSize = lingGridView.gridSizeMeters)
            gridSizeDialog.show()
        }

        binding.btnCustomGridScaleStep.setOnClickListener {
            gridScaleDialog.gridSizeInMeters = lingGridView.gridSizeMeters
            gridScaleDialog.setCurrentGridScale(
                horizonScale = lingGridView.gridScaleHorizontalStep,
                verticalScale = lingGridView.gridScaleVerticalStep,
            )
            gridScaleDialog.clearError()
            gridScaleDialog.show()
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

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap


        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        map.uiSettings.apply {
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = true
            isMyLocationButtonEnabled = true
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

        rxPermissions
            .request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .subscribe { granted ->
                if (granted) {
                    map.isMyLocationEnabled = true
                }
            }

    }

    private var isInitialized = false


}