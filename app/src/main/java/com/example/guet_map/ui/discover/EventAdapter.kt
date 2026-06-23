package com.example.guet_map.ui.discover

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemEventBinding
import com.example.guet_map.ui.discover.model.CampusEvent
import com.example.guet_map.ui.discover.model.EventStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val onEventClick: (CampusEvent) -> Unit
) : ListAdapter<CampusEvent, EventAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: CampusEvent) {
            binding.apply {
                tvTitle.text = event.title
                tvTime.text = formatTime(event.startTime)
                tvLocation.text = event.location
                tvOrganizer.text = "主办: ${event.organizer}"
                tvAttendeeCount.text = "${event.attendeeCount}${event.maxAttendees?.let { "/$it" } ?: ""}人"

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

                if (event.registrationRequired) {
                    chipRegistration.visibility = View.VISIBLE
                    if (event.isFull) {
                        chipRegistration.text = "已满员"
                    } else {
                        chipRegistration.text = "需报名"
                    }
                } else {
                    chipRegistration.visibility = View.GONE
                }

                root.setOnClickListener { onEventClick(event) }
            }
        }

        private fun formatTime(timestamp: Long): String {
            return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
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
    }

    class DiffCallback : DiffUtil.ItemCallback<CampusEvent>() {
        override fun areItemsTheSame(oldItem: CampusEvent, newItem: CampusEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CampusEvent, newItem: CampusEvent): Boolean {
            return oldItem == newItem
        }
    }
}
