# JetTreeMark

![Build](https://github.com/HichemTab-tech/JetTreeMark/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

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

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "JetTreeMark"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/HichemTab-tech/JetTreeMark/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
