#!/usr/bin/env python3
"""Measures apeira-archae's artifacts as they are drawn and writes their multiblock footprints as a Java table.

Every artifact is drawn by ``ArtifactRenderer`` (its item model in the ``fixed`` display context, at half size, laid
into the block by its display mode) or, for the animated ones, by ``AnimatedArtifactRenderer`` (the GeckoLib model at
full size). This script repeats those transforms on each model's cubes, so the boxes it writes are where the
artifact's cubes really are, in pixels relative to the artifact's own block (0-16 is that block), at rotation 0 (or,
for a wall piece, facing south). apeira-decor's ``Footprint`` turns them for the other rotations and cuts them into
the blocks the artifact covers.

The boxes are clipped where a piece meets what holds it up: floor pieces stop at the floor (y 0), pieces hung under a
block (ceiling variants, chandeliers) at the ceiling (y 16) and wall pieces at the wall. Boxes thinner than a pixel
(rugs, banners, flags) are thickened to one pixel so they can be aimed at. Each piece's boxes are simplified as
``parse_blockbench_hitboxes.py`` does; a piece left with more than ``--max-boxes`` boxes falls back to 4-pixel cells.

The table (``ArtifactFootprints.java``) was generated once and may be edited by hand from then on, like
apeira-decor's ``FurnitureCatalogue``; run this again with ``--force`` only to start over. ``--report`` prints each
piece's size in blocks.

Example::

    python scripts/artifact_footprints.py --report --dry-run
"""

from __future__ import annotations

import argparse
import math
import re
from pathlib import Path

from asset_common import REPO_ROOT, ScriptError, read_json, run
from parse_blockbench_hitboxes import Box, corners, java_rotation, rotate, simplify

ARCHAE = REPO_ROOT / "apeira-archae" / "src" / "main"
ARTIFACTS = ARCHAE / "java" / "com" / "apeira" / "archae" / "registry" / "ArchaeArtifacts.java"
MODELS = ARCHAE / "resources" / "assets" / "apeira" / "models" / "block" / "archaeology"
GEO_MODELS = ARCHAE / "resources" / "assets" / "apeira" / "geckolib" / "models" / "archaeology"
OUTPUT = ARCHAE / "java" / "com" / "apeira" / "archae" / "registry" / "ArtifactFootprints.java"

FORM_MODES = {
    "FURNITURE": "TOP_FACE", "DECORATION": "FLOOR", "FULL_DECORATION": "FLOOR", "FLOOR": "FLOOR",
    "BLOCK_UP": "BLOCK_UP", "WALL_MOUNTED": "WALL", "TWO_TALL": "TOP_FACE", "THREE_TALL": "TOP_FACE",
    "FOUR_TALL": "TOP_FACE", "FOUR_TALL_FLOOR": "FLOOR", "FIVE_TALL": "TOP_FACE",
}
FACING_CLASSES = {"FacingArtifactBlock", "CeilingArtifactBlock", "WaterloggedArtifactBlock"}
DECLARATION = re.compile(r'DeferredBlock<\w+> \w+ = (?:artifact|variant)\("(\w+)",.*?p -> new (\w+)\(p, (?:true|false)'
                         r'(?:, (Form\.\w+|DisplayMode\.\w+|\d+|"\w*"))?', re.S)


# Matrices --------------------------------------------------------------------------------------------------------

def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def chain(*matrices):
    result = translate(0, 0, 0)
    for matrix in matrices:
        result = mul(result, matrix)
    return result


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def scale(x, y, z):
    return [[x, 0, 0, 0], [0, y, 0, 0], [0, 0, z, 0], [0, 0, 0, 1]]


def turn(axis, degrees):
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    return {"x": [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]],
            "y": [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]],
            "z": [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]}[axis]


def apply(matrix, point):
    return tuple(sum(matrix[i][k] * v for k, v in enumerate((*point, 1))) for i in range(3))


