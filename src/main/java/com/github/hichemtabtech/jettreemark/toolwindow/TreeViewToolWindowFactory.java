package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool window factory for displaying tree views of folders and files.
 */
public class TreeViewToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, TreeViewPanel> projectPanels = new HashMap<>();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TreeViewPanel treeViewPanel = new TreeViewPanel();
        projectPanels.put(project, treeViewPanel);

        Content content = ContentFactory.getInstance().createContent(treeViewPanel.getContent(), null, false);
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
        TreeViewPanel panel = projectPanels.get(project);
        if (panel != null) {
            panel.addFolderToTreeView(folder);
        }
    }
}