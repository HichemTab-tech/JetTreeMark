package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/** Resolves ignore state through the IDE's configured VCS integration. */
@FunctionalInterface
interface VcsIgnoreProvider {
    VcsIgnoreProvider NONE = file -> Status.UNKNOWN;

    enum Status {
        IGNORED,
        NOT_IGNORED,
        UNKNOWN
    }

    @NotNull Status status(@NotNull VirtualFile file);

    static @NotNull VcsIgnoreProvider forProject(@NotNull Project project) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        return file -> {
            if (project.isDisposed() || vcsManager.getVcsRootFor(file) == null) {
                return Status.UNKNOWN;
            }
            if (changeListManager.isIgnoredFile(file)) {
                return Status.IGNORED;
            }
            return Status.NOT_IGNORED;
        };
    }
}
