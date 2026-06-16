package com.example.guet_map.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentNotificationsBinding
import com.example.guet_map.module.social.ui.announcement.CampusAnnouncementAdapter
import com.example.guet_map.module.social.ui.announcement.CampusAnnouncementViewModel
import com.example.guet_map.module.social.ui.announcement.AnnouncementUiState
import com.example.guet_map.ui.MainNavViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationsFragment : Fragment(R.layout.fragment_notifications) {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()
    private val announcementViewModel: CampusAnnouncementViewModel by viewModels()
    private val mainNavViewModel: MainNavViewModel by activityViewModels()

    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var announcementAdapter: CampusAnnouncementAdapter

    private var currentTab = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationsBinding.bind(view)

        setupTabs()
        setupNotificationList()
        setupAnnouncementList()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateContentVisibility()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateContentVisibility() {
        when (currentTab) {
            0 -> {
                binding.layoutNotifications.visibility = View.VISIBLE
                binding.rvAnnouncements.visibility = View.GONE
            }
            1 -> {
                binding.layoutNotifications.visibility = View.GONE
                binding.rvAnnouncements.visibility = View.VISIBLE
            }
        }
    }

    private fun setupNotificationList() {
        notificationAdapter = NotificationAdapter { notification ->
            viewModel.onNotificationClicked(notification)
            notification.locationId?.let { id ->
                mainNavViewModel.openLocationOnMap(id)
            }
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = notificationAdapter

        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllRead()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notifications.collectLatest { notificationAdapter.submitList(it) }
            }
        }
    }

    private fun setupAnnouncementList() {
        announcementAdapter = CampusAnnouncementAdapter { announcement ->
            announcementViewModel.toggleExpand(announcement.id)
        }
        binding.rvAnnouncements.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAnnouncements.adapter = announcementAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    announcementViewModel.announcements.collect { announcements ->
                        announcementAdapter.submitList(announcements)
                    }
                }
                launch {
                    announcementViewModel.expandedId.collect { id ->
                        announcementAdapter.setExpandedId(id)
                    }
                }
                launch {
                    announcementViewModel.uiState.collect { state ->
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
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
