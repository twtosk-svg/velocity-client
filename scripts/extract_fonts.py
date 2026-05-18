import os

path = os.path.join(os.path.dirname(__file__), "..", "ImguiMenu C++", "framework", "data", "fonts.h")
with open(path, "r", encoding="utf-8", errors="ignore") as f:
    lines = f.readlines()

def extract(start_marker, end_marker):
    in_block = False
    nums = []
    for line in lines:
        if start_marker in line:
            in_block = True
            continue
        if in_block and end_marker and end_marker in line and "inline" in line:
            break
        if in_block:
            part = line.strip().rstrip(",")
            for p in part.split(","):
                p = p.strip()
                if p.startswith("0x"):
                    nums.append(int(p, 16))
    return nums

out_dir = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "fonts")
os.makedirs(out_dir, exist_ok=True)
for name, data in [
    ("inter_medium.ttf", extract("inter_medium_data", "icon_data")),
    ("icons.ttf", extract("icon_data", "")),
]:
    out = os.path.join(out_dir, name)
    with open(out, "wb") as o:
        o.write(bytes(data))
    print(name, len(data))
