package com.example.guet_map.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemNotificationBinding
import com.example.guet_map.model.AppNotification

class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppNotification) {
            binding.tvNotifTitle.text = item.title
            binding.tvNotifBody.text = item.body
            binding.tvNotifTime.text = item.createdAt
            binding.root.alpha = if (item.isRead) 0.7f else 1f
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(old: AppNotification, new: AppNotification) =
            old.id == new.id

        override fun areContentsTheSame(old: AppNotification, new: AppNotification) = old == new
    }
}
