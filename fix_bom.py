import os
import glob

def remove_bom_from_file(filepath):
    with open(filepath, 'rb') as f:
        content = f.read()
    
    if content.startswith(b'\xef\xbb\xbf'):
        with open(filepath, 'wb') as f:
            f.write(content[3:])
        print(f"[已修复] {filepath}")
        return True
    else:
        print(f"[正常] {filepath}")
        return False

# 遍历 src 目录下所有 .java 文件
project_dir = os.path.dirname(os.path.abspath(__file__))
src_dir = os.path.join(project_dir, 'src')
java_files = glob.glob(os.path.join(src_dir, '**', '*.java'), recursive=True)

if not java_files:
    print("未找到 .java 文件，请确认脚本放在项目根目录")
else:
    fixed = 0
    for f in java_files:
        if remove_bom_from_file(f):
            fixed += 1
    print(f"\n共处理 {len(java_files)} 个文件，修复 {fixed} 个 BOM 文件")
input("按回车键退出...")