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

    private val tvGridLength
        get() = binding.tvGridLength

    private val lingGridView
        get() = binding.overlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnIncrease.setOnClickListener {
            lingGridView.gridSizeMeters = lingGridView.gridSizeMeters.run {
                Pair(first + 1, second + 1)
            }
        }

        binding.btnDecrease.setOnClickListener {
            lingGridView.gridSizeMeters = lingGridView.gridSizeMeters.run {
                Pair(first - 1, second - 1)
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        map.uiSettings.apply {
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
        }

        val step = LatLng(18.7649001, 98.9362624)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(step, 19f))

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