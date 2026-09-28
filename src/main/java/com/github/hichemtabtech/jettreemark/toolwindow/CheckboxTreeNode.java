package com.github.hichemtabtech.jettreemark.toolwindow;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A tree node that can be checked, unchecked or in an indeterminate state.
 */
public class CheckboxTreeNode extends DefaultMutableTreeNode {
    // Constants for the three possible states
    public static final int UNCHECKED = 0;
    public static final int CHECKED = 1;
    public static final int INDETERMINATE = 2;

    private volatile int checkState = CHECKED; // Default to checked

    private final boolean isFolder;

    public CheckboxTreeNode(Object userObject, boolean isFolder) {
        super(userObject);
        this.isFolder = isFolder;
    }

    public int getCheckState() {
        return checkState;
    }

    public boolean isFolder() {
        return isFolder;
    }

    /**
     * Sets the check state of this node and propagates the state to children and parents.
     * 
     * @param state the new check state
     */
    public void setCheckState(int state) {
        setCheckState(state, true, true);
    }

    /**
     * Sets the check state of this node with option to propagate to children.
     * 
     * @param state the new check state
     * @param propagateToChildren whether to propagate the state to children
     */
    public void setCheckState(int state, boolean propagateToChildren) {
        setCheckState(state, propagateToChildren, true);
    }

    /**
     * Sets the check state of this node with options to propagate to children and update parent.
     * 
     * @param state the new check state
     * @param propagateToChildren whether to propagate the state to children
     * @param updateParent whether to update the parent's state
     */
    public void setCheckState(int state, boolean propagateToChildren, boolean updateParent) {
        if (state < UNCHECKED || state > INDETERMINATE) {
            throw new IllegalArgumentException("Invalid check state: " + state);
        }

        checkState = state;

        // Keep this iterative: filesystem trees can be deeper than the Java stack.
        if (propagateToChildren && state != INDETERMINATE) {
            Deque<CheckboxTreeNode> nodes = new ArrayDeque<>();
            addChildren(this, nodes);
            while (!nodes.isEmpty()) {
                CheckboxTreeNode current = nodes.removeLast();
                current.checkState = state;
                addChildren(current, nodes);
            }
        }

        if (!updateParent) return;

        // Update parent node
        TreeNode parent = getParent();
        if (parent instanceof CheckboxTreeNode) {
            ((CheckboxTreeNode) parent).updateParentCheckState();
        }
    }

    /**
     * Checks only folder nodes (nodes with children) in the tree.
     * This applies to all levels (recursive).
     */
    public void checkOnlyFolders() {
        checkOnlyFolders(true);
    }

    /**
     * Checks only folder nodes (nodes with children).
     * 
     * @param recursive whether to apply to all levels or just the current level
     */
    public void checkOnlyFolders(boolean recursive) {
        checkState = isFolder ? CHECKED : UNCHECKED;
        applyToDescendants(recursive, node -> node.checkState = node.isFolder ? CHECKED : UNCHECKED);

        // Update parent node
        TreeNode parent = getParent();
        if (parent instanceof CheckboxTreeNode) {
            ((CheckboxTreeNode) parent).updateParentCheckState();
        }
    }

    /**
     * Checks only file nodes (nodes without children) in the tree.
     * This applies to all levels (recursive).
     */
    public void checkOnlyFiles() {
        checkOnlyFiles(true);
    }

    /**
     * Checks only file nodes (nodes without children).
     * 
     * @param recursive whether to apply to all levels or just the current level
     */
    public void checkOnlyFiles(boolean recursive) {
        // Folders stay selected because they are required to render paths to selected files.
        checkState = CHECKED;
        applyToDescendants(recursive, node -> node.checkState = CHECKED);

        // Update parent node
        TreeNode parent = getParent();
        if (parent instanceof CheckboxTreeNode) {
            ((CheckboxTreeNode) parent).updateParentCheckState();
        }
    }

    /**
     * Checks all nodes in the tree.
     * This applies to all levels (recursive).
     */
    public void checkAll() {
        checkAll(true);
    }

    /**
     * Checks all nodes.
     * 
     * @param recursive whether to apply to all levels or just the current level
     */
    public void checkAll(boolean recursive) {
        checkState = CHECKED;
        applyToDescendants(recursive, node -> node.checkState = CHECKED);

        // Update parent node
        TreeNode parent = getParent();
        if (parent instanceof CheckboxTreeNode) {
            ((CheckboxTreeNode) parent).updateParentCheckState();
        }
    }

    /**
     * Unchecks all nodes.
     * 
     * @param withSelf whether to uncheck this node as well
     */
    public void uncheckAll(boolean withSelf) {
        if (withSelf) {
            checkState = UNCHECKED;
        }
        applyToDescendants(true, node -> node.checkState = UNCHECKED);
    }

    /**
     * Updates the checked state of this node based on its children.
     * If all children are unchecked, this node will be unchecked.
     * If all children are checked, this node will be checked.
     * If some children are checked and others are unchecked, this node will be indeterminate.
     */
    public void updateParentCheckState() {
        CheckboxTreeNode current = this;
        while (current != null && current.getChildCount() > 0) {
            boolean allChecked = true;
            boolean allUnchecked = true;
            boolean hasCheckboxChild = false;

            for (int i = 0; i < current.getChildCount(); i++) {
                if (current.getChildAt(i) instanceof CheckboxTreeNode child) {
                    hasCheckboxChild = true;
                    allChecked &= child.checkState == CHECKED;
                    allUnchecked &= child.checkState == UNCHECKED;
                }
            }

            if (!hasCheckboxChild) {
                return;
            }

            current.checkState = allChecked ? CHECKED : allUnchecked ? UNCHECKED : INDETERMINATE;
            TreeNode parent = current.getParent();
            current = parent instanceof CheckboxTreeNode checkboxParent ? checkboxParent : null;
        }
    }

    private void applyToDescendants(boolean recursive, NodeAction action) {
        Deque<CheckboxTreeNode> nodes = new ArrayDeque<>();
        addChildren(this, nodes);
        while (!nodes.isEmpty()) {
            CheckboxTreeNode current = nodes.removeLast();
            action.apply(current);
            if (recursive) {
                addChildren(current, nodes);
            }
        }
    }

    private static void addChildren(CheckboxTreeNode node, Deque<CheckboxTreeNode> target) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckboxTreeNode child) {
                target.addLast(child);
            }
        }
    }

    @FunctionalInterface
    private interface NodeAction {
        void apply(CheckboxTreeNode node);
    }
}
