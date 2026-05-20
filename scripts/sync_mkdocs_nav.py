#!/usr/bin/env python3
"""
Generate mkdocs-awesome-pages `.pages` files for each chapter from Plan.md titles.
Run before `mkdocs build` (CI does this automatically).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path


def parse_handbook_titles(plan_text: str) -> dict[int, str]:
    """Map chapter number -> title from handbook tables (before case studies)."""
    handbook = plan_text.split("# System Design Case Studies", 1)[0]
    titles: dict[int, str] = {}
    for m in re.finditer(r"^\|\s*(\d+)\s*\|\s*([^|]+?)\s*\|", handbook, re.MULTILINE):
        n = int(m.group(1))
        title = re.sub(r"\*\*([^*]+)\*\*", r"\1", m.group(2).strip())
        if title.lower() in {"topic", "#"}:
            continue
        titles[n] = title
    return titles


def yaml_escape_title(title: str) -> str:
    if '"' in title and "'" in title:
        title = title.replace('"', '\\"')
        return f'"{title}"'
    if '"' in title:
        return repr(title)
    if any(ch in title for ch in (":", "#", "'", "\n")):
        return repr(title)
    return f'"{title}"'


def write_chapter_pages(chapter_dir: Path, titles: dict[int, str]) -> None:
    parts = chapter_dir.name.split("-", 1)
    if not parts[0].isdigit():
        return
    num = int(parts[0])
    title = titles.get(num, parts[1].replace("-", " ").title())

    nav_lines: list[str] = []
    if (chapter_dir / "README.md").exists():
        nav_lines.append("  - README.md")
    if (chapter_dir / "interview-questions.md").exists():
        nav_lines.append("  - interview-questions.md")
    diagrams = chapter_dir / "diagrams"
    if diagrams.is_dir() and any(diagrams.glob("*.md")):
        nav_lines.append("  - diagrams")
        diagrams.joinpath(".pages").write_text("sort_type: natural\n", encoding="utf-8")

    body = [f"title: {yaml_escape_title(f'{num:02d} — {title}')}"]
    if nav_lines:
        body.append("nav:")
        body.extend(nav_lines)
    chapter_dir.joinpath(".pages").write_text("\n".join(body) + "\n", encoding="utf-8")


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    plan_path = root / "Plan.md"
    chapters_root = root / "chapters"
    if not plan_path.is_file():
        print("Plan.md not found", file=sys.stderr)
        return 1
    if not chapters_root.is_dir():
        print("chapters/ not found", file=sys.stderr)
        return 1

    titles = parse_handbook_titles(plan_path.read_text(encoding="utf-8"))
    chapter_dirs = sorted(
        (p for p in chapters_root.iterdir() if p.is_dir() and p.name[0:2].isdigit()),
        key=lambda p: p.name,
    )
    for ch in chapter_dirs:
        write_chapter_pages(ch, titles)

    chapters_root.joinpath(".pages").write_text(
        "title: Handbook\nsort_type: natural\n",
        encoding="utf-8",
    )

    leadership = root / "leadership" / "diagrams"
    if leadership.is_dir():
        leadership.joinpath(".pages").write_text("sort_type: natural\n", encoding="utf-8")

    print(f"synced {len(chapter_dirs)} chapter .pages files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
