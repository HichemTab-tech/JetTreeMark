package com.github.hichemtabtech.jettreemark.toolwindow;

import com.github.hichemtabtech.jettreemark.JetTreeMarkBundle;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.tree.DefaultMutableTreeNode;

/** An ignored directory whose contents are materialized only when requested. */
final class LazyDirectoryTreeNode extends CheckboxTreeNode {
    enum DescendantSelection {
        CHECKED,
        UNCHECKED,
        FOLDERS_ONLY,
        FILES_ONLY
    }

    private final VirtualFile directory;
    private final String relativePath;
    private final GitIgnoreMatcher inheritedMatcher;
    private boolean loaded;
    private boolean loading;
    private volatile DescendantSelection descendantSelection = DescendantSelection.UNCHECKED;

    LazyDirectoryTreeNode(
            String name,
            VirtualFile directory,
            String relativePath,
            GitIgnoreMatcher inheritedMatcher
    ) {
        super(name, true);
        this.directory = directory;
        this.relativePath = relativePath;
        this.inheritedMatcher = inheritedMatcher;
        addLoadingPlaceholder();
    }

    VirtualFile directory() {
        return directory;
    }

    String relativePath() {
        return relativePath;
    }

    GitIgnoreMatcher inheritedMatcher() {
        return inheritedMatcher;
    }

    boolean beginLoading() {
        if (loaded || loading) {
            return false;
        }
        loading = true;
        return true;
    }

    boolean isLoaded() {
        return loaded;
    }

    DescendantSelection descendantSelection() {
        return descendantSelection;
    }

    void setDescendantSelection(DescendantSelection descendantSelection) {
        if (!loaded) {
            this.descendantSelection = descendantSelection;
        }
    }

    void finishLoading(CheckboxTreeNode loadedRoot) {
        removeAllChildren();
        while (loadedRoot.getChildCount() > 0) {
            add((CheckboxTreeNode) loadedRoot.getChildAt(0));
        }
        loading = false;
        loaded = true;
    }

    void loadingFailed() {
        removeAllChildren();
        addLoadingPlaceholder();
        loading = false;
    }

    @Override
    public void setCheckState(int state, boolean propagateToChildren, boolean updateParent) {
        super.setCheckState(state, propagateToChildren, updateParent);
        if (!loaded && state != INDETERMINATE) {
            descendantSelection = state == CHECKED
                    ? DescendantSelection.CHECKED : DescendantSelection.UNCHECKED;
        }
    }

    private void addLoadingPlaceholder() {
        add(new DefaultMutableTreeNode(JetTreeMarkBundle.message("loading_children"), false));
    }
}
