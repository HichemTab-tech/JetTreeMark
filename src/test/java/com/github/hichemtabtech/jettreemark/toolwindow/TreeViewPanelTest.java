package com.github.hichemtabtech.jettreemark.toolwindow;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TreeViewPanel}
 */
public class TreeViewPanelTest {

    private TreeViewPanel treeViewPanel;
    private CheckboxTreeNode testRootNode;

    @Before
    public void setUp() {
        // Initialize the panel before each test
        treeViewPanel = new TreeViewPanel();

        // Create a test tree structure
        testRootNode = new CheckboxTreeNode("TestRoot", true);
        CheckboxTreeNode folderNode = new CheckboxTreeNode("TestFolder", true);
        CheckboxTreeNode fileNode1 = new CheckboxTreeNode("TestFile1", false);
        CheckboxTreeNode fileNode2 = new CheckboxTreeNode("TestFile2", false);

        testRootNode.add(folderNode);
        folderNode.add(fileNode1);
        folderNode.add(fileNode2);
    }

    @Test
    public void testGetContent() {
        // Get the content panel
        JPanel content = treeViewPanel.getContent();

        // Verify it's not null
        assertNotNull("Content panel should not be null", content);

        // Verify it has a BorderLayout
        assertTrue("Content panel should have BorderLayout", content.getLayout() instanceof BorderLayout);
    }

    @Test
    public void testAddFolderToTreeView() {
        // Create a mock VirtualFile
        VirtualFile mockFolder = Mockito.mock(VirtualFile.class);
        when(mockFolder.getName()).thenReturn("TestFolder");
        when(mockFolder.isDirectory()).thenReturn(true);
        when(mockFolder.getChildren()).thenReturn(new VirtualFile[0]);

        // Add the folder to the tree view
        treeViewPanel.addFolderToTreeView(mockFolder);

        // Since the actual tree building happens in a SwingWorker, we can't easily verify
        // the result directly. This test mainly ensures the method doesn't throw exceptions.

        // Verify that the mock was accessed
        verify(mockFolder, atLeastOnce()).getName();
    }

    @Test
    public void testCreateWelcomePanel() {
        // Get the content panel which should contain the welcome panel
        JPanel content = treeViewPanel.getContent();

        // The welcome panel should be the first tab in the tabbed pane
        // This is a bit tricky to test without exposing internal components

        // We can verify that the content panel is not empty
        assertTrue("Content panel should have components", content.getComponentCount() > 0);
    }

    @Test
    public void testGenerateTreeText() throws Exception {
        // Use reflection to access the private method
        Method generateTreeTextMethod = TreeViewPanel.class.getDeclaredMethod(
                "generateTreeText", DefaultMutableTreeNode.class, String.class, boolean.class);
        generateTreeTextMethod.setAccessible(true);

        // Call the method with our test root node
        String treeText = (String) generateTreeTextMethod.invoke(treeViewPanel, testRootNode, "", true);

        // Verify the text was generated
        assertNotNull("Tree text should not be null", treeText);
        assertTrue("Tree text should contain the root node name", treeText.contains("TestRoot"));
    }

