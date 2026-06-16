package com.example.guet_map.module.social.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.guet_map.databinding.BottomSheetLocationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LocationSelectBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationAdapter: LocationAdapter
    var onLocationSelected: ((WeatherLocation) -> Unit)? = null

    private var currentLocation: WeatherLocation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupCustomInput()
    }

    private fun setupAdapter() {
        locationAdapter = LocationAdapter { location ->
            onLocationSelected?.invoke(location)
            dismiss()
        }
        binding.rvLocations.adapter = locationAdapter

        val allLocations = listOf(
            WeatherLocation.PRESETS.first(),  // 默认桂电
            *WeatherLocation.PRESETS.drop(1).toTypedArray()
        )
        locationAdapter.submitList(allLocations)

        currentLocation?.let { locationAdapter.setSelected(it) }
    }

    private fun setupCustomInput() {
        binding.btnConfirmCustom.setOnClickListener {
            val name = binding.etLocationName.text?.toString()?.trim()
            val latStr = binding.etLatitude.text?.toString()?.trim()
            val lngStr = binding.etLongitude.text?.toString()?.trim()

            if (name.isNullOrBlank()) {
                binding.etLocationName.error = "请输入地点名称"
                return@setOnClickListener
            }

            val lat = latStr?.toDoubleOrNull()
            val lng = lngStr?.toDoubleOrNull()

            if (lat == null || lat < -90 || lat > 90) {
                binding.etLatitude.error = "纬度需在 -90~90 之间"
                return@setOnClickListener
            }

            if (lng == null || lng < -180 || lng > 180) {
                binding.etLongitude.error = "经度需在 -180~180 之间"
                return@setOnClickListener
            }

            val location = WeatherLocation.custom(name, lat, lng)
            onLocationSelected?.invoke(location)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(currentLocation: WeatherLocation? = null): LocationSelectBottomSheet {
            return LocationSelectBottomSheet().apply {
                this.currentLocation = currentLocation
            }
        }
    }
}
