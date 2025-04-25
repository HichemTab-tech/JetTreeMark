package com.github.hichemtabtech.jettreemark.actions;

import com.github.hichemtabtech.jettreemark.toolwindow.TreeViewToolWindowFactory;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to show a tree view of a folder in the JetTreeMark tool window.
 */
public class FolderTreeViewAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (selectedFile == null || !selectedFile.isDirectory()) {
            return;
        }

        // Get the tool window
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Tree View");
        if (toolWindow == null) {
            return;
        }

        // Ensure the tool window is visible
        toolWindow.show(() -> {
            // Add the folder to the tree view
            TreeViewToolWindowFactory.addFolderToTreeView(project, selectedFile);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only if a directory is selected
        Project project = e.getProject();
        VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setEnabledAndVisible(
                project != null && selectedFile != null && selectedFile.isDirectory()
        );
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
