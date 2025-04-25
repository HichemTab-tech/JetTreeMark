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

    public CheckboxTreeNode(Object userObject) {
        super(userObject);
    }

    public int getCheckState() {
        return checkState;
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
        TreeNode parent = getParent();
        if (parent instanceof CheckboxTreeNode) {
            ((CheckboxTreeNode) parent).updateParentCheckState();
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
            if (child instanceof CheckboxTreeNode checkboxChild) {
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