package com.example.guet_map.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemRecentGuideBinding
import com.example.guet_map.model.RecentGuide

class RecentGuideAdapter(
    private val onClick: (RecentGuide) -> Unit
) : ListAdapter<RecentGuide, RecentGuideAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentGuideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRecentGuideBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentGuide) {
            binding.tvLocationName.text = item.locationName
            binding.tvRecentMeta.text =
                "${item.stepCount} 步 · ${item.contributor} · ${item.approvedAt}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<RecentGuide>() {
        override fun areItemsTheSame(old: RecentGuide, new: RecentGuide) =
            old.locationId == new.locationId && old.approvedAt == new.approvedAt

        override fun areContentsTheSame(old: RecentGuide, new: RecentGuide) = old == new
    }
}
