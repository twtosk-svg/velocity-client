package com.velocity.gui;

import com.velocity.module.combat.TriggerBot;
import com.velocity.module.combat.ShieldBreaker;
import com.velocity.module.render.LightSourceEsp;
import com.velocity.module.render.OreEsp;

import com.velocity.config.AimAssistSettings;
import com.velocity.config.UtilitySettings;
import com.velocity.config.LightSourceEspSettings;
import com.velocity.config.OreEspSettings;
import com.velocity.config.EspSettings;
import com.velocity.core.InputManager;
import com.velocity.core.LightDebugManager;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

public class MenuUI {

    // ── ESP ImGui wrappers ────────────────────────────────────────────────────
    private static final ImBoolean espEnabledImBool = new ImBoolean(true);
    private static final ImBoolean espBoxesEnabledImBool = new ImBoolean(true);
    private static final ImBoolean nametagsEnabledImBool = new ImBoolean(true);
    private static final ImBoolean equipmentEspBool = new ImBoolean(false);
    private static final ImBoolean healthbarsEnabledImBool = new ImBoolean(true);
    private static final ImBoolean distanceEnabledImBool = new ImBoolean(true);
    private static float[] maxDistanceArr = { 120f };
    private static final ImBoolean shieldEspEnabledImBool = new ImBoolean(true);
    private static final ImInt shieldEspModeImInt = new ImInt(0);
    private static final String[] shieldEspModes = { "Outline", "Fill", "Both" };
    private static final ImInt skeletonOutlineModeImInt = new ImInt(1);
    private static final String[] skeletonOutlineModes = { "None", "Black", "White" };
    private static final ImBoolean tracersEnabledImBool = new ImBoolean(false);
    private static final ImBoolean eyeTraceEnabledImBool = new ImBoolean(false);
    private static float[] eyeTraceLengthArr = { 2.0f };
    private static final ImBoolean streamproofEnabledImBool = new ImBoolean(true);

    private static final ImInt healthPositionImInt = new ImInt(1);
    private static final String[] healthPositions = { "Left", "Right", "Top", "Bottom" };

    private static float[] healthbarThicknessArr = { 4f };
    private static final ImBoolean healthV2EnabledImBool = new ImBoolean(false);
    private static float[] healthV2GlowStrengthArr = { 4f };

    private static final ImBoolean armorBarsEnabledImBool = new ImBoolean(true);
    private static final ImBoolean absorptionBarsEnabledImBool = new ImBoolean(true);
    private static final ImBoolean advancedNametagsEnabledImBool = new ImBoolean(true);
    
    // ── Friends & Admin ImGui wrappers ────────────────────────────────────────
    private static final ImBoolean friendsSystemEnabledBool = new ImBoolean(true);
    private static final ImBoolean friendEspOverrideBool = new ImBoolean(true);
    private static final ImBoolean adminOverlayEnabledBool = new ImBoolean(true);
    private static final ImBoolean playerRadarEnabledBool = new ImBoolean(false);
    private static final ImBoolean middleClickFriendBool = new ImBoolean(true);
    private static final imgui.type.ImString friendInputBuffer = new imgui.type.ImString(64);

    // ── Utility ImGui wrappers ────────────────────────────────────────────────
    private static final ImBoolean hoverRefillBool = new ImBoolean(false);
    private static final ImBoolean healKeybindBool = new ImBoolean(false);
    private static final int[] healSwitchMinArr = new int[]{1};
    private static final int[] healSwitchMaxArr = new int[]{2};
    private static final int[] healRestoreMinArr = new int[]{1};
    private static final int[] healRestoreMaxArr = new int[]{3};
    private static boolean waitingForHealKey = false;
    private static boolean focusWaitingForKey = false;
    private static boolean waitingForOreKey = false;

    // ── Aim Assist ImGui wrappers ──────────────────────────────────────────────
    private static final ImBoolean aimEnabledBool = new ImBoolean(false);
    private static final ImBoolean aimVisCheckBool = new ImBoolean(true);
    private static final ImBoolean aimFovCircleBool = new ImBoolean(true);
    private static final ImBoolean aimGlowBool = new ImBoolean(true);
    private static final ImBoolean aimZombiesBool = new ImBoolean(true);
    private static final ImBoolean aimVillagersBool = new ImBoolean(true);
    private static final ImBoolean aimPlayersBool = new ImBoolean(true);
    private static final ImBoolean aimTeammatesBool = new ImBoolean(false);
    private static final ImBoolean aimBatsBool = new ImBoolean(false);
    private static final ImBoolean aimRabbitsBool = new ImBoolean(false);
    private static final ImBoolean aimFreeMoveBool = new ImBoolean(true);
    private static final ImBoolean aimDeadzoneDirBool = new ImBoolean(true);
    private static final ImBoolean mouse4AimBool = new ImBoolean(false);
    private static float[] aimMaxDistArr = { 6.0f };


    // Nametag Glow
    private static float[] aimDurationArr = { 500f };
    private static float[] aimFovArr = { 60f };
    private static float[] aimSpeedYawArr = { 45f };
    private static float[] aimComplimentYawArr = { 15f };
    private static float[] aimSpeedPitchArr = { 45f };
    private static float[] aimComplimentPitchArr = { 15f };
    private static float[] aimDeadzoneArr = { 2f };

    // ── TriggerBot ImGui wrappers ─────────────────────────────────────────────
    private static final ImBoolean triggerBotBool = new ImBoolean(false);
    private static float[] triggerMinDelayArr = { 50f };
    private static final float[] triggerMaxDelayArr = new float[]{ 150f };
    private static final ImBoolean triggerSmartCritBool = new ImBoolean(false);
    private static final ImBoolean triggerLowHpBool = new ImBoolean(false);
    private static final ImBoolean triggerWeaponOnlyBool = new ImBoolean(false);
    

    private static final ImInt triggerModeInt = new ImInt(0);
    private static final String[] triggerModes = new String[]{ "Legit (Win32)", "Zero-Tick (Raycast)" };