    @Test
    public void testCheckboxTreeNodeFunctionality() {
        // Test that the root node is checked by default
        assertEquals("Root node should be checked by default", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());

        // Test checking/unchecking operations
        testRootNode.uncheckAll(true);
        assertEquals("Root node should be unchecked after uncheckAll", 
                CheckboxTreeNode.UNCHECKED, testRootNode.getCheckState());

        testRootNode.checkAll();
        assertEquals("Root node should be checked after checkAll", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());

        // Get the folder node
        CheckboxTreeNode folderNode = (CheckboxTreeNode) testRootNode.getChildAt(0);

        // Test check only folders
        testRootNode.uncheckAll(true);
        testRootNode.checkOnlyFolders();
        assertEquals("Root node should be checked after checkOnlyFolders", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());
        assertEquals("Folder node should be checked after checkOnlyFolders", 
                CheckboxTreeNode.CHECKED, folderNode.getCheckState());

        // Get a file node
        CheckboxTreeNode fileNode = (CheckboxTreeNode) folderNode.getChildAt(0);
        assertEquals("File node should be unchecked after checkOnlyFolders", 
                CheckboxTreeNode.UNCHECKED, fileNode.getCheckState());

        // Test check only files
        testRootNode.uncheckAll(true);
        testRootNode.checkOnlyFiles();
        assertEquals("Root node should be checked after checkOnlyFiles", 
                CheckboxTreeNode.CHECKED, testRootNode.getCheckState());
        assertEquals("Folder node should be checked after checkOnlyFiles", 
                CheckboxTreeNode.CHECKED, folderNode.getCheckState());
        assertEquals("File node should be checked after checkOnlyFiles", 
                CheckboxTreeNode.CHECKED, fileNode.getCheckState());
    }

    @Test
    public void testShouldIgnoreFile() throws Exception {
        // Get the TreeBuilderWorker class
        Class<?> treeBuilderWorkerClass = Class.forName(
                "com.github.hichemtabtech.jettreemark.toolwindow.TreeViewPanel$TreeBuilderWorker");

        // Create a mock VirtualFile for the root folder
        VirtualFile mockRootFolder = mock(VirtualFile.class);
        when(mockRootFolder.getName()).thenReturn("root");
        when(mockRootFolder.getPath()).thenReturn("/project");
        when(mockRootFolder.getChildren()).thenReturn(new VirtualFile[0]);

        // Create an instance of TreeBuilderWorker using its constructor
        Constructor<?> constructor = treeBuilderWorkerClass.getDeclaredConstructor(
                TreeViewPanel.class, VirtualFile.class);
        constructor.setAccessible(true);
        Object treeBuilderWorker = constructor.newInstance(treeViewPanel, mockRootFolder);

        // Get the shouldIgnoreFile method
        Method shouldIgnoreFileMethod = treeBuilderWorkerClass.getDeclaredMethod(
                "shouldIgnoreFile", VirtualFile.class);
        shouldIgnoreFileMethod.setAccessible(true);

        // Set up gitignorePatterns field with test patterns
        Field gitignorePatternsField = treeBuilderWorkerClass.getDeclaredField("gitignorePatterns");
        gitignorePatternsField.setAccessible(true);

        Set<String> testPatterns = new HashSet<>();
        testPatterns.add("*.txt");         // Wildcard pattern
        testPatterns.add("node_modules/"); // Directory pattern
        testPatterns.add("config.json");   // Exact match
        testPatterns.add("/logs");         // Path-specific pattern
        gitignorePatternsField.set(treeBuilderWorker, testPatterns);

        // Test cases
        // 1. File that matches wildcard pattern
        VirtualFile txtFile = mock(VirtualFile.class);
        when(txtFile.getName()).thenReturn("test.txt");
        when(txtFile.isDirectory()).thenReturn(false);
        when(txtFile.getPath()).thenReturn("/project/test.txt");
        boolean shouldIgnoreTxt = (boolean) shouldIgnoreFileMethod.invoke(treeBuilderWorker, txtFile);
        assertTrue("File matching wildcard pattern should be ignored", shouldIgnoreTxt);

        // 2. Directory that matches directory pattern
        VirtualFile nodeModulesDir = mock(VirtualFile.class);
        when(nodeModulesDir.getName()).thenReturn("node_modules");
        when(nodeModulesDir.isDirectory()).thenReturn(true);
        when(nodeModulesDir.getPath()).thenReturn("/project/node_modules");
        boolean shouldIgnoreNodeModules = (boolean) shouldIgnoreFileMethod.invoke(treeBuilderWorker, nodeModulesDir);
        assertTrue("Directory matching directory pattern should be ignored", shouldIgnoreNodeModules);

        // 3. File that matches exact pattern
        VirtualFile configFile = mock(VirtualFile.class);
        when(configFile.getName()).thenReturn("config.json");
        when(configFile.isDirectory()).thenReturn(false);
        when(configFile.getPath()).thenReturn("/project/config.json");
        boolean shouldIgnoreConfig = (boolean) shouldIgnoreFileMethod.invoke(treeBuilderWorker, configFile);
        assertTrue("File matching exact pattern should be ignored", shouldIgnoreConfig);

        // 4. File that matches path-specific pattern
        VirtualFile logsFile = mock(VirtualFile.class);
        when(logsFile.getName()).thenReturn("logs");
        when(logsFile.isDirectory()).thenReturn(true);
        when(logsFile.getPath()).thenReturn("/project/logs");
        boolean shouldIgnoreLogs = (boolean) shouldIgnoreFileMethod.invoke(treeBuilderWorker, logsFile);
        assertTrue("File matching path-specific pattern should be ignored", shouldIgnoreLogs);

        // 5. File that doesn't match any pattern
        VirtualFile regularFile = mock(VirtualFile.class);
        when(regularFile.getName()).thenReturn("regular.java");
        when(regularFile.isDirectory()).thenReturn(false);
        when(regularFile.getPath()).thenReturn("/project/regular.java");
        boolean shouldIgnoreRegular = (boolean) shouldIgnoreFileMethod.invoke(treeBuilderWorker, regularFile);
        assertFalse("File not matching any pattern should not be ignored", shouldIgnoreRegular);
    }

