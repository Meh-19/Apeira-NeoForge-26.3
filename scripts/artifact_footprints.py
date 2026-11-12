#!/usr/bin/env python3
"""Measures apeira-archae's artifacts as they are drawn and writes their hitboxes, cut from the models' cubes.

Every artifact is drawn by ``ArtifactRenderer`` (its item model in the ``fixed`` display context, at half size, laid
into the block by its display mode and turned by its rotation) or, for the animated ones, by
``AnimatedArtifactRenderer`` (the GeckoLib model at full size). This script repeats those transforms on each model's
cubes at rotation 0 and at rotation 1 (45 degrees), cuts the turned cubes into whole pixels (a pixel is in when its
centre is inside a cube; cubes thinner than a pixel, such as rugs, banners and flags, are thickened to a pixel) and
meshes the pixels into boxes, trying every axis order of the greedy meshing and keeping the fewest. So a hitbox is
only where the model has a cube: no air between a tree's branches, and no box around a turned box at the diagonals.
The other rotations are quarter turns of these two, which apeira-decor's ``Footprint`` makes exactly; wall pieces only
face the four sides, so they keep only their straight boxes.

Pixels are cut where a piece meets what holds it up: floor pieces at the floor (y 0), pieces hung under a block
(ceiling variants, chandeliers) at the ceiling (y 16) and wall pieces at the wall, and past a multiblock part's
reach from the controller.

The table (``apeira-archae/src/main/resources/apeira_archae/artifact_footprints.txt``, read by
``ArtifactFootprints``) is generated; run this again after changing an artifact's model. ``--report`` prints each
piece's size in blocks and box counts.

Example::

    python scripts/artifact_footprints.py --report --dry-run
"""

from __future__ import annotations

import argparse
import math
import re
from pathlib import Path

from asset_common import REPO_ROOT, ScriptError, read_json, run
from parse_blockbench_hitboxes import Box, java_rotation, rotate, simplify

ARCHAE = REPO_ROOT / "apeira-archae" / "src" / "main"
ARTIFACTS = ARCHAE / "java" / "com" / "apeira" / "archae" / "registry" / "ArchaeArtifacts.java"
MODELS = ARCHAE / "resources" / "assets" / "apeira" / "models" / "block" / "archaeology"
GEO_MODELS = ARCHAE / "resources" / "assets" / "apeira" / "geckolib" / "models" / "archaeology"
THIN = 1.02
# Turned cubes are cut into cells this many pixels wide.
STEP = 2.0
# Outline edges are tested every SAMPLE pixels, EPS pixels to each side.
SAMPLE = 0.5
EPS = 0.05
REACH = 4
HEIGHT = 6
OUTPUT = ARCHAE / "resources" / "apeira_archae" / "artifact_footprints.txt"

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


def renderer(mode: str, facing: bool, turn_: int):
    """``ArtifactRenderer``'s (or ``AnimatedArtifactRenderer``'s) transform at ``turn_`` 45 degree steps, in blocks.

    ``turn_`` is the block's ``ROTATION``; for a wall piece it is its facing's ``toYRot() / 45`` (south is 0).
    """
    if mode == "GECKOLIB":
        # GeoBlockRenderer stands the model on the block's bottom centre and Bedrock geometry is x mirrored.
        return chain(translate(0.5, 0, 0.5), turn("y", -45 * turn_), scale(-1 / 16, 1 / 16, 1 / 16))
    if mode == "TOP_FACE":
        steps = [translate(0.5, 0.999, 0.5), turn("x", 180), turn("x", 90), turn("z", 180), turn("z", -45 * turn_)]
    elif mode in ("FLOOR", "BLOCK_UP"):
        steps = [translate(0.5, 0.0 if mode == "FLOOR" else 1.0, 0.5), turn("x", 90), turn("z", 180),
                 turn("z", 45 * turn_)]
    else:
        steps = [translate(0.5, 0.5, 0.5)] + ([turn("y", -45 * turn_), translate(0, 0, 0.45)] if facing else [])
    return chain(*steps, scale(0.5, 0.5, 0.5))


