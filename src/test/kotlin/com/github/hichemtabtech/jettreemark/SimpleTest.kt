package com.github.hichemtabtech.jettreemark

import org.junit.Assert
import org.junit.Test

/**
 * Simple test class that doesn't depend on IntelliJ platform.
 */
class SimpleTest {

    @Test
    fun testSimpleAddition() {
        // A very simple test that doesn't depend on any platform classes
        val result = 2 + 2
        Assert.assertEquals("2 + 2 should equal 4", 4, result)
    }

    @Test
    fun testStringConcatenation() {
        // Another simple test
        val result = "Hello, " + "World!"
        Assert.assertEquals("String concatenation should work", "Hello, World!", result)
    }
}