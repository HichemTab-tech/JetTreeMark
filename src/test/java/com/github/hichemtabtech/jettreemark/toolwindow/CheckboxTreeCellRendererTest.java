package com.github.hichemtabtech.jettreemark.toolwindow;

import org.junit.Test;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckboxTreeCellRendererTest {
    @Test
    public void emptyFolderKeepsFolderSuffix() {
        CheckboxTreeNode folder = new CheckboxTreeNode("node_modules", true);
        folder.setCheckState(CheckboxTreeNode.UNCHECKED, false, false);
        JTree tree = new JTree(new DefaultTreeModel(folder));

        Component rendered = new CheckboxTreeCellRenderer()
                .getTreeCellRendererComponent(tree, folder, false, false, true, 0, false);

        assertTrue(rendered instanceof JCheckBox);
        JCheckBox checkBox = (JCheckBox) rendered;
        assertEquals("node_modules/", checkBox.getText());
    }

    @Test
    public void fileDoesNotGetFolderSuffix() {
        CheckboxTreeNode file = new CheckboxTreeNode("package.json", false);
        JTree tree = new JTree(new DefaultTreeModel(file));

        Component rendered = new CheckboxTreeCellRenderer()
                .getTreeCellRendererComponent(tree, file, false, false, true, 0, false);

        assertTrue(rendered instanceof JCheckBox);
        JCheckBox checkBox = (JCheckBox) rendered;
        assertEquals("package.json", checkBox.getText());
    }
}
