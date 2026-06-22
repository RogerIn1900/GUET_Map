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
import com.example.guet_map.databinding.FragmentFriendRequestsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FriendRequestsViewModel by viewModels()

    private lateinit var adapter: FriendRequestAdapter
    private var lastMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
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
        adapter = FriendRequestAdapter(
            onAccept = { requestId ->
                viewModel.handleRequest(requestId, true)
            },
            onReject = { requestId ->
                viewModel.handleRequest(requestId, false)
            }
        )

        binding.rvRequests.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FriendRequestsFragment.adapter
        }
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

    private fun render(state: FriendRequestsUiState) {
        binding.progressBar.isVisible = state.isLoading

        if (state.requests.isEmpty() && !state.isLoading) {
            binding.rvRequests.isVisible = false
            binding.tvEmpty.isVisible = true
        } else {
            binding.rvRequests.isVisible = true
            binding.tvEmpty.isVisible = false
            adapter.submitList(state.requests)
        }

        val msg = state.message
        if (!msg.isNullOrBlank() && msg != lastMessage) {
            lastMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRequests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
