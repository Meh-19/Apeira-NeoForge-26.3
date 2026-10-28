#!/usr/bin/env python3
"""Exports a GeckoLib or Bedrock-format Blockbench project (``.bbmodel``) as GeckoLib geometry (``.geo.json``).

GeckoLib reads ``assets/<namespace>/geckolib/models/<path>.geo.json``, Bedrock's geometry format; this writes the same
file Blockbench's GeckoLib "Export Geo Model" would, so a model can be updated from its project without opening
Blockbench. Outliner groups become bones (pivot and rotation with Bedrock's mirrored x), cubes keep their per-face UVs
(or box UV), up and down faces are flipped as Blockbench flips them, and faces whose UV has no area are left out.
Locators go into their group's bone (``locators``: name to position, mirrored x, or ``offset`` and ``rotation`` when
turned), which GeckoLib turns into ``GeoLocator``s. Meshes and groups or cubes set not to export are skipped.

``--texture`` also writes the project's first embedded texture as a PNG, and ``--animations`` writes its embedded
animations as a GeckoLib ``.animation.json`` (keyframes as plain values, linear).

Example::

    python scripts/bbmodel_to_geckolib.py ConductorCarlo.bbmodel \\
        apeira-main/src/main/resources/assets/apeira/geckolib/models/entity/balloon_conductor.geo.json \\
        --identifier geometry.balloon_conductor
"""

from __future__ import annotations

import argparse
import base64
import sys
from pathlib import Path
from typing import Any

from asset_common import ScriptError, read_json, relative, run, write_json

FACES = ("north", "east", "south", "west", "up", "down")


# Blockbench's GeckoLib ("animated_entity_model") and Bedrock project formats, whose geometry is the same.
FORMATS = ("animated_entity_model", "bedrock", "bedrock_old")


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("model", type=Path, help="the .bbmodel project (Bedrock format)")
    parser.add_argument("output", type=Path, help="the .geo.json to write")
    parser.add_argument("--identifier", help="geometry identifier (default: geometry.<output name>)")
    parser.add_argument("--texture", type=Path, help="also write the first embedded texture to this PNG")
    parser.add_argument("--animations", type=Path, help="also write the embedded animations to this .animation.json")
    parser.add_argument("--force", action="store_true", help="replace files that exist")
    parser.add_argument("--dry-run", action="store_true", help="only print what would be written")
    return parser.parse_args(argv)


def rounded(value: float) -> float | int:
    value = round(value, 4)
    return int(value) if value == int(value) else value


def vec(values: list[float]) -> list[float | int]:
    return [rounded(v) for v in values]


def bedrock_rotation(rotation: list[float]) -> list[float | int]:
    return vec([-rotation[0], -rotation[1], rotation[2]])


def convert_cube(cube: dict[str, Any], box_uv: bool) -> dict[str, Any] | None:
    start, end = cube["from"], cube["to"]
    size = [end[i] - start[i] for i in range(3)]
    result: dict[str, Any] = {"origin": vec([-(start[0] + size[0]), start[1], start[2]]), "size": vec(size)}
    if cube.get("inflate"):
        result["inflate"] = rounded(cube["inflate"])
    rotation = cube.get("rotation") or [0, 0, 0]
    if any(rotation):
        pivot = cube.get("origin") or [0, 0, 0]
        result["pivot"] = vec([-pivot[0], pivot[1], pivot[2]])
        result["rotation"] = bedrock_rotation(rotation)
    if cube.get("mirror_uv"):
        result["mirror"] = True
    if box_uv or cube.get("box_uv"):
        result["uv"] = vec(cube.get("uv_offset") or [0, 0])
        return result
    uvs: dict[str, Any] = {}
    for name in FACES:
        face = (cube.get("faces") or {}).get(name)
        if not face or face.get("texture") is None:
            continue
        u0, v0, u1, v1 = face["uv"]
        if u0 == u1 and v0 == v1:
            continue
        uv, uv_size = [u0, v0], [u1 - u0, v1 - v0]
        if name in ("up", "down"):
            uv = [uv[0] + uv_size[0], uv[1] + uv_size[1]]
            uv_size = [-uv_size[0], -uv_size[1]]
        entry: dict[str, Any] = {"uv": vec(uv), "uv_size": vec(uv_size)}
        if face.get("rotation"):
            entry["uv_rotation"] = face["rotation"]
        uvs[name] = entry
    if not uvs:
        return None
    result["uv"] = uvs
    return result


