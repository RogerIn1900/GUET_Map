package com.example.guet_map.module.social.ui.announcement

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemCampusAnnouncementBinding
import com.example.guet_map.module.social.data.model.AnnouncementCategory
import com.example.guet_map.module.social.data.model.CampusAnnouncement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampusAnnouncementAdapter(
    private val onItemClick: (CampusAnnouncement) -> Unit
) : ListAdapter<CampusAnnouncement, CampusAnnouncementAdapter.ViewHolder>(DiffCallback()) {

    private var expandedId: String? = null

    fun setExpandedId(id: String?) {
        val previous = expandedId
        expandedId = id
        previous?.let { findPosition(it)?.let { pos -> notifyItemChanged(pos) } }
        id?.let { findPosition(it)?.let { pos -> notifyItemChanged(pos) } }
    }

    private fun findPosition(id: String): Int? {
        val pos = currentList.indexOfFirst { it.id == id }
        return if (pos >= 0) pos else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCampusAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCampusAnnouncementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CampusAnnouncement) {
            binding.apply {
                tvTitle.text = item.title
                tvAuthor.text = item.author
                tvTime.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(item.publishTime))
                tvContent.text = item.content

                val isExpanded = item.id == expandedId
                tvContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE

                chipCategory.text = item.category.displayName
                chipCategory.chipBackgroundColor = ColorStateList.valueOf(categoryColor(item.category))

                ivPinned.visibility = if (item.isPinned) android.view.View.VISIBLE else android.view.View.GONE

                val readAlpha = if (item.isRead) 0.6f else 1.0f
                root.alpha = readAlpha

                tvViewCount.text = "${item.viewCount} 次阅读"

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun categoryColor(category: AnnouncementCategory): Int {
            return when (category) {
                AnnouncementCategory.EMERGENCY -> android.graphics.Color.rgb(220, 38, 38)
                AnnouncementCategory.GENERAL -> android.graphics.Color.rgb(37, 99, 235)
                AnnouncementCategory.ACADEMIC -> android.graphics.Color.rgb(126, 34, 206)
                AnnouncementCategory.ACTIVITY -> android.graphics.Color.rgb(22, 163, 74)
                AnnouncementCategory.CAREER -> android.graphics.Color.rgb(217, 119, 6)
                AnnouncementCategory.MAINTENANCE -> android.graphics.Color.rgb(100, 116, 139)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CampusAnnouncement>() {
        override fun areItemsTheSame(oldItem: CampusAnnouncement, newItem: CampusAnnouncement): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CampusAnnouncement, newItem: CampusAnnouncement): Boolean =
            oldItem == newItem
    }
}
