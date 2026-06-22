package com.example.guet_map.module.ai.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemChatMessageBinding
import com.example.guet_map.module.ai.data.model.ChatMessage
import com.example.guet_map.module.ai.data.model.ChatRole

/**
 * 聊天消息适配器
 */
class ChatMessageAdapter : ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder>(
    MessageDiffCallback()
) {

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

        fun bind(message: ChatMessage) {
            when (message.role) {
                ChatRole.USER -> {
                    binding.layoutSent.visibility = android.view.View.VISIBLE
                    binding.layoutReceived.visibility = android.view.View.GONE
                    binding.tvMessageSent.text = message.content
                    binding.tvTimeSent.text = message.timestamp.toString()
                }
                ChatRole.ASSISTANT -> {
                    binding.layoutSent.visibility = android.view.View.GONE
                    binding.layoutReceived.visibility = android.view.View.VISIBLE
                    binding.tvMessageReceived.text = message.content
                    binding.tvTimeReceived.text = message.timestamp.toString()
                }
                ChatRole.SYSTEM -> {
                    binding.layoutSent.visibility = android.view.View.GONE
                    binding.layoutReceived.visibility = android.view.View.VISIBLE
                    binding.tvMessageReceived.text = message.content
                    binding.tvTimeReceived.text = message.timestamp.toString()
                }
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
