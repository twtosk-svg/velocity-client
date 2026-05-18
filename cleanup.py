import os
import re

def process_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    for target, replacement in replacements:
        content = re.sub(target, replacement, content, flags=re.MULTILINE)
        
    if original != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {path}")
    else:
        print(f"No changes in {path}")

# AimAssistSettings
process_file('src/main/java/com/velocity/config/AimAssistSettings.java', [
    (r'\s*public static float globalOverlayScale = 1\.0f;\s*\n', '\n'),
])

# EspSettings
process_file('src/main/java/com/velocity/config/EspSettings.java', [
    (r'\s*public static float textScale = 1\.0f;\s*\n', '\n'),
])

# UtilitySettings
process_file('src/main/java/com/velocity/config/UtilitySettings.java', [
    (r'\s*public static boolean autoWTapEnabled = false;\s*\n', '\n'),
    (r'\s*public static int autoWTapDelayTicks = 1;\s*\n', '\n'),
])

# ConfigManager
process_file('src/main/java/com/velocity/config/ConfigManager.java', [
    (r'import com\.velocity\.module\.movement\.Parkour;\n', ''),
    (r'import com\.velocity\.module\.utility\.AutoBucket;\n', ''),
    (r'\s*Parkour\.class,\n', '\n'),
    (r'\s*AutoBucket\.class,\n', '\n'),
    (r'\s*AutoBucket\.class\n', '\n'), # In case it was the last one
])

# InteractionManagerMixin
process_file('src/main/java/com/velocity/mixin/InteractionManagerMixin.java', [
    (r'import com\.velocity\.module\.combat\.AutoWTap;\n', ''),
    (r'\s*AutoWTap\.onAttack\(target\);\s*\n', '\n'),
])

print("Cleanup complete.")
