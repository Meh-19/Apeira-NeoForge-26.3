#!/usr/bin/env python3
"""Find and fix cubes in GeckoLib/Bedrock geometry (``.geo.json``) that cause z-fighting or inside-out faces.

Two kinds of cube are fixed:

* Inverted cubes (a negative ``size`` on any axis, as Blockbench writes a cube whose ``to`` is below its ``from``):
  the cube is drawn inside out. It is rewritten with a positive size and its origin at its lowest corner; each face's
  UV moves to the face that now sits where it was drawn, mirrored where the turn-around mirrors it, so the texture
  lands on the same spot as before.
* Flat cubes (a ``size`` of 0, or under 0.01 pixels, on one axis) with both faces of that axis showing the same
  texture area (as is, or mirrored to line up from behind): the two faces lie in one plane, and GeckoLib's render
  types draw both sides of every face, so the pair z-fights. The back face (south, west or down) is dropped; the
  front one is seen from both sides. Pairs showing different texture areas (a flag's two sides) are only reported.

Without ``--force`` the files are only checked and every bad cube is printed (exit code 1 when there are any); with
``--force`` they are rewritten in place, two-space indented.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

FLAT = 0.01
AXES = {"x": 0, "y": 1, "z": 2}
# Each face's outward axis and the world directions of its texture's u (right) and v (down), Minecraft's convention.
FACES = {
    "north": ("z", -1, (-1, 0, 0), (0, -1, 0)),
    "south": ("z", 1, (1, 0, 0), (0, -1, 0)),
    "east": ("x", 1, (0, 0, -1), (0, -1, 0)),
    "west": ("x", -1, (0, 0, 1), (0, -1, 0)),
    "up": ("y", 1, (1, 0, 0), (0, 0, 1)),
    "down": ("y", -1, (1, 0, 0), (0, 0, -1)),
}
PAIRS = (("north", "south"), ("east", "west"), ("up", "down"))


def face_at(axis: str, sign: int) -> str:
    return next(name for name, (a, s, _, _) in FACES.items() if a == axis and s == sign)


def reflect(vector, flipped) -> tuple:
    return tuple(-c if flipped[i] else c for i, c in enumerate(vector))


def mirror(face: dict, index: int) -> dict:
    face = dict(face)
    uv, size = list(face["uv"]), list(face["uv_size"])
    uv[index] += size[index]
    size[index] = -size[index]
    face["uv"], face["uv_size"] = uv, size
    return face


def uninvert(cube: dict) -> dict:
    """The cube with a positive size, each face's UV moved and mirrored to draw where it drew before."""
    size = cube["size"]
    flipped = [v < 0 for v in size]
    cube = dict(cube)
    cube["origin"] = [round(o + s, 6) if f else o for o, s, f in zip(cube["origin"], size, flipped)]
    cube["size"] = [abs(v) for v in size]
    uv = cube.get("uv")
    if isinstance(uv, dict):
        moved = {}
        for name, face in uv.items():
            axis, sign, u, v = FACES[name]
            target = face_at(axis, -sign if flipped[AXES[axis]] else sign)
            _, _, tu, tv = FACES[target]
            if "uv_size" in face:
                if reflect(u, flipped) != tu:
                    face = mirror(face, 0)
                if reflect(v, flipped) != tv:
                    face = mirror(face, 1)
            moved[target] = face
        cube["uv"] = {name: moved[name] for name in FACES if name in moved}
    return cube


def area(face: dict) -> tuple:
    """The texture rectangle a face shows, whichever way round."""
    (u, v), (w, h) = face["uv"], face.get("uv_size", (0, 0))
    return min(u, u + w), min(v, v + h), abs(w), abs(h)


def flatten(cube: dict) -> tuple[dict, list[str]]:
    """The flat cube with the back face of each coplanar pair showing the same texture dropped, and the pairs kept."""
    uv = cube.get("uv")
    if not isinstance(uv, dict):
        return cube, []
    cube = dict(cube)
    uv = dict(uv)
    kept = []
    for front, back in PAIRS:
        axis = AXES[FACES[front][0]]
        if abs(cube["size"][axis]) < FLAT and front in uv and back in uv:
            if area(uv[front]) == area(uv[back]):
                del uv[back]
            else:
                kept.append(f"{front}/{back}")
    cube["uv"] = uv
    return cube, kept


def fix(geometry: dict, warnings: list[str] | None = None) -> list[str]:
    """Fixes the geometry in place and returns a line for every cube changed; pairs it can't fix go to warnings."""
    warnings = [] if warnings is None else warnings
    changes = []
    for model in geometry.get("minecraft:geometry", []):
        for bone in model.get("bones", []):
            cubes = []
            for index, cube in enumerate(bone.get("cubes", [])):
                where = f"{bone['name']}[{index}] origin {cube['origin']} size {cube['size']}"
                if any(v < 0 for v in cube["size"]):
                    cube = uninvert(cube)
                    changes.append(f"inverted cube {where}: now origin {cube['origin']} size {cube['size']}")
                if any(abs(v) < FLAT for v in cube["size"]):
                    flat, kept = flatten(cube)
                    if flat.get("uv") != cube.get("uv"):
                        changes.append(f"flat cube {where}: kept faces {sorted(flat['uv'])}")
                    for pair in kept:
                        warnings.append(f"flat cube {where}: {pair} show different textures, left as they are")
                    cube = flat
                cubes.append(cube)
            if "cubes" in bone:
                bone["cubes"] = cubes
    return changes


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("files", nargs="+", type=Path, help=".geo.json files to check")
    parser.add_argument("--force", action="store_true", help="rewrite the files with the bad cubes fixed")
    args = parser.parse_args(argv)
    found = False
    for path in args.files:
        geometry = json.loads(path.read_text(encoding="utf-8"))
        warnings: list[str] = []
        changes = fix(geometry, warnings)
        for line in changes:
            print(f"{path}: {line}")
        for line in warnings:
            print(f"{path}: warning: {line}", file=sys.stderr)
        if changes:
            found = True
            if args.force:
                path.write_text(json.dumps(geometry, indent=2) + "\n", encoding="utf-8")
    return 1 if found and not args.force else 0


if __name__ == "__main__":
    sys.exit(main())
