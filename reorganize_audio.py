#!/usr/bin/env python3
"""
重组音频文件命名 + 整理数据目录
步骤：
1. 创建数据目录树（season/episode 结构）
2. 在 backend/data/audio/ 中复制音频文件为新命名
3. 更新数据库 sentences.audio_file 列
4. 创建 clips 软链接
5. 验证
"""

import subprocess
import os
import re
import sys

ROOT = "/home/heaton/pengyouquan-english"
AUDIO_DIR = os.path.join(ROOT, "backend/data/audio")
DATA_DIR = os.path.join(ROOT, "data")

DB_CMD = [
    "docker", "exec", "pengyouquan-mysql",
    "mysql", "-u", "root", "-ppengyouquan123", "pengyouquan_english", "-e"
]

# show_id → (show_key, season, episode, old_prefix_pattern, description)
# old_prefix_pattern: regex to match old audio_file names
SHOW_MAP = {
    2:  ("got", 1, 1,  r"^wt(\d{4})\.mp3$",           "Game of Thrones S01E01"),
    3:  ("got", 1, 2,  r"^wtS01E02_(\d{4})\.mp3$",    "Game of Thrones S01E02"),
    4:  ("got", 1, 3,  r"^wtS01E03_(\d{4})\.mp3$",    "Game of Thrones S01E03"),
    5:  ("got", 1, 4,  r"^wtS01E04_(\d{4})\.mp3$",    "Game of Thrones S01E04"),
    6:  ("got", 1, 5,  r"^wtS01E05_(\d{4})\.mp3$",    "Game of Thrones S01E05"),
    7:  ("got", 1, 6,  r"^wtS01E06_(\d{4})\.mp3$",    "Game of Thrones S01E06"),
    8:  ("got", 1, 7,  r"^wtS01E07_(\d{4})\.mp3$",    "Game of Thrones S01E07"),
    9:  ("got", 1, 8,  r"^wtS01E08_(\d{4})\.mp3$",    "Game of Thrones S01E08"),
    10: ("got", 1, 9,  r"^wtS01E09_(\d{4})\.mp3$",    "Game of Thrones S01E09"),
    11: ("got", 1, 10, r"^wtS01E10_(\d{4})\.mp3$",    "Game of Thrones S01E10"),
    12: ("da",  1, 1,  r"^da(\d{4})\.mp3$",            "Downton Abbey S01E01"),
}

SHOW_DIRS = {
    "got": "game-of-thrones",
    "da":  "downton-abbey",
}


def run_sql(sql):
    """Execute SQL via docker mysql client, return stdout."""
    result = subprocess.run(
        DB_CMD + [sql],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"  SQL ERROR: {result.stderr.strip()}")
        return None
    return result.stdout.strip()


def make_new_name(show_key, season, episode, seq_num):
    """Generate new audio filename."""
    return f"{show_key}-S{season:02d}E{episode:02d}-{seq_num:04d}.mp3"


def make_old_to_new(audio_file, show_id):
    """Map old audio_file name to new name. Returns None if no match."""
    info = SHOW_MAP.get(show_id)
    if not info:
        return None
    show_key, season, episode, pattern, _ = info
    m = re.match(pattern, audio_file)
    if not m:
        return None
    seq = int(m.group(1))
    return make_new_name(show_key, season, episode, seq)


