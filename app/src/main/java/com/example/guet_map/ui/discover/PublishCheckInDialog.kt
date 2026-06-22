package com.example.guet_map.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.guet_map.databinding.DialogPublishCheckinBinding
import com.example.guet_map.repository.LocationRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PublishCheckInDialog : BottomSheetDialogFragment() {

    private var _binding: DialogPublishCheckinBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var locationRepository: LocationRepository

    private var onPublished: ((String, String, String, List<String>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPublishCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLocationDropdown()
        setupListeners()
    }

    private fun setupLocationDropdown() {
        CoroutineScope(Dispatchers.Main).launch {
            val locations = locationRepository.observeCachedLocations().first()
            val names = locations.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.actvLocation.setAdapter(adapter)
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnPublish.setOnClickListener {
            val location = binding.actvLocation.text.toString().trim()
            val content = binding.etContent.text.toString().trim()
            val topicsRaw = binding.etTopics.text.toString().trim()

            when {
                location.isBlank() -> {
                    binding.tilLocation.error = "请选择位置"
                    return@setOnClickListener
                }
                content.isBlank() -> {
                    binding.tilContent.error = "请输入内容"
                    return@setOnClickListener
                }
            }

            binding.tilLocation.error = null
            binding.tilContent.error = null

            val topics = topicsRaw
                .split(Regex("[,，]"))
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }

            onPublished?.invoke(location, location, content, topics)
            Toast.makeText(context, "发布成功", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnPublishedListener(listener: (String, String, String, List<String>) -> Unit) {
        onPublished = listener
    }

    companion object {
        fun newInstance(): PublishCheckInDialog {
            return PublishCheckInDialog()
        }
    }
}
