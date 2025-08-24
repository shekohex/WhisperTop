package me.shadykhalifa.whispertop.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextInsertionUtilsTest {

    @Test
    fun `getSpacingBetweenTexts returns empty string for empty inputs`() {
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("", "world"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello", ""))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("", ""))
    }

    @Test
    fun `getSpacingBetweenTexts returns empty when existing text ends with whitespace`() {
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello ", "world"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello\n", "world"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello\t", "world"))
    }

    @Test
    fun `getSpacingBetweenTexts returns empty when new text starts with whitespace`() {
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello", " world"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello", "\nworld"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello", "\tworld"))
    }

    @Test
    fun `getSpacingBetweenTexts adds space between alphanumeric characters`() {
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("hello", "world"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("test123", "abc"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("word", "123"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("123", "word"))
    }

    @Test
    fun `getSpacingBetweenTexts handles punctuation that doesn't need space`() {
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello(", "world"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("test[", "content"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("quote\"", "text"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("path/", "file"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("email@", "domain"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("price$", "100"))
    }

    @Test
    fun `getSpacingBetweenTexts handles punctuation at start of new text`() {
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("hello", "(world"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("test", "[content]"))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("word", "\"quoted\""))
        assertEquals("", TextInsertionUtils.getSpacingBetweenTexts("file", "/path"))
    }

    @Test
    fun `getSpacingBetweenTexts adds space after punctuation that needs space`() {
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("sentence.", "Next"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("item,", "another"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("clause;", "next"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("label:", "value"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("wow!", "amazing"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("really?", "yes"))
        assertEquals(" ", TextInsertionUtils.getSpacingBetweenTexts("(done)", "next"))
    }

    @Test
    fun `combineTextWithIntelligentSpacing handles empty field`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "",
            newText = "hello world"
        )
        assertEquals("hello world", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing replaces selected text`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "hello selected world",
            newText = "new",
            hasSelection = true,
            selectionStart = 6,
            selectionEnd = 14
        )
        assertEquals("hello new world", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing handles reverse selection`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "hello selected world",
            newText = "new",
            hasSelection = true,
            selectionStart = 14, // end comes first
            selectionEnd = 6     // start comes second
        )
        assertEquals("hello new world", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing appends with proper spacing`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "hello",
            newText = "world"
        )
        assertEquals("hello world", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing appends without extra space when existing has space`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "hello ",
            newText = "world"
        )
        assertEquals("hello world", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing appends without extra space when new text starts with space`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "hello",
            newText = " world"
        )
        assertEquals("hello world", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing handles punctuation correctly`() {
        val result1 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "Hello.",
            newText = "World"
        )
        assertEquals("Hello. World", result1)

        val result2 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "Hello",
            newText = ",world"
        )
        assertEquals("Hello,world", result2)
    }

    @Test
    fun `combineTextWithIntelligentSpacing handles mixed scenarios`() {
        // Sentence with period
        val result1 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "First sentence.",
            newText = "Second sentence."
        )
        assertEquals("First sentence. Second sentence.", result1)

        // List with comma
        val result2 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "apple,",
            newText = "banana"
        )
        assertEquals("apple, banana", result2)

        // Parentheses
        val result3 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "Check this",
            newText = "(important)"
        )
        assertEquals("Check this(important)", result3)

        val result4 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "Done)",
            newText = "Next"
        )
        assertEquals("Done) Next", result4)
    }

    @Test
    fun `combineTextWithIntelligentSpacing handles multiline text`() {
        val result = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "Line one\n",
            newText = "Line two"
        )
        assertEquals("Line one\nLine two", result)
    }

    @Test
    fun `combineTextWithIntelligentSpacing handles special characters`() {
        val result1 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "email@",
            newText = "example.com"
        )
        assertEquals("email@example.com", result1)

        val result2 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "Price $",
            newText = "100"
        )
        assertEquals("Price $100", result2)

        val result3 = TextInsertionUtils.combineTextWithIntelligentSpacing(
            existingText = "file/",
            newText = "path.txt"
        )
        assertEquals("file/path.txt", result3)
    }
}