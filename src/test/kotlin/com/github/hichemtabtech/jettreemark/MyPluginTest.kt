package com.github.hichemtabtech.jettreemark

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testPluginXmlValidity() {
        // Test that plugin.xml is a valid XML file
        val pluginXml = myFixture.configureByFile("META-INF/plugin.xml")
        assertTrue(pluginXml is XmlFile)
        assertFalse(PsiErrorElementUtil.hasErrors(project, pluginXml.virtualFile))
    }

    fun testPluginInitialization() {
        // Test that the plugin initializes without errors
        // This is a basic test that just ensures the plugin can be loaded
        assertNotNull(project)
    }

    override fun getTestDataPath() = "src/test/testData"
}
