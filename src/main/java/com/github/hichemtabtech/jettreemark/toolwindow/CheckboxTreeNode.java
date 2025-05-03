package com.github.hichemtabtech.jettreemark.toolwindow;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * A tree node that can be checked, unchecked or in an indeterminate state.
 */
public class CheckboxTreeNode extends DefaultMutableTreeNode {
    // Constants for the three possible states
    public static final int UNCHECKED = 0;
    public static final int CHECKED = 1;
    public static final int INDETERMINATE = 2;

    private int checkState = CHECKED; // Default to checked

    private final boolean isFolder;

    public CheckboxTreeNode(Object userObject, boolean isFolder) {
        super(userObject);
        this.isFolder = isFolder;
    }

    public int getCheckState() {
        return checkState;
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

        // Propagate checked/unchecked state to all children (not indeterminate)
        if (propagateToChildren && state != INDETERMINATE) {
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode) {
                    ((CheckboxTreeNode) child).setCheckState(state, true, false);
                }
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

        checkState = CHECKED;

        // Process children if recursive
        if (recursive) {
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode && ((CheckboxTreeNode) child).isFolder) {
                    ((CheckboxTreeNode) child).checkOnlyFolders();
                }
            }
        } else {
            // Just check/uncheck immediate children based on whether they are folders
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode checkboxChild) {
                    if (checkboxChild.isFolder) {
                        checkboxChild.setCheckState(CHECKED, false, false);
                    } else {
                        checkboxChild.setCheckState(UNCHECKED, false, false);
                    }
                }
            }
        }

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

        checkState = CHECKED;

        // Process children if recursive
        if (recursive) {
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode) {
                    ((CheckboxTreeNode) child).checkOnlyFiles();
                }
            }
        } else {
            // Just check/uncheck immediate children based on whether they are files
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode checkboxChild) {
                    if (checkboxChild.getChildCount() == 0) {
                        checkboxChild.setCheckState(CHECKED, false, false);
                    } else {
                        checkboxChild.setCheckState(UNCHECKED, false, false);
                    }
                }
            }
        }

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

        if (recursive) {
            // Check all children recursively
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode) {
                    ((CheckboxTreeNode) child).checkAll();
                }
            }
        } else {
            // Just check immediate children
            for (int i = 0; i < getChildCount(); i++) {
                Object child = getChildAt(i);
                if (child instanceof CheckboxTreeNode) {
                    ((CheckboxTreeNode) child).setCheckState(CHECKED, false, false);
                }
            }
        }

        // Update parent node
        TreeNode parent = getParent();
        if (parent instanceof CheckboxTreeNode) {
            ((CheckboxTreeNode) parent).updateParentCheckState();
        }
    }

    /**
     * Unchecks all nodes in the tree.
     */
    public void uncheckAll() {
        uncheckAll(false);
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

        // Uncheck all children recursively
        for (int i = 0; i < getChildCount(); i++) {
            Object child = getChildAt(i);
            if (child instanceof CheckboxTreeNode) {
                ((CheckboxTreeNode) child).uncheckAll(true);
            }
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

        boolean allUnchecked = true;

        for (int i = 0; i < getChildCount(); i++) {
            Object child = getChildAt(i);
            if (child instanceof CheckboxTreeNode checkboxChild) {
                if (checkboxChild.getCheckState() == CHECKED) {
                    allUnchecked = false;
                }
            }
        }

        // Determine the new state
        if (!allUnchecked) {
            // Only update if the state would change (to avoid infinite recursion)
            if (checkState != CHECKED) {
                // Set state without propagating to children
                checkState = CHECKED;

                // Update parent node if needed
                if (getParent() instanceof CheckboxTreeNode) {
                    ((CheckboxTreeNode) getParent()).updateParentCheckState();
                }
            }
        }
    }
}
