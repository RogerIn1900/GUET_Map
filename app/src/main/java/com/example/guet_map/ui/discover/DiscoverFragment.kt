package com.example.guet_map.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentDiscoverBinding
import com.example.guet_map.ui.MainNavViewModel
import com.example.guet_map.ui.common.LocationCardAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoverViewModel by viewModels()
    private val sharedViewModel: DiscoverSharedViewModel by activityViewModels()
    private val mainNavViewModel: MainNavViewModel by activityViewModels()

    private lateinit var checkInPostAdapter: CheckInPostAdapter
    private lateinit var eventAdapter: EventAdapter
    private lateinit var favoritesAdapter: LocationCardAdapter

    private var lastFavoritesState: List<com.example.guet_map.model.Location> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUserAvatar()
        setupTabs()
        setupAdapters()
        setupCalendarNav()
        setupPublishBar()
        observeData()
        updateCalendarDate()
    }

    private fun setupUserAvatar() {
        binding.tvPublishHint.text = "说点什么..."
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showCheckInTab()
                    1 -> showEventsTab()
                    2 -> showFavoritesTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        checkInPostAdapter = CheckInPostAdapter(
            onLikeClick = { postId ->
                viewModel.toggleLike(postId)
            },
            onCommentClick = { postId ->
                Toast.makeText(context, "评论功能开发中", Toast.LENGTH_SHORT).show()
            }
        )

        eventAdapter = EventAdapter { event ->
            sharedViewModel.setSelectedEvent(event)
            findNavController().navigate(R.id.nav_event_detail)
        }

        favoritesAdapter = LocationCardAdapter(
            onItemClick = { location ->
                mainNavViewModel.openLocationOnMap(location.locationId)
            },
            onItemLongClick = { location ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("取消收藏")
                    .setMessage("确定取消收藏「${location.name}」？")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.removeFavorite(location.locationId)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupCalendarNav() {
        binding.btnPrevDay.setOnClickListener {
            viewModel.shiftSelectedDate(-1)
            updateCalendarDate()
        }

        binding.btnNextDay.setOnClickListener {
            viewModel.shiftSelectedDate(1)
            updateCalendarDate()
        }

        binding.btnToday.setOnClickListener {
            viewModel.resetToToday()
            updateCalendarDate()
        }
    }

    private fun updateCalendarDate() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.selectedDateMillis.value

        val dayFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
        val weekdayFormat = SimpleDateFormat("EEEE", Locale.CHINESE)
        val weekday = weekdayFormat.format(calendar.time)
        binding.tvSelectedDate.text = "${dayFormat.format(calendar.time)} $weekday"

        // Always enable next day button to allow browsing future events
        binding.btnNextDay.isEnabled = true
        binding.btnNextDay.alpha = 1f
    }

    private fun setupPublishBar() {
        binding.tvPublishHint.setOnClickListener {
            showPublishDialog()
        }
        binding.ivMyAvatar.setOnClickListener {
            showPublishDialog()
        }
    }

    private fun showPublishDialog() {
        val dialog = PublishCheckInDialog.newInstance()
        dialog.setOnPublishedListener { locationId, locationName, content, topics ->
            viewModel.publishCheckIn(locationName, content, topics)
        }
        dialog.show(childFragmentManager, "publish_checkin")
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.checkInPosts.collect { _ ->
                        checkInPostAdapter.submitList(viewModel.getFilteredPosts())
                    }
                }
                launch {
                    viewModel.events.collect { events ->
                        val filtered = viewModel.getFilteredEvents()
                        eventAdapter.submitList(filtered)
                        updateEmptyState(filtered)
                    }
                }
                launch {
                    viewModel.selectedDateMillis.collect { _ ->
                        eventAdapter.submitList(viewModel.getFilteredEvents())
                        updateEmptyState(viewModel.getFilteredEvents())
                    }
                }
                launch {
                    viewModel.topics.collect { topics ->
                        setupTopicChips(topics)
                    }
                }
                launch {
                    viewModel.selectedTopic.collect {
                        checkInPostAdapter.submitList(viewModel.getFilteredPosts())
                    }
                }
                launch {
                    viewModel.favorites.collect { list ->
                        lastFavoritesState = list
                        favoritesAdapter.submitList(list)
                        if (binding.tabLayout.selectedTabPosition == 2) {
                            updateEmptyState(list)
                        }
                    }
                }
            }
        }
    }

    private fun setupTopicChips(topics: List<String>) {
        binding.chipGroupTopicsInner.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            text = "全部"
            isCheckable = true
            isChecked = viewModel.selectedTopic.value == null
            setOnClickListener {
                viewModel.selectTopic(null)
                updateChipSelection(null)
            }
        }
        binding.chipGroupTopicsInner.addView(allChip)

        topics.forEach { topic ->
            val chip = Chip(requireContext()).apply {
                text = "#$topic"
                isCheckable = true
                isChecked = viewModel.selectedTopic.value == topic
                setOnClickListener {
                    viewModel.selectTopic(topic)
                    updateChipSelection(topic)
                }
            }
            binding.chipGroupTopicsInner.addView(chip)
        }
    }

    private fun updateChipSelection(selectedTopic: String?) {
        for (i in 0 until binding.chipGroupTopicsInner.childCount) {
            val chip = binding.chipGroupTopicsInner.getChildAt(i) as? Chip
            chip?.isChecked = when {
                selectedTopic == null && i == 0 -> true
                selectedTopic != null && chip?.text?.contains(selectedTopic) == true -> true
                else -> false
            }
        }
    }

    private fun showCheckInTab() {
        binding.layoutPublishBar.isVisible = true
        binding.chipGroupTopics.isVisible = true
        binding.layoutCalendarNav.isVisible = false
        binding.recyclerView.adapter = checkInPostAdapter
        checkInPostAdapter.submitList(viewModel.getFilteredPosts())
    }

    private fun showEventsTab() {
        binding.layoutPublishBar.isVisible = false
        binding.chipGroupTopics.isVisible = false
        binding.layoutCalendarNav.isVisible = true
        binding.recyclerView.adapter = eventAdapter
        eventAdapter.submitList(viewModel.getFilteredEvents())
        updateEmptyState(viewModel.getFilteredEvents())
    }

    private fun showFavoritesTab() {
        binding.layoutPublishBar.isVisible = false
        binding.chipGroupTopics.isVisible = false
        binding.layoutCalendarNav.isVisible = false
        binding.recyclerView.adapter = favoritesAdapter
        favoritesAdapter.submitList(lastFavoritesState)
        updateEmptyState(lastFavoritesState)
    }

    private fun updateEmptyState(list: List<*>) {
        if (list.isEmpty()) {
            binding.recyclerView.isVisible = false
            binding.textViewEmpty.isVisible = true
        } else {
            binding.recyclerView.isVisible = true
            binding.textViewEmpty.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
