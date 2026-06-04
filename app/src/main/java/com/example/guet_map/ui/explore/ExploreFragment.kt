package com.example.guet_map.ui.explore

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentExploreBinding
import com.example.guet_map.model.Resource
import com.example.guet_map.ui.MainNavViewModel
import com.example.guet_map.ui.common.LocationCardAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()
    private val mainNavViewModel: MainNavViewModel by activityViewModels()

    private lateinit var hotAdapter: LocationCardAdapter
    private lateinit var recentAdapter: RecentGuideAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExploreBinding.bind(view)

        hotAdapter = LocationCardAdapter(onItemClick = { location ->
            mainNavViewModel.openLocationOnMap(location.locationId)
        })
        binding.rvHotLocations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHotLocations.adapter = hotAdapter

        recentAdapter = RecentGuideAdapter { guide ->
            mainNavViewModel.openLocationOnMap(guide.locationId)
        }
        binding.rvRecentGuides.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentGuides.adapter = recentAdapter

        setupCategoryChips()
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupCategoryChips() {
        viewModel.categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                setOnClickListener {
                    viewModel.filterByCategory(category)
                    mainNavViewModel.openMapWithCategory(category)
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.hotLocations.collectLatest { hotAdapter.submitList(it) }
                }
                launch {
                    viewModel.recentGuides.collectLatest { resource ->
                        binding.progressExplore.isVisible = resource is Resource.Loading
                        if (resource is Resource.Success) {
                            recentAdapter.submitList(resource.data)
                        }
                    }
                }
            }
        }
    }
}
