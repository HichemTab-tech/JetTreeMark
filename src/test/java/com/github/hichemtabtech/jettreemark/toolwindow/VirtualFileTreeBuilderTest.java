package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VFileProperty;
import org.junit.Test;

import javax.swing.tree.DefaultTreeModel;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VirtualFileTreeBuilderTest {
    @Test
    public void ignoredDirectoryIsVisibleButNeverEnumerated() throws Exception {
        VirtualFile root = directory("root");
        VirtualFile ignored = directory("ignored");
        VirtualFile kept = directory("kept");
        VirtualFile keptFile = file("Main.java");
        VirtualFile temporary = file("trace.tmp");
        VirtualFile gitignore = file(".gitignore");

        when(gitignore.exists()).thenReturn(true);
        when(gitignore.getInputStream()).thenReturn(new ByteArrayInputStream(
                "ignored/\n*.tmp\n".getBytes(StandardCharsets.UTF_8)));
        when(root.findChild(".gitignore")).thenReturn(gitignore);
        when(root.getChildren()).thenReturn(new VirtualFile[]{ignored, kept, temporary});
        when(kept.getChildren()).thenReturn(new VirtualFile[]{keptFile});

        CheckboxTreeNode tree = new VirtualFileTreeBuilder(root, () -> false).build();

        assertEquals(3, tree.getChildCount());
        CheckboxTreeNode ignoredNode = (CheckboxTreeNode) tree.getChildAt(0);
        assertTrue(ignoredNode instanceof LazyDirectoryTreeNode);
        assertEquals(CheckboxTreeNode.UNCHECKED, ignoredNode.getCheckState());
        assertEquals(1, ignoredNode.getChildCount()); // Lazy placeholder keeps the expansion handle visible.
        assertFalse(new DefaultTreeModel(tree).isLeaf(ignoredNode));
        assertEquals(CheckboxTreeNode.UNCHECKED,
                ((CheckboxTreeNode) tree.getChildAt(2)).getCheckState());
        verify(ignored, never()).getChildren();

        VirtualFile ignoredFile = file("dependency.js");
        when(ignored.getChildren()).thenReturn(new VirtualFile[]{ignoredFile});
        LazyDirectoryTreeNode lazyNode = (LazyDirectoryTreeNode) ignoredNode;
        CheckboxTreeNode loaded = new VirtualFileTreeBuilder(
                lazyNode.directory(), () -> false,
                lazyNode.relativePath(), lazyNode.inheritedMatcher()).build();

        assertEquals(1, loaded.getChildCount());
        assertEquals("dependency.js", ((CheckboxTreeNode) loaded.getChildAt(0)).getUserObject());
    }

    @Test
    public void symbolicLinkDirectoryIsNotFollowed() {
        VirtualFile root = directory("root");
        VirtualFile link = directory("link");
        when(link.is(VFileProperty.SYMLINK)).thenReturn(true);
        when(root.getChildren()).thenReturn(new VirtualFile[]{link});

        CheckboxTreeNode tree = new VirtualFileTreeBuilder(root, () -> false).build();

        assertEquals(1, tree.getChildCount());
        verify(link, never()).getChildren();
    }

    @Test
    public void appliesNegationToGitkeepInsideIgnoredContents() throws Exception {
        VirtualFile root = directory("root");
        VirtualFile idea = directory(".idea");
        VirtualFile notes = file("notes.md");
        VirtualFile ignoredFolder = directory("ignoreme-folder");
        VirtualFile ignoredFile = file("ignored.txt");
        VirtualFile gitkeep = file(".gitkeep");
        VirtualFile gitignore = file(".gitignore");

        when(gitignore.exists()).thenReturn(true);
        when(gitignore.getInputStream()).thenReturn(new ByteArrayInputStream(
                (".idea\nnotes.md\nignoreme-folder/*\n"
                        + "!ignoreme-folder/.gitkeep\n").getBytes(StandardCharsets.UTF_8)));
        when(root.findChild(".gitignore")).thenReturn(gitignore);
        when(root.getChildren()).thenReturn(new VirtualFile[]{idea, notes, ignoredFolder});
        when(ignoredFolder.getChildren()).thenReturn(new VirtualFile[]{ignoredFile, gitkeep});

        CheckboxTreeNode tree = new VirtualFileTreeBuilder(root, () -> false).build();

        assertEquals(CheckboxTreeNode.UNCHECKED, child(tree, ".idea").getCheckState());
        assertEquals(CheckboxTreeNode.UNCHECKED, child(tree, "notes.md").getCheckState());
        CheckboxTreeNode folderNode = child(tree, "ignoreme-folder");
        assertEquals(CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals(CheckboxTreeNode.UNCHECKED, child(folderNode, "ignored.txt").getCheckState());
        assertEquals(CheckboxTreeNode.CHECKED, child(folderNode, ".gitkeep").getCheckState());
    }

    @Test
    public void vcsResultIsAuthoritativeWhenFileIsUnderVersionControl() throws Exception {
        VirtualFile root = directory("root");
        VirtualFile temporary = file("tracked.tmp");
        VirtualFile gitignore = file(".gitignore");

        when(gitignore.exists()).thenReturn(true);
        when(gitignore.getInputStream()).thenReturn(new ByteArrayInputStream(
                "*.tmp\n".getBytes(StandardCharsets.UTF_8)));
        when(root.findChild(".gitignore")).thenReturn(gitignore);
        when(root.getChildren()).thenReturn(new VirtualFile[]{temporary});

        CheckboxTreeNode tree = new VirtualFileTreeBuilder(
                root, () -> false, file -> VcsIgnoreProvider.Status.NOT_IGNORED).build();

        assertEquals(CheckboxTreeNode.CHECKED, child(tree, "tracked.tmp").getCheckState());
    }

    @Test
    public void cancellationStopsBeforeEnumeration() {
        VirtualFile root = directory("root");

        assertThrows(CancellationException.class,
                () -> new VirtualFileTreeBuilder(root, () -> true).build());
        verify(root, never()).getChildren();
    }

    private static VirtualFile directory(String name) {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getName()).thenReturn(name);
        when(file.isDirectory()).thenReturn(true);
        when(file.is(VFileProperty.SYMLINK)).thenReturn(false);
        when(file.findChild(".gitignore")).thenReturn(null);
        when(file.getChildren()).thenReturn(VirtualFile.EMPTY_ARRAY);
        return file;
    }

    private static VirtualFile file(String name) {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getName()).thenReturn(name);
        when(file.isDirectory()).thenReturn(false);
        return file;
    }

    private static CheckboxTreeNode child(CheckboxTreeNode parent, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            CheckboxTreeNode child = (CheckboxTreeNode) parent.getChildAt(i);
            if (name.equals(child.getUserObject())) {
                return child;
            }
        }
        throw new AssertionError("Missing child: " + name);
    }
}
