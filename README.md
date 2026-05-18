# Velocity Client

Velocity is a high-performance, streamproof Minecraft Fabric ghost client. It renders entirely outside the game via an external transparent overlay to bypass client-side and screen-capture detection.

> [!CAUTION]
> Windows and OpenGL Only: Velocity is currently designed specifically for Windows platforms using OpenGL. Future support for Vulkan, Linux distributions, and newer Minecraft versions is planned.

---

## Key Features

*   **External Rendering:** Draws ESP, visual indicators, radars, and menus outside the game process via a transparent overlay window to ensure zero Minecraft OpenGL pipeline footprint.
*   **OBS Streamproof Mode:** Uses the Windows API (SetWindowDisplayAffinity) to hide the external overlay from capture cards, OBS, Discord, and recording software.
*   **Hardware Input Emulation:** Zero-allocation mouse injection using native Win32 API calls for humanized click timings, speed, and tracking patterns.
*   **Kinematic Bone ESP:** High-performance skeletal projections using rigid three-dimensional Euler rotations aligned with entity model structures.
*   **Persistent Configuration:** Auto-saves window coordinates, scale parameters, and menu layout settings inside local configuration files.

For a full breakdown of all client features, see the [Velocity Feature Wiki](WIKI.md).

---

## Build & Installation

### Requirements
*   Java Development Kit (JDK) 21 or higher
*   Gradle 8.x or higher
*   **Cubes Without Borders Mod:** [Modrinth Link](https://modrinth.com/mod/cubes-without-borders) is required to ensure perfect, borderless window-to-overlay synchronization.
*   **View Bobbing:** Must be turned OFF inside Minecraft's video settings to secure accurate 3D joint projection alignment.

### Compiling
Clone the repository and compile via terminal:

```bash
./gradlew clean build
```

The compiled jar will be located in `build/libs/velocity-1.0.0.jar`. Copy it to your Fabric `mods` folder, run Minecraft on version 1.21.x, and press INSERT or ESC to toggle the interface.

---

## Disclaimer

This repository is developed for educational purposes, low-level reverse engineering study, and API analysis only. Use responsibly.
