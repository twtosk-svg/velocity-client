# Velocity Feature Wiki

This document provides a detailed breakdown of the features and modules available inside the Velocity Client.

---

## Combat & Triggerbot

*   **Dual-Speed AimAssist:** Asymptotic smoothing aimbot using two distinct algorithms (Normal Speed and Complement Speed) to pull towards targets humanly within horizontal/vertical FOV thresholds.
*   **Dynamic TriggerBot:** Emulates physical mouse clicks using two distinct engines:
    *   *Legit Engine:* Uses native Win32 hardware calls to bypass server-side click check heuristics.
    *   *Zero-Tick Engine:* Integrates native client-side packet interceptors for near-instant execution.
*   **Smart Criticals:** Prevents attacks unless falling to secure maximum critical strikes on active targets.
*   **Auto Shield Breaker:** Swaps to axes instantly to disable opponent shields with customizable delay timers.

---

## Rendering & Visuals (ESP)

*   **Rigid Skeleton ESP:** Draws mathematically accurate forward-kinematics joints with customizable thickness and black/white outline modes.
*   **Look-At Eye Tracers:** Renders 3D vectors pointing outwards from player eyes, visualizing where targets are looking or aiming.
*   **Dynamic Health & Absorption:** Renders health bars with a classical/V2 pink glowing look alongside dynamic golden absorption counters.
*   **Armor Fractions & Item ESP:** Scales armor values and displays active equipment alongside custom item icons.
*   **Scanners (Ore & Light ESP):** Scans and caches blocks to highlight ore veins and light levels through solid walls.

---

## Radars & Overlays

*   **Admin Radar:** Renders Spectator and Creative administrators in a bordered table showing latency (ping) and hides itself completely when no admins are present.
*   **Player Radar:** Sorts nearby players by proximity, highlighting friends in customizable colors.

---

## Utilities & Configuration

*   **Raycasted Friend System:** Registers friends manually or in-game via middle-click raycasts up to 200 blocks away, completely bypassing the default 3-block vanilla interaction limit.
*   **Heal Keybind:** Swaps to healing potions, throws them with random switch/restore tick delays, and returns to your main hand slot.
*   **Hover Refill:** Automatically shifts healing potions to your hotbar when hovering over them inside open inventory screens.
