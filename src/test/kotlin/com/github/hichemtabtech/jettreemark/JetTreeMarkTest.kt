package com.github.hichemtabtech.jettreemark

import org.junit.Assert
import org.junit.Test

/**
 * Simple test class for JetTreeMark plugin.
 */
class JetTreeMarkTest {

    @Test
    fun testBundleExists() {
        // Test that the bundle class exists
        val bundle = JetTreeMarkBundle.INSTANCE
        Assert.assertNotNull("Bundle should not be null", bundle)
    }

    @Test
    fun testMessageKeys() {
        // Test that the bundle contains the expected message keys
        val bundle = JetTreeMarkBundle.INSTANCE
        val message = bundle.getMessage("name")
        Assert.assertNotNull("Message key 'name' should exist", message)
    }
}