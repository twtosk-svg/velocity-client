import re

with open('src/main/java/com/velocity/gui/MenuUI.java', 'r', encoding='utf-8') as f:
    code = f.read()

# Find the start and end of the TabBar
start_tabbar = code.find('if (ImGui.beginTabBar("Modules")) {')
if start_tabbar == -1:
    print('TabBar not found')
    exit(1)

end_tabbar = code.rfind('ImGui.endTabBar();')

# Extract the body inside TabBar
body = code[start_tabbar:end_tabbar]

# Regex to find each tab block
tab_pattern = re.compile(r'( *if\s*\(\s*ImGui\.beginTabItem\("([^"]+)"\)\s*\)\s*\{.*?ImGui\.endTabItem\(\);\s*\})', re.DOTALL)

tabs = {}
for match in tab_pattern.finditer(body):
    full_text = match.group(1)
    name = match.group(2)
    tabs[name] = full_text

if not tabs:
    print("No tabs found")
    exit(1)

print("Found tabs:", list(tabs.keys()))

# Desired order and groupings
# Combat: Aim Assist, TriggerBot
# Visuals: ESP, Ore ESP, Light ESP, Admin Radar
# Utilities: Utility, Friends
# Settings: Debug

def wrap_in_tab(tab_name, contents):
    inner = ""
    for name, content in contents:
        # remove the ImGui.beginTabItem and endTabItem from inner content, and add a separator
        # content is: if (ImGui.beginTabItem("...")) { ... ImGui.endTabItem(); }
        inner_content = re.sub(r'^\s*if\s*\(\s*ImGui\.beginTabItem\("[^"]+"\)\s*\)\s*\{', f'ImGui.textDisabled("{name}");\n                ImGui.spacing();', content, count=1, flags=re.MULTILINE)
        inner_content = re.sub(r'ImGui\.endTabItem\(\);\s*\}$', 'ImGui.separator();\n', inner_content, count=1, flags=re.MULTILINE)
        inner += inner_content
    
    return f'''            if (ImGui.beginTabItem("{tab_name}")) {{
{inner}
                ImGui.endTabItem();
            }}'''

combat_tab = wrap_in_tab("Combat", [("Aim Assist", tabs.get("Aim Assist", "")), ("TriggerBot", tabs.get("TriggerBot", ""))])
visuals_tab = wrap_in_tab("Visuals", [("ESP", tabs.get("ESP", "")), ("Ore ESP", tabs.get("Ore ESP", "")), ("Light ESP", tabs.get("Light ESP", "")), ("Admin Radar", tabs.get("Admin Radar", ""))])
utils_tab = wrap_in_tab("Utilities", [("Utility", tabs.get("Utility", "")), ("Friends", tabs.get("Friends", ""))])
settings_tab = wrap_in_tab("Settings", [("Config/Debug", tabs.get("Debug", ""))])

new_body = 'if (ImGui.beginTabBar("Modules")) {\n' + combat_tab + '\n' + visuals_tab + '\n' + utils_tab + '\n' + settings_tab + '\n            '

# Replace version title too
code = code.replace('"Velocity 1.4"', '"Velocity 1.5"')

new_code = code[:start_tabbar] + new_body + code[end_tabbar:]

with open('src/main/java/com/velocity/gui/MenuUI.java', 'w', encoding='utf-8') as f:
    f.write(new_code)

print("Refactored MenuUI.java successfully!")
