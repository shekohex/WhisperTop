package me.shadykhalifa.whispertop.data.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt
import me.shadykhalifa.whispertop.utils.TimeUtils

class AudioQualityManager(
    private val qualityPreset: QualityPreset = QualityPreset.MEDIUM
) {
    private val _currentMetrics = MutableStateFlow(AudioMetrics.empty())
    val currentMetrics: StateFlow<AudioMetrics> = _currentMetrics.asStateFlow()
    
    private val _recordingStatistics = MutableStateFlow<RecordingStatistics?>(null)
    val recordingStatistics: StateFlow<RecordingStatistics?> = _recordingStatistics.asStateFlow()
    
    private val metricsHistory = mutableListOf<AudioMetrics>()
    private val silenceDetector = SilenceDetector()
    private var recordingStartTime: Long = 0
    private var totalSamples: Long = 0
    private var silentSamples: Long = 0
    private var clippingEvents: Int = 0
    private var peakLevelSoFar: Float = 0f
    
    fun startMonitoring() {
        recordingStartTime = TimeUtils.currentTimeMillis()
        metricsHistory.clear()
        totalSamples = 0
        silentSamples = 0
        clippingEvents = 0
        peakLevelSoFar = 0f
        silenceDetector.reset()
    }
    
    fun processAudioBuffer(buffer: ShortArray): AudioMetrics {
        val metrics = AudioMetrics.calculate(buffer)
        _currentMetrics.value = metrics
        
        metricsHistory.add(metrics)
        if (metricsHistory.size > 100) {
            metricsHistory.removeAt(0)
        }
        
        totalSamples += buffer.size
        if (metrics.isSilent) {
            silentSamples += buffer.size
        }
        if (metrics.isClipping) {
            clippingEvents++
        }
        if (metrics.peakLevel > peakLevelSoFar) {
            peakLevelSoFar = metrics.peakLevel
        }
        
        silenceDetector.processSample(metrics.isSilent)
        
        updateStatistics()
        
        return metrics
    }
    
    fun shouldStopRecording(): Boolean {
        val stats = _recordingStatistics.value ?: return false
        
        // Stop if approaching file size limit (95% threshold)
        if (stats.estimatedFinalSize >= RecordingConstraints.MAX_FILE_SIZE_BYTES * 0.95) {
            return true
        }
        
        // Stop if too much silence (optional, can be configured)
        if (qualityPreset.silenceTrimming && 
            stats.duration > 10000 && 
            stats.silencePercentage > 80) {
            return true
        }
        
        return false
    }
    
    fun canContinueRecording(additionalDurationMs: Long): Boolean {
        val stats = _recordingStatistics.value ?: return true
        val additionalBytes = (additionalDurationMs * RecordingConstraints.BYTES_PER_SECOND) / 1000
        val projectedSize = stats.fileSize + additionalBytes
        return projectedSize < RecordingConstraints.MAX_FILE_SIZE_BYTES
    }
    
    fun getRemainingRecordingTime(): Long {
        val stats = _recordingStatistics.value ?: return RecordingConstraints.MAX_RECORDING_DURATION_MS
        return stats.remainingTime
    }
    
    fun getQualityReport(): QualityReport {
        val avgMetrics = if (metricsHistory.isNotEmpty()) {
            AudioMetrics(
                rmsLevel = metricsHistory.map { it.rmsLevel }.average().toFloat(),
                peakLevel = peakLevelSoFar,
                dbLevel = metricsHistory.map { it.dbLevel }.average().toFloat(),
                isClipping = clippingEvents > 0,
                isSilent = false,
                noiseFloor = metricsHistory.map { it.noiseFloor }.average().toFloat(),
                signalToNoise = metricsHistory.map { it.signalToNoise }.average().toFloat(),
                qualityScore = metricsHistory.map { it.qualityScore }.average().roundToInt()
            )
        } else {
            AudioMetrics.empty()
        }
        
        return QualityReport(
            overallQuality = avgMetrics.qualityScore,
            audioMetrics = avgMetrics,
            recordingStatistics = _recordingStatistics.value,
            issues = detectIssues(),
            recommendations = generateRecommendations()
        )
    }
    
    private fun updateStatistics() {
        val currentTime = TimeUtils.currentTimeMillis()
        val duration = currentTime - recordingStartTime
        val fileSize = (totalSamples * 2).toLong() // 2 bytes per sample (16-bit)
        
        // Only calculate bytes per ms if we have meaningful duration (> 100ms)
        val bytesPerMs = if (duration > 100) {
            fileSize.toFloat() / duration
        } else {
            // For very short durations, use actual file size as a conservative estimate
            // Don't assume max rate until we have actual data
            0f
        }
        
        val estimatedFinalSize = if (bytesPerMs > 0) {
            (bytesPerMs * RecordingConstraints.MAX_RECORDING_DURATION_MS).toLong()
        } else {
            // If we don't have enough data, just use current file size
            fileSize
        }
        
        val remainingBytes = RecordingConstraints.MAX_FILE_SIZE_BYTES - fileSize
        val remainingTime = if (bytesPerMs > 0) {
            (remainingBytes / bytesPerMs).toLong()
        } else {
            RecordingConstraints.MAX_RECORDING_DURATION_MS
        }
        
        val silencePercentage = if (totalSamples > 0) {
            (silentSamples * 100f) / totalSamples
        } else 0f
        
        val averageLevel = if (metricsHistory.isNotEmpty()) {
            metricsHistory.map { it.rmsLevel }.average().toFloat()
        } else 0f
        
        val overallQuality = if (metricsHistory.isNotEmpty()) {
            metricsHistory.map { it.qualityScore }.average().roundToInt()
        } else 50
        
        _recordingStatistics.value = RecordingStatistics(
            duration = duration,
            fileSize = fileSize,
            estimatedFinalSize = estimatedFinalSize.coerceAtMost(RecordingConstraints.MAX_FILE_SIZE_BYTES),
            remainingTime = remainingTime.coerceAtLeast(0),
            averageLevel = averageLevel,
            peakLevel = peakLevelSoFar,
            silencePercentage = silencePercentage,
            clippingOccurrences = clippingEvents,
            overallQuality = overallQuality
        )
    }
    
    private fun detectIssues(): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val stats = _recordingStatistics.value
        
        if (stats != null) {
            if (stats.clippingOccurrences > 0) {
                issues.add(QualityIssue.CLIPPING)
            }
            if (stats.averageLevel < 0.05f) {
                issues.add(QualityIssue.TOO_QUIET)
            }
            if (stats.silencePercentage > 50) {
                issues.add(QualityIssue.TOO_MUCH_SILENCE)
            }
            if (stats.overallQuality < 30) {
                issues.add(QualityIssue.POOR_QUALITY)
            }
        }
        
        val currentMetrics = _currentMetrics.value
        if (currentMetrics.noiseFloor > -30) {
            issues.add(QualityIssue.HIGH_NOISE)
        }
        
        return issues
    }
    
    private fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val issues = detectIssues()
        
        if (QualityIssue.CLIPPING in issues) {
            recommendations.add("Reduce microphone gain or move further from the microphone")
        }
        if (QualityIssue.TOO_QUIET in issues) {
            recommendations.add("Increase microphone gain or speak closer to the microphone")
        }
        if (QualityIssue.HIGH_NOISE in issues) {
            recommendations.add("Record in a quieter environment or use a better microphone")
        }
        if (QualityIssue.TOO_MUCH_SILENCE in issues) {
            recommendations.add("Start speaking when ready, silence will be automatically trimmed")
        }
        
        return recommendations
    }
}

enum class QualityIssue {
    CLIPPING,
    TOO_QUIET,
    TOO_MUCH_SILENCE,
    HIGH_NOISE,
    POOR_QUALITY
}

data class QualityReport(
    val overallQuality: Int,
    val audioMetrics: AudioMetrics,
    val recordingStatistics: RecordingStatistics?,
    val issues: List<QualityIssue>,
    val recommendations: List<String>
)