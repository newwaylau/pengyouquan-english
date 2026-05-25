#!/usr/bin/env python3
"""修复《唐顿庄园》S01E02-S01E07 数据编码问题

问题：SRT 源文件为正确 UTF-8，但导入数据库时中文被双重 UTF-8 编码，
英文空格被移除。本脚本直接从 SRT 源文件重新生成正确文本，并更新数据库。
"""

import re
import os
import sys

# 数据库连接
DB_HOST = os.environ.get('DB_HOST', '127.0.0.1')
DB_PORT = os.environ.get('DB_PORT', '3307')
DB_USER = os.environ.get('DB_USER', 'root')
DB_PASS = os.environ.get('DB_PASS', 'pengyouquan123')
DB_NAME = os.environ.get('DB_NAME', 'pengyouquan_english')

# 数据目录
DATA_DIR = '/home/heaton/pengyouquan-english/data/downton-abbey/season-01'

# 剧集映射：episode_number -> show_id (从数据库查询确认)
SHOW_IDS = {1: 12, 2: 114, 3: 115, 4: 116, 5: 117, 6: 118, 7: 119}


def detect_encoding(filepath):
    """Detect file encoding - try UTF-8 first, then common Chinese encodings"""
    with open(filepath, 'rb') as f:
        raw = f.read(4096)
    for enc in ['utf-8', 'gbk', 'gb2312', 'gb18030']:
        try:
            raw.decode(enc)
            return enc
        except UnicodeDecodeError:
            continue
    return 'utf-8'  # fallback


def parse_srt(filepath):
    """解析 SRT 文件，返回列表 [{'num': int, 'start': float, 'end': float, 'chinese': str, 'english': str}]"""
    entries = []
    
    encoding = detect_encoding(filepath)
    with open(filepath, 'r', encoding=encoding) as f:
        content = f.read()
    
    # Split by blank lines (SRT format)
    blocks = re.split(r'\n\n+', content.strip())
    
    for block in blocks:
        lines = block.strip().split('\n')
        if len(lines) < 3:
            continue
        
        # Parse sequence number
        try:
            num = int(lines[0].strip())
        except ValueError:
            continue
        
        # Parse timecodes
        time_match = re.match(r'(\d{2}):(\d{2}):(\d{2}),(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2}),(\d{3})', lines[1])
        if not time_match:
            continue
        
        start = (int(time_match.group(1)) * 3600 + 
                 int(time_match.group(2)) * 60 + 
                 int(time_match.group(3)) + 
                 int(time_match.group(4)) / 1000)
        end = (int(time_match.group(5)) * 3600 + 
               int(time_match.group(6)) * 60 + 
               int(time_match.group(7)) + 
               int(time_match.group(8)) / 1000)
        
        # Parse text lines - strip formatting tags like {\\fs16\\an2\\b0}
        text_lines = []
        for line in lines[2:]:
            line = re.sub(r'\{[^}]*\}', '', line).strip()
            if line:
                text_lines.append(line)
        
        chinese = ''
        english = ''
        for line in text_lines:
            line = line.strip()
            if not line:
                continue
            # Remove formatting tags inline
            line = re.sub(r'\{[^}]*\}', '', line).strip()
            if not line:
                continue
            
            # Check if contains Chinese characters
            if re.search(r'[\u4e00-\u9fff]', line):
                # Normalize spaces: replace multiple spaces with single \u3000
                line = re.sub(r' {2,}', '\u3000\u3000', line.strip())
                chinese = line
            elif line:
                english = line.strip()
        
        entries.append({
            'num': num,
            'start': round(start, 2),
            'end': round(end, 2),
            'chinese': chinese,
            'english': english
        })
    
    return entries


def main():
    import mysql.connector
    
    conn = mysql.connector.connect(
        host=DB_HOST,
        port=int(DB_PORT),
        user=DB_USER,
        password=DB_PASS,
        database=DB_NAME,
        charset='utf8mb4'
    )
    cursor = conn.cursor(dictionary=True)
    
    for episode_num in range(1, 8):
        show_id = SHOW_IDS[episode_num]
        
        srt_path = f'{DATA_DIR}/episode-{episode_num:02d}/subtitles/da-s01e{episode_num:02d}.srt'
        if not os.path.exists(srt_path):
            print(f"⚠ SRT not found: {srt_path}")
            continue
        
        entries = parse_srt(srt_path)
        print(f"📄 S01E{episode_num:02d} (show_id={show_id}): {len(entries)} sentences parsed from SRT")
        
        # Get existing data from database
        cursor.execute("SELECT id, text, start_time, end_time, audio_file FROM sentences WHERE show_id=%s ORDER BY id", (show_id,))
        db_rows = cursor.fetchall()
        print(f"📊 Database has {len(db_rows)} rows for show_id={show_id}")
        
        cursor.execute("SELECT COUNT(*) as cnt FROM sentences WHERE show_id=%s", (show_id,))
        cnt = cursor.fetchone()['cnt']
        print(f"✅ Count check: {cnt} rows")
        
        if len(db_rows) != len(entries):
            print(f"⚠  Mismatch! DB has {len(db_rows)} rows, SRT has {len(entries)} entries")
            # Use the smaller count
            n = min(len(db_rows), len(entries))
        else:
            n = len(entries)
        
        # Update each row with correct text
        updated = 0
        for i in range(n):
            srt = entries[i]
            db = db_rows[i]
            
            # Build correct text: #N English / Chinese
            correct_text = f"#{srt['num']} {srt['english']} / {srt['chinese']}"
            
            # Update only if text differs
            if db['text'] != correct_text:
                cursor.execute(
                    "UPDATE sentences SET text=%s WHERE id=%s",
                    (correct_text, db['id'])
                )
                updated += 1
        
        if updated > 0:
            conn.commit()
            print(f"✅ Updated {updated} rows for S01E{episode_num:02d}")
        else:
            print(f"✅ No updates needed for S01E{episode_num:02d}")
    
    cursor.close()
    conn.close()
    print("\n🎉 All done!")


if __name__ == '__main__':
    main()
