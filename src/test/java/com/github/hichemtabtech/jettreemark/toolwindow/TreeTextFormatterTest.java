package com.github.hichemtabtech.jettreemark.toolwindow;

import org.junit.Test;

import java.io.StringWriter;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TreeTextFormatterTest {
    @Test
    public void formatsCheckedNodesWithOneOutputBuffer() throws Exception {
        CheckboxTreeNode root = sampleTree();

        String text = TreeTextFormatter.format(root, 10_000, () -> false);

        assertEquals("""
                root/
                    └── folder/
                        ├── first.txt
                        └── second.txt
                """, text);
    }

    @Test
    public void skipsUncheckedNodesAndKeepsCorrectConnectors() throws Exception {
        CheckboxTreeNode root = sampleTree();
        CheckboxTreeNode folder = (CheckboxTreeNode) root.getChildAt(0);
        ((CheckboxTreeNode) folder.getChildAt(0)).setCheckState(
                CheckboxTreeNode.UNCHECKED, false, false);

        String text = TreeTextFormatter.format(root, 10_000, () -> false);

        assertEquals("""
                root/
                    └── folder/
                        └── second.txt
                """, text);
    }

    @Test
    public void stopsAtClipboardLimitWithoutRecursion() {
        CheckboxTreeNode root = new CheckboxTreeNode("root", true);
        CheckboxTreeNode current = root;
        for (int i = 0; i < 50_000; i++) {
            CheckboxTreeNode child = new CheckboxTreeNode("directory-" + i, true);
            current.add(child);
            current = child;
        }

        assertThrows(TreeTextFormatter.OutputLimitExceededException.class,
                () -> TreeTextFormatter.format(root, 10_000, () -> false));
    }

    @Test
    public void streamsToWriter() throws Exception {
        CheckboxTreeNode root = sampleTree();
        String expected = TreeTextFormatter.format(root, 10_000, () -> false);
        StringWriter writer = new StringWriter();

        TreeTextFormatter.write(root, writer, () -> false);

        assertEquals(expected, writer.toString());
    }

    @Test
    public void honorsCancellation() {
        assertThrows(CancellationException.class,
                () -> TreeTextFormatter.format(sampleTree(), 10_000, () -> true));
    }

    private static CheckboxTreeNode sampleTree() {
        CheckboxTreeNode root = new CheckboxTreeNode("root", true);
        CheckboxTreeNode folder = new CheckboxTreeNode("folder", true);
        folder.add(new CheckboxTreeNode("first.txt", false));
        folder.add(new CheckboxTreeNode("second.txt", false));
        root.add(folder);
        return root;
    }
}
