package com.github.hichemtabtech.jettreemark.toolwindow;

import com.github.hichemtabtech.jettreemark.JetTreeMarkBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Panel that displays project information and generated tree views. */
public class TreeViewPanel implements Disposable {
    private static final String GITHUB_URL = "https://github.com/HichemTab-tech";
    private static final int DEFAULT_CLIPBOARD_LIMIT = 5_000_000;
    private static final int DEFAULT_EXPAND_LIMIT = 20_000;
    private static final int UI_BATCH_SIZE = 1_000;
    private static final Logger LOGGER = Logger.getLogger(TreeViewPanel.class.getName());

    private final JBTabbedPane tabbedPane = new JBTabbedPane();
    private final JPanel content = new JPanel(new BorderLayout());
    private final Set<SwingWorker<?, ?>> activeWorkers =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TabSession> sessions =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final VcsIgnoreProvider vcsIgnoreProvider;
    private int nextTabNumber = 1;
    private boolean disposed;

    public TreeViewPanel() {
        this(VcsIgnoreProvider.NONE);
    }

    public TreeViewPanel(@NotNull Project project) {
        this(VcsIgnoreProvider.forProject(project));
    }

    TreeViewPanel(@NotNull VcsIgnoreProvider vcsIgnoreProvider) {
        this.vcsIgnoreProvider = vcsIgnoreProvider;
        content.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addTab(JetTreeMarkBundle.message("welcome"), createWelcomePanel());
    }

    public JPanel getContent() {
        return content;
    }

