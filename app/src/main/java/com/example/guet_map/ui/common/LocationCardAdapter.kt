package com.example.guet_map.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemLocationCardBinding
import com.example.guet_map.model.Location

class LocationCardAdapter(
    private val onItemClick: (Location) -> Unit,
    private val onItemLongClick: ((Location) -> Unit)? = null
) : ListAdapter<Location, LocationCardAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLocationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(location: Location) {
            binding.tvName.text = location.name
            binding.tvCategory.text = location.category
            val guideLabel = if (location.hasGuide) "有指引" else "无指引"
            binding.tvMeta.text = "${location.rating} ★ · $guideLabel"
            if (location.imageUrl.isNotBlank()) {
                binding.ivThumb.load(location.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_image_placeholder)
                    error(R.drawable.bg_image_placeholder)
                }
            } else {
                binding.ivThumb.setImageResource(R.drawable.bg_image_placeholder)
            }
            binding.root.setOnClickListener { onItemClick(location) }
            onItemLongClick?.let { longClick ->
                binding.root.setOnLongClickListener {
                    longClick(location)
                    true
                }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(old: Location, new: Location) =
            old.locationId == new.locationId

        override fun areContentsTheSame(old: Location, new: Location) = old == new
    }
}
