package com.example.guet_map.module.ai.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentApiKeySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * DeepSeek API Key 配置页
 */
@AndroidEntryPoint
class ApiKeySettingsFragment : Fragment() {

    private var _binding: FragmentApiKeySettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApiKeySettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApiKeySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupInput()
        setupButtons()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupInput() {
        binding.editTextApiKey.doAfterTextChanged { text ->
            viewModel.updateKey(text?.toString() ?: "")
        }
    }

    private fun setupButtons() {
        binding.buttonSave.setOnClickListener {
            viewModel.saveKey(binding.editTextApiKey.text?.toString() ?: "")
        }

        binding.buttonClear.setOnClickListener {
            viewModel.clearKey()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentKey.collect { key ->
                        if (binding.editTextApiKey.text?.toString() != key) {
                            binding.editTextApiKey.setText(key)
                        }
                        updateStatus(key.isNotBlank())
                    }
                }

                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            is SaveState.Success -> {
                                Toast.makeText(requireContext(), "API Key 保存成功", Toast.LENGTH_SHORT).show()
                                binding.progressBar.visibility = View.GONE
                                binding.buttonSave.isEnabled = true
                            }
                            is SaveState.Cleared -> {
                                Toast.makeText(requireContext(), "API Key 已清除", Toast.LENGTH_SHORT).show()
                                binding.progressBar.visibility = View.GONE
                                binding.editTextApiKey.text?.clear()
                                updateStatus(false)
                            }
                            is SaveState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                binding.progressBar.visibility = View.GONE
                                binding.buttonSave.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateStatus(configured: Boolean) {
        val (color, text) = if (configured) {
            Pair(
                ContextCompat.getColor(requireContext(), R.color.success),
                "已配置 AI 服务，可正常使用"
            )
        } else {
            Pair(
                ContextCompat.getColor(requireContext(), R.color.warning),
                "未配置 API Key，AI 功能不可用"
            )
        }
        binding.statusIndicator.background.setTint(color)
        binding.textStatus.text = text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ApiKeySettingsFragment()
    }
}
