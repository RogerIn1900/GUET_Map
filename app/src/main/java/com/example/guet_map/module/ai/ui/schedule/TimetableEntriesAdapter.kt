package com.example.guet_map.module.ai.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemTimetableEntryBinding
import com.example.guet_map.module.ai.data.model.TimetableEntry

class TimetableEntriesAdapter(
    private val onNavigate: (TimetableEntry) -> Unit,
    private val onEdit: (TimetableEntry) -> Unit,
    private val onDelete: (TimetableEntry) -> Unit
) : ListAdapter<TimetableEntry, TimetableEntriesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimetableEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTimetableEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: TimetableEntry) {
            binding.textCourseName.text = entry.courseName
            binding.textDetail.text = "${entry.formatDayOfWeek()} 第${entry.startPeriod}-${entry.endPeriod}节 · ${entry.classroomName}"
            binding.textWeekRange.text = "第${entry.weekRange}周"

            binding.buttonNavigate.setOnClickListener { onNavigate(entry) }
            binding.buttonEdit.setOnClickListener { onEdit(entry) }
            binding.buttonDelete.setOnClickListener { onDelete(entry) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TimetableEntry>() {
        override fun areItemsTheSame(oldItem: TimetableEntry, newItem: TimetableEntry) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TimetableEntry, newItem: TimetableEntry) =
            oldItem == newItem
    }
}
