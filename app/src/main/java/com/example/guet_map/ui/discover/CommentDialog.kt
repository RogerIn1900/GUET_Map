package com.example.guet_map.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guet_map.databinding.DialogCommentBinding
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.SocialRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CommentDialog : BottomSheetDialogFragment() {

    private var _binding: DialogCommentBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var socialRepository: SocialRepository

    private val viewModel: CommentViewModel by activityViewModels()

    private var postId: String = ""
    private var postAuthorName: String = ""

    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            postId = it.getString(ARG_POST_ID, "")
            postAuthorName = it.getString(ARG_POST_AUTHOR_NAME, "")
        }

        setupViews()
        setupCommentsList()
        observeComments()
        loadComments()
    }

    private fun setupViews() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnSend.setOnClickListener { sendComment() }
    }

    private fun setupCommentsList() {
        commentAdapter = CommentAdapter()
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }
    }

    private fun observeComments() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.comments.collect { comments ->
                        commentAdapter.submitList(comments)
                        updateEmptyState(comments.isEmpty())
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                    }
                }
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun loadComments() {
        viewModel.loadComments(postId)
    }

    private fun sendComment() {
        val content = binding.etComment.text?.toString()?.trim()
        if (content.isNullOrBlank()) {
            Toast.makeText(context, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSend.isEnabled = false
        viewModel.addComment(postId, content) { success ->
            binding.btnSend.isEnabled = true
            if (success) {
                binding.etComment.text?.clear()
                Toast.makeText(context, "评论成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.isVisible = isEmpty && !viewModel.isLoading.value
        binding.rvComments.isVisible = !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_POST_AUTHOR_NAME = "post_author_name"

        fun newInstance(postId: String, postAuthorName: String): CommentDialog {
            return CommentDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_POST_AUTHOR_NAME, postAuthorName)
                }
            }
        }
    }
}