def model_cubes(piece: dict) -> tuple[list, list]:
    """The model's cubes as (corner, three edges) in the model's own units, and the matrix into the renderer's frame."""
    cubes = []
    if piece["mode"] == "GECKOLIB":
        geo = read_json(GEO_MODELS / f"{piece['name']}.geo.json")["minecraft:geometry"][0]
        for bone in geo.get("bones", []):
            for cube in bone.get("cubes", []):
                o, size = cube["origin"], cube["size"]
                start = [o[i] + min(size[i], 0) for i in range(3)]
                end = [o[i] + max(size[i], 0) for i in range(3)]
                if "rotation" in cube:
                    pivot, angles = cube.get("pivot", [0, 0, 0]), cube["rotation"]
                    point = lambda p, pivot=pivot, angles=angles: rotate(p, pivot, angles)
                else:
                    point = lambda p: tuple(p)
                cubes.append(oriented(start, end, point))
        return cubes, translate(0, 0, 0)
    model = read_json(MODELS / f"{piece['name']}.json")
    fixed = model.get("display", {}).get("fixed", {})
    t = fixed.get("translation", [0, 0, 0])
    r = fixed.get("rotation", [0, 0, 0])
    s = fixed.get("scale", [1, 1, 1])
    # Vanilla's ItemTransform: translation, then rotation X, Y, Z, then scale, about the model's centre.
    item = chain(translate(*(v / 16 for v in t)), turn("x", r[0]), turn("y", r[1]), turn("z", r[2]), scale(*s),
                 translate(-0.5, -0.5, -0.5), scale(1 / 16, 1 / 16, 1 / 16))
    for element in model.get("elements", []):
        origin, angles = java_rotation(element.get("rotation"))
        if any(abs(a) > 1e-6 for a in angles):
            point = lambda p, origin=origin, angles=angles: rotate(p, origin, angles)
        else:
            point = lambda p: tuple(p)
        cubes.append(oriented(element["from"], element["to"], point))
    return cubes, item


def oriented(start, end, point) -> tuple:
    """A cube from ``start`` to ``end`` placed by ``point``, as its first corner and its three edge vectors."""
    corner = point(start)
    edges = []
    for axis in range(3):
        unit = list(start)
        unit[axis] += 1.0
        direction = [a - b for a, b in zip(point(unit), corner)]
        edges.append((direction, end[axis] - start[axis]))
    return corner, edges


def world_cube(cube, matrix):
    """A cube in world pixels (relative to the block's corner) through ``matrix``: its centre and edge vectors.

    Edges thinner than ``THIN`` pixels (flat planes, rugs, banners) are thickened to ``THIN`` so they can be hit."""
    corner, edges = cube
    origin = apply(matrix, corner)
    vectors = []
    for direction, length in edges:
        tip = apply(matrix, tuple(c + d for c, d in zip(corner, direction)))
        unit = [16 * (a - b) for a, b in zip(tip, origin)]
        vectors.append((unit, length))
    centre = [16 * o for o in origin]
    out = []
    for unit, length in vectors:
        for i in range(3):
            centre[i] += unit[i] * length / 2
        norm = math.sqrt(sum(u * u for u in unit))
        span = abs(length) * norm
        if span < THIN:
            out.append([u / norm * THIN for u in unit] if norm > 1e-9 else None)
        else:
            out.append([u * length for u in unit])
    if any(v is None for v in out):
        return None
    return centre, out