# ── Step 1: Create directory tree ──────────────────────────────────────
def step1_create_dirs():
    print("=" * 60)
    print("STEP 1: Creating directory tree")
    print("=" * 60)

    dirs_to_create = []
    for show_key, show_dir in SHOW_DIRS.items():
        # Find max episode for this show
        episodes = [(sid, info) for sid, info in SHOW_MAP.items() if info[0] == show_key]
        max_ep = max(info[2] for _, info in episodes)

        base = os.path.join(DATA_DIR, show_dir, "season-01")
        for ep in range(1, max_ep + 1):
            ep_dir = os.path.join(base, f"episode-{ep:02d}")
            for subdir in ["source", "subtitles", "clips"]:
                dirs_to_create.append(os.path.join(ep_dir, subdir))

    for d in dirs_to_create:
        os.makedirs(d, exist_ok=True)
        print(f"  mkdir -p {d}")

    # Move existing da-s01e01.mp3 to the right place
    old_source = os.path.join(DATA_DIR, "downton-abbey/audio/da-s01e01.mp3")
    new_source = os.path.join(DATA_DIR, "downton-abbey/season-01/episode-01/source/da-s01e01.mp3")
    if os.path.exists(old_source) and not os.path.exists(new_source):
        os.rename(old_source, new_source)
        print(f"  Moved: {old_source} → {new_source}")

    # Clean up old flat audio dirs (they'll be replaced by season/episode structure)
    for show_dir in SHOW_DIRS.values():
        old_audio = os.path.join(DATA_DIR, show_dir, "audio")
        if not os.path.isdir(old_audio):
            continue
        # Remove symlinks directly in audio/ (e.g. game-of-thrones/audio/wt0001.mp3)
        # and in audio/clips/ (e.g. downton-abbey/audio/clips/da0001.mp3)
        for item in os.listdir(old_audio):
            fp = os.path.join(old_audio, item)
            if os.path.islink(fp):
                os.unlink(fp)
            elif os.path.isdir(fp):
                # Remove symlinks in subdirectory first
                for sub in os.listdir(fp):
                    sub_fp = os.path.join(fp, sub)
                    if os.path.islink(sub_fp):
                        os.unlink(sub_fp)
                os.rmdir(fp)
                print(f"  Removed old dir: {fp}")
        # Now audio/ should be empty (or have the source mp3 we moved)
        remaining = os.listdir(old_audio)
        if not remaining:
            os.rmdir(old_audio)
            print(f"  Removed old audio dir: {old_audio}")
        else:
            print(f"  Kept {old_audio} ({len(remaining)} items remaining)")

    print()


# ── Step 2: Copy audio files with new names ────────────────────────────
def step2_copy_audio():
    print("=" * 60)
    print("STEP 2: Copying audio files with new names")
    print("=" * 60)

    total_copied = 0
    total_skipped = 0

    for show_id, info in SHOW_MAP.items():
        show_key, season, episode, pattern, desc = info
        # Query all audio_file for this show
        sql = f"SELECT audio_file FROM sentences WHERE show_id={show_id} AND audio_file IS NOT NULL"
        result = run_sql(sql)
        if not result:
            print(f"  [{desc}] No results from DB")
            continue

        lines = result.split("\n")[1:]  # skip header
        copied = 0
        skipped = 0
        for line in lines:
            old_name = line.strip()
            if not old_name:
                continue
            new_name = make_old_to_new(old_name, show_id)
            if not new_name:
                print(f"  WARN: Could not map '{old_name}' for show_id={show_id}")
                skipped += 1
                continue

            old_path = os.path.join(AUDIO_DIR, old_name)
            new_path = os.path.join(AUDIO_DIR, new_name)

            if os.path.exists(new_path):
                skipped += 1
                continue

            if not os.path.exists(old_path):
                print(f"  WARN: Source missing: {old_path}")
                skipped += 1
                continue

            subprocess.run(["sudo", "cp", old_path, new_path], check=True)
            copied += 1

        total_copied += copied
        total_skipped += skipped
        print(f"  [{desc}] copied={copied}, skipped={skipped}")

    print(f"  TOTAL: copied={total_copied}, skipped={total_skipped}")
    print()


# ── Step 3: Update database ────────────────────────────────────────────
def step3_update_db():
    print("=" * 60)
    print("STEP 3: Updating database")
    print("=" * 60)

    for show_id, info in SHOW_MAP.items():
        show_key, season, episode, pattern, desc = info

        if show_id == 2:
            # wt0001.mp3 → got-S01E01-0001.mp3
            # Need to pad: wt0001 → got-S01E01-0001 (already 4 digits)
            # But wt might have varying digits? Let's check: wt0001 is 4 digits.
            # Use CONCAT and SUBSTRING
            sql = (
                f"UPDATE sentences SET audio_file = CONCAT('got-S01E01-', SUBSTRING(audio_file, 3, 4), '.mp3') "
                f"WHERE show_id = {show_id} AND audio_file LIKE 'wt%.mp3'"
            )
        elif 3 <= show_id <= 11:
            ep_str = f"S01E{episode:02d}"
            # wtS01EXX_NNNN.mp3 → got-S01EXX-NNNN.mp3
            prefix = f"wt{ep_str}_"
            new_prefix = f"got-{ep_str}-"
            sql = (
                f"UPDATE sentences SET audio_file = CONCAT('{new_prefix}', SUBSTRING(audio_file, {len(prefix)+1}, 4), '.mp3') "
                f"WHERE show_id = {show_id} AND audio_file LIKE '{prefix}%.mp3'"
            )
        elif show_id == 12:
            # da0001.mp3 → da-S01E01-0001.mp3
            sql = (
                f"UPDATE sentences SET audio_file = CONCAT('da-S01E01-', SUBSTRING(audio_file, 3, 4), '.mp3') "
                f"WHERE show_id = {show_id} AND audio_file LIKE 'da%.mp3'"
            )
        else:
            continue

        print(f"  [{desc}] Running UPDATE...")
        result = run_sql(sql)
        if result is not None:
            # Check how many rows were affected
            check = run_sql(f"SELECT COUNT(*) FROM sentences WHERE show_id={show_id} AND audio_file LIKE '%S01E%'.mp3'")
            if check:
                count_line = check.split("\n")[-1]
                print(f"  [{desc}] Rows with new format: {count_line}")
        else:
            print(f"  [{desc}] UPDATE failed!")

    # Verify a few samples
    print("\n  Verification samples:")
    for show_id in [2, 3, 12]:
        check = run_sql(f"SELECT audio_file FROM sentences WHERE show_id={show_id} LIMIT 3")
        if check:
            print(f"  show_id={show_id}:")
            for line in check.split("\n")[1:]:
                print(f"    {line.strip()}")

    print()


