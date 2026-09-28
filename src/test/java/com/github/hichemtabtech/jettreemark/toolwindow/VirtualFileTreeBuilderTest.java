package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VFileProperty;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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
        assertEquals(CheckboxTreeNode.UNCHECKED, ignoredNode.getCheckState());
        assertEquals(0, ignoredNode.getChildCount());
        assertEquals(CheckboxTreeNode.UNCHECKED,
                ((CheckboxTreeNode) tree.getChildAt(2)).getCheckState());
        verify(ignored, never()).getChildren();
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
}
