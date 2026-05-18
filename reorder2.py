import re

with open('src/main/java/com/velocity/gui/MenuUI.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. TriggerBot its own tab
# Find ImGui.separator(); \n ImGui.textDisabled("TriggerBot");
tb_search = '                ImGui.separator();\nImGui.textDisabled("TriggerBot");'
tb_replace = '                ImGui.separator();\n                ImGui.endTabItem();\n            }\n            if (ImGui.beginTabItem("TriggerBot")) {\nImGui.textDisabled("TriggerBot");'
content = content.replace(tb_search, tb_replace)

# 2. Extract Skeleton ESP and Eye Tracers into its own tab
# It starts at: ImBoolean skeletonBool = new ImBoolean(EspSettings.skeletonEspEnabled);
# It ends at the end of the ESP block.
skel_start = '                    ImBoolean skeletonBool = new ImBoolean(EspSettings.skeletonEspEnabled);'

# Wait, the Tracers block is between Skeleton and Eye Tracers. Does the user want Tracers moved too?
# "make the skellton esp have its on tab with the skeleton esp and the eye tracers in it"
# I will just split the tab at Skeleton ESP! But wait, it's currently inside `if (EspSettings.espEnabled) {`.
# So I need to close the `if` block, close the tab, start the new tab, and then start the content.
skel_replace = '} // end espEnabled\n                ImGui.separator();\n                ImGui.endTabItem();\n            }\n            if (ImGui.beginTabItem("Skeleton ESP")) {\nImGui.textDisabled("Skeleton ESP");\n                ImGui.spacing();\n                ImGui.spacing();\n' + skel_start
content = content.replace(skel_start, skel_replace)

# But wait, now I have a dangling `}` at the end of the original ESP block where `if (EspSettings.espEnabled)` used to close!
# Let's find: `                }\n                ImGui.separator();\n                ImGui.endTabItem();\n            }\n            if (ImGui.beginTabItem("Utilities")) {`
# And remove the `                }\n` because we already closed it!
old_esp_close = '                }\n                ImGui.separator();\n                ImGui.endTabItem();\n            }\n            if (ImGui.beginTabItem("Utilities")) {'
new_esp_close = '                ImGui.separator();\n                ImGui.endTabItem();\n            }\n            if (ImGui.beginTabItem("Utilities")) {'
content = content.replace(old_esp_close, new_esp_close)


# 3. Advanced Nametags inline
adv_nametags_str = """                    advancedNametagsEnabledImBool.set(EspSettings.advancedNametagsEnabled);
                    if (ImGui.checkbox("Advanced Nametags (Team Tags)", advancedNametagsEnabledImBool)) {
                        EspSettings.advancedNametagsEnabled = advancedNametagsEnabledImBool.get();
                    }

"""
content = content.replace(adv_nametags_str, "") # Remove it from original spot

nametags_target = """                    nametagsEnabledImBool.set(EspSettings.nametagsEnabled);
                    if (ImGui.checkbox("Enable Nametags", nametagsEnabledImBool)) {
                        EspSettings.nametagsEnabled = nametagsEnabledImBool.get();
                    }"""
nametags_replace = nametags_target + '\n                    ImGui.sameLine();\n' + adv_nametags_str.strip() + '\n'
content = content.replace(nametags_target, nametags_replace)


# 4. Shield Indicator moved to TriggerBot
shield_str = """                    shieldEspEnabledImBool.set(EspSettings.shieldEspEnabled);
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
                    
                    ImGui.spacing();"""
# Replace it with empty
content = content.replace(shield_str + '\n                    ', '')

# Insert it after ShieldBreaker
sb_target = """                    if (AimAssistSettings.shieldBreakerEnabled) {
                        shieldCooldownArr[0] = AimAssistSettings.shieldBreakerMinCooldown;
                        ImGui.pushItemWidth(250f);
                        ImGui.sliderFloat("Min Switch Cooldown (%)", shieldCooldownArr, 0f, 100f, "%.0f%%");
                        ImGui.popItemWidth();
                        AimAssistSettings.shieldBreakerMinCooldown = shieldCooldownArr[0];
                    }"""

sb_replace = sb_target + "\n                    ImGui.spacing();\n                    ImGui.separator();\nImGui.textDisabled(\"Shield ESP\");\n                    ImGui.spacing();\n" + shield_str
content = content.replace(sb_target, sb_replace)

with open('src/main/java/com/velocity/gui/MenuUI.java', 'w', encoding='utf-8') as f:
    f.write(content)
print("Applied all GUI layout modifications.")
