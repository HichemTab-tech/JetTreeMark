package com.github.hichemtabtech.jettreemark.toolwindow;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CheckboxTreeNode}
 */
public class CheckboxTreeNodeTest {

    private CheckboxTreeNode rootNode;
    private CheckboxTreeNode folderNode;
    private CheckboxTreeNode fileNode1;
    private CheckboxTreeNode fileNode2;

    @Before
    public void setUp() {
        // Create a simple tree structure for testing
        rootNode = new CheckboxTreeNode("Root", true);
        folderNode = new CheckboxTreeNode("Folder", true);
        fileNode1 = new CheckboxTreeNode("File1", false);
        fileNode2 = new CheckboxTreeNode("File2", false);

        rootNode.add(folderNode);
        folderNode.add(fileNode1);
        folderNode.add(fileNode2);
    }

    @Test
    public void testInitialState() {
        // By default, nodes should be checked
        assertEquals("Root node should be checked by default", CheckboxTreeNode.CHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be checked by default", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node should be checked by default", CheckboxTreeNode.CHECKED, fileNode1.getCheckState());
    }

    @Test
    public void testSetCheckState() {
        // Test setting check state without propagation
        fileNode1.setCheckState(CheckboxTreeNode.UNCHECKED, false, false);
        assertEquals("File node should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
        assertEquals("Folder node should still be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        
        // Test setting check state with propagation to children
        folderNode.setCheckState(CheckboxTreeNode.UNCHECKED, true, false);
        assertEquals("Folder node should be unchecked", CheckboxTreeNode.UNCHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
        assertEquals("File node 2 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode2.getCheckState());
        
        // Test setting check state with propagation to parent
        fileNode1.setCheckState(CheckboxTreeNode.CHECKED, false, true);
        assertEquals("File node 1 should be checked", CheckboxTreeNode.CHECKED, fileNode1.getCheckState());
        assertEquals("Folder node should be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
    }

    @Test
    public void testCheckOnlyFolders() {
        // First uncheck all nodes
        rootNode.uncheckAll(true);
        assertEquals("Root node should be unchecked", CheckboxTreeNode.UNCHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be unchecked", CheckboxTreeNode.UNCHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
        
        // Test checkOnlyFolders
        rootNode.checkOnlyFolders();
        assertEquals("Root node should be checked", CheckboxTreeNode.CHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should still be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
    }

    @Test
    public void testCheckOnlyFiles() {
        // First uncheck all nodes
        rootNode.uncheckAll(true);
        assertEquals("Root node should be unchecked", CheckboxTreeNode.UNCHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be unchecked", CheckboxTreeNode.UNCHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
        
        // Test checkOnlyFiles
        rootNode.checkOnlyFiles();
        assertEquals("Root node should be checked", CheckboxTreeNode.CHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be checked", CheckboxTreeNode.CHECKED, fileNode1.getCheckState());
    }

    @Test
    public void testCheckAll() {
        // First uncheck all nodes
        rootNode.uncheckAll(true);
        assertEquals("Root node should be unchecked", CheckboxTreeNode.UNCHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be unchecked", CheckboxTreeNode.UNCHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
        
        // Test checkAll
        rootNode.checkAll();
        assertEquals("Root node should be checked", CheckboxTreeNode.CHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be checked", CheckboxTreeNode.CHECKED, fileNode1.getCheckState());
    }

    @Test
    public void testUncheckAll() {
        // Ensure all nodes are checked
        rootNode.checkAll();
        assertEquals("Root node should be checked", CheckboxTreeNode.CHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be checked", CheckboxTreeNode.CHECKED, fileNode1.getCheckState());
        
        // Test uncheckAll
        rootNode.uncheckAll(true);
        assertEquals("Root node should be unchecked", CheckboxTreeNode.UNCHECKED, rootNode.getCheckState());
        assertEquals("Folder node should be unchecked", CheckboxTreeNode.UNCHECKED, folderNode.getCheckState());
        assertEquals("File node 1 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
    }

    @Test
    public void testUpdateParentCheckState() {
        // First uncheck all nodes
        rootNode.uncheckAll(true);
        
        // Check one file node
        fileNode1.setCheckState(CheckboxTreeNode.CHECKED, false, true);
        assertEquals("File node 1 should be checked", CheckboxTreeNode.CHECKED, fileNode1.getCheckState());
        assertEquals("Folder node should be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("Root node should be checked", CheckboxTreeNode.CHECKED, rootNode.getCheckState());
        
        // Uncheck the file node
        fileNode1.setCheckState(CheckboxTreeNode.UNCHECKED, false, true);
        assertEquals("File node 1 should be unchecked", CheckboxTreeNode.UNCHECKED, fileNode1.getCheckState());
        assertEquals("Folder node should still be checked", CheckboxTreeNode.CHECKED, folderNode.getCheckState());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCheckState() {
        // Test with an invalid check state
        rootNode.setCheckState(999);
    }
}