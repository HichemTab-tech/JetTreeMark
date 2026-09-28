package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TreeViewPanelTest {
    private TreeViewPanel panel;

    @Before
    public void setUp() {
        panel = new TreeViewPanel();
    }

    @After
    public void tearDown() {
        panel.dispose();
    }

    @Test
    public void contentUsesBorderLayout() {
        JPanel content = panel.getContent();
        assertNotNull(content);
        assertTrue(content.getLayout() instanceof BorderLayout);
    }

    @Test
    public void emptyFolderIsBuiltAsynchronously() throws Exception {
        VirtualFile folder = mock(VirtualFile.class);
        when(folder.getName()).thenReturn("TestFolder");
        when(folder.findChild(".gitignore")).thenReturn(null);
        when(folder.getChildren()).thenReturn(VirtualFile.EMPTY_ARRAY);

        SwingUtilities.invokeAndWait(() -> panel.addFolderToTreeView(folder));

        verify(folder, timeout(2_000)).getChildren();
    }
}
