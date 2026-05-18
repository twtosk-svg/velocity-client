import os
import re

with open('src/main/java/com/velocity/core/EspRenderer.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove imports
content = re.sub(r'import com\.velocity\.module\.combat\.AutoWTap;\n', '', content)
content = re.sub(r'import com\.velocity\.module\.movement\.Parkour;\n', '', content)
content = re.sub(r'import com\.velocity\.module\.utility\.AutoBucket;\n', '', content)

# Remove scale setting
content = re.sub(r'        if \(EspSettings\.textScale != 1\.0f\) \{\n\s*ImGui\.getIO\(\)\.setFontGlobalScale\(.*?\);\n\s*\} else \{\n\s*ImGui\.getIO\(\)\.setFontGlobalScale\(.*?\);\n\s*\}\n', '', content, flags=re.DOTALL)
# Or if it's not exactly that:
content = re.sub(r'\s*ImGui\.getIO\(\)\.setFontGlobalScale\(AimAssistSettings\.globalOverlayScale \* EspSettings\.textScale\);\n', '\n', content)
content = re.sub(r'\s*ImGui\.getIO\(\)\.setFontGlobalScale\(AimAssistSettings\.globalOverlayScale\);\n', '\n', content)

# Remove textScale multiplication
content = re.sub(r'\s*\*\s*EspSettings\.textScale', '', content)

# Remove Parkour block
content = re.sub(r'\s*// 0e ── Parkour tick ──\n\s*if \(inGame && Parkour\.enabled[^}]+\}?', '', content)
# Remove AutoBucket block
content = re.sub(r'\s*// 0f ── AutoBucket tick ──\n\s*if \(inGame && AutoBucket\.enabled[^}]+\}?', '', content)
# Remove AutoWTap block
content = re.sub(r'\s*// 0g ── AutoWTap tick ──\n\s*if \(inGame\) \{\n\s*AutoWTap\.tick\(client\);\n\s*\}', '', content)

with open('src/main/java/com/velocity/core/EspRenderer.java', 'w', encoding='utf-8') as f:
    f.write(content)

# MenuUI
with open('src/main/java/com/velocity/gui/MenuUI.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'import com\.velocity\.module\.movement\.Parkour;\n', '', content)
content = re.sub(r'import com\.velocity\.module\.utility\.AutoBucket;\n', '', content)

# Remove wrappers
content = re.sub(r'\s*private static float\[\] espTextScaleArr = \{ 1\.0f \};\n', '\n', content)
content = re.sub(r'\s*private static float\[\] globalScaleArr = \{ 1\.0f \};\n', '\n', content)
content = re.sub(r'\s*private static final ImBoolean parkourBool = new ImBoolean\(false\);\n', '\n', content)
content = re.sub(r'\s*private static final ImBoolean autoBucketBool = new ImBoolean\(false\);\n', '\n', content)
content = re.sub(r'\s*private static final ImBoolean autoWTapBool = new ImBoolean\(false\);\n', '\n', content)
content = re.sub(r'\s*private static final int\[\] autoWTapDelayArr = new int\[\]\{1\};\n', '\n', content)

# Remove UI logic
# Global Scale
content = re.sub(r'\s*globalScaleArr\[0\] = AimAssistSettings\.globalOverlayScale;\n\s*ImGui\.pushItemWidth\(150f\);\n\s*if \(ImGui\.sliderFloat\("Global UI Scale", globalScaleArr, 0\.5f, 2\.0f, "%\.2fx"\)\) \{\n\s*AimAssistSettings\.globalOverlayScale = globalScaleArr\[0\];\n\s*ImGui\.getIO\(\)\.setFontGlobalScale\(AimAssistSettings\.globalOverlayScale\);\n\s*\}\n\s*ImGui\.popItemWidth\(\);\n\s*ImGui\.spacing\(\);\n', '\n', content)

# Text scale
content = re.sub(r'\s*espTextScaleArr\[0\] = EspSettings\.textScale;\n\s*ImGui\.pushItemWidth\(150f\);\n\s*if \(ImGui\.sliderFloat\("Text Scale##esp", espTextScaleArr, 0\.5f, 2\.0f, "%\.2fx"\)\) \{\n\s*EspSettings\.textScale = espTextScaleArr\[0\];\n\s*\}\n\s*ImGui\.popItemWidth\(\);\n', '\n', content)

# AutoWTap block
wtap = r'\s*autoWTapBool\.set\(UtilitySettings\.autoWTapEnabled\);\n\s*if \(ImGui\.checkbox\("Auto W-Tap \(Sprint Reset\)##util", autoWTapBool\)\) \{\n\s*UtilitySettings\.autoWTapEnabled = autoWTapBool\.get\(\);\n\s*\}\n\s*if \(ImGui\.isItemHovered\(\)\) \{\n\s*ImGui\.setTooltip\("Instantly resets your sprint packet right after an attack to bypass Knockback limits\."\);\n\s*\}\n\s*if \(UtilitySettings\.autoWTapEnabled\) \{\n\s*ImGui\.sameLine\(\);\n\s*autoWTapDelayArr\[0\] = UtilitySettings\.autoWTapDelayTicks;\n\s*ImGui\.pushItemWidth\(150f\);\n\s*if \(ImGui\.sliderInt\("Delay \(Ticks\)##wtap", autoWTapDelayArr, 0, 5\)\) \{\n\s*UtilitySettings\.autoWTapDelayTicks = autoWTapDelayArr\[0\];\n\s*\}\n\s*ImGui\.popItemWidth\(\);\n\s*\}'
content = re.sub(wtap, '', content)

# Parkour Block
parkour = r'\s*parkourBool\.set\(Parkour\.enabled\);\n\s*if \(ImGui\.checkbox\("Parkour Jump##util", parkourBool\)\) \{\n\s*Parkour\.enabled = parkourBool\.get\(\);\n\s*\}\n\s*if \(ImGui\.isItemHovered\(\)\) \{\n\s*ImGui\.setTooltip\("Automatically jump when arriving at a block edge or gap\."\);\n\s*\}'
content = re.sub(parkour, '', content)

# AutoBucket block
bucket = r'\s*autoBucketBool\.set\(AutoBucket\.enabled\);\n\s*if \(ImGui\.checkbox\("Auto Bucket##util", autoBucketBool\)\) \{\n\s*AutoBucket\.enabled = autoBucketBool\.get\(\);\n\s*\}\n\s*if \(ImGui\.isItemHovered\(\)\) \{\n\s*ImGui\.setTooltip\("Hold Left-Click to auto-spam Right-Click with Water/Snow/Webs\.\\nAlso allows Insta-scooping Lava\."\);\n\s*\}'
content = re.sub(bucket, '', content)

with open('src/main/java/com/velocity/gui/MenuUI.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("MenuUI and EspRenderer cleaned up.")
