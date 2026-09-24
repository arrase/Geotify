package dev.arrase.geotify.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTextTest {

    @Test
    fun dynamicString_holdsValueAndImplementsUiText() {
        val text: UiText = UiText.DynamicString("Sample message")

        assertTrue(text is UiText.DynamicString)
        assertEquals("Sample message", (text as UiText.DynamicString).value)
    }

    @Test
    fun dynamicString_equalityAndHashCode() {
        val first = UiText.DynamicString("test")
        val second = UiText.DynamicString("test")
        val different = UiText.DynamicString("other")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
    }

    @Test
    fun dynamicString_copy() {
        val original = UiText.DynamicString("original")
        val copied = original.copy(value = "modified")

        assertEquals("modified", copied.value)
    }

    @Test
    fun stringResource_holdsResIdAndImplementsUiText() {
        val text: UiText = UiText.StringResource(1001)

        assertTrue(text is UiText.StringResource)
        assertEquals(1001, (text as UiText.StringResource).resId)
    }

    @Test
    fun stringResource_equalityAndHashCode() {
        val first = UiText.StringResource(100)
        val second = UiText.StringResource(100)
        val different = UiText.StringResource(200)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, different)
    }

    @Test
    fun stringResource_copy() {
        val original = UiText.StringResource(100)
        val copied = original.copy(resId = 200)

        assertEquals(200, copied.resId)
    }

    @Test
    fun uiText_patternMatchingExhaustive() {
        val items: List<UiText> = listOf(
            UiText.DynamicString("hello"),
            UiText.StringResource(42)
        )

        val results = items.map { item ->
            when (item) {
                is UiText.DynamicString -> item.value
                is UiText.StringResource -> item.resId.toString()
            }
        }

        assertEquals(listOf("hello", "42"), results)
    }
}
