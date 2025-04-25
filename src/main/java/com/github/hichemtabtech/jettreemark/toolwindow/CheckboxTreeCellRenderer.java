package com.github.hichemtabtech.jettreemark.toolwindow;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * A cell renderer that displays a tri-state checkbox for each tree node.
 */
public class CheckboxTreeCellRenderer extends DefaultTreeCellRenderer {
    private final TriStateCheckBox checkBox = new TriStateCheckBox();

    public CheckboxTreeCellRenderer() {
        checkBox.setOpaque(false);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, 
                                                 boolean leaf, int row, boolean hasFocus) {
        Component renderer = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof CheckboxTreeNode node) {
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