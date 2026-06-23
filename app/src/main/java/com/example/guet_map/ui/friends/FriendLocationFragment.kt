package com.example.guet_map.ui.friends

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentFriendLocationBinding
import com.example.guet_map.util.CampusGeo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendLocationFragment : Fragment() {

    private var _binding: FragmentFriendLocationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FriendLocationViewModel by viewModels()

    private lateinit var adapter: FriendLocationAdapter
    private var lastMessage: String? = null
    private var aMap: AMap? = null
    private var mapViewCreated = false
    private var isSharingLocation = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupMap(savedInstanceState)
        setupRecyclerView()
        setupShareSwitch()
        observeState()
        checkLocationPermission()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        if (!mapViewCreated) {
            aMap = binding.mapView.map
            configureMap()
            mapViewCreated = true
        }
    }

    private fun configureMap() {
        aMap?.apply {
            uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = true
                isScaleControlsEnabled = true
                isMyLocationButtonEnabled = false
            }

            val cameraUpdate = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                LatLng(CampusGeo.CENTER_LAT, CampusGeo.CENTER_LNG), 16f
            )
            moveCamera(cameraUpdate)
        }
    }

    private fun setupRecyclerView() {
        adapter = FriendLocationAdapter { friend, location ->
            // 点击好友，在地图上显示位置
            showFriendOnMap(friend.nickname, location.latitude, location.longitude)
        }

        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = this@FriendLocationFragment.adapter
        }
    }

    private fun setupShareSwitch() {
        binding.switchShareLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkLocationPermissionAndStart()
            } else {
                stopLocationUpdates()
            }
            isSharingLocation = isChecked
            updateShareStatus()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.switchShareLocation.isChecked = isSharingLocation
            updateShareStatus()
        }
    }

    private fun checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationUpdates() {
        binding.tvShareStatus.text = "正在获取位置..."
        // TODO: 集成高德定位 SDK 来定时更新位置
        // 这里简化处理，使用模拟数据
        Toast.makeText(context, "位置共享已开启", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationUpdates() {
        binding.tvShareStatus.text = "位置共享已关闭"
        Toast.makeText(context, "位置共享已关闭", Toast.LENGTH_SHORT).show()
    }

    private fun updateShareStatus() {
        if (isSharingLocation) {
            binding.tvShareStatus.text = "正在共享你的位置"
            binding.tvShareStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.success)
            )
        } else {
            binding.tvShareStatus.text = "位置共享已关闭"
            binding.tvShareStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }
    }

    private fun showFriendOnMap(name: String, latitude: Double, longitude: Double) {
        aMap?.let { map ->
            // 清除之前的标记
            map.clear()

            // 添加好友位置标记
            val friendLatLng = LatLng(latitude, longitude)
            map.addMarker(
                MarkerOptions()
                    .position(friendLatLng)
                    .title(name)
                    .snippet("好友位置")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )

            // 移动到好友位置
            val cameraUpdate = com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(friendLatLng, 18f)
            map.animateCamera(cameraUpdate)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: FriendLocationUiState) {
        binding.progressBar.isVisible = state.isLoading

        // 过滤出有位置的好友
        val friendsWithLocation = state.friends.mapNotNull { friend ->
            val location = state.friendLocations[friend.userId]
            if (location != null) {
                friend to location
            } else null
        }

        if (friendsWithLocation.isNotEmpty()) {
            binding.rvFriends.isVisible = true
            binding.tvEmpty.isVisible = false
            binding.tvFriendsHeader.isVisible = true
            adapter.submitList(friendsWithLocation.map { (friend, location) ->
                FriendWithLocation(friend, location)
            })

            // 在地图上显示所有好友位置
            showAllFriendsOnMap(friendsWithLocation)
        } else {
            binding.rvFriends.isVisible = false
            binding.tvEmpty.isVisible = true
            binding.tvFriendsHeader.isVisible = false
        }

        val msg = state.message
        if (!msg.isNullOrBlank() && msg != lastMessage) {
            lastMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAllFriendsOnMap(friendsWithLocation: List<Pair<com.example.guet_map.model.FriendInfo, com.example.guet_map.model.FriendLocation>>) {
        aMap?.let { map ->
            friendsWithLocation.forEach { (friend, location) ->
                val latLng = LatLng(location.latitude, location.longitude)
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(friend.nickname)
                        .snippet("好友位置")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mapViewCreated) {
            binding.mapView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mapViewCreated) {
            binding.mapView.onPause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mapViewCreated) {
            binding.mapView.onDestroy()
            mapViewCreated = false
        }
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            if (mapViewCreated && _binding != null) {
                binding.mapView.onSaveInstanceState(outState)
            }
        } catch (_: Exception) {
        }
    }
}
