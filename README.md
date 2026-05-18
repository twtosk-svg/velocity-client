# Velocity Client

Velocity is a high-performance, streamproof Minecraft Fabric ghost client. It renders entirely outside the game via an external transparent overlay to bypass screen-capture detection.

> [!CAUTION]
> Windows and OpenGL only but it is planned to update to Vulkan and support linux distros in the future aswell as newer minecraft versions


## Build & Installation

### Requirements
*   Java Development Kit (JDK) 21 or higher
*   Gradle 8.x or higher
*   **Cubes Without Borders Mod:** [Modrinth Link](https://modrinth.com/mod/cubes-without-borders) because the overlay is external
*   **View Bobbing:** Recommended to be off so it looks cleaner can be found in Accessibility Settings

### Compiling
Clone the repository and compile via terminal:

```bash
./gradlew clean build
```

The compiled jar will be located in `build/libs/velocity-1.0.0.jar`. Copy it to your Fabric `mods` folder, run Minecraft on version 1.21.x, and press INSERT or ESC to toggle the interface.

---

