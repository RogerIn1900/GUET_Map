package com.example.guet_map.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentFriendsBinding
import com.example.guet_map.model.FriendInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendsFragment : Fragment() {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FriendsViewModel by viewModels()

    private lateinit var adapter: FriendAdapter
    private var lastMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = FriendAdapter(
            onItemClick = { friend ->
                navigateToChat(friend)
            },
            onItemLongClick = { friend ->
                showDeleteConfirmation(friend)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FriendsFragment.adapter
        }
    }

    private fun navigateToChat(friend: FriendInfo) {
        // 跳转到聊天页面，传递好友信息
        val bundle = Bundle().apply {
            putLong("friendId", friend.userId)
            putString("friendName", friend.nickname)
        }
        findNavController().navigate(R.id.nav_chat, bundle)
    }

    private fun showDeleteConfirmation(friend: FriendInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除好友")
            .setMessage("确定要删除好友「${friend.nickname}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.removeFriend(friend.userId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: FriendsUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.textViewEmpty.isVisible = !state.isLoading && state.friends.isEmpty()
        binding.recyclerView.isVisible = !state.isLoading && state.friends.isNotEmpty()

        adapter.submitList(state.friends)

        val msg = state.message
        if (!msg.isNullOrBlank() && msg != lastMessage) {
            lastMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
