package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TreeViewPanel}
 */
public class TreeViewPanelTest {

    private TreeViewPanel treeViewPanel;
    private CheckboxTreeNode testRootNode;

    @Before
    public void setUp() {
        // Initialize the panel before each test
        treeViewPanel = new TreeViewPanel();

        // Create a test tree structure
        testRootNode = new CheckboxTreeNode("TestRoot", true);
        CheckboxTreeNode folderNode = new CheckboxTreeNode("TestFolder", true);
        CheckboxTreeNode fileNode1 = new CheckboxTreeNode("TestFile1", false);
        CheckboxTreeNode fileNode2 = new CheckboxTreeNode("TestFile2", false);

        testRootNode.add(folderNode);
        folderNode.add(fileNode1);
        folderNode.add(fileNode2);
    }

    @Test
    public void testGetContent() {
        // Get the content panel
        JPanel content = treeViewPanel.getContent();

        // Verify it's not null
        assertNotNull("Content panel should not be null", content);

        // Verify it has a BorderLayout
        assertTrue("Content panel should have BorderLayout", content.getLayout() instanceof BorderLayout);
    }

    @Test
    public void testAddFolderToTreeView() {
        // Create a mock VirtualFile
        VirtualFile mockFolder = Mockito.mock(VirtualFile.class);
        when(mockFolder.getName()).thenReturn("TestFolder");
        when(mockFolder.isDirectory()).thenReturn(true);
        when(mockFolder.getChildren()).thenReturn(new VirtualFile[0]);

        // Add the folder to the tree view
        treeViewPanel.addFolderToTreeView(mockFolder);

        // Since the actual tree building happens in a SwingWorker, we can't easily verify
        // the result directly. This test mainly ensures the method doesn't throw exceptions.

        // Verify that the mock was accessed
        verify(mockFolder, atLeastOnce()).getName();
    }

    @Test
    public void testCreateWelcomePanel() {
        // Get the content panel which should contain the welcome panel
        JPanel content = treeViewPanel.getContent();

        // The welcome panel should be the first tab in the tabbed pane
        // This is a bit tricky to test without exposing internal components

        // We can verify that the content panel is not empty
        assertTrue("Content panel should have components", content.getComponentCount() > 0);
    }

    @Test
    public void testGenerateTreeText() throws Exception {
        // Use reflection to access the private method
        Method generateTreeTextMethod = TreeViewPanel.class.getDeclaredMethod(
                "generateTreeText", DefaultMutableTreeNode.class, String.class, boolean.class);
        generateTreeTextMethod.setAccessible(true);

        // Call the method with our test root node
        String treeText = (String) generateTreeTextMethod.invoke(treeViewPanel, testRootNode, "", true);

        // Verify the text was generated
        assertNotNull("Tree text should not be null", treeText);
        assertTrue("Tree text should contain the root node name", treeText.contains("TestRoot"));
    }

    @Test
    public void testCheckboxTreeNodeFunctionality() {
        // Test that the root node is checked by default
        assertEquals("Root node should be checked by default", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());

        // Test checking/unchecking operations
        testRootNode.uncheckAll(true);
        assertEquals("Root node should be unchecked after uncheckAll", 
                CheckboxTreeNode.UNCHECKED, testRootNode.getCheckState());

        testRootNode.checkAll();
        assertEquals("Root node should be checked after checkAll", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());

        // Get the folder node
        CheckboxTreeNode folderNode = (CheckboxTreeNode) testRootNode.getChildAt(0);

        // Test check only folders
        testRootNode.uncheckAll(true);
        testRootNode.checkOnlyFolders();
        assertEquals("Root node should be checked after checkOnlyFolders", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());
        assertEquals("Folder node should be checked after checkOnlyFolders", 
                CheckboxTreeNode.CHECKED, folderNode.getCheckState());

        // Get a file node
        CheckboxTreeNode fileNode = (CheckboxTreeNode) folderNode.getChildAt(0);
        assertEquals("File node should be unchecked after checkOnlyFolders", 
                CheckboxTreeNode.UNCHECKED, fileNode.getCheckState());

        // Test check only files
        testRootNode.uncheckAll(true);
        testRootNode.checkOnlyFiles();
        assertEquals("Root node should be checked after checkOnlyFiles", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());
        assertEquals("Folder node should be checked after checkOnlyFiles", 
                CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node should be checked after checkOnlyFiles", 
                CheckboxTreeNode.CHECKED, fileNode.getCheckState());
    }
}