# Artifacts -------------------------------------------------------------------------------------------------------

def artifacts() -> list[dict]:
    """Every artifact's name, display mode and how it is held up, from ``ArchaeArtifacts``."""
    pieces = []
    for name, cls, arg in DECLARATION.findall(ARTIFACTS.read_text(encoding="utf-8")):
        facing = cls in FACING_CLASSES
        if cls == "FacingArtifactBlock":
            mode = "WALL" if "wall" in name else "FLOOR" if "side" in name else "TOP_FACE"
        elif cls == "CeilingArtifactBlock" or cls == "SeatArtifactBlock":
            mode = "TOP_FACE"
        elif cls == "WaterloggedArtifactBlock":
            mode = arg.split(".")[1]
        elif cls == "AnimatedArtifactBlock":
            mode = "GECKOLIB"
        else:
            mode = FORM_MODES[arg.split(".")[1]]
        if mode == "WALL" and facing:
            held = "wall"
        elif "ceiling" in name or cls == "CeilingArtifactBlock":
            held = "ceiling"
        elif mode == "WALL":
            held = "none"
        else:
            held = "floor"
        pieces.append({"name": name, "mode": mode, "facing": facing, "held": held})
    if not pieces:
        raise ScriptError(f"{ARTIFACTS}: no artifacts found")
    return pieces


def renderer(mode: str, facing: bool):
    """``ArtifactRenderer``'s transform at rotation 0 (a wall piece facing south), then its half scale."""
    if mode == "TOP_FACE":
        steps = [translate(0.5, 0.999, 0.5), turn("x", 180), turn("x", 90), turn("z", 180)]
    elif mode in ("FLOOR", "BLOCK_UP"):
        steps = [translate(0.5, 0.0 if mode == "FLOOR" else 1.0, 0.5), turn("x", 90), turn("z", 180)]
    else:
        steps = [translate(0.5, 0.5, 0.5)] + ([translate(0, 0, 0.45)] if facing else [])
    return chain(*steps, scale(0.5, 0.5, 0.5))


def item_boxes(piece: dict) -> list[Box]:
    model = read_json(MODELS / f"{piece['name']}.json")
    fixed = model.get("display", {}).get("fixed", {})
    t = fixed.get("translation", [0, 0, 0])
    r = fixed.get("rotation", [0, 0, 0])
    s = fixed.get("scale", [1, 1, 1])
    # Vanilla's ItemTransform: translation, then rotation X, Y, Z, then scale, about the model's centre.
    item = chain(translate(*(v / 16 for v in t)), turn("x", r[0]), turn("y", r[1]), turn("z", r[2]), scale(*s),
                 translate(-0.5, -0.5, -0.5))
    matrix = mul(renderer(piece["mode"], piece["facing"]), item)
    boxes = []
    for element in model.get("elements", []):
        origin, angles = java_rotation(element.get("rotation"))
        points = corners(element["from"], element["to"])
        if any(abs(a) > 1e-6 for a in angles):
            points = [rotate(p, origin, angles) for p in points]
        boxes.append(Box.around(tuple(v * 16 for v in apply(matrix, tuple(c / 16 for c in p))) for p in points))
    return boxes


def geo_boxes(piece: dict) -> list[Box]:
    """A GeckoLib model at full size on the block's bottom centre, x mirrored as GeckoLib draws Bedrock geometry."""
    geo = read_json(GEO_MODELS / f"{piece['name']}.geo.json")["minecraft:geometry"][0]
    boxes = []
    for bone in geo.get("bones", []):
        for cube in bone.get("cubes", []):
            o, size = cube["origin"], cube["size"]
            start = [o[i] + min(size[i], 0) for i in range(3)]
            end = [o[i] + max(size[i], 0) for i in range(3)]
            points = corners(start, end)
            if "rotation" in cube:
                points = [rotate(p, cube.get("pivot", [0, 0, 0]), cube["rotation"]) for p in points]
            boxes.append(Box.around((8 - x, y, 8 + z) for x, y, z in points))
    return boxes