    @Test
    public void testFindGitIgnore() throws Exception {
        // Get the TreeBuilderWorker class
        Class<?> treeBuilderWorkerClass = Class.forName(
                "com.github.hichemtabtech.jettreemark.toolwindow.TreeViewPanel$TreeBuilderWorker");

        // Create a mock VirtualFile for the root folder
        VirtualFile mockRootFolder = mock(VirtualFile.class);
        when(mockRootFolder.getName()).thenReturn("root");
        when(mockRootFolder.getPath()).thenReturn("/project");
        when(mockRootFolder.getChildren()).thenReturn(new VirtualFile[0]);

        // Create an instance of TreeBuilderWorker using its constructor
        Constructor<?> constructor = treeBuilderWorkerClass.getDeclaredConstructor(
                TreeViewPanel.class, VirtualFile.class);
        constructor.setAccessible(true);
        Object treeBuilderWorker = constructor.newInstance(treeViewPanel, mockRootFolder);

        // Use reflection to access the private method
        Method findGitIgnoreMethod = treeBuilderWorkerClass.getDeclaredMethod(
                "findGitIgnore", VirtualFile.class, Set.class);
        findGitIgnoreMethod.setAccessible(true);

        // Create a mock VirtualFile for .gitignore
        VirtualFile mockGitignoreFile = mock(VirtualFile.class);
        when(mockGitignoreFile.isDirectory()).thenReturn(false);
        when(mockGitignoreFile.exists()).thenReturn(true);

        // Create gitignore content with various pattern types
        String gitignoreContent = 
                "# Comment line\n" +
                "*.txt\n" +
                "node_modules/\n" +
                "config.json\n" +
                "/logs\n" +
                "  # Comment with leading spaces\n" +
                "  \n" + // Empty line with spaces
                "dist/";

        InputStream gitignoreStream = new ByteArrayInputStream(
                gitignoreContent.getBytes(StandardCharsets.UTF_8));
        when(mockGitignoreFile.getInputStream()).thenReturn(gitignoreStream);

        // Create a mock folder that contains the .gitignore file
        VirtualFile mockFolder = mock(VirtualFile.class);
        when(mockFolder.findChild(".gitignore")).thenReturn(mockGitignoreFile);

        // Create a set to hold the patterns
        Set<String> patterns = new HashSet<>();

        // Call the method
        findGitIgnoreMethod.invoke(treeBuilderWorker, mockFolder, patterns);

        // Verify the patterns were correctly loaded
        assertEquals("Should have 5 patterns", 5, patterns.size());
        assertTrue("Should contain *.txt pattern", patterns.contains("*.txt"));
        assertTrue("Should contain node_modules/ pattern", patterns.contains("node_modules/"));
        assertTrue("Should contain config.json pattern", patterns.contains("config.json"));
        assertTrue("Should contain /logs pattern", patterns.contains("/logs"));
        assertTrue("Should contain dist/ pattern", patterns.contains("dist/"));

        // Verify comments and empty lines were skipped
        assertFalse("Should not contain comment lines", patterns.contains("# Comment line"));
        assertFalse("Should not contain comment with leading spaces", 
                patterns.contains("  # Comment with leading spaces"));
        assertFalse("Should not contain empty lines", patterns.contains(""));
        assertFalse("Should not contain empty lines with spaces", patterns.contains("  "));
    }

