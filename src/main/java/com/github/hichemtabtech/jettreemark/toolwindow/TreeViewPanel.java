package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.logging.Logger;

import static java.util.logging.Logger.getLogger;

/**
 * Panel that displays project information and tree views.
 */
public class TreeViewPanel {
    private static final String GITHUB_URL = "https://github.com/HichemTab-tech";
    private static final String MESSAGE = "Welcome to JetTreeMark!";
    private static final String GITHUB_LINK_TEXT = "Visit HichemTab-tech on GitHub";

    private final JBTabbedPane tabbedPane;
    private int tabCounter = 1;
    private static final Logger logger = getLogger(TreeViewPanel.class.getName());

    public TreeViewPanel() {
        tabbedPane = new JBTabbedPane();
        JPanel welcomePanel = createWelcomePanel();
        tabbedPane.addTab("Welcome", welcomePanel);
        // Welcome tab is not closable
    }

    public JPanel getContent() {
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
        JBLabel githubLink = createGitHubLinkLabel();

        panel.add(githubLink, BorderLayout.CENTER);

        // Add some padding
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        return panel;
    }

    private static @NotNull JBLabel createGitHubLinkLabel() {
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
                    logger.severe("Failed to open GitHub link: " + ex.getMessage());
                }
            }
        });
        return githubLink;
    }

    /**
     * Adds a folder to the tree view.
     *
     * @param folder the folder to add
     */
    public void addFolderToTreeView(@NotNull VirtualFile folder) {
        // Create a tree model for the folder with checkbox nodes
        CheckboxTreeNode rootNode = new CheckboxTreeNode(folder.getName());
        buildTreeNodes(rootNode, folder);

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        Tree tree = new Tree(treeModel);

        // Set the cell renderer to display checkboxes
        tree.setCellRenderer(new CheckboxTreeCellRenderer());

        // Add mouse listener to handle checkbox clicks
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int row = tree.getRowForLocation(x, y);

                if (row != -1) {
                    TreePath path = tree.getPathForRow(row);
                    if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                        Rectangle checkBoxBounds = tree.getRowBounds(row);

                        // Check if click was on the checkbox (roughly the first 20 pixels)
                        if (x <= checkBoxBounds.x + 20) {
                            // Cycle through the states: UNCHECKED -> CHECKED -> UNCHECKED
                            // We don't directly set INDETERMINATE state via clicks, it's determined by children
                            if (node.getCheckState() == CheckboxTreeNode.CHECKED) {
                                node.setCheckState(CheckboxTreeNode.UNCHECKED);
                            } else {
                                node.setCheckState(CheckboxTreeNode.CHECKED);
                            }
                            // Repaint the tree
                            tree.repaint();
                        }
                    }
                }
            }
        });

        // Create a panel for the tree view and copy button
        JPanel treePanel = createTreeViewPanel(tree, rootNode);

        // Add a new tab with the tree view
        String tabTitle = folder.getName() + " (" + tabCounter++ + ")";
        tabbedPane.addTab(tabTitle, treePanel);

        // Add close button to the tab
        int tabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(tabIndex, createTabComponent(tabTitle));

        // Select the new tab
        tabbedPane.setSelectedIndex(tabIndex);
    }

    private @NotNull JPanel createTreeViewPanel(Tree tree, CheckboxTreeNode rootNode) {
        JPanel treePanel = new JPanel(new BorderLayout());

        // Create a scroll pane for the tree
        JBScrollPane scrollPane = new JBScrollPane(tree);
        treePanel.add(scrollPane, BorderLayout.CENTER);

        // Create a panel for the copy button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copy Tree");
        copyButton.addActionListener(e -> {
            // Generate text representation of the tree with only checked nodes
            String treeText = generateTreeText(rootNode, "", true);

            // Copy the tree text to clipboard
            StringSelection stringSelection = new StringSelection(treeText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            // Provide visual feedback
            copyButton.setText("Copied!");
            Timer timer = new Timer(1500, event -> copyButton.setText("Copy Tree"));
            timer.setRepeats(false);
            timer.start();
        });
        buttonPanel.add(copyButton);
        treePanel.add(buttonPanel, BorderLayout.SOUTH);
        return treePanel;
    }

    /**
     * Generates a text representation of the tree structure.
     *
     * @param node the root node
     * @param prefix the prefix for the current line
     * @param isLast whether the current node is the last child of its parent
     * @return the text representation of the tree
     */
    private String generateTreeText(DefaultMutableTreeNode node, String prefix, boolean isLast) {
        StringBuilder result = new StringBuilder();

        // Skip unchecked nodes (except the root node)
        if (node instanceof CheckboxTreeNode checkboxNode) {
            int state = checkboxNode.getCheckState();

            // Skip if unchecked and not the root node
            if (state == CheckboxTreeNode.UNCHECKED && node.getParent() != null) {
                return "";
            }

            // For indeterminate nodes, we include them as they have some checked children
        }

        // Add the current node
        if (node.getParent() != null) { // Skip the root node prefix
            result.append(prefix).append(isLast ? "└── " : "├── ");
            result.append(node.getUserObject());

            // Add directory indicator
            if (node.getChildCount() > 0) {
                result.append("/");
            }

            result.append("\n");
        } else {
            // Root node
            result.append(node.getUserObject()).append("/\n");
        }

        // Process children
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        // Count visible children (checked or indeterminate nodes)
        int visibleChildCount = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (!(child instanceof CheckboxTreeNode checkboxChild)) {
                visibleChildCount++;
            } else {
                int state = checkboxChild.getCheckState();
                if (state == CheckboxTreeNode.CHECKED || state == CheckboxTreeNode.INDETERMINATE) {
                    visibleChildCount++;
                }
            }
        }

        // Track the current visible child index
        int currentVisibleChild = 0;

        // Process each child
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);

            // Skip unchecked nodes
            if (child instanceof CheckboxTreeNode checkboxChild) {
                if (checkboxChild.getCheckState() == CheckboxTreeNode.UNCHECKED) {
                    continue;
                }
            }

            // Determine if this visible child is the last one
            boolean childIsLast = (++currentVisibleChild == visibleChildCount);

            // Generate text for this child
            result.append(generateTreeText(child, childPrefix, childIsLast));
        }

        return result.toString();
    }

    /**
     * Creates a tab component with a close button.
     *
     * @param title the title of the tab
     * @return the tab component
     */
    private JPanel createTabComponent(String title) {
        // Create a panel with FlowLayout (horizontal, left-aligned)
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);

        // Add the title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        tabPanel.add(titleLabel);

        // Create the close button
        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setToolTipText("Close this tab");
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        closeButton.setBorderPainted(false);
        closeButton.setFocusable(false);

        // Add action listener to close the tab
        closeButton.addActionListener(e -> {
            // Find the tab by its component
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabPanel.equals(tabbedPane.getTabComponentAt(i))) {
                    tabbedPane.remove(i);
                    break;
                }
            }
        });

        tabPanel.add(closeButton);
        return tabPanel;
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
            // Create a CheckboxTreeNode if the parent is a CheckboxTreeNode, otherwise use DefaultMutableTreeNode
            DefaultMutableTreeNode childNode;
            if (parentNode instanceof CheckboxTreeNode) {
                childNode = new CheckboxTreeNode(child.getName());
            } else {
                childNode = new DefaultMutableTreeNode(child.getName());
            }
            parentNode.add(childNode);

            if (child.isDirectory()) {
                buildTreeNodes(childNode, child);
            }
        }
    }
}