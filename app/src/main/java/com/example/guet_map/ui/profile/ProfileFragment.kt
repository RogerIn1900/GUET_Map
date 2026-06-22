package com.example.guet_map.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private var lastMessage: String? = null
    private var currentPhotoPath: String? = null

    // 图片选择器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    // 拍照
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoPath != null) {
            val file = File(currentPhotoPath!!)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                handleSelectedImage(uri)
            } else {
                Toast.makeText(context, "拍照失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 相机权限请求
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 相册权限请求（Android 12 及以下）
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "需要相册权限才能选择头像", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeState()
    }

    private fun setupClickListeners() {
        // 点击头像选择图片
        binding.ivAvatar.setOnClickListener {
            showAvatarPickerDialog()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.itemFriends.setOnClickListener {
            findNavController().navigate(R.id.nav_friends)
        }

        binding.itemAddFriend.setOnClickListener {
            findNavController().navigate(R.id.nav_add_friend)
        }

        binding.itemFriendLocation.setOnClickListener {
            findNavController().navigate(R.id.nav_friend_location)
        }

        binding.itemFriendRequests.setOnClickListener {
            findNavController().navigate(R.id.nav_friend_requests)
        }

        binding.itemAddFriend.setOnClickListener {
            findNavController().navigate(R.id.nav_add_friend)
        }

        binding.itemFavorites.setOnClickListener {
            findNavController().navigate(R.id.nav_favorites)
        }

        binding.itemLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showAvatarPickerDialog() {
        val options = arrayOf("拍照", "从相册选择")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("更换头像")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        try {
            val photoFile = createImageFile()
            currentPhotoPath = photoFile.absolutePath

            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "无法创建图片文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pickImageLauncher.launch("image/*")
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 及以下使用 READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pickImageLauncher.launch("image/*")
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("AVATAR_${timeStamp}_", ".jpg", storageDir)
    }

    private fun handleSelectedImage(uri: Uri) {
        // 获取真实路径
        val path = getPathFromUri(uri)
        if (path != null) {
            viewModel.updateAvatar(path)
            // 预览头像
            binding.ivAvatar.load(uri) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            // 如果无法获取路径，直接使用 URI
            viewModel.updateAvatar(uri.toString())
            binding.ivAvatar.load(uri) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                uri.path
            } else {
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                requireContext().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        cursor.getString(columnIndex)
                    } else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val currentNickname = binding.tvNickname.text?.toString() ?: ""
        
        val editText = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            setText(currentNickname)
            hint = "请输入新昵称"
            setPadding(48, 32, 48, 32)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑昵称")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newNickname = editText.text?.toString()?.trim() ?: ""
                if (newNickname.isNotBlank() && newNickname != currentNickname) {
                    viewModel.updateNickname(newNickname)
                }
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

    private fun render(state: ProfileUiState) {
        binding.progressBar.isVisible = state.isLoading

        if (state.isLoggedIn) {
            binding.tvNickname.text = state.nickname
            binding.tvEmail.text = state.email
            binding.tvPoints.text = state.points.toString()
            binding.tvContributions.text = state.contributionCount.toString()
            binding.tvFriends.text = state.friendCount.toString()

            // 更新头像
            if (!state.avatar.isNullOrEmpty()) {
                val avatarUrl = if (state.avatar.startsWith("http")) {
                    state.avatar
                } else if (state.avatar.startsWith("/")) {
                    File(state.avatar).takeIf { it.exists() }?.let { Uri.fromFile(it).toString() }
                } else {
                    state.avatar
                }

                if (avatarUrl != null) {
                    binding.ivAvatar.load(Uri.parse(avatarUrl)) {
                        crossfade(true)
                        placeholder(R.drawable.ic_avatar)
                        error(R.drawable.ic_avatar)
                        transformations(CircleCropTransformation())
                    }
                }
            }

            // 显示待处理的好友请求数量
            if (state.pendingFriendRequests > 0) {
                binding.tvFriendRequestCount.isVisible = true
                binding.tvFriendRequestCount.text = state.pendingFriendRequests.toString()
            } else {
                binding.tvFriendRequestCount.isVisible = false
            }
        } else {
            // 未登录，跳转到登录页面
            findNavController().navigate(R.id.nav_login)
        }

        // 显示消息
        val msg = state.message
        if (!msg.isNullOrBlank() && msg != lastMessage) {
            lastMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