    @Test
    public void testLoadGitignorePatternsForFolder() throws Exception {
        // Get the TreeBuilderWorker class
        Class<?> treeBuilderWorkerClass = Class.forName(
                "com.github.hichemtabtech.jettreemark.toolwindow.TreeViewPanel$TreeBuilderWorker");

        // Create a mock VirtualFile for the root folder
        VirtualFile mockRootFolder = mock(VirtualFile.class);
        when(mockRootFolder.getName()).thenReturn("root");
        when(mockRootFolder.getPath()).thenReturn("/project");
        when(mockRootFolder.getChildren()).thenReturn(new VirtualFile[0]);

        // Create an instance of TreeBuilderWorker using its constructor
        Constructor<?> constructor = treeBuilderWorkerClass.getDeclaredConstructor(
                TreeViewPanel.class, VirtualFile.class);
        constructor.setAccessible(true);
        Object treeBuilderWorker = constructor.newInstance(treeViewPanel, mockRootFolder);

        // Use reflection to access the private method
        Method loadPatternsMethod = treeBuilderWorkerClass.getDeclaredMethod(
                "loadGitignorePatternsForFolder", VirtualFile.class);
        loadPatternsMethod.setAccessible(true);

        // Create a mock VirtualFile for .gitignore
        VirtualFile mockGitignoreFile = mock(VirtualFile.class);
        when(mockGitignoreFile.isDirectory()).thenReturn(false);
        when(mockGitignoreFile.exists()).thenReturn(true);

        // Create gitignore content
        String gitignoreContent = "*.txt\nnode_modules/\nconfig.json";
        InputStream gitignoreStream = new ByteArrayInputStream(
                gitignoreContent.getBytes(StandardCharsets.UTF_8));
        when(mockGitignoreFile.getInputStream()).thenReturn(gitignoreStream);

        // Create a mock folder that contains the .gitignore file
        VirtualFile mockFolder = mock(VirtualFile.class);
        when(mockFolder.findChild(".gitignore")).thenReturn(mockGitignoreFile);

        // Call the method
        @SuppressWarnings("unchecked")
        Set<String> patterns = (Set<String>) loadPatternsMethod.invoke(treeBuilderWorker, mockFolder);

        // Verify the patterns were correctly loaded
        assertEquals("Should have 3 patterns", 3, patterns.size());
        assertTrue("Should contain *.txt pattern", patterns.contains("*.txt"));
        assertTrue("Should contain node_modules/ pattern", patterns.contains("node_modules/"));
        assertTrue("Should contain config.json pattern", patterns.contains("config.json"));

        // Test with a folder that doesn't have a .gitignore file
        VirtualFile mockFolderNoGitignore = mock(VirtualFile.class);
        when(mockFolderNoGitignore.findChild(".gitignore")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Set<String> emptyPatterns = (Set<String>) loadPatternsMethod.invoke(treeBuilderWorker, mockFolderNoGitignore);

        // Verify an empty set is returned
        assertTrue("Should return an empty set when no .gitignore file exists", emptyPatterns.isEmpty());
    }
}