# ── Step 4: Create symlinks ────────────────────────────────────────────
def step4_create_symlinks():
    print("=" * 60)
    print("STEP 4: Creating symlinks in data/ tree")
    print("=" * 60)

    total_links = 0

    for show_id, info in SHOW_MAP.items():
        show_key, season, episode, pattern, desc = info
        show_dir = SHOW_DIRS[show_key]

        # Query new audio_file names for this show
        sql = f"SELECT audio_file FROM sentences WHERE show_id={show_id} AND audio_file IS NOT NULL"
        result = run_sql(sql)
        if not result:
            continue

        lines = result.split("\n")[1:]  # skip header
        clips_dir = os.path.join(
            DATA_DIR, show_dir, f"season-{season:02d}",
            f"episode-{episode:02d}", "clips"
        )
        os.makedirs(clips_dir, exist_ok=True)

        created = 0
        for line in lines:
            fname = line.strip()
            if not fname:
                continue
            link_path = os.path.join(clips_dir, fname)
            target = os.path.join(AUDIO_DIR, fname)

            if os.path.exists(link_path):
                continue  # already exists

            if not os.path.exists(target):
                print(f"  WARN: Target missing for symlink: {target}")
                continue

            os.symlink(target, link_path)
            created += 1

        total_links += created
        print(f"  [{desc}] created {created} symlinks in {clips_dir}")

    print(f"  TOTAL: {total_links} symlinks created")
    print()


