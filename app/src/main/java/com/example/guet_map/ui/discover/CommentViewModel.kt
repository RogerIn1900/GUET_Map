package com.example.guet_map.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guet_map.data.UserPrefs
import com.example.guet_map.model.CommentWithUser
import com.example.guet_map.model.Resource
import com.example.guet_map.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _comments = MutableStateFlow<List<CommentWithUser>>(emptyList())
    val comments: StateFlow<List<CommentWithUser>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val postIdLong = postId.toLongOrNull() ?: return@launch

            socialRepository.getPostComments(postIdLong).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _comments.value = resource.data
                    }
                    is Resource.Error -> {
                        _error.value = resource.message
                    }
                    is Resource.Loading -> {}
                }
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun addComment(postId: String, content: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val postIdLong = postId.toLongOrNull() ?: return@launch

            socialRepository.addPostComment(postIdLong, content).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        // 重新加载评论
                        loadComments(postId)
                        onComplete?.invoke(true)
                    }
                    is Resource.Error -> {
                        _error.value = resource.message
                        onComplete?.invoke(false)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
}
