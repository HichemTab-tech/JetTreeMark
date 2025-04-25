package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.ContentManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TreeViewToolWindowFactory}
 */
public class TreeViewToolWindowFactoryTest {

    @Mock
    private Project mockProject;

    @Mock
    private ToolWindow mockToolWindow;

    @Mock
    private ContentManager mockContentManager;

    private TreeViewToolWindowFactory factory;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup common mock behavior
        when(mockToolWindow.getContentManager()).thenReturn(mockContentManager);

        // Create the factory
        factory = new TreeViewToolWindowFactory();
    }

    @Test
    public void testShouldBeAvailable() {
        // Call the method to test
        boolean result = factory.shouldBeAvailable(mockProject);

        // Verify the result
        assertTrue("shouldBeAvailable should return true", result);
    }
}
