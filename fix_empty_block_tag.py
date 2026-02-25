import re
import sys
import os

def fix_file(filepath, lines_to_fix):
    if not os.path.exists(filepath): return
    with open(filepath, 'r') as f:
        lines = f.readlines()

    # Sort lines in reverse order
    lines_to_fix.sort(reverse=True)

    for line_num in lines_to_fix:
        idx = line_num - 1
        if idx >= len(lines): continue
        line = lines[idx]

        # Check for empty @param
        match_param = re.search(r'(@param\s+([a-zA-Z0-9_]+))\s*$', line.strip())
        if match_param:
            param_name = match_param.group(2)
            lines[idx] = line.replace(match_param.group(1), f"{match_param.group(1)} the {param_name}")
            continue

        # Check for empty @return
        if '@return' in line and line.strip() == '* @return':
            lines[idx] = line.replace('@return', '@return the result')
            continue

        # Check for empty @throws
        match_throws = re.search(r'(@throws\s+([a-zA-Z0-9_]+))\s*$', line.strip())
        if match_throws:
            lines[idx] = line.replace(match_throws.group(1), f"{match_throws.group(1)} if an error occurs")
            continue

        # Check for empty @deprecated
        if '@deprecated' in line and line.strip() == '* @deprecated':
             lines[idx] = line.replace('@deprecated', '@deprecated (no longer used)')
             continue

    with open(filepath, 'w') as f:
        f.writelines(lines)

files_to_fix = {}
with open('empty_block_tag_warnings.txt', 'r') as f:
    for line in f:
        parts = line.split(':')
        if len(parts) > 1:
            filepath = parts[0].replace('[WARNING] /app/', '').strip()
            try:
                line_num = int(parts[1].split(',')[0].replace('[', ''))
                if filepath not in files_to_fix:
                    files_to_fix[filepath] = []
                if line_num not in files_to_fix[filepath]:
                    files_to_fix[filepath].append(line_num)
            except:
                pass

for filepath, lines in files_to_fix.items():
    fix_file(filepath, lines)
