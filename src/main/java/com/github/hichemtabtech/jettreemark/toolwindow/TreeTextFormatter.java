package com.github.hichemtabtech.jettreemark.toolwindow;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Formats checked tree nodes without recursively materializing every subtree. */
final class TreeTextFormatter {
    private static final int INITIAL_CAPACITY = 16 * 1024;

    private TreeTextFormatter() {
    }

    static @NotNull String format(
            @NotNull CheckboxTreeNode root,
            int maximumCharacters,
            @NotNull BooleanSupplier cancelled
    ) throws IOException {
        if (maximumCharacters <= 0) {
            throw new IllegalArgumentException("maximumCharacters must be positive");
        }

        StringBuilder result = new StringBuilder(Math.min(INITIAL_CAPACITY, maximumCharacters));
        writeTree(root, new BoundedAppendable(result, maximumCharacters), cancelled);
        return result.toString();
    }

    static void write(
            @NotNull CheckboxTreeNode root,
            @NotNull Writer writer,
            @NotNull BooleanSupplier cancelled
    ) throws IOException {
        writeTree(root, writer, cancelled);
    }

    private static void writeTree(
            CheckboxTreeNode root,
            Appendable output,
            BooleanSupplier cancelled
    ) throws IOException {
        Deque<Frame> stack = new ArrayDeque<>();
        stack.addLast(new Frame(root, "", true, true));

        while (!stack.isEmpty()) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException();
            }

            Frame frame = stack.removeLast();
            CheckboxTreeNode node = frame.node();
            if (!frame.root() && node.getCheckState() == CheckboxTreeNode.UNCHECKED) {
                continue;
            }

            if (frame.root()) {
                append(output, String.valueOf(node.getUserObject()), "/\n");
            } else {
                append(output, frame.prefix(), frame.last() ? "└── " : "├── ",
                        String.valueOf(node.getUserObject()), node.isFolder() ? "/\n" : "\n");
            }

            int lastVisibleChild = findLastVisibleChild(node);
            if (lastVisibleChild < 0) {
                continue;
            }

            String childPrefix = frame.prefix() + (frame.last() ? "    " : "│   ");
            for (int i = lastVisibleChild; i >= 0; i--) {
                if (node.getChildAt(i) instanceof CheckboxTreeNode child && isVisible(child)) {
                    stack.addLast(new Frame(child, childPrefix, i == lastVisibleChild, false));
                }
            }
        }
    }

    private static int findLastVisibleChild(CheckboxTreeNode node) {
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            if (node.getChildAt(i) instanceof CheckboxTreeNode child && isVisible(child)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isVisible(CheckboxTreeNode node) {
        return node.getCheckState() != CheckboxTreeNode.UNCHECKED;
    }

    private static void append(Appendable output, String... values) throws IOException {
        for (String value : values) {
            output.append(value);
        }
    }

    static final class OutputLimitExceededException extends IOException {
        OutputLimitExceededException(int maximumCharacters) {
            super("Tree exceeds the clipboard limit of " + maximumCharacters + " characters");
        }
    }

    private record Frame(CheckboxTreeNode node, String prefix, boolean last, boolean root) {
    }

    private record BoundedAppendable(StringBuilder delegate, int maximumCharacters) implements Appendable {

        @Override
            public Appendable append(CharSequence value) throws IOException {
                return append(value, 0, value.length());
            }

            @Override
            public Appendable append(CharSequence value, int start, int end) throws IOException {
                int addedLength = end - start;
                if (addedLength > maximumCharacters - delegate.length()) {
                    throw new OutputLimitExceededException(maximumCharacters);
                }
                delegate.append(value, start, end);
                return this;
            }

            @Override
            public Appendable append(char value) throws IOException {
                if (delegate.length() == maximumCharacters) {
                    throw new OutputLimitExceededException(maximumCharacters);
                }
                delegate.append(value);
                return this;
            }
        }
}
