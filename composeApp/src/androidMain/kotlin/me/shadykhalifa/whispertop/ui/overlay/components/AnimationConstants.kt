package me.shadykhalifa.whispertop.ui.overlay.components

/**
 * Constants for animation configurations and visual parameters
 */
object AnimationConstants {
    
    // Animation durations (in milliseconds)
    const val PULSING_ANIMATION_DURATION = 1000
    const val PROCESSING_SPINNER_DURATION = 1200
    const val AUDIO_LEVEL_ANIMATION_DURATION = 150
    const val SUCCESS_CHECKMARK_DURATION = 300
    const val ERROR_PULSE_DURATION = 500
    
    // Scale factors
    const val PULSING_SCALE_MIN = 1f
    const val PULSING_SCALE_MAX = 1.3f
    const val BUTTON_PRESSED_SCALE = 0.95f
    const val BUTTON_NORMAL_SCALE = 1f
    
    // Alpha values
    const val PULSING_ALPHA_MAX = 0.8f
    const val PULSING_ALPHA_MIN = 0.2f
    const val ERROR_ALPHA_MIN = 0.3f
    const val ERROR_ALPHA_MAX = 1f
    const val AUDIO_BAR_ALPHA_MIN = 0.3f
    const val AUDIO_BAR_ALPHA_RANGE = 0.7f
    
    // Size multipliers
    const val PROCESSING_SPINNER_SIZE_FACTOR = 0.8f
    const val AUDIO_VISUALIZATION_SIZE_FACTOR = 0.3f
    const val SUCCESS_CHECKMARK_SIZE_FACTOR = 0.4f
    const val ICON_SIZE_FACTOR = 0.6f
    
    // Audio visualization
    const val AUDIO_BAR_INTENSITY_FACTOR = 0.7f
    const val AUDIO_BAR_HEIGHT_FACTOR = 0.6f
    const val DEFAULT_AUDIO_BAR_COUNT = 8
    
    // Button dimensions (in DP)
    const val BUTTON_SIZE_DP = 56
    const val SNAP_THRESHOLD_DP = 48
    const val EDGE_MARGIN_DP = 16
    
    // Stroke widths (in DP)
    const val DEFAULT_STROKE_WIDTH_DP = 4
    const val SPINNER_STROKE_WIDTH_DP = 3
    const val CHECKMARK_STROKE_WIDTH_DP = 3
    const val THIN_STROKE_WIDTH_DP = 2
    
    // Frame rate limiting
    const val MIN_FRAME_TIME_MS = 16L // 60 FPS
    const val MIN_AUDIO_LEVEL_CHANGE = 0.01f // Minimum change to trigger redraw
}