package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Tool window factory for displaying tree views of folders and files.
 */
public class TreeViewToolWindowFactory implements ToolWindowFactory {

    private static final Key<TreeViewPanel> PROJECT_PANEL =
            Key.create("com.github.hichemtabtech.jettreemark.treeViewPanel");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TreeViewPanel treeViewPanel = new TreeViewPanel(project);
        project.putUserData(PROJECT_PANEL, treeViewPanel);

        Content content = ContentFactory.getInstance().createContent(treeViewPanel.getContent(), null, false);
        content.setDisposer(() -> {
            if (project.getUserData(PROJECT_PANEL) == treeViewPanel) {
                project.putUserData(PROJECT_PANEL, null);
            }
            treeViewPanel.dispose();
        });
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * Adds a folder to the tree view.
     *
     * @param project    the project
     * @param folder     the folder to add
     */
    public static void addFolderToTreeView(@NotNull Project project, @NotNull VirtualFile folder) {
        TreeViewPanel panel = project.getUserData(PROJECT_PANEL);
        if (panel != null) {
            panel.addFolderToTreeView(folder);
        }
    }
}
