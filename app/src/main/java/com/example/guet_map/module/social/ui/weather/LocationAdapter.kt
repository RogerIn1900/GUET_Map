package com.example.guet_map.module.social.ui.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemLocationBinding

class LocationAdapter(
    private val onItemClick: (WeatherLocation) -> Unit
) : ListAdapter<WeatherLocation, LocationAdapter.ViewHolder>(Diff) {

    private var selectedId: String? = null

    fun setSelected(location: WeatherLocation) {
        val oldId = selectedId
        selectedId = "${location.lat},${location.lng}"
        oldId?.let { id -> notifyItemChanged(currentList.indexOfFirst { "${it.lat},${it.lng}" == id }) }
        selectedId?.let { id -> notifyItemChanged(currentList.indexOfFirst { "${it.lat},${it.lng}" == id }) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLocationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(item: WeatherLocation) {
            binding.tvLocationName.text = item.name
            binding.tvCoords.text = String.format("%.4f, %.4f", item.lat, item.lng)
            binding.ivSelected.visibility = if ("${item.lat},${item.lng}" == selectedId) View.VISIBLE else View.GONE
        }
    }

    private object Diff : DiffUtil.ItemCallback<WeatherLocation>() {
        override fun areItemsTheSame(oldItem: WeatherLocation, newItem: WeatherLocation) =
            oldItem.lat == newItem.lat && oldItem.lng == newItem.lng

        override fun areContentsTheSame(oldItem: WeatherLocation, newItem: WeatherLocation) =
            oldItem == newItem
    }
}
