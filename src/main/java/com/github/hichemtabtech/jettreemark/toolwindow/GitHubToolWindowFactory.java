package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool window factory for displaying project information and GitHub repository link.
 * Shows a tree view of folders and files when selecting a folder in the project tool window.
 */
public class GitHubToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, TreeViewPanel> projectPanels = new HashMap<>();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TreeViewPanel treeViewPanel = new TreeViewPanel();
        projectPanels.put(project, treeViewPanel);

        Content content = ContentFactory.getInstance().createContent(treeViewPanel.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * Adds a folder to the tree view.
     *
     * @param project    the project
     * @param toolWindow the tool window
     * @param folder     the folder to add
     */
    public static void addFolderToTreeView(@NotNull Project project, @NotNull ToolWindow toolWindow, @NotNull VirtualFile folder) {
        TreeViewPanel panel = projectPanels.get(project);
        if (panel != null) {
            panel.addFolderToTreeView(folder);
        }
    }

    /**
     * Panel that displays project information, GitHub repository link, and tree views.
     */
    static class TreeViewPanel {
        private static final String GITHUB_URL = "https://github.com/HichemTab-tech";
        private static final String MESSAGE = "Welcome to JetTreeMark!";
        private static final String GITHUB_LINK_TEXT = "Visit HichemTab-tech on GitHub";

        private final JBTabbedPane tabbedPane;
        private final JPanel welcomePanel;
        private int tabCounter = 1;

        TreeViewPanel() {
            tabbedPane = new JBTabbedPane();
            welcomePanel = createWelcomePanel();
            tabbedPane.addTab("Welcome", welcomePanel);
        }

        JPanel getContent() {
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(tabbedPane, BorderLayout.CENTER);
            return mainPanel;
        }

        /**
         * Creates the welcome panel with GitHub link.
         *
         * @return the welcome panel
         */
        private JPanel createWelcomePanel() {
            JBPanel<JBPanel<?>> panel = new JBPanel<>();
            panel.setLayout(new BorderLayout());

            // Welcome message
            JBLabel welcomeLabel = new JBLabel(MESSAGE);
            welcomeLabel.setFont(new Font(welcomeLabel.getFont().getName(), Font.BOLD, 16));
            welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(welcomeLabel, BorderLayout.NORTH);

            // GitHub link
            JBLabel githubLink = new JBLabel("<html><a href='" + GITHUB_URL + "'>" + GITHUB_LINK_TEXT + "</a></html>");
            githubLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
            githubLink.setHorizontalAlignment(SwingConstants.CENTER);

            // Add click listener to open browser
            githubLink.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(GITHUB_URL));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            panel.add(githubLink, BorderLayout.CENTER);

            // Add some padding
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            return panel;
        }

        /**
         * Adds a folder to the tree view.
         *
         * @param folder the folder to add
         */
        void addFolderToTreeView(@NotNull VirtualFile folder) {
            // Create a tree model for the folder
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(folder.getName());
            buildTreeNodes(rootNode, folder);

            DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
            Tree tree = new Tree(treeModel);

            // Create a scroll pane for the tree
            JBScrollPane scrollPane = new JBScrollPane(tree);

            // Add a new tab with the tree view
            tabbedPane.addTab(folder.getName() + " (" + tabCounter++ + ")", scrollPane);

            // Select the new tab
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        }

        /**
         * Recursively builds tree nodes for a folder.
         *
         * @param parentNode the parent node
         * @param parentFile the parent file
         */
        private void buildTreeNodes(DefaultMutableTreeNode parentNode, VirtualFile parentFile) {
            VirtualFile[] children = parentFile.getChildren();
            for (VirtualFile child : children) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child.getName());
                parentNode.add(childNode);

                if (child.isDirectory()) {
                    buildTreeNodes(childNode, child);
                }
            }
        }
    }
}