    private static final ImInt triggerClickMethodInt = new ImInt(0);
    private static final String[] triggerClickMethods = new String[]{ "Win32", "Internal", "Mix" };

    // ShieldBreaker
    private static final ImBoolean shieldBreakerBool = new ImBoolean(false);
    private static final float[] shieldCooldownArr = new float[]{15f};

    public static void draw(float menuAlpha, float sliderWidthMult) {

        // Capture the bind key while in menu (must happen before ImGui.begin)
        if (waitingForHealKey) {
            int captured = InputManager.pollForKeyPress();
            if (captured == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                UtilitySettings.healKeybindKey = -1;
                waitingForHealKey = false;
            } else if (captured >= 0) {
                UtilitySettings.healKeybindKey = captured;
                waitingForHealKey = false;
            }
        }
        if (focusWaitingForKey) {
            int captured = InputManager.pollForKeyPress();
            if (captured == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                AimAssistSettings.focusKeybindKey = -1;
                focusWaitingForKey = false; 
            } else if (captured >= 0) {
                AimAssistSettings.focusKeybindKey = captured;
                focusWaitingForKey = false;
            }
        }
        if (waitingForOreKey) {
            int captured = InputManager.pollForKeyPress();
            if (captured == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                OreEspSettings.oreToggleKey = -1;
                waitingForOreKey = false;
            } else if (captured >= 0) {
                OreEspSettings.oreToggleKey = captured;
                waitingForOreKey = false;
            }
        }

        ImGui.setNextWindowBgAlpha(0.92f);
        ImGui.setNextWindowSize(650, 680, ImGuiCond.FirstUseEver);

        ImGui.begin("  ⚡ Velocity 1.4", imgui.flag.ImGuiWindowFlags.AlwaysVerticalScrollbar);

        if (ImGui.beginTabBar("Modules")) {
            if (ImGui.beginTabItem("Combat")) {
ImGui.textDisabled("Aim Assist");
                ImGui.spacing();
                ImGui.spacing();
                aimEnabledBool.set(AimAssistSettings.enabled);
                if (ImGui.checkbox("Enable Aim Assist", aimEnabledBool)) {
                    AimAssistSettings.enabled = aimEnabledBool.get();
                }

                if (AimAssistSettings.enabled) {
                    ImGui.separator();

                    aimZombiesBool.set(AimAssistSettings.targetZombies);
                    if (ImGui.checkbox("Target Zombies##aim", aimZombiesBool)) {
                        AimAssistSettings.targetZombies = aimZombiesBool.get();
                    }

                    ImGui.sameLine();
                    aimVillagersBool.set(AimAssistSettings.targetVillagers);
                    if (ImGui.checkbox("Target Villagers##aim", aimVillagersBool)) {
                        AimAssistSettings.targetVillagers = aimVillagersBool.get();
                    }

                    ImGui.sameLine();
                    aimPlayersBool.set(AimAssistSettings.targetPlayers);
                    if (ImGui.checkbox("Target Players##aim", aimPlayersBool)) {
                        AimAssistSettings.targetPlayers = aimPlayersBool.get();
                    }

                    if (AimAssistSettings.targetPlayers) {
                        ImGui.sameLine();
                        aimTeammatesBool.set(AimAssistSettings.ignoreTeammates);
                        if (ImGui.checkbox("Ignore Teammates##aim", aimTeammatesBool)) {
                            AimAssistSettings.ignoreTeammates = aimTeammatesBool.get();
                        }
                        if (ImGui.isItemHovered()) {
                            ImGui.setTooltip("Automatically skips players on the same scoreboard team for Aim Assist and TriggerBot.");
                        }
                    }

                    aimBatsBool.set(AimAssistSettings.targetBats);
                    if (ImGui.checkbox("Target Bats##aim", aimBatsBool)) {
                        AimAssistSettings.targetBats = aimBatsBool.get();
                    }

                    ImGui.sameLine();
                    aimRabbitsBool.set(AimAssistSettings.targetRabbits);
                    if (ImGui.checkbox("Target Rabbits##aim", aimRabbitsBool)) {
                        AimAssistSettings.targetRabbits = aimRabbitsBool.get();
                    }

                    aimMaxDistArr[0] = AimAssistSettings.maxDistance;
                    ImGui.pushItemWidth(250f);
                    if (ImGui.sliderFloat("Max Target Distance##aim", aimMaxDistArr, 1.0f, 15.0f, "%.1f blocks")) {
                        AimAssistSettings.maxDistance = aimMaxDistArr[0];
                    }
                    ImGui.popItemWidth();

                    aimDurationArr[0] = AimAssistSettings.assistDurationMs;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Duration (ms)##aim", aimDurationArr, 1f, 2000f, "%.0f");
                    ImGui.popItemWidth();
                    AimAssistSettings.assistDurationMs = (int) aimDurationArr[0];

                    aimFovArr[0] = AimAssistSettings.fovDegrees;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("FOV (deg)##aim", aimFovArr, 10f, 360f, "%.0f");
                    ImGui.popItemWidth();
                    AimAssistSettings.fovDegrees = aimFovArr[0];

                    aimSpeedYawArr[0] = AimAssistSettings.speedYaw;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Speed Yaw##aim", aimSpeedYawArr, 1f, 100f, "%.1f");
                    ImGui.popItemWidth();
                    AimAssistSettings.speedYaw = aimSpeedYawArr[0];

                    aimComplimentYawArr[0] = AimAssistSettings.complimentYaw;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Compliment Yaw##aim", aimComplimentYawArr, 0f, 100f, "%.1f");
                    ImGui.popItemWidth();
                    AimAssistSettings.complimentYaw = aimComplimentYawArr[0];

                    aimSpeedPitchArr[0] = AimAssistSettings.speedPitch;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Speed Pitch##aim", aimSpeedPitchArr, 1f, 100f, "%.1f");
                    ImGui.popItemWidth();
                    AimAssistSettings.speedPitch = aimSpeedPitchArr[0];

                    aimComplimentPitchArr[0] = AimAssistSettings.complimentPitch;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Compliment Pitch##aim", aimComplimentPitchArr, 0f, 100f, "%.1f");
                    ImGui.popItemWidth();
                    AimAssistSettings.complimentPitch = aimComplimentPitchArr[0];

                    aimDeadzoneArr[0] = AimAssistSettings.deadzonePixels;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Deadzone (px)##aim", aimDeadzoneArr, 0f, 20f, "%.1f");
                    ImGui.popItemWidth();
                    AimAssistSettings.deadzonePixels = aimDeadzoneArr[0];

                    ImGui.spacing();

                    aimFreeMoveBool.set(AimAssistSettings.freeMovement);
                    if (ImGui.checkbox("Free Movement##aim", aimFreeMoveBool)) {
                        AimAssistSettings.freeMovement = aimFreeMoveBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Allows free vertical movement while locking horizontal Yaw.");
                    }

                    aimDeadzoneDirBool.set(AimAssistSettings.deadzoneDirectionCheck);
                    if (ImGui.checkbox("Deadzone Direction Check##aim", aimDeadzoneDirBool)) {
                        AimAssistSettings.deadzoneDirectionCheck = aimDeadzoneDirBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Limits corrections when actively tracking in the correct direction.");
                    }

                    aimVisCheckBool.set(AimAssistSettings.visibilityCheck);
                    if (ImGui.checkbox("Visibility Check##aim", aimVisCheckBool)) {
                        AimAssistSettings.visibilityCheck = aimVisCheckBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Requires direct line of sight to assist aim.");
                    }

                    mouse4AimBool.set(AimAssistSettings.mouse4Aim);
                    if (ImGui.checkbox("Require Mouse4##aim", mouse4AimBool)) {
                        AimAssistSettings.mouse4Aim = mouse4AimBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Aim Assist only activates while Mouse4 is held down.");
                    }



                    ImGui.spacing();

                    String focusLabel = focusWaitingForKey 
                        ? "[ Press a key... ]"
                        : (AimAssistSettings.focusKeybindKey < 0 
                            ? "[ Unbound ]" 
                            : "[ " + InputManager.getKeyName(AimAssistSettings.focusKeybindKey) + " ]");
                    if (ImGui.button(focusLabel + "##focuskey")) {
                        focusWaitingForKey = true;
                    }
                    ImGui.sameLine();
                    ImGui.text("Focus Mode Keybind");
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Press this to lock the aimbot onto the current target exclusively.");
                    }

                    ImGui.spacing();

                    aimFovCircleBool.set(AimAssistSettings.fovCircleEnabled);
                    if (ImGui.checkbox("Show FOV Circle##aim", aimFovCircleBool)) {
                        AimAssistSettings.fovCircleEnabled = aimFovCircleBool.get();
                    }
                    if (AimAssistSettings.fovCircleEnabled) {
                        ImGui.sameLine();
                        ImGui.pushItemWidth(200f);
                        ImGui.colorEdit4("##fovCircleColor", AimAssistSettings.fovCircleColor);
                        ImGui.popItemWidth();
                    }

                    aimGlowBool.set(AimAssistSettings.nametagGlowEnabled);
                    if (ImGui.checkbox("Target Nametag Glow##aim", aimGlowBool)) {
                        AimAssistSettings.nametagGlowEnabled = aimGlowBool.get();
                    }
                    if (AimAssistSettings.nametagGlowEnabled) {
                        ImGui.sameLine();
                        ImGui.pushItemWidth(200f);
                        ImGui.colorEdit4("##glowColor", AimAssistSettings.nametagGlowColor);
                        ImGui.popItemWidth();
                    }
                }
                ImGui.separator();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("TriggerBot")) {
ImGui.textDisabled("TriggerBot");
                ImGui.spacing();
                ImGui.spacing();
                triggerBotBool.set(AimAssistSettings.triggerBotEnabled);
                if (ImGui.checkbox("Enable TriggerBot", triggerBotBool)) {
                    AimAssistSettings.triggerBotEnabled = triggerBotBool.get();
                }

                if (AimAssistSettings.triggerBotEnabled) {
                    ImGui.separator();

                    // ImGui Mode dropdown
                    triggerModeInt.set(AimAssistSettings.triggerBotVersion);
                    ImGui.pushItemWidth(250f);
                    if (ImGui.combo("TriggerBot Engine", triggerModeInt, triggerModes, triggerModes.length)) {
                        AimAssistSettings.triggerBotVersion = triggerModeInt.get();
                    }
                    ImGui.popItemWidth();
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Legit = Win32 OS-Level safe mouse clicks.\nZero-Tick = Server-Level native raw interceptor (EXTREMELY FAST).");
                    }
                    
                    ImGui.spacing();

                    triggerClickMethodInt.set(AimAssistSettings.triggerBotClickMethod);
                    ImGui.pushItemWidth(250f);
                    if (ImGui.combo("Click Method", triggerClickMethodInt, triggerClickMethods, triggerClickMethods.length)) {
                        AimAssistSettings.triggerBotClickMethod = triggerClickMethodInt.get();
                    }
                    ImGui.popItemWidth();
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Win32: Hardware click via OS\nInternal: Game logic click\nMix: Both Win32 and Internal");
                    }
                    


                    ImGui.spacing();

                    triggerMinDelayArr[0] = AimAssistSettings.triggerBotMinDelayMs;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Min Delay (ms)", triggerMinDelayArr, 0f, 500f, "%.0f");
                    ImGui.popItemWidth();
                    AimAssistSettings.triggerBotMinDelayMs = (int) triggerMinDelayArr[0];

                    triggerMaxDelayArr[0] = AimAssistSettings.triggerBotMaxDelayMs;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("Max Delay (ms)", triggerMaxDelayArr, 0f, 500f, "%.0f");
                    ImGui.popItemWidth();
                    AimAssistSettings.triggerBotMaxDelayMs = (int) triggerMaxDelayArr[0];
                    
                    triggerSmartCritBool.set(AimAssistSettings.triggerBotSmartCrit);
                    if (ImGui.checkbox("Smart Crit##trigger", triggerSmartCritBool)) {
                        AimAssistSettings.triggerBotSmartCrit = triggerSmartCritBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Only attack while falling in mid-air to ensure critical hits.");
                    }
                    
                    triggerLowHpBool.set(AimAssistSettings.triggerBotLowHpOverride);
                    if (ImGui.checkbox("Low HP Auto-kill##trigger", triggerLowHpBool)) {
                        AimAssistSettings.triggerBotLowHpOverride = triggerLowHpBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Attack early if current weapon charge damage is enough to kill.");
                    }

                    triggerWeaponOnlyBool.set(AimAssistSettings.triggerBotWeaponOnly);
                    if (ImGui.checkbox("Weapon Only##trigger", triggerWeaponOnlyBool)) {
                        AimAssistSettings.triggerBotWeaponOnly = triggerWeaponOnlyBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Only trigger the bot when holding a Sword, Axe, Mace, or Trident.");
                    }
                    
                    ImGui.separator();
                    // ShieldBreaker
                    shieldBreakerBool.set(AimAssistSettings.shieldBreakerEnabled);
                    if (ImGui.checkbox("Auto Shield Breaker (Axe)##trigger", shieldBreakerBool)) {
                        AimAssistSettings.shieldBreakerEnabled = shieldBreakerBool.get();
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Immediately shatters enemy shields when you hold an Axe.");
                    }
                    if (AimAssistSettings.shieldBreakerEnabled) {
                        shieldCooldownArr[0] = AimAssistSettings.shieldBreakerMinCooldown;
                        ImGui.pushItemWidth(250f);
                        ImGui.sliderFloat("Min Switch Cooldown (%)", shieldCooldownArr, 0f, 100f, "%.0f%%");
                        ImGui.popItemWidth();
                        AimAssistSettings.shieldBreakerMinCooldown = shieldCooldownArr[0];
                    }
                    ImGui.spacing();
                    ImGui.separator();
ImGui.textDisabled("Shield ESP");
                    ImGui.spacing();
                    shieldEspEnabledImBool.set(EspSettings.shieldEspEnabled);
                    if (ImGui.checkbox("Enable Shield Indicator", shieldEspEnabledImBool)) {
                        EspSettings.shieldEspEnabled = shieldEspEnabledImBool.get();
                    }
                    if (EspSettings.shieldEspEnabled) {
                        ImGui.sameLine();
                        float[] shieldCol = EspSettings.shieldColor;
                        if (ImGui.colorEdit4("Shield Color", shieldCol)) {
                            System.arraycopy(shieldCol, 0, EspSettings.shieldColor, 0, 4);
                        }
                        
                        float[] shieldThicknessArr = { EspSettings.shieldEspThickness };
                        ImGui.pushItemWidth(150f);
                        if (ImGui.sliderFloat("Shield Thickness##esp", shieldThicknessArr, 1.0f, 10.0f, "%.1f px")) {
                            EspSettings.shieldEspThickness = shieldThicknessArr[0];
                        }
                        ImGui.popItemWidth();
                        
                        shieldEspModeImInt.set(EspSettings.shieldEspMode);
                        ImGui.pushItemWidth(150f);
                        if (ImGui.combo("Shield Mode", shieldEspModeImInt, shieldEspModes)) {
                            EspSettings.shieldEspMode = shieldEspModeImInt.get();
                        }
                        ImGui.popItemWidth();
                    }
                    
                    ImGui.spacing();
                }
                ImGui.separator();

                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("ESP")) {
ImGui.textDisabled("ESP");
                ImGui.spacing();
                ImGui.spacing();
                espEnabledImBool.set(EspSettings.espEnabled);
                if (ImGui.checkbox("Master ESP Toggle", espEnabledImBool)) {
                    EspSettings.espEnabled = espEnabledImBool.get();
                }

                streamproofEnabledImBool.set(EspSettings.streamproofEnabled);
                if (ImGui.checkbox("OBS Streamproof Mode", streamproofEnabledImBool)) {
                    EspSettings.streamproofEnabled = streamproofEnabledImBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Hides the overlay from recording software. Disable to record the ESP.");
                }

                if (EspSettings.espEnabled) {
                    ImGui.separator();
                    espBoxesEnabledImBool.set(EspSettings.espBoxesEnabled);
                    if (ImGui.checkbox("Enable Boxes", espBoxesEnabledImBool)) {
                        EspSettings.espBoxesEnabled = espBoxesEnabledImBool.get();
                    }
                    if (EspSettings.espBoxesEnabled) {
                        ImGui.sameLine();
                        float[] boxCol = EspSettings.boxColor;
                        if (ImGui.colorEdit4("Box Color", boxCol)) {
                            System.arraycopy(boxCol, 0, EspSettings.boxColor, 0, 4);
                        }
                    }

                    nametagsEnabledImBool.set(EspSettings.nametagsEnabled);
                    if (ImGui.checkbox("Enable Nametags", nametagsEnabledImBool)) {
                        EspSettings.nametagsEnabled = nametagsEnabledImBool.get();
                    }
                    ImGui.sameLine();
advancedNametagsEnabledImBool.set(EspSettings.advancedNametagsEnabled);
                    if (ImGui.checkbox("Advanced Nametags (Team Tags)", advancedNametagsEnabledImBool)) {
                        EspSettings.advancedNametagsEnabled = advancedNametagsEnabledImBool.get();
                    }


                    healthbarsEnabledImBool.set(EspSettings.healthbarsEnabled);
                    if (ImGui.checkbox("Enable Healthbars", healthbarsEnabledImBool)) {
                        EspSettings.healthbarsEnabled = healthbarsEnabledImBool.get();
                    }

                    if (EspSettings.healthbarsEnabled) {
                        ImGui.sameLine();
                        healthPositionImInt.set(EspSettings.healthPosition);
                        ImGui.pushItemWidth(100f);
                        if (ImGui.combo("Position##health", healthPositionImInt, healthPositions)) {
                            EspSettings.healthPosition = healthPositionImInt.get();
                        }
                        ImGui.popItemWidth();

                        ImGui.sameLine();
                        ImGui.pushItemWidth(80f);
                        healthbarThicknessArr[0] = EspSettings.healthbarThickness;
                        ImGui.sliderFloat("Thickness##health", healthbarThicknessArr, 1f, 10f, "%.1f");
                        EspSettings.healthbarThickness = healthbarThicknessArr[0];
                        ImGui.popItemWidth();

                        ImGui.sameLine();
                        healthV2EnabledImBool.set(EspSettings.healthV2Enabled);
                        if (ImGui.checkbox("Health v2 (Pink/Glow)", healthV2EnabledImBool)) {
                            EspSettings.healthV2Enabled = healthV2EnabledImBool.get();
                        }

                        if (EspSettings.healthV2Enabled) {
                            ImGui.sameLine();
                            ImGui.pushItemWidth(80f);
                            healthV2GlowStrengthArr[0] = EspSettings.healthV2GlowStrength;
                            ImGui.sliderFloat("Glow Strength##health", healthV2GlowStrengthArr, 1f, 10f, "%.1f");
                            EspSettings.healthV2GlowStrength = healthV2GlowStrengthArr[0];
                            ImGui.popItemWidth();
                        }
                    }

                    armorBarsEnabledImBool.set(EspSettings.armorBarsEnabled);
                    if (ImGui.checkbox("Enable Armor Bars", armorBarsEnabledImBool)) {
                        EspSettings.armorBarsEnabled = armorBarsEnabledImBool.get();
                    }

                    absorptionBarsEnabledImBool.set(EspSettings.absorptionBarsEnabled);
                    if (ImGui.checkbox("Enable Golden Health (Absorption)", absorptionBarsEnabledImBool)) {
                        EspSettings.absorptionBarsEnabled = absorptionBarsEnabledImBool.get();
                    }

                    distanceEnabledImBool.set(EspSettings.distanceEnabled);
                    if (ImGui.checkbox("Enable Distance", distanceEnabledImBool)) {
                        EspSettings.distanceEnabled = distanceEnabledImBool.get();
                    }

                    equipmentEspBool.set(EspSettings.equipmentEspEnabled);
                    if (ImGui.checkbox("Enable Equipment ESP (Armor/Items)", equipmentEspBool)) {
                        EspSettings.equipmentEspEnabled = equipmentEspBool.get();
                    }

                    maxDistanceArr[0] = EspSettings.maxDistance;
                    ImGui.pushItemWidth(250f);
                    ImGui.sliderFloat("ESP Max Distance##esp", maxDistanceArr, 10f, 1000f, "%.0f blocks");
                    ImGui.popItemWidth();
                    EspSettings.maxDistance = maxDistanceArr[0];


} // end espEnabled
                ImGui.separator();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Skeleton ESP")) {
ImGui.textDisabled("Skeleton ESP");
                ImGui.spacing();
                ImGui.spacing();
                    ImBoolean skeletonBool = new ImBoolean(EspSettings.skeletonEspEnabled);
                    if (ImGui.checkbox("Enable Skeleton ESP", skeletonBool)) {
                        EspSettings.skeletonEspEnabled = skeletonBool.get();
                    }
                    if (EspSettings.skeletonEspEnabled) {
                        ImGui.sameLine();
                        float[] skelColor = EspSettings.skeletonColor;
                        if (ImGui.colorEdit4("Skeleton Color", skelColor)) {
                            System.arraycopy(skelColor, 0, EspSettings.skeletonColor, 0, 4);
                        }
                        
                        float[] skelThick = { EspSettings.skeletonThickness };
                        ImGui.pushItemWidth(150f);
                        if (ImGui.sliderFloat("Skeleton Thickness", skelThick, 0.5f, 5.0f, "%.1f px")) {
                            EspSettings.skeletonThickness = skelThick[0];
                        }
                        ImGui.popItemWidth();
                        
                        skeletonOutlineModeImInt.set(EspSettings.skeletonOutlineMode);
                        ImGui.pushItemWidth(150f);
                        if (ImGui.combo("Skeleton Outline", skeletonOutlineModeImInt, skeletonOutlineModes)) {
                            EspSettings.skeletonOutlineMode = skeletonOutlineModeImInt.get();
                        }
                        ImGui.popItemWidth();
                    }

                    tracersEnabledImBool.set(EspSettings.tracersEnabled);
                    if (ImGui.checkbox("Enable Tracers", tracersEnabledImBool)) {
                        EspSettings.tracersEnabled = tracersEnabledImBool.get();
                    }
                    if (EspSettings.tracersEnabled) {
                        ImGui.sameLine();
                        float[] tCol = EspSettings.tracerColor;
                        if (ImGui.colorEdit4("Tracer Color", tCol)) {
                            System.arraycopy(tCol, 0, EspSettings.tracerColor, 0, 4);
                        }
                    }
                    
                    ImGui.spacing();

                    eyeTraceEnabledImBool.set(EspSettings.eyeTraceEnabled);
                    if (ImGui.checkbox("Enable Eye Tracers", eyeTraceEnabledImBool)) {
                        EspSettings.eyeTraceEnabled = eyeTraceEnabledImBool.get();
                    }
                    if (EspSettings.eyeTraceEnabled) {
                        ImGui.sameLine();
                        float[] eCol = EspSettings.eyeTraceColor;
                        if (ImGui.colorEdit4("Eye Trace Color", eCol)) {
                            System.arraycopy(eCol, 0, EspSettings.eyeTraceColor, 0, 4);
                        }
                        
                        eyeTraceLengthArr[0] = EspSettings.eyeTraceLength;
                        ImGui.pushItemWidth(150f);
                        if (ImGui.sliderFloat("Eye Trace Length", eyeTraceLengthArr, 0.5f, 10.0f, "%.1f blocks")) {
                            EspSettings.eyeTraceLength = eyeTraceLengthArr[0];
                        }
                        ImGui.popItemWidth();
                    }
                ImGui.separator();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Utilities")) {
ImGui.textDisabled("Utility");
                ImGui.spacing();
                ImGui.spacing();

                hoverRefillBool.set(UtilitySettings.hoverRefillEnabled);
                if (ImGui.checkbox("Hover Refill##util", hoverRefillBool)) {
                    UtilitySettings.hoverRefillEnabled = hoverRefillBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("While inventory is open, hovering a healing\npotion instantly moves it to your hotbar.");
                }

                ImGui.spacing();

                healKeybindBool.set(UtilitySettings.healKeybindEnabled);
                if (ImGui.checkbox("Heal Keybind##util", healKeybindBool)) {
                    UtilitySettings.healKeybindEnabled = healKeybindBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Press the bound key to auto-throw a healing\npotion from your hotbar, then restore your slot.");
                }

                if (UtilitySettings.healKeybindEnabled) {
                    ImGui.sameLine();
                    String keyLabel = waitingForHealKey
                            ? "[ Press a key... ]"
                            : (UtilitySettings.healKeybindKey < 0
                                    ? "[ Unbound ]"
                                    : "[ " + InputManager.getKeyName(UtilitySettings.healKeybindKey) + " ]");
                    if (ImGui.button(keyLabel + "##healkey")) {
                        waitingForHealKey = true;
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Click to bind a key.");
                    }
                    
                    ImGui.spacing();

                    // Switch delay (min/max)
                    healSwitchMinArr[0] = UtilitySettings.healSwitchMinTicks;
                    healSwitchMaxArr[0] = UtilitySettings.healSwitchMaxTicks;
                    ImGui.pushItemWidth(120f);
                    if (ImGui.sliderInt("Switch Min##heal", healSwitchMinArr, 1, 10)) {
                        UtilitySettings.healSwitchMinTicks = Math.max(1, healSwitchMinArr[0]);
                        if (UtilitySettings.healSwitchMinTicks > UtilitySettings.healSwitchMaxTicks)
                            UtilitySettings.healSwitchMaxTicks = UtilitySettings.healSwitchMinTicks;
                    }
                    ImGui.sameLine();
                    if (ImGui.sliderInt("Switch Max (Ticks)##heal", healSwitchMaxArr, 1, 10)) {
                        UtilitySettings.healSwitchMaxTicks = Math.max(1, healSwitchMaxArr[0]);
                        if (UtilitySettings.healSwitchMaxTicks < UtilitySettings.healSwitchMinTicks)
                            UtilitySettings.healSwitchMinTicks = UtilitySettings.healSwitchMaxTicks;
                    }
                    ImGui.popItemWidth();

                    // Restore delay (min/max)
                    healRestoreMinArr[0] = UtilitySettings.healRestoreMinTicks;
                    healRestoreMaxArr[0] = UtilitySettings.healRestoreMaxTicks;
                    ImGui.pushItemWidth(120f);
                    if (ImGui.sliderInt("Restore Min##heal", healRestoreMinArr, 1, 10)) {
                        UtilitySettings.healRestoreMinTicks = Math.max(1, healRestoreMinArr[0]);
                        if (UtilitySettings.healRestoreMinTicks > UtilitySettings.healRestoreMaxTicks)
                            UtilitySettings.healRestoreMaxTicks = UtilitySettings.healRestoreMinTicks;
                    }
                    ImGui.sameLine();
                    if (ImGui.sliderInt("Restore Max (Ticks)##heal", healRestoreMaxArr, 1, 10)) {
                        UtilitySettings.healRestoreMaxTicks = Math.max(1, healRestoreMaxArr[0]);
                        if (UtilitySettings.healRestoreMaxTicks < UtilitySettings.healRestoreMinTicks)
                            UtilitySettings.healRestoreMinTicks = UtilitySettings.healRestoreMaxTicks;
                    }
                    ImGui.popItemWidth();
                }

                ImGui.spacing();

                ImGui.spacing();

                ImGui.separator();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Friends")) {
ImGui.textDisabled("Friends");
                ImGui.spacing();
                ImGui.spacing();

                friendsSystemEnabledBool.set(EspSettings.friendsSystemEnabled);
                if (ImGui.checkbox("Enable Friends System", friendsSystemEnabledBool)) {
                    EspSettings.friendsSystemEnabled = friendsSystemEnabledBool.get();
                }

                if (EspSettings.friendsSystemEnabled) {
                    ImGui.separator();

                    middleClickFriendBool.set(AimAssistSettings.middleClickFriendEnabled);
                    if (ImGui.checkbox("Middle-Click Friend In-Game", middleClickFriendBool)) {
                        AimAssistSettings.middleClickFriendEnabled = middleClickFriendBool.get();
                    }

                    friendEspOverrideBool.set(EspSettings.friendEspOverride);
                    if (ImGui.checkbox("Override ESP Color for Friends", friendEspOverrideBool)) {
                        EspSettings.friendEspOverride = friendEspOverrideBool.get();
                    }
                    if (EspSettings.friendEspOverride) {
                        ImGui.sameLine();
                        float[] fCol = EspSettings.friendColor;
                        if (ImGui.colorEdit4("Friend ESP Color", fCol)) {
                            System.arraycopy(fCol, 0, EspSettings.friendColor, 0, 4);
                        }
                    }

                    ImGui.spacing();
                    ImGui.separator();
                    
                    ImGui.text("Add Friend Manually:");
                    ImGui.pushItemWidth(200f);
                    if (ImGui.inputText("##friendname", friendInputBuffer)) {
                        // Just taking input
                    }
                    ImGui.popItemWidth();
                    ImGui.sameLine();
                    if (ImGui.button("Add##manualfriend")) {
                        String newF = friendInputBuffer.get().trim();
                        if (!newF.isEmpty()) {
                            com.velocity.config.FriendManager.addFriend(newF);
                            friendInputBuffer.set("");
                        }
                    }

                    ImGui.spacing();
                    ImGui.separator();
                    
                    // Tables for World Players and Friends
                    ImGui.beginChild("##friendsLayout", 0, 0, false, 0);
                    
                    ImGui.columns(2, "friends_columns", true);
                    
                    // Left Column: World Players
                    ImGui.text("Players in World");
                    ImGui.separator();
                    
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    if (client != null && client.world != null && client.player != null) {
                        java.util.List<net.minecraft.entity.player.PlayerEntity> players = new java.util.ArrayList<>();
                        for (net.minecraft.entity.player.PlayerEntity p : client.world.getPlayers()) {
                            if (p != client.player) players.add(p);
                        }
                        // Sort by distance
                        players.sort(java.util.Comparator.comparingDouble(p -> p.squaredDistanceTo(client.player)));
                        
                        for (net.minecraft.entity.player.PlayerEntity p : players) {
                            String name = p.getName().getString();
                            float dist = client.player.distanceTo(p);
                            boolean isF = com.velocity.config.FriendManager.isFriend(name);
                            
                            ImGui.pushID("p_" + name);
                            if (isF) {
                                ImGui.beginDisabled();
                                ImGui.button("Friended");
                                ImGui.endDisabled();
                            } else {
                                if (ImGui.button("Add")) {
                                    com.velocity.config.FriendManager.addFriend(name);
                                }
                            }
                            ImGui.sameLine();
                            ImGui.text(name + " (" + String.format("%.1fm", dist) + ")");
                            ImGui.popID();
                        }
                    } else {
                        ImGui.textDisabled("Not in a world.");
                    }
                    
                    ImGui.nextColumn();
                    
                    // Right Column: Current Friends
                    ImGui.text("Your Friends");
                    ImGui.separator();
                    
                    for (String f : com.velocity.config.FriendManager.getFriends()) {
                        ImGui.pushID("f_" + f);
                        if (ImGui.button("Remove")) {
                            com.velocity.config.FriendManager.removeFriend(f);
                        }
                        ImGui.sameLine();
                        ImGui.text(f);
                        ImGui.popID();
                    }
                    
                    ImGui.columns(1, "friends_columns", false);
                    ImGui.endChild();
                }
                ImGui.separator();

                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Ore ESP")) {
ImGui.textDisabled("Ore ESP");
                ImGui.spacing();
                ImGui.spacing();

                ImBoolean oreEspBool = new ImBoolean(OreEspSettings.enabled);
                if (ImGui.checkbox("Enable Ore ESP", oreEspBool)) {
                    OreEspSettings.enabled = oreEspBool.get();
                    if (OreEspSettings.enabled) OreEsp.invalidateCache();
                }

                ImGui.sameLine();
                String oreKeyLabel = waitingForOreKey
                        ? "[ Press a key... ]"
                        : (OreEspSettings.oreToggleKey < 0
                                ? "[ Unbound ]"
                                : "[ " + InputManager.getKeyName(OreEspSettings.oreToggleKey) + " ]");
                if (ImGui.button(oreKeyLabel + "##orekey")) {
                    waitingForOreKey = true;
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Click to bind a key to toggle Ore ESP.");
                }

                if (OreEspSettings.enabled) {
                    ImGui.separator();

                    // Scan radius slider
                    int[] radiusArr = { OreEspSettings.scanRadius };
                    ImGui.pushItemWidth(250f);
                    if (ImGui.sliderInt("Scan Radius (blocks)##ore", radiusArr, 8, 64)) {
                        OreEspSettings.scanRadius = radiusArr[0];
                        OreEsp.invalidateCache();
                    }
                    ImGui.popItemWidth();

                    // Rescan interval
                    int[] rescanArr = { OreEspSettings.rescanTicks };
                    ImGui.pushItemWidth(250f);
                    if (ImGui.sliderInt("Rescan Interval (ticks)##ore", rescanArr, 1, 40)) {
                        OreEspSettings.rescanTicks = rescanArr[0];
                    }
                    ImGui.popItemWidth();

                    ImGui.spacing();
                    ImGui.separator();
                    ImGui.text("Ore Types:");
                    ImGui.spacing();

                    // Row 1: Diamond, Emerald, Lapis
                    ImBoolean diaBool = new ImBoolean(OreEspSettings.diamond);
                    if (ImGui.checkbox("Diamond##ore", diaBool)) {
                        OreEspSettings.diamond = diaBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.0f, 1.0f, 1.0f, 1.0f, "\u2588");
                    ImGui.sameLine();

                    ImBoolean emBool = new ImBoolean(OreEspSettings.emerald);
                    if (ImGui.checkbox("Emerald##ore", emBool)) {
                        OreEspSettings.emerald = emBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.0f, 1.0f, 0.3f, 1.0f, "\u2588");
                    ImGui.sameLine();

                    ImBoolean lapBool = new ImBoolean(OreEspSettings.lapis);
                    if (ImGui.checkbox("Lapis##ore", lapBool)) {
                        OreEspSettings.lapis = lapBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.1f, 0.1f, 0.8f, 1.0f, "\u2588");

                    // Row 2: Redstone, Gold, Iron
                    ImBoolean rsBool = new ImBoolean(OreEspSettings.redstone);
                    if (ImGui.checkbox("Redstone##ore", rsBool)) {
                        OreEspSettings.redstone = rsBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "\u2588");
                    ImGui.sameLine();

                    ImBoolean goldBool = new ImBoolean(OreEspSettings.gold);
                    if (ImGui.checkbox("Gold##ore", goldBool)) {
                        OreEspSettings.gold = goldBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(1.0f, 0.85f, 0.0f, 1.0f, "\u2588");
                    ImGui.sameLine();

                    ImBoolean ironBool = new ImBoolean(OreEspSettings.iron);
                    if (ImGui.checkbox("Iron##ore", ironBool)) {
                        OreEspSettings.iron = ironBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.85f, 0.55f, 0.35f, 1.0f, "\u2588");

                    // Row 3: Coal, Copper
                    ImBoolean coalBool = new ImBoolean(OreEspSettings.coal);
                    if (ImGui.checkbox("Coal##ore", coalBool)) {
                        OreEspSettings.coal = coalBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.3f, 0.3f, 0.3f, 1.0f, "\u2588");
                    ImGui.sameLine();

                    ImBoolean copBool = new ImBoolean(OreEspSettings.copper);
                    if (ImGui.checkbox("Copper##ore", copBool)) {
                        OreEspSettings.copper = copBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.9f, 0.5f, 0.2f, 1.0f, "\u2588");

                    // Row 4: Ancient Debris, Quartz
                    ImBoolean debBool = new ImBoolean(OreEspSettings.ancientDebris);
                    if (ImGui.checkbox("Ancient Debris##ore", debBool)) {
                        OreEspSettings.ancientDebris = debBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(0.55f, 0.3f, 0.2f, 1.0f, "\u2588");
                    ImGui.sameLine();

                    ImBoolean qtzBool = new ImBoolean(OreEspSettings.quartz);
                    if (ImGui.checkbox("Quartz##ore", qtzBool)) {
                        OreEspSettings.quartz = qtzBool.get();
                        OreEsp.invalidateCache();
                    }
                    ImGui.sameLine();
                    ImGui.textColored(1.0f, 1.0f, 1.0f, 1.0f, "\u2588");
                }
                ImGui.separator();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Radar")) {
ImGui.textDisabled("Overlays & Radars");
                ImGui.spacing();
                ImGui.spacing();

                adminOverlayEnabledBool.set(EspSettings.adminOverlayEnabled);
                if (ImGui.checkbox("Enable Admin Radar Overlay", adminOverlayEnabledBool)) {
                    EspSettings.adminOverlayEnabled = adminOverlayEnabledBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Shows an overlay listing all players on the server who are in Creative or Spectator mode.");
                }
                
                ImGui.spacing();
                
                playerRadarEnabledBool.set(EspSettings.playerRadarEnabled);
                if (ImGui.checkbox("Enable Player Radar Overlay", playerRadarEnabledBool)) {
                    EspSettings.playerRadarEnabled = playerRadarEnabledBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Shows an overlay listing all nearby players, sorted by distance. Friends are highlighted in their custom color.");
                }

                ImGui.separator();

                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Light ESP")) {
ImGui.textDisabled("Light ESP");
                ImGui.spacing();
                ImGui.spacing();

                ImBoolean lightEspBool = new ImBoolean(LightSourceEspSettings.enabled);
                if (ImGui.checkbox("Enable Light Source ESP", lightEspBool)) {
                    LightSourceEspSettings.enabled = lightEspBool.get();
                    if (LightSourceEspSettings.enabled) LightSourceEsp.invalidateCache();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Shows all light-emitting blocks (torches, glowstone,\nlava, lanterns, etc.) through walls.");
                }

                if (LightSourceEspSettings.enabled) {
                    ImGui.separator();

                    int[] lRadiusArr = { LightSourceEspSettings.scanRadius };
                    ImGui.pushItemWidth(250f);
                    if (ImGui.sliderInt("Scan Radius##light", lRadiusArr, 8, 64)) {
                        LightSourceEspSettings.scanRadius = lRadiusArr[0];
                        LightSourceEsp.invalidateCache();
                    }
                    ImGui.popItemWidth();

                    int[] lRescanArr = { LightSourceEspSettings.rescanTicks };
                    ImGui.pushItemWidth(250f);
                    if (ImGui.sliderInt("Rescan Interval (ticks)##light", lRescanArr, 1, 40)) {
                        LightSourceEspSettings.rescanTicks = lRescanArr[0];
                    }
                    ImGui.popItemWidth();

                    int[] lMinLumArr = { LightSourceEspSettings.minLuminance };
                    ImGui.pushItemWidth(250f);
                    if (ImGui.sliderInt("Min Luminance##light", lMinLumArr, 1, 15)) {
                        LightSourceEspSettings.minLuminance = lMinLumArr[0];
                        LightSourceEsp.invalidateCache();
                    }
                    ImGui.popItemWidth();
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Filter out dim light sources.\n1 = show all, 14 = only max-brightness (torches/glowstone).");
                    }
                }

                ImGui.separator();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Settings")) {
ImGui.textDisabled("Config/Debug");
                ImGui.spacing();
                ImGui.spacing();
                ImGui.text("Light Debug Renderers");
                ImGui.separator();
                ImGui.spacing();

                ImBoolean skyLightBool = new ImBoolean(LightDebugManager.skyLightEnabled);
                if (ImGui.checkbox("Sky Light Levels", skyLightBool)) {
                    LightDebugManager.skyLightEnabled = skyLightBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Shows sky light level numbers at each block position.");
                }

                ImBoolean blockLightBool = new ImBoolean(LightDebugManager.blockLightEnabled);
                if (ImGui.checkbox("Block Light Levels", blockLightBool)) {
                    LightDebugManager.blockLightEnabled = blockLightBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Shows block light level numbers (torches, etc.) at each block position.");
                }

                ImBoolean skyLightSecBool = new ImBoolean(LightDebugManager.skyLightSectionsEnabled);
                if (ImGui.checkbox("Sky Light Sections", skyLightSecBool)) {
                    LightDebugManager.skyLightSectionsEnabled = skyLightSecBool.get();
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Visualizes sky light section boundaries and their readiness state.");
                }

                ImGui.separator();

                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }

        ImGui.separator();
        ImGui.textDisabled("INSERT or ESC to close");

        ImGui.end();
    }
}
