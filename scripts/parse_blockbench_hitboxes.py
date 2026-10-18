#!/usr/bin/env python3
"""Turns the cubes of a Blockbench model into Java ``VoxelShape`` code for apeira-decor.

Reads Blockbench projects (``.bbmodel``) and Java block/item model JSON. Every cube becomes a box in pixel
coordinates (0-16 is one block), which is what ``Block.box(x1, y1, z1, x2, y2, z2)`` takes. Rotated cubes
become the box around their rotated corners; mesh elements become the box around their vertices.

To keep shapes cheap the boxes are then simplified: flat planes (zero thickness) are dropped, boxes inside
other boxes are removed, and boxes that line up into one larger box are merged. ``--bounds`` collapses
everything into a single box and ``--min-size`` drops small details such as handles or studs.

If a ``.bbmodel`` has an outliner group named ``hitbox`` or ``collision`` (any case), only the cubes in it are
used, so a model can carry simple invisible cubes for its shape (choose another with ``--group``).

``--facings`` also prints the shape turned to face east, south and west, assuming the model faces north as
Blockbench shows it by default, ready for a direction-aware furniture block.

Example::

    python scripts/parse_blockbench_hitboxes.py models/chair.bbmodel --facings
    python scripts/parse_blockbench_hitboxes.py lamp.json table.bbmodel -o build/shapes.java
"""

from __future__ import annotations

import argparse
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

from asset_common import ScriptError, read_json, relative, run, to_snake_case

EPSILON = 1e-6
HITBOX_GROUPS = ("hitbox", "collision", "hitboxes")
IMPORTS = ("net.minecraft.world.level.block.Block", "net.minecraft.world.phys.shapes.Shapes",
           "net.minecraft.world.phys.shapes.VoxelShape")


@dataclass(frozen=True)
class Box:
    x1: float
    y1: float
    z1: float
    x2: float
    y2: float
    z2: float

    @staticmethod
    def around(points: Iterable[tuple[float, float, float]]) -> "Box":
        xs, ys, zs = zip(*points)
        return Box(min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))

    def lo(self, axis: int) -> float:
        return (self.x1, self.y1, self.z1)[axis]

    def hi(self, axis: int) -> float:
        return (self.x2, self.y2, self.z2)[axis]

    def size(self, axis: int) -> float:
        return self.hi(axis) - self.lo(axis)

    def contains(self, other: "Box") -> bool:
        return all(self.lo(a) <= other.lo(a) + EPSILON and other.hi(a) <= self.hi(a) + EPSILON for a in range(3))

    def union(self, other: "Box") -> "Box":
        return Box(min(self.x1, other.x1), min(self.y1, other.y1), min(self.z1, other.z1),
                   max(self.x2, other.x2), max(self.y2, other.y2), max(self.z2, other.z2))

    def clamped(self, low: float = 0.0, high: float = 16.0) -> "Box":
        c = lambda v: min(max(v, low), high)  # noqa: E731
        return Box(c(self.x1), c(self.y1), c(self.z1), c(self.x2), c(self.y2), c(self.z2))

    def rounded(self, digits: int) -> "Box":
        return Box(*(round(v, digits) + 0.0 for v in (self.x1, self.y1, self.z1, self.x2, self.y2, self.z2)))

    def turned(self, quarter_turns: int) -> "Box":
        """This box turned clockwise (seen from above) around the block's centre, like blockstate ``y`` rotation."""
        box = self
        for _ in range(quarter_turns % 4):
            # (x, z) -> (16 - z, x): the north face (z = 0) ends up on the east side (x = 16).
            box = Box(16 - box.z2, box.y1, box.x1, 16 - box.z1, box.y2, box.x2)
        return box


# Reading -----------------------------------------------------------------------------------------------------

def rotate(point, origin, angles_deg) -> tuple[float, float, float]:
    """Rotates ``point`` around ``origin`` by X, then Y, then Z Euler angles in degrees (Blockbench's order)."""
    x, y, z = (p - o for p, o in zip(point, origin))
    ax, ay, az = (math.radians(a) for a in angles_deg)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return x + origin[0], y + origin[1], z + origin[2]


def corners(start, end) -> list[tuple[float, float, float]]:
    return [(x, y, z) for x in (start[0], end[0]) for y in (start[1], end[1]) for z in (start[2], end[2])]


def cube_box(start, end, origin, angles, inflate: float = 0.0) -> Box:
    start = [s - inflate for s in start]
    end = [e + inflate for e in end]
    points = corners(start, end)
    if any(abs(a) > EPSILON for a in angles):
        points = [rotate(p, origin, angles) for p in points]
    return Box.around(points)


def java_rotation(rotation: Any) -> tuple[list[float], list[float]]:
    """Origin and XYZ angles of a Java model element's ``rotation`` (single ``axis``/``angle``, or ``x``/``y``/``z``)."""
    if not isinstance(rotation, dict):
        return [8.0, 8.0, 8.0], [0.0, 0.0, 0.0]
    origin = [float(v) for v in rotation.get("origin", [8, 8, 8])]
    if "axis" in rotation:
        angles = [0.0, 0.0, 0.0]
        angles["xyz".index(rotation["axis"])] = float(rotation.get("angle", 0))
        return origin, angles
    return origin, [float(rotation.get(axis, 0)) for axis in "xyz"]


