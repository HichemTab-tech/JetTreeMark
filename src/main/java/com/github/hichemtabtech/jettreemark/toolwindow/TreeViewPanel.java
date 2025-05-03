package com.github.hichemtabtech.jettreemark.toolwindow;

import com.github.hichemtabtech.jettreemark.JetTreeMarkBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

import static java.util.logging.Logger.getLogger;

/**
 * Panel that displays project information and tree views.
 */
public class TreeViewPanel {
    private static final String GITHUB_URL = "https://github.com/HichemTab-tech";
    private static final String MESSAGE = JetTreeMarkBundle.message("welcome_to_jet_tree_mark");
    private static final String GITHUB_LINK_TEXT = "Visit HichemTab-tech on GitHub";

    private final JBTabbedPane tabbedPane;
    private int tabCounter = 1;
    private static final Logger logger = getLogger(TreeViewPanel.class.getName());

    public TreeViewPanel() {
        tabbedPane = new JBTabbedPane();
        JPanel welcomePanel = createWelcomePanel();
        tabbedPane.addTab(JetTreeMarkBundle.message("welcome"), welcomePanel);
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
        // Increment tab counter
        tabCounter++;

        // Create and start the tree builder worker
        TreeBuilderWorker worker = new TreeBuilderWorker(folder);

        // Show loading panel first
        worker.showLoadingPanel();

        // Execute the worker to start building the tree in the background
        worker.execute();
    }

