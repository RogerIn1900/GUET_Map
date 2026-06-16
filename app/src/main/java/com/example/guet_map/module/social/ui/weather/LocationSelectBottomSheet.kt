package com.example.guet_map.module.social.ui.weather

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.example.guet_map.databinding.BottomSheetLocationBinding
import com.example.guet_map.module.social.data.remote.OpenMeteoGeocodingService
import com.example.guet_map.module.social.data.remote.dto.GeocodingResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationSelectBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var locationAdapter: LocationAdapter
    private lateinit var searchAdapter: LocationAdapter

    @Inject
    lateinit var geocodingService: OpenMeteoGeocodingService

    var onLocationSelected: ((WeatherLocation) -> Unit)? = null

    private var currentLocation: WeatherLocation? = null
    private var searchJob: Job? = null

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
        setupPresetAdapter()
        setupSearchAdapter()
        setupSearchBox()
    }

    private fun setupPresetAdapter() {
        locationAdapter = LocationAdapter { location ->
            onLocationSelected?.invoke(location)
            dismiss()
        }
        binding.rvLocations.adapter = locationAdapter
        locationAdapter.submitList(WeatherLocation.PRESETS)
        currentLocation?.let { locationAdapter.setSelected(it) }
    }

    private fun setupSearchAdapter() {
        searchAdapter = LocationAdapter { location ->
            onLocationSelected?.invoke(location)
            dismiss()
        }
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun setupSearchBox() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                binding.ivClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                scheduleSearch(query)
            }
        })

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                val query = binding.etSearch.text?.toString()?.trim().orEmpty()
                if (query.isNotEmpty()) performSearch(query)
                true
            } else false
        }

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            clearSearchResults()
        }
    }

    /** 防抖：用户连续输入时只触发最后一次搜索 */
    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.isEmpty()) {
            clearSearchResults()
            return
        }
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        showSearchLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = geocodingService.search(name = query, count = 10)
                val results = resp.results.orEmpty()
                showSearchLoading(false)
                if (results.isEmpty()) {
                    showEmptySearch()
                } else {
                    showSearchResults(results)
                }
            } catch (e: Exception) {
                showSearchLoading(false)
                Toast.makeText(
                    requireContext(),
                    "搜索失败：${e.message ?: "网络异常"}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptySearch()
            }
        }
    }

    private fun showSearchLoading(loading: Boolean) {
        binding.progressSearch.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showSearchResults(results: List<GeocodingResult>) {
        binding.tvSearchHeader.visibility = View.VISIBLE
        binding.tvSearchHeader.text = "搜索结果（${results.size}）"
        binding.rvSearchResults.visibility = View.VISIBLE
        binding.tvEmptySearch.visibility = View.GONE

        val locations = results.mapNotNull { it.toWeatherLocation() }
        searchAdapter.submitList(locations)
    }

    private fun showEmptySearch() {
        binding.tvSearchHeader.visibility = View.VISIBLE
        binding.tvSearchHeader.text = "搜索结果"
        binding.rvSearchResults.visibility = View.GONE
        binding.tvEmptySearch.visibility = View.VISIBLE
        searchAdapter.submitList(emptyList())
    }

    private fun clearSearchResults() {
        binding.tvSearchHeader.visibility = View.GONE
        binding.rvSearchResults.visibility = View.GONE
        binding.tvEmptySearch.visibility = View.GONE
        searchAdapter.submitList(emptyList())
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 350L

        fun newInstance(currentLocation: WeatherLocation? = null): LocationSelectBottomSheet {
            return LocationSelectBottomSheet().apply {
                this.currentLocation = currentLocation
            }
        }
    }
}

/**
 * GeocodingResult → WeatherLocation 的扩展（写在文件内避免污染公共 API）。
 * displayName 已经把"桂林市 / 广西 / 中国"格式拼好，足够用户辨认。
 */
private fun GeocodingResult.toWeatherLocation(): WeatherLocation? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val name = displayName().ifEmpty { return null }
    return WeatherLocation.custom(name = name, lat = lat, lng = lng)
}
