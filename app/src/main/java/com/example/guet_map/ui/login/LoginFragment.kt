package com.example.guet_map.ui.login

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.guet_map.R
import com.example.guet_map.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var lastMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)
        viewModel.refresh()

        setupTabSwitch()
        setupClickListeners()
        observeState()
    }

    private fun setupTabSwitch() {
        binding.tabLogin.setOnClickListener {
            switchToLogin()
        }
        binding.tabRegister.setOnClickListener {
            switchToRegister()
        }
    }

    private fun switchToLogin() {
        binding.tabLogin.setBackgroundResource(R.drawable.bg_tab_selected)
        binding.tabLogin.setTextColor(requireContext().getColor(R.color.white))
        binding.tabRegister.setBackgroundResource(android.R.color.transparent)
        binding.tabRegister.setTextColor(requireContext().getColor(R.color.text_secondary))
        viewModel.setMode(LoginMode.LOGIN)
    }

    private fun switchToRegister() {
        binding.tabRegister.setBackgroundResource(R.drawable.bg_tab_selected)
        binding.tabRegister.setTextColor(requireContext().getColor(R.color.white))
        binding.tabLogin.setBackgroundResource(android.R.color.transparent)
        binding.tabLogin.setTextColor(requireContext().getColor(R.color.text_secondary))
        viewModel.setMode(LoginMode.REGISTER)
    }

    private fun setupClickListeners() {
        binding.btnLoginAction.setOnClickListener {
            when {
                viewModel.uiState.value.isLoggedIn -> viewModel.logout()
                viewModel.uiState.value.mode == LoginMode.LOGIN -> performLogin()
                else -> performRegister()
            }
        }

        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            if (validateEmail(email)) {
                viewModel.sendCode(email)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            if (validateEmail(email)) {
                viewModel.requestResetPassword(email)
                Snackbar.make(binding.root, "验证码已发送到邮箱", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.tvSwitchAction.setOnClickListener {
            if (viewModel.uiState.value.mode == LoginMode.LOGIN) {
                switchToRegister()
            } else {
                switchToLogin()
            }
        }

        binding.btnWechatLogin.setOnClickListener {
            Snackbar.make(binding.root, "微信登录暂未开放", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (!validateEmail(email)) return
        if (password.isBlank()) {
            binding.tilPassword.error = "请输入密码"
            return
        }
        binding.tilPassword.error = null

        viewModel.login(email, password)
    }

    private fun performRegister() {
        val email = binding.etEmail.text?.toString().orEmpty()
        val code = binding.etCode.text?.toString().orEmpty()
        val nickname = binding.etNickname.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        if (!validateEmail(email)) return
        if (code.length != 6) {
            binding.tilCode.error = "请输入6位验证码"
            return
        }
        binding.tilCode.error = null
        if (nickname.isBlank()) {
            binding.tilNickname.error = "请输入昵称"
            return
        }
        binding.tilNickname.error = null
        if (password.length < 6) {
            binding.tilPassword.error = "密码至少6位"
            return
        }
        binding.tilPassword.error = null
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "两次密码不一致"
            return
        }
        binding.tilConfirmPassword.error = null

        viewModel.register(email, code, nickname, password)
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isBlank()) {
            binding.tilEmail.error = "请输入邮箱地址"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "请输入有效邮箱地址"
            return false
        }
        binding.tilEmail.error = null
        return true
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

    private fun render(state: LoginUiState) {
        binding.progressBar.isVisible = state.loading

        if (state.isLoggedIn) {
            renderLoggedIn(state)
        } else {
            renderNotLoggedIn(state)
        }

        // 更新按钮状态
        binding.btnLoginAction.isEnabled = !state.loading
        binding.btnSendCode.isEnabled = !state.loading && !state.sendingCode && state.countdown <= 0

        // 验证码倒计时
        binding.btnSendCode.text = when {
            state.sendingCode -> "发送中..."
            state.countdown > 0 -> "${state.countdown}s"
            else -> "获取验证码"
        }

        // 消息提示
        val msg = state.message
        if (!msg.isNullOrBlank() && msg != lastMessage) {
            lastMessage = msg
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun renderLoggedIn(state: LoginUiState) {
        binding.cardTabContainer.isVisible = false
        binding.tilEmail.isVisible = false
        binding.tilPassword.isVisible = false
        binding.tvForgotPassword.isVisible = false
        binding.layoutCode.isVisible = false
        binding.tilNickname.isVisible = false
        binding.tilConfirmPassword.isVisible = false
        binding.cardLoggedInInfo.isVisible = true
        binding.btnLoginAction.text = "退出登录"
        binding.btnLoginAction.isVisible = true
        binding.layoutDivider.isVisible = false
        binding.btnWechatLogin.isVisible = false
        binding.layoutSwitchHint.isVisible = false

        binding.tvLoggedInInfo.text = "${state.nickname}\n${state.email}"
        binding.tvPoints.text = "${state.points} 积分"
    }

    private fun renderNotLoggedIn(state: LoginUiState) {
        binding.cardTabContainer.isVisible = true
        binding.tilEmail.isVisible = true
        binding.cardLoggedInInfo.isVisible = false
        binding.btnLoginAction.isVisible = true
        binding.layoutDivider.isVisible = true
        binding.btnWechatLogin.isVisible = true
        binding.layoutSwitchHint.isVisible = true

        when (state.mode) {
            LoginMode.LOGIN -> {
                binding.tilPassword.isVisible = true
                binding.tvForgotPassword.isVisible = true
                binding.layoutCode.isVisible = false
                binding.tilNickname.isVisible = false
                binding.tilConfirmPassword.isVisible = false
                binding.btnLoginAction.text = "登录"
                binding.tvSwitchHint.text = "还没有账号？"
                binding.tvSwitchAction.text = "去注册"
            }
            LoginMode.REGISTER -> {
                binding.tilPassword.isVisible = true
                binding.tvForgotPassword.isVisible = false
                binding.layoutCode.isVisible = true
                binding.tilNickname.isVisible = true
                binding.tilConfirmPassword.isVisible = true
                binding.btnLoginAction.text = "注册"
                binding.tvSwitchHint.text = "已有账号？"
                binding.tvSwitchAction.text = "去登录"
            }
            LoginMode.RESET_PASSWORD -> {
                binding.tilPassword.isVisible = true
                binding.tvForgotPassword.isVisible = false
                binding.layoutCode.isVisible = true
                binding.tilNickname.isVisible = false
                binding.tilConfirmPassword.isVisible = true
                binding.btnLoginAction.text = "重置密码"
                binding.tvSwitchHint.text = ""
                binding.tvSwitchAction.text = ""
                binding.layoutSwitchHint.isVisible = false
            }
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
