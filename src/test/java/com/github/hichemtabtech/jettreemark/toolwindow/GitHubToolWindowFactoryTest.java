package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import javax.swing.*;
import java.awt.*;

public class GitHubToolWindowFactoryTest extends BasePlatformTestCase {

    public void testToolWindowContent() {
        // Create an instance of the tool window
        GitHubToolWindowFactory.TreeViewPanel treeViewPanel = new GitHubToolWindowFactory.TreeViewPanel();

        // Get the content panel
        JPanel content = treeViewPanel.getContent();

        // Verify the panel has components
        assertTrue("Content panel should have components", content.getComponentCount() > 0);

        // Check for welcome message
        boolean foundWelcomeLabel = false;
        boolean foundGitHubLink = false;

        for (Component component : content.getComponents()) {
            if (component instanceof JBLabel) {
                JBLabel label = (JBLabel) component;
                String text = label.getText();

                if (text.contains("Welcome to JetTreeMark")) {
                    foundWelcomeLabel = true;
                } else if (text.contains("Visit HichemTab-tech on GitHub")) {
                    foundGitHubLink = true;
                }
            }
        }

        assertTrue("Welcome message should be present", foundWelcomeLabel);
        assertTrue("GitHub link should be present", foundGitHubLink);
    }
}