    public void addFolderToTreeView(@NotNull VirtualFile folder) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addFolderToTreeView(folder));
            return;
        }
        if (disposed) {
            return;
        }

        TabSession session = new TabSession(folder.getName(), nextTabNumber++);
        sessions.add(session);
        TreeBuilderWorker worker = new TreeBuilderWorker(session, folder);
        session.showLoadingPanel();
        startWorker(session, worker);
    }

    @Override
    public void dispose() {
        disposed = true;
        for (SwingWorker<?, ?> worker : Set.copyOf(activeWorkers)) {
            worker.cancel(true);
        }
        for (TabSession session : Set.copyOf(sessions)) {
            session.stopTimers();
        }
        activeWorkers.clear();
        sessions.clear();
    }

    private @NotNull JPanel createWelcomePanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new BorderLayout());
        JBLabel welcomeLabel = new JBLabel(JetTreeMarkBundle.message("welcome_to_jet_tree_mark"));
        welcomeLabel.setFont(new Font(welcomeLabel.getFont().getName(), Font.BOLD, 16));
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(welcomeLabel, BorderLayout.NORTH);

        JBLabel githubLink = createGithubLink();
        panel.add(githubLink, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }

    private static @NonNull JBLabel createGithubLink() {
        JBLabel githubLink = new JBLabel("<html><a href='" + GITHUB_URL + "'>Visit HichemTab-tech on GitHub</a></html>");
        githubLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        githubLink.setHorizontalAlignment(SwingConstants.CENTER);
        githubLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                try {
                    Desktop.getDesktop().browse(new URI(GITHUB_URL));
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Failed to open GitHub link", exception);
                }
            }
        });
        return githubLink;
    }

    private @NotNull JPanel createTreeViewPanel(
            TabSession session,
            Tree tree,
            CheckboxTreeNode rootNode
    ) {
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(JetTreeMarkBundle.message("save_tree"));
        JButton copyButton = new JButton(JetTreeMarkBundle.message("copy_tree"));
        session.setControls(tree, copyButton, saveButton);
        saveButton.addActionListener(event -> chooseAndSaveTree(session, tree, rootNode, copyButton, saveButton));
        copyButton.addActionListener(event -> copyTree(session, tree, rootNode, copyButton, saveButton));
        buttonPanel.add(saveButton);
        buttonPanel.add(copyButton);
        treePanel.add(buttonPanel, BorderLayout.SOUTH);
        return treePanel;
    }

    private void copyTree(
            TabSession session,
            Tree tree,
            CheckboxTreeNode rootNode,
            JButton copyButton,
            JButton saveButton
    ) {
        setTreeOperationEnabled(tree, copyButton, saveButton, false);
        int limit = positiveIntegerProperty("jettreemark.maxClipboardCharacters", DEFAULT_CLIPBOARD_LIMIT);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return TreeTextFormatter.format(rootNode, limit, this::isCancelled);
            }

            @Override
            protected void done() {
                finishWorker(session, this);
                if (!session.isOpen()) {
                    return;
                }
                setTreeOperationEnabled(tree, copyButton, saveButton, true);
                try {
                    String text = get();
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                    copyButton.setText(JetTreeMarkBundle.message("copied"));
                    Timer reset = new Timer(1_500, event -> {
                        copyButton.setText(JetTreeMarkBundle.message("copy_tree"));
                        session.stopTimer((Timer) event.getSource());
                    });
                    reset.setRepeats(false);
                    session.startTimer(reset);
                } catch (CancellationException ignored) {
                    // Closing the tab or project is an expected cancellation path.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof TreeTextFormatter.OutputLimitExceededException) {
                        showWarning(tree, JetTreeMarkBundle.message("copy_too_large", limit));
                    } else {
                        showError(tree, JetTreeMarkBundle.message("errors.copy_failed", messageOf(cause)));
                    }
                } catch (IllegalStateException | HeadlessException exception) {
                    showError(tree, JetTreeMarkBundle.message("errors.copy_failed", messageOf(exception)));
                }
            }
        };
        startWorker(session, worker);
    }

    private void chooseAndSaveTree(
            TabSession session,
            Tree tree,
            CheckboxTreeNode rootNode,
            JButton copyButton,
            JButton saveButton
    ) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(JetTreeMarkBundle.message("save_tree"));
        chooser.setSelectedFile(new java.io.File(session.rootName + ".txt"));
        if (chooser.showSaveDialog(content) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destination = chooser.getSelectedFile().toPath().toAbsolutePath();
        if (Files.exists(destination) && JOptionPane.showConfirmDialog(content,
                JetTreeMarkBundle.message("confirm_overwrite", destination),
                JetTreeMarkBundle.message("save_tree"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        setTreeOperationEnabled(tree, copyButton, saveButton, false);
        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                Path parent = destination.getParent();
                Path temporary = Files.createTempFile(parent, ".jettreemark-", ".tmp");
                boolean completed = false;
                try {
                    try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                        TreeTextFormatter.write(rootNode, writer, this::isCancelled);
                    }
                    if (isCancelled()) {
                        throw new CancellationException();
                    }
                    try {
                        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                    completed = true;
                } finally {
                    if (!completed) {
                        Files.deleteIfExists(temporary);
                    }
                }
                return destination;
            }

            @Override
            protected void done() {
                finishWorker(session, this);
                if (!session.isOpen()) {
                    return;
                }
                setTreeOperationEnabled(tree, copyButton, saveButton, true);
                try {
                    Path saved = get();
                    JOptionPane.showMessageDialog(content,
                            JetTreeMarkBundle.message("tree_saved", saved),
                            JetTreeMarkBundle.message("save_tree"), JOptionPane.INFORMATION_MESSAGE);
                } catch (CancellationException ignored) {
                    // Closing the tab or project is an expected cancellation path.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    showError(tree, JetTreeMarkBundle.message("errors.save_failed", messageOf(exception.getCause())));
                }
            }
        };
        startWorker(session, worker);
    }

    private static void setTreeOperationEnabled(Tree tree, JButton copyButton, JButton saveButton, boolean enabled) {
        tree.setEnabled(enabled);
        copyButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
    }

    private JPanel createTabComponent(String title, TabSession session) {
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        tabPanel.add(titleLabel);

        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setToolTipText(JetTreeMarkBundle.message("close_this_tab"));
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setBorderPainted(false);
        closeButton.setFocusable(false);
        closeButton.addActionListener(event -> closeSession(session));
        tabPanel.add(closeButton);
        return tabPanel;
    }

    private void closeSession(TabSession session) {
        if (session.closed) {
            return;
        }
        session.closed = true;
        for (SwingWorker<?, ?> worker : Set.copyOf(session.workers)) {
            worker.cancel(true);
        }
        session.stopTimers();
        int index = tabbedPane.indexOfComponent(session.container);
        if (index >= 0) {
            tabbedPane.removeTabAt(index);
        }
        sessions.remove(session);
    }

    private void startWorker(TabSession session, SwingWorker<?, ?> worker) {
        if (disposed || session.closed) {
            worker.cancel(true);
            return;
        }
        session.workers.add(worker);
        activeWorkers.add(worker);
        worker.execute();
    }

    private void finishWorker(TabSession session, SwingWorker<?, ?> worker) {
        session.workers.remove(worker);
        activeWorkers.remove(worker);
    }

    private Tree createTree(TabSession session, DefaultTreeModel treeModel) {
        Tree tree = new Tree(treeModel);
        tree.setCellRenderer(new CheckboxTreeCellRenderer());
        JPopupMenu popupMenu = new JPopupMenu(JetTreeMarkBundle.message("context_menu.title"));

        JMenuItem checkAll = menuItem("context_menu.check_all_children", event ->
                runBulkOperation(session, tree, selectedOrRoot(tree, treeModel), BulkMode.CHECK_ALL, true, true));
        JMenuItem checkFolders = menuItem("context_menu.check_all_folders", event ->
                runBulkOperation(session, tree, selectedOrRoot(tree, treeModel), BulkMode.FOLDERS_ONLY, true, true));
        JMenuItem uncheckAll = menuItem("context_menu.uncheck_all_children", event ->
                runBulkOperation(session, tree, selectedOrRoot(tree, treeModel), BulkMode.UNCHECK_ALL, true, false));
        JMenuItem checkWithoutChildren = menuItem("context_menu.check_without_children", event -> {
            CheckboxTreeNode node = selected(tree);
            if (node != null) {
                node.setCheckState(CheckboxTreeNode.CHECKED, false);
                tree.repaint();
            }
        });

        JMenu levelOperations = new JMenu(JetTreeMarkBundle.message("context_menu.level_operations"));
        levelOperations.add(menuItem("context_menu.check_only_folders_this_level", event ->
                runBulkOperation(session, tree, selectedOrRoot(tree, treeModel), BulkMode.FOLDERS_ONLY, false, true)));
        levelOperations.add(menuItem("context_menu.check_only_files_this_level", event ->
                runBulkOperation(session, tree, selectedOrRoot(tree, treeModel), BulkMode.FILES_ONLY, false, true)));
        levelOperations.addSeparator();
        levelOperations.add(menuItem("context_menu.check_all_children_this_level", event ->
                runBulkOperation(session, tree, selectedOrRoot(tree, treeModel), BulkMode.CHECK_ALL, false, true)));

        popupMenu.add(checkAll);
        popupMenu.add(checkFolders);
        popupMenu.add(uncheckAll);
        popupMenu.addSeparator();
        popupMenu.add(checkWithoutChildren);
        popupMenu.addSeparator();
        popupMenu.add(levelOperations);
        popupMenu.addSeparator();
        popupMenu.add(menuItem("context_menu.expand_all", event -> expandTree(session, tree)));
        popupMenu.add(menuItem("context_menu.collapse_all", event -> collapseTree(session, tree)));

        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if (event.getPath().getLastPathComponent() instanceof LazyDirectoryTreeNode lazyNode) {
                    if (session.expandingAll && !lazyNode.isLoaded()) {
                        throw new ExpandVetoException(event, "Lazy directories are skipped by Expand All");
                    }
                    loadLazyDirectory(session, tree, treeModel, lazyNode, event.getPath());
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                // Nothing to do.
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = tree.getRowForLocation(event.getX(), event.getY());
                if (row < 0) {
                    return;
                }
                TreePath path = tree.getPathForRow(row);
                Rectangle bounds = tree.getRowBounds(row);
                if (path != null && bounds != null && event.getX() <= bounds.x + 20
                        && path.getLastPathComponent() instanceof CheckboxTreeNode node) {
                    BulkMode mode = node.getCheckState() == CheckboxTreeNode.CHECKED
                            ? BulkMode.UNCHECK_ALL : BulkMode.CHECK_ALL;
                    if (node instanceof LazyDirectoryTreeNode lazyNode
                            && !lazyNode.isLoaded() && mode == BulkMode.CHECK_ALL) {
                        lazyNode.setCheckState(CheckboxTreeNode.CHECKED, false, true);
                        tree.repaint();
                        loadLazyDirectory(session, tree, treeModel, lazyNode, path);
                    } else {
                        runBulkOperation(session, tree, node, mode, true, true);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowPopup(event);
            }

            private void maybeShowPopup(MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int row = tree.getRowForLocation(event.getX(), event.getY());
                if (row >= 0) {
                    tree.setSelectionRow(row);
                }
                popupMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });
        return tree;
    }

    private static JMenuItem menuItem(String key, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(JetTreeMarkBundle.message(key));
        item.addActionListener(listener);
        return item;
    }

    private static CheckboxTreeNode selected(Tree tree) {
        TreePath path = tree.getSelectionPath();
        return path != null && path.getLastPathComponent() instanceof CheckboxTreeNode node ? node : null;
    }

    private static CheckboxTreeNode selectedOrRoot(Tree tree, DefaultTreeModel model) {
        CheckboxTreeNode selected = selected(tree);
        return selected != null ? selected : (CheckboxTreeNode) model.getRoot();
    }

    private void loadLazyDirectory(
            TabSession session,
            Tree tree,
            DefaultTreeModel model,
            LazyDirectoryTreeNode node,
            TreePath path
    ) {
        if (!node.beginLoading()) {
            return;
        }
        session.setControlsEnabled(false);

        SwingWorker<CheckboxTreeNode, Void> worker = new SwingWorker<>() {
            @Override
            protected CheckboxTreeNode doInBackground() {
                CheckboxTreeNode loadedRoot = new VirtualFileTreeBuilder(
                        node.directory(), this::isCancelled,
                        node.relativePath(), node.inheritedMatcher(), vcsIgnoreProvider).build();

                // Selection can change while the VFS traversal runs. Reapply if it did.
                LazyDirectoryTreeNode.DescendantSelection applied;
                do {
                    applied = node.descendantSelection();
                    applyDescendantSelection(loadedRoot, applied);
                } while (applied != node.descendantSelection() && !isCancelled());
                return loadedRoot;
            }

            @Override
            protected void done() {
                finishWorker(session, this);
                if (!session.isOpen()) {
                    return;
                }
                try {
                    boolean wasExpanded = tree.isExpanded(path);
                    node.finishLoading(get());
                    model.reload(node);
                    if (wasExpanded) {
                        SwingUtilities.invokeLater(() -> tree.expandPath(path));
                    }
                } catch (CancellationException ignored) {
                    // Closing the tab or project is an expected cancellation path.
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    node.loadingFailed();
                    model.reload(node);
                    LOGGER.log(Level.WARNING, "Unable to lazily load directory", exception.getCause());
                    showError(tree, JetTreeMarkBundle.message(
                            "errors.unable_to_load_directory", messageOf(exception.getCause())));
                } finally {
                    if (session.isOpen()) {
                        session.setControlsEnabled(true);
                    }
                }
            }
        };
        startWorker(session, worker);
    }

    private static void applyDescendantSelection(
            CheckboxTreeNode root,
            LazyDirectoryTreeNode.DescendantSelection selection
    ) {
        switch (selection) {
            case CHECKED -> root.checkAll(true);
            case UNCHECKED -> root.uncheckAll(true);
            case FOLDERS_ONLY -> root.checkOnlyFolders(true);
            case FILES_ONLY -> root.checkOnlyFiles(true);
        }

        Deque<CheckboxTreeNode> pending = new ArrayDeque<>();
        pending.addLast(root);
        while (!pending.isEmpty()) {
            CheckboxTreeNode current = pending.removeLast();
            if (current instanceof LazyDirectoryTreeNode lazyNode) {
                lazyNode.setDescendantSelection(selection);
            }
            for (int i = 0; i < current.getChildCount(); i++) {
                if (current.getChildAt(i) instanceof CheckboxTreeNode child) {
                    pending.addLast(child);
                }
            }
        }
    }

    private void runBulkOperation(
            TabSession session,
            Tree tree,
            CheckboxTreeNode start,
            BulkMode mode,
            boolean recursive,
            boolean includeStart
    ) {
        Deque<MutationFrame> pending = new ArrayDeque<>();
        if (includeStart) {
            pending.addLast(new MutationFrame(start, 0));
        } else {
            addMutationChildren(start, 1, pending);
        }
        session.setControlsEnabled(false);

        Timer timer = new Timer(1, null);
        timer.addActionListener(event -> {
            int processed = 0;
            while (processed++ < UI_BATCH_SIZE && !pending.isEmpty()) {
                MutationFrame frame = pending.removeLast();
                CheckboxTreeNode node = frame.node();
                node.setCheckState(mode.stateFor(node), false, false);
                if (node instanceof LazyDirectoryTreeNode lazyNode) {
                    lazyNode.setDescendantSelection(mode.descendantSelection());
                }
                if (recursive || frame.depth() == 0) {
                    addMutationChildren(node, frame.depth() + 1, pending);
                }
            }
            if (!pending.isEmpty()) {
                return;
            }

            session.stopTimer(timer);
            if (start.getParent() instanceof CheckboxTreeNode parent) {
                parent.updateParentCheckState();
            }
            if (session.isOpen()) {
                session.setControlsEnabled(true);
                tree.repaint();
            }
        });
        session.startTimer(timer);
    }

    private static void addMutationChildren(
            CheckboxTreeNode node,
            int depth,
            Deque<MutationFrame> pending
    ) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckboxTreeNode child) {
                pending.addLast(new MutationFrame(child, depth));
            }
        }
    }

    private void expandTree(TabSession session, Tree tree) {
        int maximumRows = positiveIntegerProperty("jettreemark.maxExpandedRows", DEFAULT_EXPAND_LIMIT);
        session.setControlsEnabled(false);
        session.expandingAll = true;
        int[] row = {0};
        Timer timer = new Timer(1, null);
        timer.addActionListener(event -> {
            int processed = 0;
            while (processed++ < UI_BATCH_SIZE && row[0] < tree.getRowCount() && row[0] < maximumRows) {
                tree.expandRow(row[0]++);
            }
            if (row[0] < tree.getRowCount() && row[0] >= maximumRows) {
                session.stopTimer(timer);
                session.expandingAll = false;
                session.setControlsEnabled(true);
                showWarning(tree, JetTreeMarkBundle.message("expand_limit_reached", maximumRows));
            } else if (row[0] >= tree.getRowCount()) {
                session.stopTimer(timer);
                session.expandingAll = false;
                session.setControlsEnabled(true);
            }
        });
        session.startTimer(timer);
    }

    private void collapseTree(TabSession session, Tree tree) {
        session.setControlsEnabled(false);
        int[] row = {tree.getRowCount() - 1};
        Timer timer = new Timer(1, null);
        timer.addActionListener(event -> {
            int processed = 0;
            while (processed++ < UI_BATCH_SIZE && row[0] >= 0) {
                tree.collapseRow(row[0]--);
                row[0] = Math.min(row[0], tree.getRowCount() - 1);
            }
            if (row[0] < 0) {
                session.stopTimer(timer);
                session.setControlsEnabled(true);
            }
        });
        session.startTimer(timer);
    }

    private static int positiveIntegerProperty(String key, int defaultValue) {
        int value = Integer.getInteger(key, defaultValue);
        return value > 0 ? value : defaultValue;
    }

    private void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                JetTreeMarkBundle.message("warning"), JOptionPane.WARNING_MESSAGE);
    }

    private void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                JetTreeMarkBundle.message("error"), JOptionPane.ERROR_MESSAGE);
    }

    private static String messageOf(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
                ? String.valueOf(throwable) : throwable.getMessage();
    }

    private final class TreeBuilderWorker extends SwingWorker<CheckboxTreeNode, Void> {
        private final TabSession session;
        private final VirtualFile rootFolder;

        private TreeBuilderWorker(TabSession session, VirtualFile rootFolder) {
            this.session = session;
            this.rootFolder = rootFolder;
        }

        @Override
        protected CheckboxTreeNode doInBackground() {
            return new VirtualFileTreeBuilder(rootFolder, this::isCancelled, vcsIgnoreProvider).build();
        }

        @Override
        protected void done() {
            finishWorker(session, this);
            if (isCancelled() || !session.isOpen()) {
                return;
            }
            try {
                CheckboxTreeNode rootNode = get();
                DefaultTreeModel model = new DefaultTreeModel(rootNode);
                Tree tree = createTree(session, model);
                session.setContent(createTreeViewPanel(session, tree, rootNode));
                session.setTitle(session.rootName + " (" + session.number + ")");
            } catch (CancellationException ignored) {
                // Closing the tab or project is an expected cancellation path.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException exception) {
                LOGGER.log(Level.WARNING, "Error building tree", exception.getCause());
                session.showError(JetTreeMarkBundle.message(
                        "errors.unable_to_load_directory", messageOf(exception.getCause())));
            }
        }
    }

    private final class TabSession {
        private final String rootName;
        private final int number;
        private final JPanel container = new JPanel(new BorderLayout());
        private final Set<SwingWorker<?, ?>> workers =
                Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<Timer> timers = Collections.newSetFromMap(new IdentityHashMap<>());
        private Tree tree;
        private JButton copyButton;
        private JButton saveButton;
        private boolean expandingAll;
        private boolean closed;

        private TabSession(String rootName, int number) {
            this.rootName = rootName;
            this.number = number;
        }

        private void showLoadingPanel() {
            JPanel loadingPanel = new JPanel(new BorderLayout());
            loadingPanel.add(new JLabel(
                    JetTreeMarkBundle.message("loading_of.text") + " " + rootName + "...",
                    SwingConstants.CENTER), BorderLayout.CENTER);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            loadingPanel.add(progressBar, BorderLayout.SOUTH);
            setContent(loadingPanel);

            String title = rootName + JetTreeMarkBundle.message("loading.text");
            tabbedPane.addTab(title, container);
            int index = tabbedPane.indexOfComponent(container);
            tabbedPane.setTabComponentAt(index, createTabComponent(title, this));
            tabbedPane.setSelectedIndex(index);
        }

        private void setContent(Component component) {
            container.removeAll();
            container.add(component, BorderLayout.CENTER);
            container.revalidate();
            container.repaint();
        }

        private void setTitle(String title) {
            int index = tabbedPane.indexOfComponent(container);
            if (index >= 0) {
                tabbedPane.setTitleAt(index, title);
                tabbedPane.setTabComponentAt(index, createTabComponent(title, this));
            }
        }

        private void showError(String message) {
            JLabel error = new JLabel(message, SwingConstants.CENTER);
            error.setForeground(JBColor.RED);
            setContent(error);
        }

        private void setControls(Tree tree, JButton copyButton, JButton saveButton) {
            this.tree = tree;
            this.copyButton = copyButton;
            this.saveButton = saveButton;
        }

        private void setControlsEnabled(boolean enabled) {
            if (tree != null) {
                tree.setEnabled(enabled);
            }
            if (copyButton != null) {
                copyButton.setEnabled(enabled);
            }
            if (saveButton != null) {
                saveButton.setEnabled(enabled);
            }
        }

        private boolean isOpen() {
            return !disposed && !closed && tabbedPane.indexOfComponent(container) >= 0;
        }

        private void startTimer(Timer timer) {
            if (isOpen()) {
                timers.add(timer);
                timer.start();
            }
        }

        private void stopTimer(Timer timer) {
            timer.stop();
            timers.remove(timer);
        }

        private void stopTimers() {
            for (Timer timer : Set.copyOf(timers)) {
                timer.stop();
            }
            timers.clear();
        }
    }

    private enum BulkMode {
        CHECK_ALL,
        UNCHECK_ALL,
        FOLDERS_ONLY,
        FILES_ONLY;

        private int stateFor(CheckboxTreeNode node) {
            return switch (this) {
                case CHECK_ALL, FILES_ONLY -> CheckboxTreeNode.CHECKED;
                case UNCHECK_ALL -> CheckboxTreeNode.UNCHECKED;
                case FOLDERS_ONLY -> node.isFolder()
                        ? CheckboxTreeNode.CHECKED : CheckboxTreeNode.UNCHECKED;
            };
        }

        private LazyDirectoryTreeNode.DescendantSelection descendantSelection() {
            return switch (this) {
                case CHECK_ALL -> LazyDirectoryTreeNode.DescendantSelection.CHECKED;
                case UNCHECK_ALL -> LazyDirectoryTreeNode.DescendantSelection.UNCHECKED;
                case FOLDERS_ONLY -> LazyDirectoryTreeNode.DescendantSelection.FOLDERS_ONLY;
                case FILES_ONLY -> LazyDirectoryTreeNode.DescendantSelection.FILES_ONLY;
            };
        }
    }

    private record MutationFrame(CheckboxTreeNode node, int depth) {
    }

}
