# JetTreeMark

![Build](https://github.com/HichemTab-tech/JetTreeMark/workflows/Build/badge.svg)
[![Version](https://img.shields.io/badge/version-0.0.1-blue.svg)](https://github.com/HichemTab-tech/JetTreeMark/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/HichemTab-tech/JetTreeMark/blob/main/LICENSE)

## Description

JetTreeMark is an IntelliJ IDEA plugin that displays a copiable tree view of folders and files when selecting a folder in the project tool window. This makes it easy to share your project structure with others.

Example of the tree view:
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

## Features

- Display a tree view of folders and files when selecting a folder in the project tool window
- Copy the tree view to clipboard
- Customize the tree view display

<!-- Plugin description -->
JetTreeMark is a plugin for IntelliJ IDEA that generates a copiable tree view representation of your project's folder structure. When you select a folder in the project tool window, JetTreeMark displays a formatted tree view that you can easily copy and share with others.
<!-- Plugin description end -->

## Installation

> **Note:** The plugin is not yet available on the JetBrains Marketplace. You can install it manually from the GitHub releases.

### Manual Installation

1. Download the [latest release](https://github.com/HichemTab-tech/JetTreeMark/releases/latest) from GitHub
2. In IntelliJ IDEA, go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
3. Select the downloaded ZIP file
4. Restart the IDE when prompted

### Build from Source

1. Clone the repository: `git clone https://github.com/HichemTab-tech/JetTreeMark.git`
2. Open the project in IntelliJ IDEA
3. Build the plugin: `./gradlew buildPlugin`
4. The plugin ZIP file will be available in `build/distributions/`
5. Install the plugin as described in the Manual Installation section


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
