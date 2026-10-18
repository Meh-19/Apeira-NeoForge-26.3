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
