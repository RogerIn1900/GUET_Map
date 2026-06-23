package com.example.guet_map.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.guet_map.databinding.FragmentAddFriendBinding
import com.example.guet_map.model.FriendRequestWithUser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddFriendFragment : Fragment() {

    private var _binding: FragmentAddFriendBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddFriendViewModel by viewModels()

    private var lastMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSearch()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.etEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val email = binding.etEmail.text?.toString().orEmpty()
        viewModel.searchUser(email)
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

    private fun render(state: AddFriendUiState) {
        binding.progressBar.isVisible = state.isLoading

        // 搜索结果
        state.searchResult?.let { result ->
            binding.cardSearchResult.isVisible = true
            binding.tvResultNickname.text = result.user.nickname
            binding.tvResultEmail.text = result.user.email

            when {
                result.isFriend -> {
                    binding.tvResultStatus.isVisible = true
                    binding.tvResultStatus.text = "已是你好友"
                    binding.btnSendRequest.isVisible = false
                }
                result.hasPendingRequest -> {
                    binding.tvResultStatus.isVisible = true
                    binding.tvResultStatus.text = "待对方确认"
                    binding.btnSendRequest.isVisible = false
                }
                state.requestSent -> {
                    binding.tvResultStatus.isVisible = true
                    binding.tvResultStatus.text = "已发送申请"
                    binding.btnSendRequest.isVisible = false
                }
                else -> {
                    binding.tvResultStatus.isVisible = false
                    binding.btnSendRequest.isVisible = true
                    binding.btnSendRequest.setOnClickListener {
                        viewModel.sendFriendRequest(result.user.userId)
                    }
                }
            }
        } ?: run {
            binding.cardSearchResult.isVisible = false
        }

        // 待处理申请
        if (state.pendingRequests.isNotEmpty()) {
            binding.tvPendingHeader.isVisible = true
            binding.rvPendingRequests.isVisible = true
            binding.tvPendingEmpty.isVisible = false
            // TODO: 显示待处理申请列表
        } else {
            binding.tvPendingHeader.isVisible = false
            binding.rvPendingRequests.isVisible = false
            binding.tvPendingEmpty.isVisible = true
        }

        // 消息
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