def read_java_model(data: dict, path: Path) -> list[Box]:
    elements = data.get("elements")
    if not isinstance(elements, list):
        hint = f" (its parent is {data['parent']})" if "parent" in data else ""
        raise ScriptError(f"{path}: the model has no elements{hint}")
    boxes = []
    for element in elements:
        origin, angles = java_rotation(element.get("rotation"))
        boxes.append(cube_box(element["from"], element["to"], origin, angles))
    return boxes


def bbmodel_groups(data: dict) -> dict[str, dict]:
    """Every outliner group by uuid, with ``children`` as uuids or nested groups (both bbmodel layouts)."""
    groups = {g["uuid"]: g for g in data.get("groups", []) if isinstance(g, dict) and "uuid" in g}

    def collect(nodes):
        for node in nodes:
            if isinstance(node, dict):
                groups.setdefault(node.get("uuid", id(node)), node)
                collect(node.get("children", []))
    collect(data.get("outliner", []))
    return groups


def elements_in(group: dict, groups: dict[str, dict]) -> set[str]:
    found: set[str] = set()
    for child in group.get("children", []):
        if isinstance(child, dict):
            found |= elements_in(child, groups)
        elif child in groups:
            found |= elements_in(groups[child], groups)
        else:
            found.add(child)
    return found


def hidden_elements(data: dict, groups: dict[str, dict]) -> set[str]:
    hidden: set[str] = set()
    for group in groups.values():
        if group.get("visibility") is False:
            hidden |= elements_in(group, groups)
    return hidden


def read_bbmodel(data: dict, path: Path, group_name: str | None, include_hidden: bool,
                 offset: list[float] | None) -> list[Box]:
    meta = data.get("meta", {})
    if offset is None:
        # Java block models use 0-16; Blockbench's other formats centre the model on the block's middle.
        offset = [0.0, 0.0, 0.0] if meta.get("model_format", "java_block") == "java_block" else [8.0, 0.0, 8.0]
    groups = bbmodel_groups(data)
    wanted = None
    names = [group_name] if group_name else list(HITBOX_GROUPS)
    for group in groups.values():
        if str(group.get("name", "")).lower() in [n.lower() for n in names]:
            wanted = elements_in(group, groups)
            include_hidden = True  # a hitbox group is usually hidden on purpose
            break
    if group_name and wanted is None:
        raise ScriptError(f"{path}: no outliner group named {group_name!r}")
    hidden = set() if include_hidden else hidden_elements(data, groups)

    boxes = []
    for element in data.get("elements", []):
        uuid = element.get("uuid")
        if wanted is not None and uuid not in wanted:
            continue
        if not include_hidden and (element.get("visibility") is False or uuid in hidden):
            continue
        if element.get("export") is False:
            continue
        kind = element.get("type", "cube")
        origin = [float(v) for v in element.get("origin", [0, 0, 0])]
        angles = [float(v) for v in element.get("rotation", [0, 0, 0])]
        if kind == "cube":
            box = cube_box(element["from"], element["to"], origin, angles, float(element.get("inflate", 0)))
        elif kind == "mesh":
            vertices = element.get("vertices", {}).values()
            if not vertices:
                continue
            points = [tuple(o + float(v) for o, v in zip(origin, vertex)) for vertex in vertices]
            box = Box.around(rotate(p, origin, angles) for p in points)
        else:
            continue  # locators, null objects, texture meshes: not part of the shape
        boxes.append(Box(box.x1 + offset[0], box.y1 + offset[1], box.z1 + offset[2],
                         box.x2 + offset[0], box.y2 + offset[1], box.z2 + offset[2]))
    return boxes


def read_boxes(path: Path, args: argparse.Namespace) -> list[Box]:
    data = read_json(path)
    if not isinstance(data, dict):
        raise ScriptError(f"{path}: expected a JSON object")
    try:
        if path.suffix.lower() == ".bbmodel" or "meta" in data and "outliner" in data:
            return read_bbmodel(data, path, args.group, args.include_hidden, args.offset)
        return read_java_model(data, path)
    except (KeyError, TypeError, ValueError, IndexError) as error:
        raise ScriptError(f"{path}: unexpected element layout ({error!r})") from error


# Simplifying -------------------------------------------------------------------------------------------------

def merge_pair(a: Box, b: Box) -> Box | None:
    """The single box covering exactly ``a`` and ``b``, if they line up on two axes and touch on the third."""
    for axis in range(3):
        others = [o for o in range(3) if o != axis]
        if all(abs(a.lo(o) - b.lo(o)) < EPSILON and abs(a.hi(o) - b.hi(o)) < EPSILON for o in others) \
                and a.lo(axis) <= b.hi(axis) + EPSILON and b.lo(axis) <= a.hi(axis) + EPSILON:
            return a.union(b)
    return None


