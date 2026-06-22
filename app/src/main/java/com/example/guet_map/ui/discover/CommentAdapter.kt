package com.example.guet_map.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemCommentBinding
import com.example.guet_map.model.CommentWithUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommentAdapter : ListAdapter<CommentWithUser, CommentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(
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
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentWithUser) {
            binding.apply {
                val comment = item.comment
                val userInfo = item.userInfo

                tvUserName.text = userInfo?.nickname ?: "匿名用户"
                tvContent.text = comment.content
                tvTime.text = formatTime(comment.createdAt)

                val firstChar = userInfo?.nickname?.firstOrNull() ?: '?'
                tvAvatar.text = firstChar.uppercaseChar().toString()
            }
        }

        private fun formatTime(timestamp: String): String {
            return try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(timestamp)
                val now = System.currentTimeMillis()
                val diff = now - (date?.time ?: 0)

                when {
                    diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
                    diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
                    diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
                    else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date!!)
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CommentWithUser>() {
        override fun areItemsTheSame(oldItem: CommentWithUser, newItem: CommentWithUser): Boolean {
            return oldItem.comment.id == newItem.comment.id
        }

        override fun areContentsTheSame(oldItem: CommentWithUser, newItem: CommentWithUser): Boolean {
            return oldItem == newItem
        }
    }
}
