package com.example.guet_map.ui.discover

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.guet_map.databinding.FragmentEventDetailBinding
import com.example.guet_map.ui.discover.model.CampusEvent
import com.example.guet_map.ui.discover.model.EventStatus
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: DiscoverSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        sharedViewModel.selectedEvent.value?.let { bindEventData(it) }
    }

    private fun bindEventData(event: CampusEvent) {
        binding.apply {
            tvTitle.text = event.title
            tvDescription.text = event.description
            tvOrganizer.text = event.organizer
            tvLocation.text = event.location

            val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = buildString {
                append(dateFormat.format(Date(event.startTime)))
                append(" ")
                append(timeFormat.format(Date(event.startTime)))
                append(" - ")
                append(timeFormat.format(Date(event.endTime)))
            }

            chipCategory.text = "${event.category.emoji} ${event.category.displayName}"
            chipCategory.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                getCategoryColor(event.category)
            )

            when (event.status) {
                EventStatus.UPCOMING -> {
                    tvStatus.text = "即将开始"
                    tvStatus.setTextColor(Color.parseColor("#059669"))
                }
                EventStatus.ONGOING -> {
                    tvStatus.text = "正在进行"
                    tvStatus.setTextColor(Color.parseColor("#DC2626"))
                }
                EventStatus.ENDED -> {
                    tvStatus.text = "已结束"
                    tvStatus.setTextColor(Color.parseColor("#6B7280"))
                }
            }

            val attendeeText = if (event.maxAttendees != null) {
                "${event.attendeeCount} / ${event.maxAttendees} 人"
            } else {
                "${event.attendeeCount} 人"
            }
            tvAttendeeCount.text = attendeeText

            if (event.registrationRequired) {
                chipRegistration.visibility = View.VISIBLE
                chipRegistration.text = if (event.isFull) "已满员" else "需报名"
            } else {
                chipRegistration.visibility = View.GONE
            }
        }
    }

    private fun getCategoryColor(category: com.example.guet_map.ui.discover.model.EventCategory): Int {
        return when (category) {
            com.example.guet_map.ui.discover.model.EventCategory.CULTURE -> Color.parseColor("#DBEAFE")
            com.example.guet_map.ui.discover.model.EventCategory.SPORTS -> Color.parseColor("#FEF3C7")
            com.example.guet_map.ui.discover.model.EventCategory.ACADEMIC -> Color.parseColor("#E0E7FF")
            com.example.guet_map.ui.discover.model.EventCategory.VOLUNTEER -> Color.parseColor("#FCE7F3")
            com.example.guet_map.ui.discover.model.EventCategory.CAREER -> Color.parseColor("#D1FAE5")
            com.example.guet_map.ui.discover.model.EventCategory.OTHER -> Color.parseColor("#F3F4F6")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
