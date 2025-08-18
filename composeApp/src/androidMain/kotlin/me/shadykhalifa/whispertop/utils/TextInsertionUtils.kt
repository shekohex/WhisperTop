package me.shadykhalifa.whispertop.utils

object TextInsertionUtils {
    
    /**
     * Combines existing text with new text, adding intelligent spacing when needed.
     * 
     * @param existingText The current text in the field
     * @param newText The new text to append
     * @param hasSelection Whether there's text currently selected
     * @param selectionStart Start position of selection (if any)
     * @param selectionEnd End position of selection (if any)
     * @return The final combined text with proper spacing
     */
    fun combineTextWithIntelligentSpacing(
        existingText: String,
        newText: String,
        hasSelection: Boolean = false,
        selectionStart: Int = 0,
        selectionEnd: Int = 0
    ): String {
        return if (hasSelection || existingText.isEmpty()) {
            // Replace selected text or insert into empty field
            if (hasSelection) {
                val start = minOf(selectionStart, selectionEnd)
                val end = maxOf(selectionStart, selectionEnd)
                existingText.substring(0, start) + newText + existingText.substring(end)
            } else {
                newText
            }
        } else {
            // Append to existing text with smart spacing
            existingText + getSpacingBetweenTexts(existingText, newText) + newText
        }
    }
    
    /**
     * Determines what spacing (if any) should be added between existing and new text.
     * 
     * @param existingText The current text that new text will be appended to
     * @param newText The new text to append
     * @return A string representing the spacing to add ("" for no space, " " for space)
     */
    fun getSpacingBetweenTexts(existingText: String, newText: String): String {
        if (existingText.isEmpty() || newText.isEmpty()) {
            return ""
        }
        
        // Don't add space if existing text already ends with whitespace
        if (existingText.endsWith(' ') || 
            existingText.endsWith('\n') || 
            existingText.endsWith('\t')) {
            return ""
        }
        
        // Don't add space if new text starts with whitespace
        if (newText.startsWith(' ') || 
            newText.startsWith('\n') || 
            newText.startsWith('\t')) {
            return ""
        }
        
        // Don't add space if existing text ends with punctuation that doesn't need space
        val lastChar = existingText.last()
        if (isPunctuationThatDoesntNeedSpace(lastChar)) {
            return ""
        }
        
        // Don't add space if new text starts with punctuation that doesn't need space
        val firstChar = newText.first()
        if (isPunctuationThatDoesntNeedSpace(firstChar)) {
            return ""
        }
        
        // Add space if both texts have alphanumeric characters at the boundary
        if (lastChar.isLetterOrDigit() && firstChar.isLetterOrDigit()) {
            return " "
        }
        
        // Add space if existing text ends with certain punctuation that needs space
        if (isPunctuationThatNeedsSpace(lastChar) && firstChar.isLetterOrDigit()) {
            return " "
        }
        
        return ""
    }
    
    /**
     * Checks if a character is punctuation that typically doesn't need a space after it
     * when followed by other text.
     */
    private fun isPunctuationThatDoesntNeedSpace(char: Char): Boolean {
        return char in setOf('(', '[', '{', '"', '\'', '`', '/', '\\', '@', '#', '$')
    }
    
    /**
     * Checks if a character is punctuation that typically needs a space after it
     * when followed by other text.
     */
    private fun isPunctuationThatNeedsSpace(char: Char): Boolean {
        return char in setOf('.', ',', ';', ':', '!', '?', ')', ']', '}')
    }
}