def invert(m):
    """The inverse of a 3x3 matrix, or ``None`` when it is singular."""
    a, b, c = m[0]
    d, e, f = m[1]
    g, h, i = m[2]
    det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    if abs(det) < 1e-12:
        return None
    return [[(e * i - f * h) / det, (c * h - b * i) / det, (b * f - c * e) / det],
            [(f * g - d * i) / det, (a * i - c * g) / det, (c * d - a * f) / det],
            [(d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det]]


def voxelize(cubes, filled: set) -> None:
    """Adds the pixels whose centres lie in each cube (centre, edges) to ``filled``, a row along x at a time."""
    for centre, edges in cubes:
        # Columns of the edge matrix; its inverse takes a point to the cube's local coordinates, each -0.5 to 0.5.
        inverse = invert([[edges[j][i] for j in range(3)] for i in range(3)])
        if inverse is None:
            continue
        tolerance = [0.5 + 0.01 / math.sqrt(sum(v * v for v in edge)) for edge in edges]
        reach = [sum(abs(edge[i]) for edge in edges) / 2 for i in range(3)]
        lo = [math.floor(centre[i] - reach[i]) for i in range(3)]
        hi = [math.ceil(centre[i] + reach[i]) for i in range(3)]
        for y in range(lo[1], hi[1]):
            for z in range(lo[2], hi[2]):
                dy, dz = y + 0.5 - centre[1], z + 0.5 - centre[2]
                start, end = -math.inf, math.inf
                for row, limit in zip(inverse, tolerance):
                    # local = row . (x + 0.5 - cx, dy, dz) = slope * x + offset, held within -limit..limit.
                    slope = row[0]
                    offset = row[0] * (0.5 - centre[0]) + row[1] * dy + row[2] * dz
                    if abs(slope) < 1e-12:
                        if abs(offset) > limit:
                            start, end = 1, 0
                            break
                        continue
                    a, b = (-limit - offset) / slope, (limit - offset) / slope
                    start, end = max(start, min(a, b)), min(end, max(a, b))
                for x in range(math.ceil(start - 1e-9), math.floor(end + 1e-9) + 1):
                    filled.add((x, y, z))


def greedy(filled: set, order: tuple[int, int, int]) -> list[Box]:
    """Greedy meshing of pixels: runs along ``order[0]``, rows of equal runs along ``order[1]``, then ``order[2]``."""
    a, b, c = order
    remaining = set(filled)
    boxes = []
    for voxel in sorted(filled, key=lambda v: (v[c], v[b], v[a])):
        if voxel not in remaining:
            continue
        lo = list(voxel)
        size = [1, 1, 1]

        # Grow along a.
        while True:
            nxt = list(lo)
            nxt[a] += size[a]
            if tuple(nxt) in remaining:
                size[a] += 1
            else:
                break
        # Grow along b while the whole run is free.
        def free(axis):
            ranges = [range(lo[i], lo[i] + size[i]) for i in range(3)]
            ranges[axis] = [lo[axis] + size[axis]]
            return all((x, y, z) in remaining for x in ranges[0] for y in ranges[1] for z in ranges[2])
        while free(b):
            size[b] += 1
        while free(c):
            size[c] += 1
        for x in range(lo[0], lo[0] + size[0]):
            for y in range(lo[1], lo[1] + size[1]):
                for z in range(lo[2], lo[2] + size[2]):
                    remaining.discard((x, y, z))
        boxes.append(Box(lo[0], lo[1], lo[2], lo[0] + size[0], lo[1] + size[1], lo[2] + size[2]))
    return boxes


ORDERS = ((0, 2, 1), (2, 0, 1), (0, 1, 2), (1, 0, 2), (1, 2, 0), (2, 1, 0))


def aligned(edges) -> bool:
    """Whether a cube's edges all lie along the axes, so the cube is exactly one box."""
    return all(sum(1 for v in edge if abs(v) > 1e-6) == 1 for edge in edges)


def clip(box: Box, held: str) -> Box | None:
    """A box cut at what holds the piece up and at a part's reach, rounded outwards to quarter pixels."""
    lo = [box.x1, box.y1, box.z1]
    hi = [box.x2, box.y2, box.z2]
    if held == "floor":
        lo[1] = max(lo[1], 0.0)
    elif held == "ceiling":
        hi[1] = min(hi[1], 16.0)
    elif held == "wall":
        hi[2] = min(hi[2], 16.0)
    for axis, reach in ((0, REACH), (1, HEIGHT), (2, REACH)):
        lo[axis] = max(lo[axis], -16.0 * reach)
        hi[axis] = min(hi[axis], 16.0 * (reach + 1))
    lo = [math.floor(round(v * 4, 3)) / 4 for v in lo]
    hi = [math.ceil(round(v * 4, 3)) / 4 for v in hi]
    if any(h <= l for l, h in zip(lo, hi)):
        return None
    return Box(*lo, *hi)


def scaled(cube, step: float):
    """A cube (centre, edges) in units of ``step`` pixels, edges thinner than ``THIN`` units thickened."""
    centre, edges = cube
    out = []
    for edge in edges:
        edge = [v / step for v in edge]
        norm = math.sqrt(sum(v * v for v in edge))
        out.append([v / norm * THIN for v in edge] if norm < THIN else edge)
    return [v / step for v in centre], out


def footprint(piece: dict, turn_: int, step: float = STEP) -> list[Box]:
    """The piece's boxes, in pixels relative to its block, as it is drawn at ``turn_``.

    A cube that lies along the axes is its own box, exactly; a turned cube is cut into ``step``-pixel cells (a cell is
    in when its centre is inside the cube) and the cells are meshed into boxes. Boxes inside others are dropped and
    boxes that line up are merged."""
    cubes, item = model_cubes(piece)
    mode = piece["mode"]
    matrix = mul(renderer(mode, piece["facing"], turn_), item)
    held = "floor" if mode == "GECKOLIB" else piece["held"]
    world = [w for w in (world_cube(cube, matrix) for cube in cubes) if w is not None]
    boxes = []
    turned = []
    for centre, edges in world:
        if aligned(edges):
            half = [sum(abs(edge[i]) for edge in edges) / 2 for i in range(3)]
            boxes.append(Box(*(centre[i] - half[i] for i in range(3)), *(centre[i] + half[i] for i in range(3))))
        else:
            turned.append(scaled((centre, edges), step))
    filled = set()
    voxelize(turned, filled)
    if filled:
        cells = min((greedy(filled, order) for order in ORDERS), key=len)
        boxes += [Box(b.x1 * step, b.y1 * step, b.z1 * step, b.x2 * step, b.y2 * step, b.z2 * step) for b in cells]
    boxes = [c for c in (clip(b, held) for b in boxes) if c is not None]
    if not boxes:
        return [Box(0, 0, 0, 16, 16, 16)]
    if len(boxes) <= 400:
        boxes = simplify(boxes, 0.0)[0]
    return boxes


def outline(piece: dict) -> list[tuple]:
    """The model's outline at rotation 0 (a wall piece facing south): the edges of its cubes, turned as drawn, where
    they are a crease of the model's outside. Along each edge, the four quarters around it are tested (the cube's
    own, the two beside it across each face, and the one across both): the edge shows where one or three of them are
    inside a cube, or the cube's own and the one across both. So edges inside the model, and seams between cubes that
    meet flush, are left out; an edge partly hidden keeps its visible runs."""
    cubes, item = model_cubes(piece)
    matrix = mul(renderer(piece["mode"], piece["facing"], 0), item)
    return creases([w for w in (world_cube(cube, matrix) for cube in cubes) if w is not None])


def creases(world: list) -> list[tuple]:
    """The crease edges of cubes given as (centre, edges) in pixels, as ``outline`` describes."""
    solids = []
    for centre, edges in world:
        inverse = invert([[edges[j][i] for j in range(3)] for i in range(3)])
        if inverse is None:
            continue
        reach = [sum(abs(edge[i]) for edge in edges) / 2 for i in range(3)]
        solids.append((centre, edges, inverse, reach))
    # A grid of 8 pixel cells, holding the cubes whose box reaches into each, so a point is only tested against those.
    grid = {}
    for index, (centre, _, _, reach) in enumerate(solids):
        lo = [math.floor((centre[i] - reach[i]) / 8) for i in range(3)]
        hi = [math.floor((centre[i] + reach[i]) / 8) for i in range(3)]
        for x in range(lo[0], hi[0] + 1):
            for y in range(lo[1], hi[1] + 1):
                for z in range(lo[2], hi[2] + 1):
                    grid.setdefault((x, y, z), []).append(index)

    def inside(point, skip):
        for index in grid.get(tuple(math.floor(v / 8) for v in point), ()):
            if index == skip:
                continue
            centre, _, inverse, _ = solids[index]
            d = [point[i] - centre[i] for i in range(3)]
            if all(abs(sum(row[i] * d[i] for i in range(3))) <= 0.5 for row in inverse):
                return True
        return False

    lines = []
    for index, (centre, edges, _, _) in enumerate(solids):
        lengths = [math.sqrt(sum(v * v for v in edge)) for edge in edges]
        units = [[v / length for v in edge] for edge, length in zip(edges, lengths)]
        for axis in range(3):
            j, k = (axis + 1) % 3, (axis + 2) % 3
            for sj in (-1, 1):
                for sk in (-1, 1):
                    middle = [centre[i] + sj * edges[j][i] / 2 + sk * edges[k][i] / 2 for i in range(3)]
                    nj = [sj * v * EPS for v in units[j]]
                    nk = [sk * v * EPS for v in units[k]]
                    samples = max(2, math.ceil(lengths[axis] / SAMPLE))
                    run = None
                    for n in range(samples + 1):
                        t = n / samples - 0.5
                        p = [middle[i] + t * edges[axis][i] for i in range(3)]
                        # Ends are tested a hair inside the edge, where the neighbouring faces don't count.
                        q = [p[i] - (t * 2) * EPS * units[axis][i] for i in range(3)]
                        a = inside([q[i] + nj[i] - nk[i] for i in range(3)], index)
                        b = inside([q[i] - nj[i] + nk[i] for i in range(3)], index)
                        c = inside([q[i] + nj[i] + nk[i] for i in range(3)], index)
                        own = True
                        count = own + a + b + c
                        shown = count in (1, 3) or (count == 2 and c)
                        if shown and run is None:
                            run = p
                        if (not shown or n == samples) and run is not None:
                            end = p if shown else [middle[i] + (t - 1 / samples / 2) * edges[axis][i] for i in range(3)]
                            if math.dist(run, end) > 0.05:
                                lines.append((*run, *end))
                            run = None
    return lines


def blocks(boxes: list[Box]) -> tuple[int, int, int]:
    lo = [min(b.lo(a) for b in boxes) for a in range(3)]
    hi = [max(b.hi(a) for b in boxes) for a in range(3)]
    return tuple(math.ceil(hi[a] / 16 - 1e-6) - math.floor(lo[a] / 16 + 1e-6) for a in range(3))


def table(pieces: list[tuple[dict, list[Box], list[Box] | None, list[tuple]]]) -> str:
    def number(v):
        text = f"{v:.2f}".rstrip("0").rstrip(".")
        return "0" if text in ("-0", "") else text

    def text(boxes):
        return ",".join(" ".join(number(v) for v in (b.x1, b.y1, b.z1, b.x2, b.y2, b.z2)) for b in boxes or [])

    def lines(edges):
        return ",".join(" ".join(number(v) for v in edge) for edge in edges)
    header = [
        "# Artifact hitboxes and outlines, written by scripts/artifact_footprints.py from the artifacts' models; do not",
        "# edit by hand. name|straight|diagonal|outline: x1 y1 z1 x2 y2 z2 pixel boxes relative to the artifact's block",
        "# at rotation 0 (a wall piece facing south) and at rotation 1, the other rotations being these turned by quarter",
        "# turns (an empty diagonal list turns the straight one), then the outline's lines (x1 y1 z1 x2 y2 z2, pixels) at",
        "# rotation 0, turned for each rotation.",
    ]
    body = [f"{piece['name']}|{text(straight)}|{text(diagonal)}|{lines(edges)}"
            for piece, straight, diagonal, edges in pieces]
    return "\n".join(header + body) + "\n"


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("-o", "--output", type=Path, default=OUTPUT, help="the table to write")
    parser.add_argument("--report", action="store_true", help="print each piece's size in blocks and box counts")
    parser.add_argument("--dry-run", action="store_true", help="only print what would be written")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    pieces = []
    for piece in artifacts():
        straight = footprint(piece, 0)
        # Wall pieces only face the four sides; those are quarter turns of their straight boxes.
        diagonal = None if piece["mode"] == "WALL" else footprint(piece, 1)
        pieces.append((piece, straight, diagonal, outline(piece)))
    if args.report:
        for piece, straight, diagonal, edges in pieces:
            x, y, z = blocks(straight) if straight else (1, 1, 1)
            print(f"{piece['name']:40} {piece['mode']:9} {piece['held']:8} {x}x{y}x{z} "
                  f"{len(straight or [])}/{len(diagonal or [])} boxes, {len(edges)} lines")
    multi = sum(1 for _, straight, _, _ in pieces if straight and blocks(straight) != (1, 1, 1))
    print(f"{len(pieces)} artifacts, {multi} of them more than one block")
    if not args.dry_run:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(table(pieces), encoding="utf-8", newline="\n")
        print(f"wrote {args.output}")
    return 0


if __name__ == "__main__":
    run(main)
