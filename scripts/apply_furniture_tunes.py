#!/usr/bin/env python3
"""Apply the furniture tuner's tune log to code and delete the applied entries from the log.

apeira-dev's furniture tuner logs every tuned piece to ``logs/apeira_furniture_tune_log.json`` in the game directory
(``apeira-main/run/logs/`` in dev), keyed by block id: removed footprint boxes, seats, light, collision and the model's offset and yaw. This script
moves those entries into decor's baked tunes, ``apeira-decor/src/main/resources/apeira_decor/furniture_tunes.json``
(read by ``FurnitureTunes``), and removes every entry it applied from the log, so a stale entry is never applied twice.
Entries already in the baked file are replaced. ``--only`` picks block ids, ``--dry-run`` writes nothing.

The game also prunes the log on its own: on each join, apeira-dev drops every entry the code already matches.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

DEFAULT_LOG = Path("apeira-main/run/logs/apeira_furniture_tune_log.json")
DEFAULT_TUNES = Path("apeira-decor/src/main/resources/apeira_decor/furniture_tunes.json")
# Fields the log keeps only for review.
LOG_ONLY = ("logged_at",)


def load(path: Path) -> dict:
    if not path.is_file():
        return {}
    text = path.read_text(encoding="utf-8").strip()
    return json.loads(text) if text else {}


def dump(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def baked_entry(entry: dict) -> dict:
    """The log entry as the baked file keeps it: without log-only fields and empty lists."""
    baked = {}
    for key, value in entry.items():
        if key in LOG_ONLY or value == []:
            continue
        if key == "light" and value == -1:
            continue
        baked[key] = value
    return baked


def apply(log: dict, tunes: dict, only: set[str] | None) -> tuple[dict, dict, list[str]]:
    """Returns the new log, the new baked tunes and the applied block ids."""
    applied = [block for block in log if only is None or block in only]
    new_tunes = dict(tunes)
    for block in applied:
        new_tunes[block] = baked_entry(log[block])
    new_log = {block: entry for block, entry in log.items() if block not in applied}
    return new_log, new_tunes, applied


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--log", type=Path, default=DEFAULT_LOG, help=f"the tune log (default {DEFAULT_LOG})")
    parser.add_argument("--tunes", type=Path, default=DEFAULT_TUNES, help=f"the baked tunes file (default {DEFAULT_TUNES})")
    parser.add_argument("--only", action="append", metavar="BLOCK_ID", help="apply only this block id (repeatable)")
    parser.add_argument("--dry-run", action="store_true", help="only print what would be applied")
    args = parser.parse_args(argv)

    log = load(args.log)
    if not log:
        print(f"Nothing to apply: {args.log} is empty or missing")
        return 0
    new_log, new_tunes, applied = apply(log, load(args.tunes), set(args.only) if args.only else None)
    for block in applied:
        print(f"{'would apply' if args.dry_run else 'applied'} {block}: {json.dumps(baked_entry(log[block]), sort_keys=True)}")
    if not applied:
        print("No log entry matched --only")
        return 1
    if not args.dry_run:
        dump(args.tunes, new_tunes)
        dump(args.log, new_log)
        print(f"Wrote {args.tunes}; removed {len(applied)} applied entries from {args.log} ({len(new_log)} left)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
