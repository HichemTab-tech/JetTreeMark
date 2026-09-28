package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VFileProperty;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Builds a tree in one pass while pruning ignored directory contents. */
final class VirtualFileTreeBuilder {
    private static final Logger LOGGER = Logger.getLogger(VirtualFileTreeBuilder.class.getName());

    private final VirtualFile rootFolder;
    private final BooleanSupplier cancelled;

    VirtualFileTreeBuilder(@NotNull VirtualFile rootFolder, @NotNull BooleanSupplier cancelled) {
        this.rootFolder = rootFolder;
        this.cancelled = cancelled;
    }

    @NotNull CheckboxTreeNode build() {
        CheckboxTreeNode rootNode = new CheckboxTreeNode(rootFolder.getName(), true);
        GitIgnoreMatcher rootMatcher = loadRules(rootFolder, "", GitIgnoreMatcher.EMPTY);
        Deque<BuildFrame> pending = new ArrayDeque<>();
        pending.addLast(new BuildFrame(rootNode, rootFolder, "", rootMatcher));

        while (!pending.isEmpty()) {
            checkCancelled();
            BuildFrame frame = pending.removeLast();
            for (VirtualFile child : frame.file().getChildren()) {
                checkCancelled();
                boolean directory = child.isDirectory();
                String relativePath = frame.relativePath().isEmpty()
                        ? child.getName() : frame.relativePath() + '/' + child.getName();
                boolean ignored = frame.matcher().isIgnored(relativePath, directory);
                CheckboxTreeNode childNode = new CheckboxTreeNode(child.getName(), directory);
                if (ignored) {
                    childNode.setCheckState(CheckboxTreeNode.UNCHECKED, false, false);
                }
                frame.node().add(childNode);

                // Show ignored and symbolic-link directories, but do not materialize their contents.
                if (directory && !ignored && !child.is(VFileProperty.SYMLINK)) {
                    GitIgnoreMatcher childMatcher = loadRules(child, relativePath, frame.matcher());
                    pending.addLast(new BuildFrame(childNode, child, relativePath, childMatcher));
                }
            }
        }
        return rootNode;
    }

    private void checkCancelled() {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }

    private GitIgnoreMatcher loadRules(VirtualFile folder, String basePath, GitIgnoreMatcher inherited) {
        VirtualFile ignoreFile = folder.findChild(".gitignore");
        if (ignoreFile == null || ignoreFile.isDirectory() || !ignoreFile.exists()) {
            return inherited;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(ignoreFile.getInputStream(), StandardCharsets.UTF_8))) {
            return inherited.withRules(basePath, reader);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to read " + ignoreFile.getPath(), exception);
            return inherited;
        }
    }

    private record BuildFrame(
            CheckboxTreeNode node,
            VirtualFile file,
            String relativePath,
            GitIgnoreMatcher matcher
    ) {
    }
}