def clip(box: Box, held: str) -> Box | None:
    lo = [box.x1, box.y1, box.z1]
    hi = [box.x2, box.y2, box.z2]
    if held == "floor":
        lo[1] = max(lo[1], 0.0)
    elif held == "ceiling":
        hi[1] = min(hi[1], 16.0)
    elif held == "wall":
        hi[2] = min(hi[2], 16.0)
    if any(h <= l for l, h in zip(lo, hi)):
        return None
    for axis in range(3):
        if hi[axis] - lo[axis] < 1.0:
            middle = (lo[axis] + hi[axis]) / 2
            lo[axis], hi[axis] = middle - 0.5, middle + 0.5
    # Thickening must not push a box through what holds it up.
    for axis, limit, low in ((1, 0.0, True), (1, 16.0, False), (2, 16.0, False)):
        if (held, axis) not in (("floor", 1), ("ceiling", 1), ("wall", 2)) or (held == "floor") != low:
            continue
        shift = limit - lo[axis] if low else limit - hi[axis]
        if (low and shift > 0) or (not low and shift < 0):
            lo[axis] += shift
            hi[axis] += shift
    # Rounded outwards to quarter pixels.
    return Box(*(math.floor(round(v * 4, 3)) / 4 for v in lo), *(math.ceil(round(v * 4, 3)) / 4 for v in hi))


def cells(boxes: list[Box], size: float = 4.0) -> list[Box]:
    """The 4-pixel cells the boxes reach into, merged into rows."""
    filled = set()
    for b in boxes:
        for x in range(math.floor(b.x1 / size), math.ceil(b.x2 / size)):
            for y in range(math.floor(b.y1 / size), math.ceil(b.y2 / size)):
                for z in range(math.floor(b.z1 / size), math.ceil(b.z2 / size)):
                    filled.add((x, y, z))
    # Greedy meshing: runs along x, then runs of equal runs along z, then along y.
    runs = {}
    for y, z, x in sorted((y, z, x) for x, y, z in filled):
        key = (y, z)
        if key in runs and runs[key][-1][1] == x:
            runs[key][-1][1] = x + 1
        else:
            runs.setdefault(key, []).append([x, x + 1])
    slabs = {}
    for (y, z), spans in sorted(runs.items()):
        for x1, x2 in spans:
            key = (y, x1, x2)
            if key in slabs and slabs[key][-1][1] == z:
                slabs[key][-1][1] = z + 1
            else:
                slabs.setdefault(key, []).append([z, z + 1])
    blocks_ = {}
    for (y, x1, x2), spans in sorted(slabs.items()):
        for z1, z2 in spans:
            key = (x1, x2, z1, z2)
            if key in blocks_ and blocks_[key][-1][1] == y:
                blocks_[key][-1][1] = y + 1
            else:
                blocks_.setdefault(key, []).append([y, y + 1])
    return [Box(x1 * size, y1 * size, z1 * size, x2 * size, y2 * size, z2 * size)
            for (x1, x2, z1, z2), spans in blocks_.items() for y1, y2 in spans]


def footprint(piece: dict, max_boxes: int) -> list[Box]:
    raw = geo_boxes(piece) if piece["mode"] == "GECKOLIB" else item_boxes(piece)
    held = "floor" if piece["mode"] == "GECKOLIB" else piece["held"]
    boxes = [b for b in (clip(b, held) for b in raw) if b is not None]
    if not boxes:
        return [Box(0, 0, 0, 16, 16, 16)]
    if len(boxes) <= 4 * max_boxes:
        boxes = simplify(boxes, 0.0)[0]
    if len(boxes) > max_boxes:
        bounds = boxes[0]
        for b in boxes[1:]:
            bounds = bounds.union(b)
        # The cells, cut back to the piece's own bounds (a rug stays a pixel thick).
        boxes = [Box(max(c.x1, bounds.x1), max(c.y1, bounds.y1), max(c.z1, bounds.z1),
                     min(c.x2, bounds.x2), min(c.y2, bounds.y2), min(c.z2, bounds.z2)) for c in cells(boxes)]
        if len(boxes) > 4 * max_boxes:
            boxes = [bounds]
    return boxes


