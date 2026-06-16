package com.example.guet_map.module.social.ui.announcement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.FragmentCampusAnnouncementBinding
import com.example.guet_map.module.social.data.model.AnnouncementCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CampusAnnouncementFragment : Fragment() {

    private var _binding: FragmentCampusAnnouncementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CampusAnnouncementViewModel by viewModels()

    private lateinit var adapter: CampusAnnouncementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusAnnouncementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        setupSwipeRefresh()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = CampusAnnouncementAdapter { announcement ->
            viewModel.toggleExpand(announcement.id)
        }

        binding.recyclerViewAnnouncements.apply {
            adapter = this@CampusAnnouncementFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCategoryChips() {
        binding.chipAll.setOnClickListener {
            clearChipSelection()
            binding.chipAll.isChecked = true
            viewModel.selectCategory(null)
        }

        val chipMap = mapOf(
            binding.chipGeneral to AnnouncementCategory.GENERAL,
            binding.chipAcademic to AnnouncementCategory.ACADEMIC,
            binding.chipActivity to AnnouncementCategory.ACTIVITY,
            binding.chipCareer to AnnouncementCategory.CAREER,
            binding.chipMaintenance to AnnouncementCategory.MAINTENANCE,
            binding.chipEmergency to AnnouncementCategory.EMERGENCY
        )

        chipMap.forEach { (chip, category) ->
            chip.setOnClickListener {
                clearChipSelection()
                chip.isChecked = true
                viewModel.selectCategory(category)
            }
        }
    }

    private fun clearChipSelection() {
        binding.chipAll.isChecked = false
        binding.chipGeneral.isChecked = false
        binding.chipAcademic.isChecked = false
        binding.chipActivity.isChecked = false
        binding.chipCareer.isChecked = false
        binding.chipMaintenance.isChecked = false
        binding.chipEmergency.isChecked = false
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.announcements.collect { announcements ->
                        adapter.submitList(announcements)
                    }
                }

                launch {
                    viewModel.expandedId.collect { id ->
                        adapter.setExpandedId(id)
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = false

                        when (state) {
                            is AnnouncementUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is AnnouncementUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                            }
                            is AnnouncementUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                            is AnnouncementUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CampusAnnouncementFragment()
    }
}
