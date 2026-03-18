package com.yourname.videoeditor.feature.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.yourname.videoeditor.feature.editor.databinding.FragmentExportSettingsBinding
import com.yourname.videoeditor.library.core.compatibility.DeviceCompatibilityManager
import com.yourname.videoeditor.library.rendering.export.ExportPreset
import com.yourname.videoeditor.library.rendering.export.ExportQualityConfig
import com.yourname.videoeditor.library.rendering.export.ExportQualityManager
import com.yourname.videoeditor.library.rendering.export.VideoCodec
import android.util.Size

class ExportSettingsFragment : Fragment() {

    private var _binding: FragmentExportSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExportSettingsViewModel by viewModels {
        ExportSettingsViewModelFactory(requireContext())
    }

    private var videoDurationSec: Int = 0
    private var sourceResolution: Size = Size(1920, 1080)
    private var sourceBitrate: Int = 20_000_000

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            videoDurationSec = it.getInt("duration", 0)
            sourceResolution = it.getSerializable("resolution") as? Size ?: Size(1920, 1080)
            sourceBitrate = it.getInt("bitrate", 20_000_000)
        }

        setupToolbar()
        setupTabs()
        setupObservers()
        setupButtons()

        viewModel.initialize(sourceResolution, sourceBitrate, videoDurationSec)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_advanced -> {
                    toggleAdvancedSettings()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.selectPreset(ExportPreset.SOCIAL_MEDIA)
                    1 -> viewModel.selectPreset(ExportPreset.YOUTUBE)
                    2 -> viewModel.selectPreset(ExportPreset.ARCHIVE)
                    3 -> viewModel.selectPreset(ExportPreset.CUSTOM)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        viewModel.exportConfig.observe(viewLifecycleOwner) { config ->
            updateUI(config)
        }

        viewModel.estimatedSize.observe(viewLifecycleOwner) { size ->
            binding.tvEstimatedSize.text = "Estimated size: ${size} MB"
        }

        viewModel.estimatedTime.observe(viewLifecycleOwner) { time ->
            binding.tvEstimatedTime.text = "Estimated time: ${time}"
        }
    }

    private fun updateUI(config: ExportQualityConfig) {
        // Resolution
        binding.tvResolution.text = "${config.resolution.width}x${config.resolution.height}"
        
        // Codec
        binding.tvCodec.text = config.videoCodec.name
        
        // Bitrate
        binding.tvBitrate.text = "${config.videoBitrate / 1_000_000} Mbps"
        
        // Frame Rate
        binding.tvFramerate.text = "${config.frameRate} fps"
        
        // Quality preset
        binding.sliderQuality.value = when (config.qualityPreset) {
            "FAST" -> 0f
            "BALANCED" -> 1f
            "QUALITY" -> 2f
            else -> 1f
        }
        
        // Hardware encoding
        binding.switchHardware.isChecked = config.enableHardwareEncoding
        
        // Two-pass encoding
        binding.switchTwoPass.isChecked = config.enableTwoPass
        binding.switchTwoPass.isEnabled = videoDurationSec > 60
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnExport.setOnClickListener {
            // Start export with current config
            val result = Bundle().apply {
                putSerializable("export_config", viewModel.exportConfig.value)
            }
            parentFragmentManager.setFragmentResult("export_start", result)
            parentFragmentManager.popBackStack()
        }

        binding.btnShare.setOnClickListener {
            // Share directly after export
            viewModel.shareAfterExport = true
            binding.btnExport.performClick()
        }

        binding.sliderQuality.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setQualityPreset(when (value.toInt()) {
                    0 -> "FAST"
                    1 -> "BALANCED"
                    2 -> "QUALITY"
                    else -> "BALANCED"
                })
            }
        }

        binding.switchHardware.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setHardwareEncoding(isChecked)
        }

        binding.switchTwoPass.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTwoPass(isChecked)
        }

        binding.btnResolution.setOnClickListener {
            showResolutionDialog()
        }

        binding.btnCodec.setOnClickListener {
            showCodecDialog()
        }
    }

    private fun showResolutionDialog() {
        val resolutions = arrayOf("480p (854x480)", "720p (1280x720)", "1080p (1920x1080)", "4K (3840x2160)")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Resolution")
            .setItems(resolutions) { _, which ->
                viewModel.setResolution(when (which) {
                    0 -> Size(854, 480)
                    1 -> Size(1280, 720)
                    2 -> Size(1920, 1080)
                    3 -> Size(3840, 2160)
                    else -> Size(1920, 1080)
                })
            }
            .show()
    }

    private fun showCodecDialog() {
        val codecs = VideoCodec.values().map { it.name }
        AlertDialog.Builder(requireContext())
            .setTitle("Select Codec")
            .setItems(codecs.toTypedArray()) { _, which ->
                viewModel.setCodec(VideoCodec.values()[which])
            }
            .show()
    }

    private fun toggleAdvancedSettings() {
        binding.advancedSettings.visibility = if (binding.advancedSettings.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ExportSettingsViewModel(
    private val context: Context,
    private val deviceManager: DeviceCompatibilityManager,
    private val qualityManager: ExportQualityManager
) : ViewModel() {

    private val _exportConfig = MutableLiveData<ExportQualityConfig>()
    val exportConfig: LiveData<ExportQualityConfig> = _exportConfig

    private val _estimatedSize = MutableLiveData<Long>()
    val estimatedSize: LiveData<Long> = _estimatedSize

    private val _estimatedTime = MutableLiveData<String>()
    val estimatedTime: LiveData<String> = _estimatedTime

    var shareAfterExport: Boolean = false

    fun initialize(sourceResolution: Size, sourceBitrate: Int, durationSec: Int) {
        qualityManager.addListener(object : ExportQualityManager.ExportQualityListener {
            override fun onQualityPresetApplied(preset: ExportPreset) {}

            override fun onEncodingOptimized(settings: Map<String, Any>) {}

            override fun onFileSizeEstimated(sizeMb: Long, durationSec: Int) {
                _estimatedSize.postValue(sizeMb)
                _estimatedTime.postValue(estimateExportTime(sizeMb, durationSec))
            }
        })

        val config = qualityManager.getRecommendedSettings(sourceResolution, sourceBitrate, durationSec)
        _exportConfig.value = config
    }

    fun selectPreset(preset: ExportPreset) {
        qualityManager.applyPreset(preset)
        _exportConfig.value = qualityManager.getPresetConfig(preset)
        updateEstimates()
    }

    fun setResolution(size: Size) {
        _exportConfig.value?.let { config ->
            config.resolution = size
            _exportConfig.value = config
            updateEstimates()
        }
    }

    fun setCodec(codec: VideoCodec) {
        _exportConfig.value?.let { config ->
            config.videoCodec = codec
            _exportConfig.value = config
            updateEstimates()
        }
    }

    fun setQualityPreset(preset: String) {
        _exportConfig.value?.let { config ->
            config.qualityPreset = preset
            _exportConfig.value = config
            updateEstimates()
        }
    }

    fun setHardwareEncoding(enabled: Boolean) {
        _exportConfig.value?.let { config ->
            config.enableHardwareEncoding = enabled
            _exportConfig.value = config
            updateEstimates()
        }
    }

    fun setTwoPass(enabled: Boolean) {
        _exportConfig.value?.let { config ->
            config.enableTwoPass = enabled
            _exportConfig.value = config
            updateEstimates()
        }
    }

    private fun updateEstimates() {
        _exportConfig.value?.let { config ->
            val duration = arguments?.getInt("duration") ?: 0
            val size = qualityManager.estimateFileSize(duration)
            _estimatedSize.value = size
            _estimatedTime.value = estimateExportTime(size, duration)
        }
    }

    private fun estimateExportSize(durationSec: Int): Long {
        return qualityManager.estimateFileSize(durationSec)
    }

    private fun estimateExportTime(sizeMb: Long, durationSec: Int): String {
        // Rough estimate based on device performance and settings
        val speedFactor = when (deviceManager.determinePerformanceProfile()) {
            DeviceCompatibilityManager.PerformanceProfile.ULTRA -> 5
            DeviceCompatibilityManager.PerformanceProfile.HIGH_END -> 3
            DeviceCompatibilityManager.PerformanceProfile.MID_RANGE -> 1.5
            DeviceCompatibilityManager.PerformanceProfile.LOW_END -> 0.8
        }

        val estimatedSeconds = (durationSec / speedFactor).toInt()
        return when {
            estimatedSeconds < 60 -> "${estimatedSeconds} seconds"
            estimatedSeconds < 3600 -> "${estimatedSeconds / 60} minutes"
            else -> "${estimatedSeconds / 3600} hours"
        }
    }
}

class ExportSettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val deviceManager = DeviceCompatibilityManager(context)
        val qualityManager = ExportQualityManager(deviceManager)
        return ExportSettingsViewModel(context, deviceManager, qualityManager) as T
    }
}
