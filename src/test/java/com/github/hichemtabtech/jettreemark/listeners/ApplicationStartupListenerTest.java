package com.github.hichemtabtech.jettreemark.listeners;

import com.intellij.openapi.wm.IdeFrame;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link ApplicationStartupListener}
 */
public class ApplicationStartupListenerTest {

    @Mock
    private IdeFrame mockIdeFrame;
    
    private ApplicationStartupListener listener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new ApplicationStartupListener();
    }

    @Test
    public void testApplicationActivated() {
        // This method only logs a message, so we just verify it doesn't throw an exception
        listener.applicationActivated(mockIdeFrame);
        
        // If we wanted to verify the logging, we would need to use a framework like SLF4J TestLogger
        // or mock the Logger, but that's beyond the scope of this basic test
    }
}