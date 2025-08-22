package me.shadykhalifa.whispertop.presentation.models

data class TranscriptionDisplayModel(
    val previewText: String,
    val fullText: String,
    val insertionStatus: TextInsertionStatus,
    val errorMessage: String? = null
)

enum class TextInsertionStatus {
    NotStarted,
    InProgress,
    Completed,
    Failed
}

fun String.toDisplayModel(
    insertionStatus: TextInsertionStatus = TextInsertionStatus.NotStarted,
    errorMessage: String? = null
): TranscriptionDisplayModel {
    val preview = if (this.length > 47) {
        "${this.take(47)}..."
    } else {
        this
    }
    
    return TranscriptionDisplayModel(
        previewText = preview,
        fullText = this,
        insertionStatus = insertionStatus,
        errorMessage = errorMessage
    )
}