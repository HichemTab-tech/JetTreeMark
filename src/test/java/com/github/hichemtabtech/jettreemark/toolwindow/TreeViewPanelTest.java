package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TreeViewPanel}
 */
public class TreeViewPanelTest {

    private TreeViewPanel treeViewPanel;

    @Before
    public void setUp() {
        // Initialize the panel before each test
        treeViewPanel = new TreeViewPanel();
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
}