def simplify(boxes: list[Box], min_size: float) -> tuple[list[Box], int]:
    """Drops flat and small boxes, removes contained ones and merges aligned ones. Returns boxes and drops."""
    kept = [b for b in boxes if all(b.size(a) > max(min_size, EPSILON) for a in range(3))]
    dropped = len(boxes) - len(kept)
    changed = True
    while changed:
        changed = False
        kept = [b for i, b in enumerate(kept)
                if not any(j != i and o.contains(b) and (not b.contains(o) or j < i) for j, o in enumerate(kept))]
        for i in range(len(kept)):
            for j in range(i + 1, len(kept)):
                merged = merge_pair(kept[i], kept[j])
                if merged:
                    kept[i] = merged
                    del kept[j]
                    changed = True
                    break
            if changed:
                break
    kept.sort(key=lambda b: (b.y1, b.z1, b.x1, b.y2, b.z2, b.x2))
    return kept, dropped


# Writing -----------------------------------------------------------------------------------------------------

def number(value: float) -> str:
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return "0" if text == "-0" else text


def box_call(box: Box) -> str:
    return "Block.box(" + ", ".join(number(v) for v in (box.x1, box.y1, box.z1, box.x2, box.y2, box.z2)) + ")"


def shape_code(name: str, boxes: list[Box], indent: str = "    ") -> str:
    if len(boxes) == 1:
        return f"{indent}public static final VoxelShape {name} = {box_call(boxes[0])};"
    calls = f",\n{indent}        ".join(box_call(b) for b in boxes)
    return f"{indent}public static final VoxelShape {name} = Shapes.or(\n{indent}        {calls});"


def constant_name(path: Path, explicit: str | None) -> str:
    if explicit:
        return explicit
    return to_snake_case(path.name.split(".")[0]).upper() + "_SHAPE"


def convert(path: Path, args: argparse.Namespace) -> str:
    raw = read_boxes(path, args)
    if not raw:
        raise ScriptError(f"{path}: no cubes to build a shape from")
    boxes = [b.rounded(4) for b in raw]
    outside = [b for b in boxes if not Box(0, 0, 0, 16, 16, 16).contains(b)]
    notes = []
    if outside:
        if args.clamp:
            boxes = [b.clamped() for b in boxes]
            notes.append(f"{len(outside)} boxes clamped to the block")
        else:
            notes.append(f"{len(outside)} boxes reach outside the block; collision outside 0-16 is unreliable "
                         "(--clamp cuts them)")
    if args.bounds:
        boxes, dropped = [Box.around(c for b in boxes for c in corners((b.x1, b.y1, b.z1), (b.x2, b.y2, b.z2)))], 0
    else:
        boxes, dropped = simplify(boxes, args.min_size)
        if not boxes:
            raise ScriptError(f"{path}: every cube is flat or smaller than --min-size")
    if dropped:
        notes.append(f"{dropped} flat or small cubes dropped")
    for note in notes:
        print(f"warning: {relative(path)}: {note}", file=sys.stderr)

    name = constant_name(path, args.name if len(args.models) == 1 else None)
    header = f"    // {relative(path)}: {len(raw)} cubes -> {len(boxes)} boxes."
    if not args.facings:
        return header + "\n" + shape_code(name, boxes)
    parts = [header]
    for turns, facing in enumerate(("NORTH", "EAST", "SOUTH", "WEST")):
        parts.append(shape_code(f"{name}_{facing}", [b.turned(turns) for b in boxes]))
    return "\n".join(parts)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("models", nargs="+", type=Path, help=".bbmodel or model .json files")
    parser.add_argument("-o", "--output", type=Path, help="write the code to this file instead of printing it")
    parser.add_argument("--force", action="store_true", help="overwrite --output if it exists")
    parser.add_argument("--name", help="constant name, for a single model (default: <FILE_NAME>_SHAPE)")
    parser.add_argument("--facings", action="store_true",
                        help="also print the shape turned east, south and west (the model faces north)")
    parser.add_argument("--bounds", action="store_true", help="one box around the whole model")
    parser.add_argument("--min-size", type=float, default=0.0, metavar="PX",
                        help="drop cubes thinner than this many pixels on any axis (default: %(default)s)")
    parser.add_argument("--clamp", action="store_true", help="cut boxes to the block (0-16)")
    parser.add_argument("--group", help="only use the cubes in this .bbmodel outliner group")
    parser.add_argument("--include-hidden", action="store_true", help="include cubes hidden in Blockbench")
    parser.add_argument("--offset", type=float, nargs=3, metavar=("X", "Y", "Z"),
                        help="pixels added to .bbmodel coordinates (default: 0 0 0 for Java block models, "
                             "8 0 8 for Blockbench's other formats)")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.output and args.output.exists() and not args.force:
        raise ScriptError(f"{args.output} exists (pass --force to overwrite it)")
    snippets = [convert(path, args) for path in args.models]
    code = "\n\n".join([f"    // Generated by scripts/parse_blockbench_hitboxes.py. Imports: {', '.join(IMPORTS)}",
                        *snippets]) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(code, encoding="utf-8", newline="\n")
        print(f"Wrote {relative(args.output)}", file=sys.stderr)
    else:
        sys.stdout.write(code)
    return 0


if __name__ == "__main__":
    run(main)
