package com.example.guet_map.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemChatMessageBinding
import com.example.guet_map.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

class ChatMessageAdapter(
    private val onOwnMessage: (Message) -> Boolean
) : ListAdapter<Message, ChatMessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val isOwn = onOwnMessage(message)

            binding.layoutReceived.visibility = if (isOwn) android.view.View.GONE else android.view.View.VISIBLE
            binding.layoutSent.visibility = if (isOwn) android.view.View.VISIBLE else android.view.View.GONE

            if (isOwn) {
                binding.tvMessageSent.text = message.content
                binding.tvTimeSent.text = timeFormat.format(parseTime(message.createdAt))
            } else {
                binding.tvMessageReceived.text = message.content
                binding.tvTimeReceived.text = timeFormat.format(parseTime(message.createdAt))
            }
        }

        private fun parseTime(timeStr: String): java.util.Date {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                format.parse(timeStr) ?: java.util.Date()
            } catch (e: Exception) {
                java.util.Date()
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