def blocks(boxes: list[Box]) -> tuple[int, int, int]:
    lo = [min(b.lo(a) for b in boxes) for a in range(3)]
    hi = [max(b.hi(a) for b in boxes) for a in range(3)]
    return tuple(math.ceil(hi[a] / 16 - 1e-6) - math.floor(lo[a] / 16 + 1e-6) for a in range(3))


def number(value: float) -> str:
    return str(int(value)) if value == int(value) else f"{value:g}"


def java(pieces: list[tuple[dict, list[Box]]]) -> str:
    lines = []
    for piece, boxes in pieces:
        text = ", ".join(" ".join(number(v) for v in (b.x1, b.y1, b.z1, b.x2, b.y2, b.z2)) for b in boxes)
        lines.append(f'        put("{piece["name"]}", "{text}");')
    body = "\n".join(lines)
    return f'''package com.apeira.archae.registry;

import java.util.HashMap;
import java.util.Map;

import com.apeira.decor.multiblock.Footprint;

/**
 * Where each artifact's cubes are as it is drawn, for apeira-decor's multiblock {{@link Footprint}}: boxes in pixels
 * relative to the artifact's own block (0-16 is that block, so a box may reach into the blocks around it), at rotation
 * 0, or for a wall piece facing south. Floor pieces stop at the floor, hanging pieces at the ceiling and wall pieces at
 * the wall; flat parts are a pixel thick.
 *
 * <p>Generated once from the artifacts' models by {{@code scripts/artifact_footprints.py}}, which repeats the
 * renderers' transforms; edited by hand from now on. Each line is a list of {{@code x1 y1 z1 x2 y2 z2}} boxes.
 */
public final class ArtifactFootprints {{
    private static final Map<String, Footprint> BY_NAME = new HashMap<>();

    static {{
{body}
    }}

    private ArtifactFootprints() {{}}

    /** The footprint of the artifact {{@code name}}, or a single full block for one not in the table. */
    public static Footprint of(String name) {{
        return BY_NAME.getOrDefault(name, Footprint.BLOCK);
    }}

    private static void put(String name, String boxes) {{
        BY_NAME.put(name, Footprint.parse(boxes));
    }}
}}
'''


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("-o", "--output", type=Path, default=OUTPUT, help="the Java table to write")
    parser.add_argument("--max-boxes", type=int, default=12, help="boxes a piece may keep before 4-pixel cells")
    parser.add_argument("--report", action="store_true", help="print each piece's size in blocks")
    parser.add_argument("--force", action="store_true", help="replace an existing table")
    parser.add_argument("--dry-run", action="store_true", help="only print what would be written")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    pieces = [(piece, footprint(piece, args.max_boxes)) for piece in artifacts()]
    if args.report:
        for piece, boxes in pieces:
            x, y, z = blocks(boxes)
            print(f"{piece['name']:40} {piece['mode']:9} {piece['held']:8} {x}x{y}x{z} {len(boxes)} boxes")
    multi = sum(1 for _, boxes in pieces if blocks(boxes) != (1, 1, 1))
    print(f"{len(pieces)} artifacts, {multi} of them more than one block")
    if args.output.exists() and not args.force:
        print(f"{args.output} exists; use --force to replace it")
        return 0
    if not args.dry_run:
        args.output.write_text(java(pieces), encoding="utf-8", newline="\n")
        print(f"wrote {args.output}")
    return 0


if __name__ == "__main__":
    run(main)
