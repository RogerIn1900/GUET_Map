package com.example.guet_map.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.guet_map.R
import com.example.guet_map.databinding.ItemFriendRequestBinding
import com.example.guet_map.model.FriendRequestWithUser

class FriendRequestAdapter(
    private val onAccept: (Long) -> Unit,
    private val onReject: (Long) -> Unit
) : ListAdapter<FriendRequestWithUser, FriendRequestAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
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
        private val binding: ItemFriendRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendRequestWithUser) {
            val user = item.fromUserInfo ?: return
            val request = item.request

            binding.tvNickname.text = user.nickname
            binding.tvEmail.text = user.email
            binding.tvMessage.text = request.message ?: "申请添加你为好友"

            // 头像
            if (!user.avatar.isNullOrEmpty()) {
                binding.ivAvatar.load(user.avatar) {
                    crossfade(true)
                    placeholder(R.drawable.ic_avatar)
                    error(R.drawable.ic_avatar)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_avatar)
            }

            binding.btnAccept.setOnClickListener {
                onAccept(request.id)
            }

            binding.btnReject.setOnClickListener {
                onReject(request.id)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FriendRequestWithUser>() {
        override fun areItemsTheSame(
            oldItem: FriendRequestWithUser,
            newItem: FriendRequestWithUser
        ): Boolean = oldItem.request.id == newItem.request.id

        override fun areContentsTheSame(
            oldItem: FriendRequestWithUser,
            newItem: FriendRequestWithUser
        ): Boolean = oldItem == newItem
    }
}