def convert(project: dict[str, Any], identifier: str) -> dict[str, Any]:
    if project.get("meta", {}).get("model_format") not in FORMATS:
        raise ScriptError("not a GeckoLib or Bedrock-format project (File > Convert Project to GeckoLib Animated Model first)")
    box_uv = bool(project.get("meta", {}).get("box_uv"))
    groups = {group["uuid"]: group for group in project.get("groups", [])}
    elements = {element["uuid"]: element for element in project.get("elements", [])}
    bones: list[dict[str, Any]] = []
    names: set[str] = set()

    def walk(nodes: list[Any], parent: dict[str, Any] | None) -> None:
        for node in nodes:
            if isinstance(node, str):
                element = elements.get(node)
                if parent is None or not element or element.get("export") is False:
                    continue
                if element.get("type") == "locator":
                    position = vec([-element["position"][0], element["position"][1], element["position"][2]])
                    rotation = element.get("rotation") or [0, 0, 0]
                    locator: Any = {"offset": position, "rotation": bedrock_rotation(rotation)} if any(rotation) else position
                    parent.setdefault("locators", {})[element["name"]] = locator
                    continue
                if element.get("type", "cube") != "cube":
                    continue
                cube = convert_cube(element, box_uv)
                if cube:
                    parent.setdefault("cubes", []).append(cube)
                continue
            group = groups.get(node["uuid"], node)
            if group.get("export") is False:
                continue
            name = group["name"]
            if name in names:
                raise ScriptError(f"two groups are named {name}; Bedrock bones need unique names")
            names.add(name)
            origin = group.get("origin") or [0, 0, 0]
            bone: dict[str, Any] = {"name": name}
            if parent is not None:
                bone["parent"] = parent["name"]
            bone["pivot"] = vec([-origin[0], origin[1], origin[2]])
            rotation = group.get("rotation") or [0, 0, 0]
            if any(rotation):
                bone["rotation"] = bedrock_rotation(rotation)
            bones.append(bone)
            walk(node.get("children", []), bone)

    walk(project.get("outliner", []), None)
    resolution = project.get("resolution", {"width": 16, "height": 16})
    description: dict[str, Any] = {
        "identifier": identifier,
        "texture_width": resolution["width"],
        "texture_height": resolution["height"],
    }
    box = project.get("visible_box")
    if box:
        description["visible_bounds_width"] = box[0]
        description["visible_bounds_height"] = box[1]
        description["visible_bounds_offset"] = [0, box[2], 0]
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": description, "bones": bones}]}


def animations(project: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for animation in project.get("animations", []):
        bones: dict[str, Any] = {}
        for animator in (animation.get("animators") or {}).values():
            channels: dict[str, Any] = {}
            for keyframe in animator.get("keyframes", []):
                point = keyframe["data_points"][0]
                values = [float(point.get(axis, 0)) for axis in ("x", "y", "z")]
                channels.setdefault(keyframe["channel"], {})[str(rounded(keyframe["time"]))] = vec(values)
            if channels and animator.get("name"):
                bones[animator["name"]] = {channel: dict(sorted(frames.items(), key=lambda item: float(item[0])))
                                           for channel, frames in channels.items()}
        entry: dict[str, Any] = {}
        if animation.get("loop") == "loop":
            entry["loop"] = True
        entry["animation_length"] = animation.get("length", 0)
        entry["bones"] = bones
        result[animation["name"]] = entry
    return {"format_version": "1.8.0", "animations": result}


def check_target(path: Path, args: argparse.Namespace) -> None:
    if path.exists() and not args.force:
        raise ScriptError(f"{relative(path)} exists (pass --force to overwrite it)")


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    project = read_json(args.model)
    identifier = args.identifier or "geometry." + args.output.name.split(".")[0]
    check_target(args.output, args)
    write_json(args.output, convert(project, identifier), dry_run=args.dry_run)
    print(f"{'Would write' if args.dry_run else 'Wrote'} {relative(args.output)}", file=sys.stderr)
    if args.texture:
        textures = project.get("textures", [])
        source = textures[0].get("source", "") if textures else ""
        if not source.startswith("data:image/png;base64,"):
            raise ScriptError("the project has no embedded PNG texture")
        check_target(args.texture, args)
        if not args.dry_run:
            args.texture.parent.mkdir(parents=True, exist_ok=True)
            args.texture.write_bytes(base64.b64decode(source.split(",", 1)[1]))
        print(f"{'Would write' if args.dry_run else 'Wrote'} {relative(args.texture)}", file=sys.stderr)
    if args.animations:
        check_target(args.animations, args)
        write_json(args.animations, animations(project), dry_run=args.dry_run)
        print(f"{'Would write' if args.dry_run else 'Wrote'} {relative(args.animations)}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    run(main)
