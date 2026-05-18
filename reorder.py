import re

with open('src/main/java/com/velocity/gui/MenuUI.java', 'r', encoding='utf-8') as f:
    content = f.read()

prefix, rest = content.split('if (ImGui.beginTabBar("Modules")) {\n', 1)
tabs_content, suffix = rest.split('            ImGui.endTabBar();\n', 1)

# Extract each tab block
tab_blocks = []
current_block = ''
depth = 0
for line in tabs_content.splitlines(True):
    current_block += line
    if '{' in line:
        depth += line.count('{')
    if '}' in line:
        depth -= line.count('}')
        if depth == 0 and current_block.strip() != '':
            tab_blocks.append(current_block)
            current_block = ''

def get_name(blk):
    m = re.search(r'beginTabItem\("([^"]+)"\)', blk)
    return m.group(1) if m else ''

order = ['Combat', 'ESP', 'Utilities', 'Friends', 'Ore ESP', 'Admin Radar', 'Light ESP', 'Settings']
sorted_blocks = sorted(tab_blocks, key=lambda b: order.index(get_name(b)) if get_name(b) in order else 999)

new_content = prefix + 'if (ImGui.beginTabBar("Modules")) {\n' + ''.join(sorted_blocks) + '            ImGui.endTabBar();\n' + suffix

with open('src/main/java/com/velocity/gui/MenuUI.java', 'w', encoding='utf-8') as f:
    f.write(new_content)
print('Reordered tabs!')
