"""Helpers shared by the Apeira asset scripts in this folder.

Only the Python standard library is used, so the scripts run on any Python 3.10+ without installing anything.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

#: Root of the repository, which the scripts' default paths are relative to.
REPO_ROOT = Path(__file__).resolve().parent.parent

#: Assets of the content module, where converted resources go.
MAIN_ASSETS = REPO_ROOT / "apeira-main" / "src" / "main" / "resources" / "assets" / "apeira"

_CAMEL_BOUNDARY = re.compile(r"(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
_NOT_SNAKE = re.compile(r"[^a-z0-9]+")


class ScriptError(Exception):
    """A problem the user has to fix, reported without a traceback."""


# Hitbox exclusion: a cube, Java model element, outliner group or GeckoLib bone whose name contains this tag, in any
# place among the name's other tags (``fan_blades_ignore``, ``door_tintable_ignore``, ``door_ignore_tintable``), is
# left out of every generated hitbox, as is everything inside such a group or bone. It changes nothing else: the
# piece still draws it, and a ``_tintable`` tag in the same name still takes paint.
IGNORE_TAG = "_ignore"


def hitbox_ignored(name: Any) -> bool:
    """Whether ``name`` (a cube, element, group or bone name) carries the ``_ignore`` hitbox tag, in any case."""
    return isinstance(name, str) and IGNORE_TAG in name.lower()


def java_ignored_elements(data: dict) -> set[int]:
    """Indices of a Java block model's elements left out of hitboxes: an element named with ``_ignore``, or one inside
    a Blockbench ``groups`` entry (nested, children as element indices) whose name, or an enclosing group's, has it."""
    ignored = {i for i, element in enumerate(data.get("elements", [])) if hitbox_ignored(element.get("name"))}

    def walk(nodes, inside):
        for node in nodes:
            if isinstance(node, dict):
                walk(node.get("children", []), inside or hitbox_ignored(node.get("name")))
            elif isinstance(node, int) and inside:
                ignored.add(node)
    walk(data.get("groups", []), False)
    return ignored


def to_snake_case(name: str) -> str:
    """Turns ``"Platinum Sword-Glow2"`` or ``"platinumSword"`` into ``"platinum_sword_glow2"``/``"platinum_sword"``.

    The result only holds ``a-z``, ``0-9`` and single underscores, so it is a valid identifier path segment.
    """
    snake = _NOT_SNAKE.sub("_", _CAMEL_BOUNDARY.sub("_", name).lower()).strip("_")
    if not snake:
        raise ScriptError(f"{name!r} has no letters or digits to build a name from")
    return snake


def split_id(value: str, default_namespace: str = "minecraft") -> tuple[str, str]:
    """Splits ``"ns:path"`` into its parts; a bare path belongs to ``default_namespace``."""
    namespace, sep, path = value.partition(":")
    return (namespace, path) if sep else (default_namespace, value)


def read_json(path: Path) -> Any:
    try:
        with path.open(encoding="utf-8-sig") as handle:
            return json.load(handle)
    except json.JSONDecodeError as error:
        raise ScriptError(f"{path}: invalid JSON ({error})") from error
    except OSError as error:
        raise ScriptError(f"{path}: cannot read ({error.strerror})") from error


def write_json(path: Path, data: Any, *, dry_run: bool) -> None:
    """Writes ``data`` as two-space indented JSON, creating parent folders. Does nothing on a dry run."""
    if dry_run:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def relative(path: Path) -> str:
    """``path`` relative to the repository when it is inside it, for shorter log lines."""
    try:
        return path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return str(path)


def run(main) -> None:
    """Runs a script's ``main()``, turning :class:`ScriptError` and Ctrl+C into a message and exit code."""
    try:
        sys.exit(main())
    except ScriptError as error:
        print(f"error: {error}", file=sys.stderr)
        sys.exit(2)
    except KeyboardInterrupt:
        print("interrupted", file=sys.stderr)
        sys.exit(130)