# ── Step 5: Update README ──────────────────────────────────────────────
def step5_update_readme():
    print("=" * 60)
    print("STEP 5: Updating README.md")
    print("=" * 60)

    readme_content = """# 数据目录 — Data

## 目录结构

```
data/
├── README.md                           ← 本文件
├── game-of-thrones/                    ← 权游内容
│   ├── README.md                       ← 剧集元数据
│   └── season-01/
│       ├── episode-01/
│       │   ├── source/                 ← 原始整集音频（空，等上传）
│       │   ├── subtitles/              ← 字幕文件（空，等上传）
│       │   └── clips/                  ← 软链接 → backend/data/audio/got-S01E01-*.mp3
│       ├── episode-02/
│       │   └── clips/
│       ├── ...
│       └── episode-10/
│           └── clips/
└── downton-abbey/                      ← 唐顿庄园内容
    ├── README.md
    └── season-01/
        └── episode-01/
            ├── source/
            │   └── da-s01e01.mp3       ← 原始整集音频
            ├── subtitles/              ← 空（等上传）
            └── clips/                  ← 软链接 → backend/data/audio/da-S01E01-*.mp3
```

## 规则

- **`backend/data/audio/`** — 后端服务目录，所有音频片段的正式位置，**不要移动**
- **`data/<show>/season-XX/episode-XX/clips/`** — 软链接指向 backend，便于浏览
- **`data/<show>/season-XX/episode-XX/source/`** — 原始整集音频文件（上传源）
- **`data/<show>/season-XX/episode-XX/subtitles/`** — 字幕文件（.srt / .ass）

## 命名规范

| 格式 | 示例 | 说明 |
|------|------|------|
| `{show_key}-S{季}E{集}-{序号}.mp3` | `got-S01E01-0001.mp3` | 音频片段命名 |

| show_key | 剧集 | 范围 |
|----------|------|------|
| `got` | Game of Thrones (权游) | got-S01E01-0001 ~ got-S01E10-NNNN |
| `da` | Downton Abbey (唐顿) | da-S01E01-0001 ~ da-S01E01-1097 |
"""

    readme_path = os.path.join(DATA_DIR, "README.md")
    with open(readme_path, "w") as f:
        f.write(readme_content)
    print(f"  Updated {readme_path}")

    # Create show-level READMEs
    got_readme = """# Game of Thrones

## 剧集信息
- **show_key**: got
- **季数**: Season 01
- **集数**: 10 episodes

## 音频统计
| 集数 | 句数 |
|------|------|
| S01E01 | 384 |
| S01E02 | 426 |
| S01E03 | 543 |
| S01E04 | 542 |
| S01E05 | 579 |
| S01E06 | 389 |
| S01E07 | 527 |
| S01E08 | 421 |
| S01E09 | 459 |
| S01E10 | 371 |
"""
    got_readme_path = os.path.join(DATA_DIR, "game-of-thrones", "README.md")
    with open(got_readme_path, "w") as f:
        f.write(got_readme)
    print(f"  Created {got_readme_path}")

    da_readme = """# Downton Abbey

## 剧集信息
- **show_key**: da
- **季数**: Season 01
- **集数**: 1 episode (so far)

## 音频统计
| 集数 | 句数 |
|------|------|
| S01E01 | 1097 |
"""
    da_readme_path = os.path.join(DATA_DIR, "downton-abbey", "README.md")
    with open(da_readme_path, "w") as f:
        f.write(da_readme)
    print(f"  Created {da_readme_path}")
    print()


# ── Step 6: Verify ─────────────────────────────────────────────────────
def step6_verify():
    print("=" * 60)
    print("STEP 6: Verification")
    print("=" * 60)

    # Check file counts
    wt_count = len([f for f in os.listdir(AUDIO_DIR) if f.startswith("wt")])
    da_count = len([f for f in os.listdir(AUDIO_DIR) if f.startswith("da") and not f.startswith("da-")])
    got_count = len([f for f in os.listdir(AUDIO_DIR) if f.startswith("got-")])
    da_new_count = len([f for f in os.listdir(AUDIO_DIR) if f.startswith("da-S01E01-")])
    hash_count = len([f for f in os.listdir(AUDIO_DIR)
                       if not f.startswith("wt") and not f.startswith("da") and not f.startswith("got-")])
    total = len(os.listdir(AUDIO_DIR))

    print(f"  backend/data/audio/ file counts:")
    print(f"    Old wt* files:     {wt_count}")
    print(f"    Old da* files:     {da_count}")
    print(f"    New got-* files:   {got_count}")
    print(f"    New da-S01E01-*:   {da_new_count}")
    print(f"    Hash TTS cache:    {hash_count}")
    print(f"    Total files:       {total}")

    # Check symlinks
    for show_dir in SHOW_DIRS.values():
        for ep in range(1, 11):
            clips = os.path.join(DATA_DIR, show_dir, "season-01", f"episode-{ep:02d}", "clips")
            if os.path.isdir(clips):
                link_count = len([f for f in os.listdir(clips) if os.path.islink(os.path.join(clips, f))])
                if link_count > 0:
                    print(f"  Symlinks in {show_dir}/season-01/episode-{ep:02d}/clips/: {link_count}")

    # DB verification
    print("\n  Database verification:")
    for show_id in [2, 3, 4, 12]:
        result = run_sql(f"SELECT audio_file FROM sentences WHERE show_id={show_id} LIMIT 2")
        if result:
            lines = result.split("\n")[1:]
            names = [l.strip() for l in lines if l.strip()]
            print(f"    show_id={show_id}: {names}")

    print()


# ── Main ───────────────────────────────────────────────────────────────
def main():
    print("\n" + "=" * 60)
    print("Audio Reorganization Script")
    print("=" * 60 + "\n")

    step1_create_dirs()
    step2_copy_audio()
    step3_update_db()
    step4_create_symlinks()
    step5_update_readme()
    step6_verify()

    print("=" * 60)
    print("DONE! All steps completed.")
    print("=" * 60)


if __name__ == "__main__":
    main()
