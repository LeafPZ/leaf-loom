<div align="center">

<h1>
    The Gradle development plugin for
    <a href="https://pzwiki.net/wiki/Leaf">
        <img src="https://github.com/LeafPZ.png" width="24px" alt="LeafPZ Icon">
        leaf
    </a>
</h1>

![License](https://img.shields.io/github/license/LeafPZ/leaf-loom?label=License)
![Gradle version](https://img.shields.io/badge/Gradle-9.7.1-teal?logo=gradle)
![Build status](https://github.com/LeafPZ/leaf-loom/actions/workflows/build.yml/badge.svg?branch=main&label=build)
![Code Size](https://img.shields.io/github/languages/code-size/LeafPZ/leaf-loom?label=Code%20Size)
![Maven status](https://img.shields.io/website?url=https%3A%2F%2Fmaven.aoqia.dev%2F&label=Maven)

</div>

A [Gradle][Gradle] plugin to set up a development environment for Project Zomboid mods. Primarily used in the Leaf
toolchain.

### Features

- Has built in support for tiny mappings (used with [Yarn][LeafYarn])
- Utilises the Vineflower and CFR decompilers to generate source code with comments.
- Designed to support modern versions of Project Zomboid (Tested with 41.78.16 and upwards, mostly including unstable
  versions)
- Built in support for IntelliJ IDEA, Eclipse and Visual Studio Code to generate run configurations for Zomboid.
- Loom targets the latest version of Gradle 7 or newer (though it is recommended to use the latest)

### Requirements

- Java 17 or newer

### Usage

To get started using Loom to develop your own mods, please follow the guide on [Setting up][FabricDocsSetup]. Despite
this guide originally being created for FabricMC/fabric, if you understand the concepts it presents, it also works here.

### Development

*This guide assumes you are using IntelliJ IDEA, other IDE's have not been tested; your experience may vary.*

<details open>
<summary>Debugging</summary>

1. Import as a Gradle project by opening the build.gradle
2. Create a Gradle run configuration to run the following tasks `build publishToMavenLocal -x test`. This will build
   Loom and publish to a local maven repo without running the test suite.
3. Prepare a project for using the local version of Loom:
    * A good starting point is to clone [leaf-example-mod][LeafExampleMod] into your working directory
    * Add `mavenLocal()` to the `repositories` block inside of `pluginManagement` in settings.gradle
    * Change the loom version to `<version>.local`. For example `id("leaf-loom") version "0.6.3.local"`
4. Create a Gradle run configuration:
    * Set the Gradle project path to the project you have just configured above
    * Set some tasks to run, such as `clean build` you can change these to suit your needs.
    * Add the run configuration you created earlier to the "Before Launch" section to rebuild loom each time you debug
5. You should now be able to run the configuration in debug mode, with working breakpoints.

</details>

### Special Thanks

- The entire [FabricMC team][FabricMC]

[FabricMC]: https://github.com/FabricMC
[FabricDocsSetup]: https://docs.fabricmc.net/develop/getting-started/setting-up
[Gradle]: https://gradle.org
[LeafExampleMod]: https://github.com/LeafPZ/leaf-example-mod
[LeafYarn]: https://github.com/LeafPZ/leaf-yarn