    private @NotNull JPanel createTreeViewPanel(Tree tree, CheckboxTreeNode rootNode) {
        JPanel treePanel = new JPanel(new BorderLayout());

        // Create a scroll pane for the tree
        JBScrollPane scrollPane = new JBScrollPane(tree);
        treePanel.add(scrollPane, BorderLayout.CENTER);

        // Create a panel for the copy button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton(JetTreeMarkBundle.message("copy_tree"));
        copyButton.addActionListener(e -> {
            // Generate text representation of the tree with only checked nodes
            String treeText = generateTreeText(rootNode, "", true);

            // Copy the tree text to clipboard
            StringSelection stringSelection = new StringSelection(treeText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            // Provide visual feedback
            copyButton.setText(JetTreeMarkBundle.message("copied"));
            Timer timer = new Timer(1500, event -> copyButton.setText(JetTreeMarkBundle.message("copy_tree")));
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
        closeButton.setToolTipText(JetTreeMarkBundle.message("close_this_tab"));
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
     * SwingWorker implementation for asynchronous tree building.
     * This prevents UI freezing when loading large directory structures.
     */
    private class TreeBuilderWorker extends SwingWorker<DefaultMutableTreeNode, Void> {
        private final VirtualFile rootFolder;
        private final String rootName;
        private int tabIndex;
        private Set<String> gitignorePatterns;

        public TreeBuilderWorker(VirtualFile rootFolder) {
            this.rootFolder = rootFolder;
            this.rootName = rootFolder.getName();
            this.gitignorePatterns = new HashSet<>();
            loadGitignorePatterns(rootFolder);
        }

        /**
         * Loads patterns from .gitignore file if it exists
         * 
         * @param rootFolder the root folder to search for .gitignore
         */
        private void loadGitignorePatterns(VirtualFile rootFolder) {
            gitignorePatterns = new HashSet<>();
            findGitIgnore(rootFolder, gitignorePatterns);
        }

        /**
         * Loads patterns from .gitignore file if it exists and returns them
         * 
         * @param folder the folder to search for .gitignore
         * @return the set of gitignore patterns
         */
        private Set<String> loadGitignorePatternsForFolder(VirtualFile folder) {
            Set<String> patterns = new HashSet<>();
            findGitIgnore(folder, patterns);
            return patterns;
        }

        private void findGitIgnore(VirtualFile folder, Set<String> patterns) {
            VirtualFile gitignoreFile = folder.findChild(".gitignore");
            if (gitignoreFile != null && !gitignoreFile.isDirectory() && gitignoreFile.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(gitignoreFile.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Skip empty lines and comments
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            patterns.add(line);
                        }
                    }
                } catch (IOException e) {
                    logger.warning("Failed to read .gitignore file: " + e.getMessage());
                }
            }
        }

        /**
         * Checks if a file should be ignored based on gitignore patterns
         * 
         * @param file the file to check
         * @return true if the file should be ignored, false otherwise
         */
        private boolean shouldIgnoreFile(VirtualFile file) {
            if (gitignorePatterns.isEmpty()) {
                return false;
            }

            String filePath = file.getName();

            // Check if the file name or path matches any pattern
            for (String pattern : gitignorePatterns) {
                // Simple exact match
                if (pattern.equals(filePath)) {
                    return true;
                }

                // Directory match (pattern ends with /)
                if (pattern.endsWith("/") && file.isDirectory() && 
                    pattern.substring(0, pattern.length() - 1).equals(filePath)) {
                    return true;
                }

                // Wildcard match (pattern contains *)
                if (pattern.contains("*")) {
                    String regex = pattern.replace(".", "\\.").replace("*", ".*");
                    if (filePath.matches(regex)) {
                        return true;
                    }
                }

                if (pattern.startsWith("/") || pattern.startsWith("./")) {
                    String relativePath = file.getPath().substring(rootFolder.getPath().length());
                    if (relativePath.startsWith(pattern)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        protected DefaultMutableTreeNode doInBackground() {
            // Create root node
            CheckboxTreeNode rootNode = new CheckboxTreeNode(rootName, true);

            // Build tree structure in the background
            buildTreeNodesAsync(rootNode, rootFolder);

            return rootNode;
        }

        /**
         * Non-recursive version of buildTreeNodes to avoid stack overflow with large directories
         */
        private void buildTreeNodesAsync(DefaultMutableTreeNode parentNode, VirtualFile parentFile) {
            List<Object[]> stack = new ArrayList<>();
            stack.add(new Object[]{parentNode, parentFile});

            while (!stack.isEmpty()) {
                Object[] current = stack.removeLast();
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) current[0];
                VirtualFile currentFile = (VirtualFile) current[1];

                VirtualFile[] children = currentFile.getChildren();
                for (VirtualFile child : children) {
                    // Create a CheckboxTreeNode if the parent is a CheckboxTreeNode,
                    // otherwise use DefaultMutableTreeNode
                    DefaultMutableTreeNode childNode;
                    if (currentNode instanceof CheckboxTreeNode) {
                        childNode = new CheckboxTreeNode(child.getName(), child.isDirectory());
                    } else {
                        childNode = new DefaultMutableTreeNode(child.getName());
                    }
                    currentNode.add(childNode);

                    if (child.isDirectory()) {
                        // Add to the stack instead of recursing
                        stack.add(new Object[]{childNode, child});
                    }
                }
            }

            // After tree construction is complete, unselect gitignore files
            unselectGitignoreFiles(parentNode, parentFile);
        }

        /**
         * Unselects nodes that match gitignore patterns at the current level
         * For folders that match gitignore patterns, uncheck them with propagation to children
         * Only traverse into folders that don't match gitignore patterns
         * <p>
         * Non-recursive version to avoid calling VirtualFile.getChildren() from a recursive method
         * 
         * @param parentNode the parent node in the tree
         * @param parentFile the parent file in the file system
         */
        private void unselectGitignoreFiles(DefaultMutableTreeNode parentNode, VirtualFile parentFile) {
            if (!(parentNode instanceof CheckboxTreeNode)) {
                return;
            }

            // Use a stack to avoid recursion
            List<Object[]> stack = new ArrayList<>();

            // Each stack entry contains: [node, file, patterns]
            stack.add(new Object[]{parentNode, parentFile, new HashSet<>(gitignorePatterns)});

            while (!stack.isEmpty()) {
                Object[] current = stack.removeLast();
                DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) current[0];
                VirtualFile currentFile = (VirtualFile) current[1];
                @SuppressWarnings("unchecked")
                Set<String> currentPatterns = (Set<String>) current[2];

                // Temporarily set the patterns for this level
                Set<String> originalPatterns = gitignorePatterns;
                gitignorePatterns = currentPatterns;

                logger.info(gitignorePatterns.toString());

                if (currentNode instanceof CheckboxTreeNode) {
                    VirtualFile[] children = currentFile.getChildren();
                    for (int i = 0; i < Math.min(children.length, currentNode.getChildCount()); i++) {
                        VirtualFile childFile = children[i];
                        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) currentNode.getChildAt(i);

                        if (childNode instanceof CheckboxTreeNode) {
                            if (shouldIgnoreFile(childFile)) {
                                // If it's a folder that matches gitignore pattern,
                                // uncheck it with propagation to children
                                // If it's a file that matches gitignore pattern,
                                // just uncheck it
                                ((CheckboxTreeNode) childNode).setCheckState(CheckboxTreeNode.UNCHECKED, childFile.isDirectory(), false);
                            } else if (childFile.isDirectory() && childNode.getChildCount() > 0) {
                                // Only traverse into folders that don't match gitignore patterns
                                // Check if the folder has a .gitignore file
                                // and load its patterns
                                Set<String> folderPatterns = loadGitignorePatternsForFolder(childFile);

                                // Combine the current patterns with the folder's patterns
                                Set<String> combinedPatterns = new HashSet<>(currentPatterns);
                                combinedPatterns.addAll(folderPatterns);

                                // Add this child to the stack with its combined patterns
                                stack.add(new Object[]{childNode, childFile, combinedPatterns});
                            }
                        }
                    }
                }

                // Restore the original patterns
                gitignorePatterns = originalPatterns;
            }
        }

        @Override
        protected void done() {
            try {
                // Get the built tree root node
                DefaultMutableTreeNode rootNode = get();

                // Create a tree model and tree
                DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
                Tree tree = getTree(treeModel);

                // Create a panel for the tree view and copy button
                JPanel treePanel = createTreeViewPanel(tree, (CheckboxTreeNode) rootNode);

                // Replace the loading panel with the tree panel
                tabbedPane.setComponentAt(tabIndex, treePanel);

                // Update the tab title to remove "Loading..." text
                String tabTitle = rootName + " (" + (tabCounter - 1) + ")";
                tabbedPane.setTitleAt(tabIndex, tabTitle);
                tabbedPane.setTabComponentAt(tabIndex, createTabComponent(tabTitle));

            } catch (InterruptedException | ExecutionException e) {
                logger.severe("Error building tree: " + e.getMessage());
                // Show error in the tab
                JLabel errorLabel = new JLabel(JetTreeMarkBundle.message("errors.unable_to_load_directory")+": " + e.getMessage());
                errorLabel.setForeground(JBColor.RED);
                tabbedPane.setComponentAt(tabIndex, errorLabel);
            }
        }

        private static @NotNull Tree getTree(DefaultTreeModel treeModel) {
            Tree tree = new Tree(treeModel);

            // Set the cell renderer to display checkboxes
            tree.setCellRenderer(new CheckboxTreeCellRenderer());

            // Create a popup menu for tree operations
            JPopupMenu popupMenu = new JPopupMenu(JetTreeMarkBundle.message("context_menu.title"));

            // Add menu items for tree operations (all levels)
            JMenuItem checkAllChildrenItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.check_all_children"));
            JMenuItem checkAllFoldersItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.check_all_folders"));
            JMenuItem uncheckAllChildrenItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.uncheck_all_children"));
            JMenuItem checkWithoutChildrenItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.check_without_children"));
            JMenuItem expandAllItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.expand_all"));
            JMenuItem collapseAllItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.collapse_all"));

            // Create a submenu for level-specific operations
            JMenu levelOperationsMenu = new JMenu(JetTreeMarkBundle.message("context_menu.level_operations"));
            JMenuItem checkOnlyFoldersThisLevelItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.check_only_folders_this_level"));
            JMenuItem checkOnlyFilesThisLevelItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.check_only_files_this_level"));
            JMenuItem checkAllThisLevelItem = new JMenuItem(JetTreeMarkBundle.message("context_menu.check_all_children_this_level"));

            // Add action listeners to menu items (all levels)
            checkAllChildrenItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.checkAll(true);
                } else {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                    if (root instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) root).checkAll();
                    }
                }
                tree.repaint();
            });

            checkAllFoldersItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.checkOnlyFolders();
                } else {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                    if (root instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) root).checkOnlyFolders();
                    }
                }
                tree.repaint();
            });

            uncheckAllChildrenItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.uncheckAll();
                } else {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                    if (root instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) root).uncheckAll();
                    }
                }
                tree.repaint();
            });

            checkWithoutChildrenItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.setCheckState(CheckboxTreeNode.CHECKED, false);
                    tree.repaint();
                }
            });

            // Add action listeners to level-specific menu items
            checkOnlyFoldersThisLevelItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.checkOnlyFolders(false);
                } else {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                    if (root instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) root).checkOnlyFolders(false);
                    }
                }
                tree.repaint();
            });

            checkOnlyFilesThisLevelItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.checkOnlyFiles(false);
                } else {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                    if (root instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) root).checkOnlyFiles(false);
                    }
                }
                tree.repaint();
            });

            checkAllThisLevelItem.addActionListener(e -> {
                TreePath path = tree.getSelectionPath();
                if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    node.checkAll(false);
                } else {
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                    if (root instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) root).checkAll(false);
                    }
                }
                tree.repaint();
            });

            expandAllItem.addActionListener(e -> {
                for (int i = 0; i < tree.getRowCount(); i++) {
                    tree.expandRow(i);
                }
            });

            collapseAllItem.addActionListener(e -> {
                for (int i = tree.getRowCount() - 1; i >= 0; i--) {
                    tree.collapseRow(i);
                }
            });

            // Add level-specific items to submenu
            levelOperationsMenu.add(checkOnlyFoldersThisLevelItem);
            levelOperationsMenu.add(checkOnlyFilesThisLevelItem);
            levelOperationsMenu.addSeparator();
            levelOperationsMenu.add(checkAllThisLevelItem);

            // Add menu items to popup menu
            popupMenu.add(checkAllChildrenItem);
            popupMenu.add(checkAllFoldersItem);
            popupMenu.add(uncheckAllChildrenItem);
            popupMenu.addSeparator();
            popupMenu.add(checkWithoutChildrenItem);
            popupMenu.addSeparator();
            popupMenu.add(levelOperationsMenu);
            popupMenu.addSeparator();
            popupMenu.add(expandAllItem);
            popupMenu.add(collapseAllItem);

            // Add mouse listener to handle checkbox clicks and show context menu
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

                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShowPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShowPopup(e);
                }

                private void maybeShowPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        int row = tree.getRowForLocation(e.getX(), e.getY());
                        if (row != -1) {
                            tree.setSelectionRow(row);
                        }
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
            return tree;
        }

        /**
         * Creates and shows a loading panel in a new tab
         */
        public void showLoadingPanel() {
            JPanel loadingPanel = new JPanel(new BorderLayout());
            JLabel loadingLabel = new JLabel(JetTreeMarkBundle.message("loading_of.text") + " " + rootName + "...", SwingConstants.CENTER);

            // Add a spinner icon
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            loadingPanel.add(loadingLabel, BorderLayout.CENTER);
            loadingPanel.add(progressBar, BorderLayout.SOUTH);

            // Add a new tab with the loading panel
            String tabTitle = rootName + JetTreeMarkBundle.message("loading.text");
            tabbedPane.addTab(tabTitle, loadingPanel);

            // Store the tab index for later use
            tabIndex = tabbedPane.getTabCount() - 1;

            // Select the new tab
            tabbedPane.setSelectedIndex(tabIndex);
        }
    }
}
