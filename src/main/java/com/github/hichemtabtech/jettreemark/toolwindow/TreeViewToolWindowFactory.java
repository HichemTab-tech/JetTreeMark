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
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool window factory for displaying tree views of folders and files.
 */
public class TreeViewToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, TreeViewPanel> projectPanels = new HashMap<>();

    /**
     * A tree node that can be checked, unchecked, or in an indeterminate state.
     */
    static class CheckboxTreeNode extends DefaultMutableTreeNode {
        // Constants for the three possible states
        public static final int UNCHECKED = 0;
        public static final int CHECKED = 1;
        public static final int INDETERMINATE = 2;

        private int checkState = CHECKED; // Default to checked

        public CheckboxTreeNode(Object userObject) {
            super(userObject);
        }

        public int getCheckState() {
            return checkState;
        }

        public boolean isChecked() {
            return checkState == CHECKED;
        }

        public boolean isIndeterminate() {
            return checkState == INDETERMINATE;
        }

        public void setChecked(boolean checked) {
            setCheckState(checked ? CHECKED : UNCHECKED);
        }

        public void setCheckState(int state) {
            if (state < UNCHECKED || state > INDETERMINATE) {
                throw new IllegalArgumentException("Invalid check state: " + state);
            }

            checkState = state;

            // Propagate checked/unchecked state to all children (not indeterminate)
            if (state != INDETERMINATE) {
                for (int i = 0; i < getChildCount(); i++) {
                    Object child = getChildAt(i);
                    if (child instanceof CheckboxTreeNode) {
                        ((CheckboxTreeNode) child).setCheckState(state);
                    }
                }
            }

            // Update parent node
            if (getParent() instanceof CheckboxTreeNode) {
                CheckboxTreeNode parent = (CheckboxTreeNode) getParent();
                parent.updateParentCheckState();
            }
        }

        /**
         * Updates the checked state of this node based on its children.
         * If all children are unchecked, this node will be unchecked.
         * If all children are checked, this node will be checked.
         * If some children are checked and others are unchecked, this node will be indeterminate.
         */
        public void updateParentCheckState() {
            if (getChildCount() == 0) {
                return; // No children to check
            }

            boolean allChecked = true;
            boolean allUnchecked = true;

            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode) {
                    CheckboxTreeNode checkboxChild = (CheckboxTreeNode) child;
                    if (checkboxChild.getCheckState() == CHECKED || checkboxChild.getCheckState() == INDETERMINATE) {
                        allUnchecked = false;
                    }
                    if (checkboxChild.getCheckState() == UNCHECKED || checkboxChild.getCheckState() == INDETERMINATE) {
                        allChecked = false;
                    }
                }
            }

            // Determine the new state
            int newState;
            if (allChecked) {
                newState = CHECKED;
            } else if (allUnchecked) {
                newState = UNCHECKED;
            } else {
                newState = INDETERMINATE;
            }

            // Only update if the state would change (to avoid infinite recursion)
            if (checkState != newState) {
                // Set state without propagating to children
                checkState = newState;

                // Update parent node if needed
                if (getParent() instanceof CheckboxTreeNode) {
                    ((CheckboxTreeNode) getParent()).updateParentCheckState();
                }
            }
        }
    }

    /**
     * A cell renderer that displays a tri-state checkbox for each tree node.
     */
    static class CheckboxTreeCellRenderer extends DefaultTreeCellRenderer {
        private final TriStateCheckBox checkBox = new TriStateCheckBox();

        public CheckboxTreeCellRenderer() {
            checkBox.setOpaque(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, 
                                                     boolean leaf, int row, boolean hasFocus) {
            Component renderer = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof CheckboxTreeNode) {
                CheckboxTreeNode node = (CheckboxTreeNode) value;
                checkBox.setText(node.getUserObject().toString() + (node.getChildCount() > 0 ? "/" : ""));

                // Set the appropriate state
                switch (node.getCheckState()) {
                    case CheckboxTreeNode.CHECKED:
                        checkBox.setState(TriStateCheckBox.State.SELECTED);
                        break;
                    case CheckboxTreeNode.UNCHECKED:
                        checkBox.setState(TriStateCheckBox.State.UNSELECTED);
                        break;
                    case CheckboxTreeNode.INDETERMINATE:
                        checkBox.setState(TriStateCheckBox.State.INDETERMINATE);
                        break;
                }

                checkBox.setFont(renderer.getFont());
                checkBox.setForeground(renderer.getForeground());
                checkBox.setBackground(renderer.getBackground());
                return checkBox;
            }
            return renderer;
        }
    }

    /**
     * A checkbox that can be in one of three states: selected, unselected, or indeterminate.
     */
    static class TriStateCheckBox extends JCheckBox {
        public enum State { SELECTED, UNSELECTED, INDETERMINATE }

        private State state = State.UNSELECTED;

        public TriStateCheckBox() {
            super();
            setState(State.UNSELECTED);
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;

            // Update the visual appearance
            switch (state) {
                case SELECTED:
                    setSelected(true);
                    setIcon(null); // Use default selected icon
                    break;
                case UNSELECTED:
                    setSelected(false);
                    setIcon(null); // Use default unselected icon
                    break;
                case INDETERMINATE:
                    setSelected(false);
                    // Create a custom icon for indeterminate state (a dash in the checkbox)
                    setIcon(createIndeterminateIcon());
                    break;
            }
        }

        private Icon createIndeterminateIcon() {
            // Create a custom icon that shows a dash in the checkbox
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    // Get the default checkbox icon
                    Icon defaultIcon = UIManager.getIcon("CheckBox.icon");
                    if (defaultIcon != null) {
                        // Draw the default unchecked icon
                        defaultIcon.paintIcon(c, g, x, y);

                        // Draw a dash in the middle
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setColor(getForeground());
                        int width = getIconWidth();
                        int height = getIconHeight();
                        int dashWidth = width / 2;
                        int dashHeight = height / 6;
                        g2d.fillRect(x + (width - dashWidth) / 2, y + (height - dashHeight) / 2, dashWidth, dashHeight);
                        g2d.dispose();
                    }
                }

                @Override
                public int getIconWidth() {
                    Icon defaultIcon = UIManager.getIcon("CheckBox.icon");
                    return defaultIcon != null ? defaultIcon.getIconWidth() : 16;
                }

                @Override
                public int getIconHeight() {
                    Icon defaultIcon = UIManager.getIcon("CheckBox.icon");
                    return defaultIcon != null ? defaultIcon.getIconHeight() : 16;
                }
            };
        }
    }

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
     * Panel that displays project information and tree views.
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
            // Welcome tab is not closable
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
                        if (path != null && path.getLastPathComponent() instanceof CheckboxTreeNode) {
                            CheckboxTreeNode node = (CheckboxTreeNode) path.getLastPathComponent();
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
            JPanel treePanel = new JPanel(new BorderLayout());

            // Create a scroll pane for the tree
            JBScrollPane scrollPane = new JBScrollPane(tree);
            treePanel.add(scrollPane, BorderLayout.CENTER);

            // Create a panel for the copy button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton copyButton = new JButton("Copy Tree");
            copyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
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
                }
            });
            buttonPanel.add(copyButton);
            treePanel.add(buttonPanel, BorderLayout.SOUTH);

            // Add a new tab with the tree view
            String tabTitle = folder.getName() + " (" + tabCounter++ + ")";
            tabbedPane.addTab(tabTitle, treePanel);

            // Add close button to the tab
            int tabIndex = tabbedPane.getTabCount() - 1;
            tabbedPane.setTabComponentAt(tabIndex, createTabComponent(tabTitle, tabIndex));

            // Select the new tab
            tabbedPane.setSelectedIndex(tabIndex);
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
            if (node instanceof CheckboxTreeNode && !((CheckboxTreeNode) node).isChecked() && node.getParent() != null) {
                return "";
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

            // Count visible children (checked nodes)
            int visibleChildCount = 0;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (!(child instanceof CheckboxTreeNode) || ((CheckboxTreeNode) child).isChecked()) {
                    visibleChildCount++;
                }
            }

            // Track the current visible child index
            int currentVisibleChild = 0;

            // Process each child
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);

                // Skip unchecked nodes
                if (child instanceof CheckboxTreeNode && !((CheckboxTreeNode) child).isChecked()) {
                    continue;
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
         * @param index the index of the tab
         * @return the tab component
         */
        private JPanel createTabComponent(String title, int index) {
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
                Component tabComponent = tabPanel;
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
}
