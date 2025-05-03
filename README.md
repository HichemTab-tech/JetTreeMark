# JetTreeMark

![Build](https://github.com/HichemTab-tech/JetTreeMark/workflows/Build/badge.svg)
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/HichemTab-tech/JetTreeMark/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/HichemTab-tech/JetTreeMark/blob/main/LICENSE)

---

<!-- Plugin description -->
## 🚀 What is JetTreeMark?

**JetTreeMark** is an IntelliJ IDEA plugin that lets
you instantly generate and copy a **beautiful tree view** of any folder you select inside your project.  
Perfect for sharing your project structure, documentation, code reviews, or just showing off your clean organization.
😉

---

## ✨ Features

- 📂 Generate a clean tree view of any selected folder
- 📋 One-click copy to clipboard
- 🎨 Flexible node control with context menu options:
  - Check only folders or only files
  - Check nodes without affecting their children
  - Expand or collapse all nodes
- ⚡ Ultra lightweight and seamless integration with the IDE

---

## 📷 Example Output

```
└── folder/
    ├── subfolder1/
    │   └── file15
    ├── subfolder2/
    │   ├── file56
    │   └── file88
    ├── file10
    └── file11
```

---

<!-- Plugin description end -->

## 🛠️ Installation

JetTreeMark is now available on the JetBrains Marketplace! 🎉

### Marketplace Installation

1. Open IntelliJ IDEA or any Jetbrains IDE.
2. Go to **Settings/Preferences → Plugins → Marketplace**.
3. Search for **JetTreeMark**.
4. Click **Install** and restart the IDE when prompted.

Or you can find it directly [here](https://plugins.jetbrains.com/plugin/27198-jettreemark).

### VS Code Availability

JetTreeMark is also available for **VS Code**!
🎉  
You can find it on the Visual Studio Code Marketplace [here](https://marketplace.visualstudio.com/items?itemName=HichemTab-tech.jettreemark).

### Manual Installation

1. [Download the latest release](https://github.com/HichemTab-tech/JetTreeMark/releases/latest)
2. In IntelliJ IDEA, go to **Settings/Preferences → Plugins → ⚙️ → Install plugin from disk...**
3. Select the downloaded `.zip` file
4. Restart the IDE when prompted

---

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/HichemTab-tech/JetTreeMark.git
   ```
2. Open it with IntelliJ IDEA
3. Run the build task:
   ```bash
   ./gradlew buildPlugin
   ```
4. Find your plugin zip inside `build/distributions/`
5. Install it manually as described above

---

## 🎯 How to Use

1. **Right-click** on any folder inside your Project tool window.
2. Select **"Show Tree View"** from the context menu.

   ![How to use the JetTreeMark plugin from folder context menu](meta/screenshot-1.png "Screenshot -JetTreeMark in context menu-")

3. A **JetTreeMark Tool Window** will open, showing the folder structure.
4. **Optionally**, select which files or folders you want to **exclude** from the final tree.

   ![How to use the JetTreeMark plugin to exclude nodes from the tree view result](meta/screenshot-2.png "Screenshot - filter nodes from tree results -")

5. **Right-click** on the tree view to access the **context menu** with additional options:
   - **Check Only Folders (All Levels)**: Select only directory nodes throughout the entire tree
   - **Check Only Files (All Levels)**: Select only file nodes throughout the entire tree
   - **Check All**: Select all nodes
   - **Uncheck All**: Deselect all nodes
   - **Check Without Children**: Select a node without affecting its children
   - **Level Operations**: Submenu with level-specific operations:
     - **Check Only Folders (This Level)**: Select only directory nodes at the current level
     - **Check Only Files (This Level)**: Select only file nodes at the current level
     - **Check All (This Level)**: Select all nodes at the current level
     - **Uncheck All (This Level)**: Deselect all nodes at the current level
   - **Expand All**: Expand all tree nodes
   - **Collapse All**: Collapse all tree nodes

6. Click the **"Copy Tree"** button at the bottom to copy the tree view to your clipboard. 🚀

That's it! You can now paste your clean project structure anywhere you like.

---

## ℹ️ About

JetTreeMark is based on the [IntelliJ Platform Plugin Template][template].  
It follows the [JetBrains Plugin UX guidelines][docs:plugin-description]
to ensure a clean and smooth experience inside your IDE.

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation

---

# 🌳 JetTreeMark — Draw your project structure, copy it instantly!
