#!/usr/bin/env python3
"""Flattens nested texture folders and renames texture files to lowercase snake_case.

``textures/item/custom/minerals/Platinum Sword.png`` becomes ``textures/item/platinum_sword.png``. The first
folder level (``item``, ``block``, ``entity`` and so on) is kept, because the texture atlases load sprites by
those folders; ``--keep-depth`` changes how many levels stay. Animation files (``.png.mcmeta``) move with their
texture.

Two textures that would end up with the same name are a collision. By default neither is moved and the run
exits with code 1; ``--on-collision prefix`` instead prefixes the parent folder names (``platinum_gear_sword``)
until the names are unique. The script never replaces a file: one that is already at a target is a collision too.

Models that refer to a moved texture, such as ``apeira:item/custom/minerals/platinum_sword``, are updated in
``<assets>/models`` so they keep working (turn off with ``--no-update-models``).

Example::

    python scripts/flatten_textures.py --dry-run
    python scripts/flatten_textures.py --on-collision prefix
"""

from __future__ import annotations

import argparse
import os
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from asset_common import MAIN_ASSETS, ScriptError, read_json, relative, run, to_snake_case, write_json

MCMETA = ".mcmeta"


@dataclass
class Move:
    source: Path  # texture file; its .mcmeta follows it
    target: Path

    @property
    def renamed(self) -> bool:
        return self.source != self.target


def texture_files(textures: Path) -> list[Path]:
    """Every texture under ``textures``; ``.mcmeta`` files whose texture exists are left to that texture."""
    files = []
    for path in sorted(p for p in textures.rglob("*") if p.is_file()):
        if path.name.endswith(MCMETA) and path.with_name(path.name[: -len(MCMETA)]).is_file():
            continue
        files.append(path)
    return files


def snake_file_name(name: str) -> str:
    """``"Platinum Sword.PNG"`` -> ``"platinum_sword.png"``; a lone ``.mcmeta`` keeps its inner extension."""
    if name.endswith(MCMETA):
        return snake_file_name(name[: -len(MCMETA)]) + MCMETA
    stem, dot, extension = name.rpartition(".")
    if not dot or not stem:
        return to_snake_case(name)
    return f"{to_snake_case(stem)}.{extension.lower()}"


def plan(textures: Path, keep_depth: int, on_collision: str) -> tuple[list[Move], list[str]]:
    """Works out every move. Returns the moves and the collisions that could not be resolved."""
    files = texture_files(textures)
    moves: dict[Path, Path] = {}
    folders: dict[Path, list[str]] = {}
    for path in files:
        parts = path.relative_to(textures).parts
        kept = [to_snake_case(part) for part in parts[: min(keep_depth, len(parts) - 1)]]
        moves[path] = textures.joinpath(*kept, snake_file_name(parts[-1]))
        folders[path] = list(parts[len(kept):-1])

    collisions: list[str] = []
    blocked: set[Path] = set()
    while True:
        by_target: dict[Path, list[Path]] = {}
        for source, target in moves.items():
            if source not in blocked:
                by_target.setdefault(target, []).append(source)
        clashes = {target: sources for target, sources in by_target.items() if len(sources) > 1}
        if not clashes:
            break
        for target, sources in clashes.items():
            movable = [s for s in sources if folders[s]] if on_collision == "prefix" else []
            if movable and len(sources) - len(movable) <= 1:
                # Files still in a folder take its name; at most one (already flat) file keeps the plain name.
                for source in movable:
                    folder = to_snake_case(folders[source].pop())
                    moves[source] = moves[source].with_name(f"{folder}_{moves[source].name}")
            else:
                names = ", ".join(relative(s) for s in sources)
                collisions.append(f"{relative(target)} is claimed by {names}")
                blocked.update(sources)

    result = []
    for source, target in moves.items():
        if source in blocked:
            continue
        if target != source and target.exists() and not _same_file(source, target):
            collisions.append(f"{relative(target)} already exists (not moving {relative(source)} onto it)")
            blocked.add(source)
            continue
        result.append(Move(source, target))
    return result, collisions


def _same_file(a: Path, b: Path) -> bool:
    """True when ``a`` and ``b`` are one file, as with a case-only rename on Windows or macOS."""
    try:
        return os.path.samefile(a, b)
    except OSError:
        return False


