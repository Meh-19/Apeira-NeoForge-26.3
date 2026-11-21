#!/usr/bin/env python3
"""Measures every furniture piece as it is drawn and writes its hitboxes, cut from the models' cubes.

Every module with furniture is in ``MODULES``: apeira-archae's artifacts (``ArchaeArtifacts``, table
``apeira_archae/artifact_footprints.txt``) and apeira-decor's furniture (``DecorFurniture``, table
``apeira_decor/furniture_footprints.txt``), both read by decor's ``FurnitureFootprints``. A piece's ``renderOffset``
moves its model as ``FurnitureRenderer`` does.

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

from asset_common import REPO_ROOT, ScriptError, hitbox_ignored, java_ignored_elements, read_json, run
from parse_blockbench_hitboxes import Box, java_rotation, rotate, simplify

ARCHAE = REPO_ROOT / "apeira-archae" / "src" / "main"
DECOR = REPO_ROOT / "apeira-decor" / "src" / "main"
# Every module with furniture: where its pieces are declared, its models and GeckoLib models, and its table.
MODULES = {
    "archae": {
        "source": ARCHAE / "java" / "com" / "apeira" / "archae" / "registry" / "ArchaeArtifacts.java",
        "models": ARCHAE / "resources" / "assets" / "apeira" / "models" / "block" / "archaeology",
        "geo": ARCHAE / "resources" / "assets" / "apeira" / "geckolib" / "models" / "archaeology",
        "output": ARCHAE / "resources" / "apeira_archae" / "artifact_footprints.txt",
        "title": "Artifact",
    },
    "decor": {
        "source": DECOR / "java" / "com" / "apeira" / "decor" / "registry" / "DecorFurniture.java",
        "models": DECOR / "resources" / "assets" / "apeira" / "models" / "item" / "furniture",
        "geo": DECOR / "resources" / "assets" / "apeira" / "geckolib" / "models" / "furniture",
        "output": DECOR / "resources" / "apeira_decor" / "furniture_footprints.txt",
        "title": "Furniture",
    },
}
THIN = 1.02
# Turned cubes are cut into cells this many pixels wide.
STEP = 2.0
# Outline edges are tested every SAMPLE pixels, EPS pixels to each side.
SAMPLE = 0.5
EPS = 0.05
REACH = 4
HEIGHT = 6
FORM_MODES = {
    "FURNITURE": "TOP_FACE", "DECORATION": "FLOOR", "FULL_DECORATION": "FLOOR", "FLOOR": "FLOOR",
    "BLOCK_UP": "BLOCK_UP", "WALL_MOUNTED": "WALL", "TWO_TALL": "TOP_FACE", "THREE_TALL": "TOP_FACE",
    "FOUR_TALL": "TOP_FACE", "FOUR_TALL_FLOOR": "FLOOR", "FIVE_TALL": "TOP_FACE",
}
# Animated pieces whose hitbox is only the cubes under one bone (the elephant's, not its floating head and trunk).
HITBOX_BONES = {"elephant_inflatable": "elephant_inflatable_body_tintable"}
FACING_CLASSES = {"FacingFurnitureBlock", "CeilingFurnitureBlock", "WaterloggedFurnitureBlock", "WallFurnitureBlock"}
# A piece: its name, its block class, the argument after its paintability (a Form, a DisplayMode, a seat height or an
# animation name) and the rest of its declaration, which may hold a renderOffset(x, y, z) in pixels. An animated
# piece's folder comes before its paintability.
DECLARATION = re.compile(r'DeferredBlock<\w+> \w+ = (?:artifact|variant|piece)\("(\w+)",(.*?)p -> new (\w+)\(p, (?:"\w+", )?'
                         r'(?:true|false)(?:, (Form\.\w+|DisplayMode\.\w+|\d+|"\w*"))?([^;]*);', re.S)
OFFSET = re.compile(r'renderOffset\(([-\d.]+)F?, ([-\d.]+)F?, ([-\d.]+)F?\)')
YAW = re.compile(r'modelYaw\(([-\d.]+)F?\)')
# FurnitureBlock#displayAs: the display mode a piece is drawn in whatever its class and id say.
DISPLAY = re.compile(r'displayAs\(DisplayMode\.(\w+)\)')


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

def artifacts(module: dict) -> list[dict]:
    """Every piece's name, display mode, how it is held up and its render offset, from the module's declarations."""
    pieces = []
    for name, before, cls, arg, after in DECLARATION.findall(module["source"].read_text(encoding="utf-8")):
        facing = cls in FACING_CLASSES
        if cls == "FacingFurnitureBlock":
            mode = "WALL" if "wall" in name else "FLOOR" if "side" in name else "TOP_FACE"
        elif cls == "CeilingFurnitureBlock":
            mode = "TOP_FACE"
        elif cls == "WallFurnitureBlock":
            # A still piece held only by a wall (the bathroom mirror).
            mode = "WALL"
        elif cls == "SeatFurnitureBlock":
            # A seat lies on the top face unless it names its Form (the froggy chair's FLOOR).
            mode = FORM_MODES[arg.split(".")[1]] if arg.startswith("Form.") else "TOP_FACE"
        elif cls == "WaterloggedFurnitureBlock":
            mode = arg.split(".")[1]
        elif cls in ("AnimatedFurnitureBlock", "AnimatedWallFurnitureBlock", "AnimatedCeilingFurnitureBlock"):
            mode = "GECKOLIB"
        else:
            mode = FORM_MODES[arg.split(".")[1]]
        shown = DISPLAY.search(after)
        if shown:
            mode = shown.group(1)
        # A wall variant drawn in another mode (displayAs) is still held by its wall, turned by its facing.
        if (mode == "WALL" and facing) or cls == "AnimatedWallFurnitureBlock" or (facing and "wall" in name and "ceiling" not in name) \
                or cls == "WallFurnitureBlock":
            held = "wall"
        elif "ceiling" in name or cls in ("CeilingArtifactBlock", "AnimatedCeilingFurnitureBlock"):
            held = "ceiling"
        elif mode == "WALL":
            held = "none"
        else:
            held = "floor"
        found = OFFSET.search(after)
        offset = tuple(float(v) / 16 for v in found.groups()) if found else (0.0, 0.0, 0.0)
        yawed = YAW.search(after)
        pieces.append({"name": name, "mode": mode, "facing": facing, "held": held, "offset": offset, "wall": held == "wall",
                       "yaw": float(yawed.group(1)) if yawed else 0.0, "module": module})
    if not pieces:
        raise ScriptError(f"{module['source']}: no furniture found")
    return pieces


def renderer(mode: str, facing: bool, turn_: int):
    """``ArtifactRenderer``'s (or ``AnimatedArtifactRenderer``'s) transform at ``turn_`` 45 degree steps, in blocks.

    ``turn_`` is the block's ``ROTATION``; for a wall piece it is its facing's ``toYRot() / 45`` (south is 0).
    """
    if mode == "GECKOLIB":
        # GeoBlockRenderer stands the model on the block's bottom centre; geo_cubes has undone the geometry's mirrored x.
        return chain(translate(0.5, 0, 0.5), turn("y", -45 * turn_), scale(1 / 16, 1 / 16, 1 / 16))
    if mode == "TOP_FACE":
        steps = [translate(0.5, 0.999, 0.5), turn("x", 180), turn("x", 90), turn("z", 180), turn("z", -45 * turn_)]
    elif mode in ("FLOOR", "BLOCK_UP"):
        steps = [translate(0.5, 0.0 if mode == "FLOOR" else 1.0, 0.5), turn("x", 90), turn("z", 180),
                 turn("z", 45 * turn_)]
    else:
        steps = [translate(0.5, 0.5, 0.5)] + ([turn("y", -45 * turn_), translate(0, 0, 0.45)] if facing else [])
    return chain(*steps, scale(0.5, 0.5, 0.5))


def placed(piece: dict, turn_: int, item):
    """The model's matrix into the block as ``FurnitureRenderer`` draws it: moved by the piece's ``renderOffset`` (in
    blocks, before its turn; not for GeckoLib pieces, whose renderer doesn't move them), laid in and turned."""
    if piece["mode"] == "GECKOLIB":
        # AnimatedFurnitureRenderer adds the piece's modelYaw to its turn.
        return mul(chain(translate(0.5, 0, 0.5), turn("y", -45 * turn_ + piece["yaw"]), scale(1 / 16, 1 / 16, 1 / 16)), item)
    matrix = mul(renderer(piece["mode"], piece["facing"], turn_), item)
    # FurnitureRenderer moves the model by renderOffset, then turns it by modelYaw about the block's centre column.
    yaw = chain(translate(0.5, 0, 0.5), turn("y", piece["yaw"]), translate(-0.5, 0, -0.5))
    return mul(translate(*piece["offset"]), mul(yaw, matrix))


def model_cubes(piece: dict) -> tuple[list, list]:
    """The model's cubes as (corner, three edges) in the model's own units, and the matrix into the renderer's frame."""
    cubes = []
    if piece["mode"] == "GECKOLIB":
        return geo_cubes(piece), translate(0, 0, 0)
    model = read_json(piece["module"]["models"] / f"{piece['name']}.json")
    fixed = model.get("display", {}).get("fixed", {})
    t = fixed.get("translation", [0, 0, 0])
    r = fixed.get("rotation", [0, 0, 0])
    s = fixed.get("scale", [1, 1, 1])
    # Vanilla's ItemTransform: translation, then rotation X, Y, Z, then scale, about the model's centre.
    item = chain(translate(*(v / 16 for v in t)), turn("x", r[0]), turn("y", r[1]), turn("z", r[2]), scale(*s),
                 translate(-0.5, -0.5, -0.5), scale(1 / 16, 1 / 16, 1 / 16))
    ignored = java_ignored_elements(model)
    for index, element in enumerate(model.get("elements", [])):
        if index in ignored:
            continue
        origin, angles = java_rotation(element.get("rotation"))
        if any(abs(a) > 1e-6 for a in angles):
            point = lambda p, origin=origin, angles=angles: rotate(p, origin, angles)
        else:
            point = lambda p: tuple(p)
        cubes.append(oriented(element["from"], element["to"], point))
    return cubes, item


def rest_pose(piece: dict) -> dict:
    """Each bone's position, rotation and scale at the start of the piece's first animation (keyframe values that are
    numbers; Molang is taken as 0), as GeckoLib holds a bone before its first keyframe: the recorded item scales
    (the elephant's 3.6) and a face the animation hides."""
    folder = piece["module"]["geo"]
    path = folder.parent.parent / "animations" / folder.name / f"{piece['name']}.animation.json"
    if not path.is_file():
        return {}
    animations = read_json(path).get("animations", {})
    if not animations:
        return {}
    clip = next(iter(animations.values()))

    def first(channel, default):
        if channel is None:
            return default
        if isinstance(channel, dict) and "vector" not in channel:
            if not channel:
                return default
            channel = channel[min(channel, key=float)]
        if isinstance(channel, dict):
            channel = channel.get("vector", channel.get("post", channel.get("pre", default)))
            if isinstance(channel, dict):
                channel = channel.get("vector", default)
        if not isinstance(channel, list):
            channel = [channel] * 3
        return tuple(float(v) if isinstance(v, (int, float)) else 0.0 for v in channel)

    pose = {}
    for name, bone in clip.get("bones", {}).items():
        pose[name] = (first(bone.get("position"), (0, 0, 0)), first(bone.get("rotation"), (0, 0, 0)),
                      first(bone.get("scale"), (1, 1, 1)))
    return pose


def geo_cubes(piece: dict) -> list:
    """A GeckoLib model's cubes in Blockbench's own space (pixels, x not mirrored), as GeckoLib draws them at rest:
    each cube turned about its pivot and then by every bone above it, about each bone's pivot (Z, Y, X order, as
    ``RenderUtil`` turns a bone; the geometry's mirrored x and its negated x and y angles undone). Only the bones
    under ``HITBOX_BONES``' bone, when the piece names one, count, and never a bone tagged ``_ignore`` or one under it."""
    geo = read_json(piece["module"]["geo"] / f"{piece['name']}.geo.json")["minecraft:geometry"][0]
    bones = {bone["name"]: bone for bone in geo.get("bones", [])}
    only = HITBOX_BONES.get(piece["name"])

    rest = rest_pose(piece)

    def pose(pivot, angles, moved=(0, 0, 0), scaled=(1, 1, 1)):
        p = (-pivot[0], pivot[1], pivot[2])
        a = (-angles[0], -angles[1], angles[2])
        return chain(translate(-moved[0], moved[1], moved[2]), translate(*p), turn("z", a[2]), turn("y", a[1]),
                     turn("x", a[0]), scale(*scaled), translate(*(-v for v in p)))

    def matrix(name):
        bone = bones[name]
        moved, turned, scaled = rest.get(name, ((0, 0, 0), (0, 0, 0), (1, 1, 1)))
        angles = [a + b for a, b in zip(bone.get("rotation", [0, 0, 0]), turned)]
        own = pose(bone.get("pivot", [0, 0, 0]), angles, moved, scaled)
        parent = bone.get("parent")
        return mul(matrix(parent), own) if parent in bones else own

    def hidden(name):
        # A bone the animation starts scaled to nothing (one of the cat clock's two faces) has no hitbox.
        while name in bones:
            if min(abs(v) for v in rest.get(name, (None, None, (1, 1, 1)))[2]) < 0.01:
                return True
            name = bones[name].get("parent")
        return False

    def ignored(name):
        # The _ignore tag (asset_common.IGNORE_TAG), on the bone or any bone above it, in any order among its other
        # tags (door_tintable_ignore, door_ignore_tintable): drawn, but no hitbox.
        while name in bones:
            if hitbox_ignored(name):
                return True
            name = bones[name].get("parent")
        return False

    def under(name):
        while name in bones:
            if name == only:
                return True
            name = bones[name].get("parent")
        return False

    cubes = []
    for name, bone in bones.items():
        if only is not None and not under(name):
            continue
        if hidden(name) or ignored(name):
            continue
        placed_bone = matrix(name)
        for cube in bone.get("cubes", []):
            o, size = cube["origin"], cube["size"]
            # Blockbench's from and to: the geometry's x is mirrored, its origin the cube's mirrored low corner.
            lo = [-(o[0] + size[0]), o[1], o[2]]
            start = [lo[i] + min(size[i], 0) for i in range(3)]
            end = [lo[i] + max(size[i], 0) for i in range(3)]
            m = mul(placed_bone, pose(cube.get("pivot", [0, 0, 0]), cube.get("rotation", [0, 0, 0])))
            cubes.append(oriented(start, end, lambda p, m=m: apply(m, p)))
    return cubes


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
    matrix = placed(piece, turn_, item)
    held = "floor" if mode == "GECKOLIB" and piece["held"] not in ("wall", "ceiling") else piece["held"]
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
    matrix = placed(piece, 0, item)
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


def table(pieces: list[tuple[dict, list[Box], list[Box] | None, list[tuple]]], module: dict) -> str:
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
    if module["title"] != "Artifact":
        header = [line.replace("Artifact hitboxes", "Furniture hitboxes").replace("artifacts' models", "furniture models")
                  .replace("artifact's block", "piece's block") for line in header]
    body = [f"{piece['name']}|{text(straight)}|{text(diagonal)}|{lines(edges)}"
            for piece, straight, diagonal, edges in pieces]
    return "\n".join(header + body) + "\n"


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--module", choices=sorted(MODULES), action="append",
                        help="only this module's furniture (repeatable; default: every module)")
    parser.add_argument("--report", action="store_true", help="print each piece's size in blocks and box counts")
    parser.add_argument("--dry-run", action="store_true", help="only print what would be written")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    for key in args.module or sorted(MODULES):
        module = MODULES[key]
        pieces = []
        for piece in artifacts(module):
            straight = footprint(piece, 0)
            # Wall pieces only face the four sides; those are quarter turns of their straight boxes.
            diagonal = None if piece["mode"] == "WALL" or piece["wall"] else footprint(piece, 1)
            pieces.append((piece, straight, diagonal, outline(piece)))
        if args.report:
            for piece, straight, diagonal, edges in pieces:
                x, y, z = blocks(straight) if straight else (1, 1, 1)
                print(f"{piece['name']:40} {piece['mode']:9} {piece['held']:8} {x}x{y}x{z} "
                      f"{len(straight or [])}/{len(diagonal or [])} boxes, {len(edges)} lines")
        multi = sum(1 for _, straight, _, _ in pieces if straight and blocks(straight) != (1, 1, 1))
        print(f"{key}: {len(pieces)} pieces, {multi} of them more than one block")
        if not args.dry_run:
            output = module["output"]
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(table(pieces, module), encoding="utf-8", newline="\n")
            print(f"wrote {output}")
    return 0


if __name__ == "__main__":
    run(main)
