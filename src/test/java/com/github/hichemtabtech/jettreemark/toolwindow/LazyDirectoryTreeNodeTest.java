package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class LazyDirectoryTreeNodeTest {
    @Test
    public void placeholderMakesUnloadedDirectoryExpandable() {
        LazyDirectoryTreeNode node = lazyNode();

        assertEquals(1, node.getChildCount());
        assertTrue(node.beginLoading());
        assertFalse(node.beginLoading());

        node.loadingFailed();
        assertEquals(1, node.getChildCount());
        assertTrue(node.beginLoading());
    }

    @Test
    public void loadedChildrenReplacePlaceholder() {
        LazyDirectoryTreeNode node = lazyNode();
        CheckboxTreeNode loadedRoot = new CheckboxTreeNode("node_modules", true);
        loadedRoot.add(new CheckboxTreeNode("dependency.js", false));

        node.beginLoading();
        node.finishLoading(loadedRoot);

        assertTrue(node.isLoaded());
        assertEquals(1, node.getChildCount());
        assertEquals("dependency.js", ((CheckboxTreeNode) node.getChildAt(0)).getUserObject());
    }

    @Test
    public void selectionIsInheritedByFutureChildren() {
        LazyDirectoryTreeNode node = lazyNode();

        node.setCheckState(CheckboxTreeNode.CHECKED, false, false);
        assertEquals(LazyDirectoryTreeNode.DescendantSelection.CHECKED, node.descendantSelection());

        node.setCheckState(CheckboxTreeNode.UNCHECKED, false, false);
        assertEquals(LazyDirectoryTreeNode.DescendantSelection.UNCHECKED, node.descendantSelection());
    }

    private static LazyDirectoryTreeNode lazyNode() {
        return new LazyDirectoryTreeNode(
                "node_modules", mock(VirtualFile.class), "node_modules", GitIgnoreMatcher.EMPTY);
    }
}