def move_file(source: Path, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    if _same_file(source, target):
        # Case-insensitive file systems need a detour for "Foo.png" -> "foo.png".
        detour = source.with_name(f".{uuid.uuid4().hex}.tmp")
        source.rename(detour)
        source = detour
    os.replace(source, target)


def texture_id(textures: Path, namespace: str, path: Path) -> str:
    rel = path.relative_to(textures).as_posix()
    return f"{namespace}:{rel.rsplit('.', 1)[0]}"


def update_models(models: Path, renames: dict[str, str], namespace: str, dry_run: bool) -> list[Path]:
    """Rewrites texture references in every model JSON under ``models``. Returns the models changed."""
    changed = []

    def rewrite(value: Any) -> Any:
        if isinstance(value, dict):
            return {key: rewrite(item) for key, item in value.items()}
        if isinstance(value, list):
            return [rewrite(item) for item in value]
        if isinstance(value, str) and value.startswith(f"{namespace}:"):
            return renames.get(value, value)
        return value

    for path in sorted(models.rglob("*.json")):
        data = read_json(path)
        textures = data.get("textures") if isinstance(data, dict) else None
        if not isinstance(textures, dict):
            continue
        new = rewrite(textures)
        if new != textures:
            data["textures"] = new
            write_json(path, data, dry_run=dry_run)
            changed.append(path)
    return changed


def remove_empty_folders(root: Path) -> None:
    for folder in sorted((p for p in root.rglob("*") if p.is_dir()), key=lambda p: len(p.parts), reverse=True):
        if not any(folder.iterdir()):
            folder.rmdir()


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--assets", type=Path, default=MAIN_ASSETS,
                        help=f"namespace assets folder holding textures/ and models/ (default: {relative(MAIN_ASSETS)})")
    parser.add_argument("--namespace", default="apeira", help="namespace of --assets (default: %(default)s)")
    parser.add_argument("--keep-depth", type=int, default=1, metavar="N",
                        help="folder levels under textures/ to keep, such as item/ (default: %(default)s)")
    parser.add_argument("--on-collision", choices=("skip", "prefix"), default="skip",
                        help="skip: leave clashing files where they are and fail; "
                             "prefix: add parent folder names until unique (default: %(default)s)")
    parser.add_argument("--no-update-models", dest="update_models", action="store_false",
                        help="do not rewrite texture references in models/")
    parser.add_argument("--dry-run", action="store_true", help="print the moves without changing anything")
    parser.add_argument("-v", "--verbose", action="store_true", help="list every move")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    textures: Path = args.assets / "textures"
    if not textures.is_dir():
        raise ScriptError(f"{textures} does not exist (pass --assets)")
    if args.keep_depth < 0:
        raise ScriptError("--keep-depth must be 0 or more")

    moves, collisions = plan(textures, args.keep_depth, args.on_collision)

    prefix = "[dry run] " if args.dry_run else ""
    renames: dict[str, str] = {}
    count = 0
    for move in moves:
        if not move.renamed:
            continue
        renames[texture_id(textures, args.namespace, move.source)] = texture_id(textures, args.namespace, move.target)
        count += 1
        if args.verbose or args.dry_run:
            print(f"{prefix}{relative(move.source)} -> {relative(move.target)}")
        if not args.dry_run:
            move_file(move.source, move.target)
            meta = move.source.with_name(move.source.name + MCMETA)
            if meta.is_file():
                move_file(meta, move.target.with_name(move.target.name + MCMETA))
    if not args.dry_run:
        remove_empty_folders(textures)

    changed: list[Path] = []
    models = args.assets / "models"
    if args.update_models and renames and models.is_dir():
        changed = update_models(models, renames, args.namespace, args.dry_run)
        for path in changed:
            if args.verbose or args.dry_run:
                print(f"{prefix}update {relative(path)}")

    for collision in collisions:
        print(f"collision: {collision}")
    print(f"{prefix}{count} textures renamed or moved, {len(changed)} models updated, "
          f"{len(collisions)} collisions left in place")
    return 1 if collisions else 0


if __name__ == "__main__":
    run(main)
