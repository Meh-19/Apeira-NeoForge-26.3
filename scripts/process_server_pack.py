#!/usr/bin/env python3
"""Converts the CustomModelData models of a server resource pack into standalone Apeira item models.

A server pack puts its custom items on top of vanilla ones: a stick with CustomModelData 5 renders as a fir
sapling. The pack says so in one of two formats, and this script reads both:

* legacy (before 1.21.4): ``assets/minecraft/models/item/<item>.json`` with ``overrides`` whose ``predicate``
  holds ``custom_model_data``;
* current (1.21.4 and 26.x): ``assets/minecraft/items/<item>.json`` item definitions that switch on the
  ``minecraft:custom_model_data`` property with ``range_dispatch`` (numbers), ``select`` (strings) or
  ``condition`` (flags).

Every custom model found becomes ``<output>/models/item/<name>.json``, named after the model's file name in
snake_case. Its parent models from the pack are copied the same way, and texture references move into the
``apeira`` namespace (``textures/item/...`` keeps the pack's folders; run ``flatten_textures.py`` afterwards).
Vanilla parents and textures such as ``minecraft:item/generated`` are kept as they are.

Minecraft 26.x also needs an item definition in ``assets/apeira/items/<name>.json`` for each item. The data
generators write those for registered items (``ApeiraItemModelProvider#existingModel``), so the script only
writes them with ``--item-definitions``, for items whose definition is more than a plain model (bows that pull,
armour trims) and would otherwise be lost.

Example::

    python scripts/process_server_pack.py --base-item diamond_sword --dry-run
    python scripts/process_server_pack.py --match "custom_gear/" --copy-textures
"""

from __future__ import annotations

import argparse
import copy
import re
import shutil
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterator

from asset_common import MAIN_ASSETS, REPO_ROOT, ScriptError, read_json, relative, run, split_id, to_snake_case, write_json

DEFAULT_PACK = REPO_ROOT / "reference_code" / "server_resource_pack"
DEFAULT_MANIFEST = REPO_ROOT / "build" / "asset-scripts" / "server_pack_manifest.json"

#: Model folders that belong to vanilla; references into them stay in the minecraft namespace.
VANILLA_MODEL_ROOTS = {"item", "block", "builtin"}
#: Texture folders that belong to vanilla.
VANILLA_TEXTURE_ROOTS = {
    "block", "item", "entity", "misc", "gui", "environment", "particle", "painting", "mob_effect", "trims", "map",
    "colormap", "effect", "font", "models",
}
#: Keys walked first when looking for an entry's main model, so the idle bow comes before the pulling one.
DEFAULT_BRANCHES = ("on_false", "fallback")


@dataclass
class CmdEntry:
    """One custom model of a vanilla item, keyed by its CustomModelData value."""

    base_item: str
    kind: str  # range_dispatch, select, condition or legacy_override
    key: Any  # threshold, string case(s) or flag index
    node: dict  # item model tree (26.x format) drawn for this value
    source: Path
    notes: list[str] = field(default_factory=list)

    def model_refs(self) -> list[str]:
        """Model references in the tree, the main one first."""
        refs: list[str] = []
        for ref in iter_model_refs(self.node):
            if ref not in refs:
                refs.append(ref)
        return refs


def strip_ns(value: Any) -> Any:
    return value.removeprefix("minecraft:") if isinstance(value, str) else value


def iter_model_refs(node: Any) -> Iterator[str]:
    """Yields every model reference in an item model tree, default branches first."""
    if isinstance(node, list):
        for child in node:
            yield from iter_model_refs(child)
        return
    if not isinstance(node, dict):
        return
    kind = strip_ns(node.get("type"))
    if kind == "model" and isinstance(node.get("model"), str):
        yield node["model"]
    elif kind == "special" and isinstance(node.get("base"), str):
        yield node["base"]
    keys = [k for k in DEFAULT_BRANCHES if k in node] + [k for k in node if k not in DEFAULT_BRANCHES]
    for key in keys:
        if isinstance(node[key], (dict, list)):
            yield from iter_model_refs(node[key])


def find_cmd_switches(node: Any, base_item: str, source: Path) -> Iterator[CmdEntry]:
    """Finds the CustomModelData switches of a 26.x item definition and yields one entry per branch."""
    if isinstance(node, list):
        for child in node:
            yield from find_cmd_switches(child, base_item, source)
        return
    if not isinstance(node, dict):
        return
    kind = strip_ns(node.get("type"))
    if strip_ns(node.get("property")) == "custom_model_data" and kind in ("range_dispatch", "select", "condition"):
        index = node.get("index", 0)
        if kind == "range_dispatch":
            for entry in node.get("entries", []):
                yield CmdEntry(base_item, kind, _with_index(entry.get("threshold"), index), entry.get("model", {}), source)
        elif kind == "select":
            for case in node.get("cases", []):
                yield CmdEntry(base_item, kind, _with_index(case.get("when"), index), case.get("model", {}), source)
        else:
            yield CmdEntry(base_item, kind, f"flag[{index}]", node.get("on_true", {}), source)
        return
    for value in node.values():
        if isinstance(value, (dict, list)):
            yield from find_cmd_switches(value, base_item, source)


def _with_index(key: Any, index: int) -> Any:
    return key if index == 0 else f"[{index}] {key}"


def scan_item_definitions(pack: Path) -> list[CmdEntry]:
    entries: list[CmdEntry] = []
    for path in sorted((pack / "assets" / "minecraft" / "items").glob("*.json")):
        data = read_json(path)
        if isinstance(data, dict):
            entries.extend(find_cmd_switches(data.get("model", {}), path.stem, path))
    return entries


def scan_legacy_overrides(pack: Path) -> list[CmdEntry]:
    """Reads ``overrides`` from legacy item models. Overrides that also test other predicates (a bow's ``pulling``)
    are kept as extra models of the same entry, since they have no direct 26.x equivalent here."""
    entries: list[CmdEntry] = []
    for path in sorted((pack / "assets" / "minecraft" / "models" / "item").glob("*.json")):
        data = read_json(path)
        overrides = data.get("overrides") if isinstance(data, dict) else None
        if not isinstance(overrides, list):
            continue
        by_value: dict[Any, list[dict]] = {}
        for override in overrides:
            predicate = override.get("predicate", {}) if isinstance(override, dict) else {}
            if "custom_model_data" in predicate and isinstance(override.get("model"), str):
                by_value.setdefault(predicate["custom_model_data"], []).append(override)
        for value, group in by_value.items():
            plain = [o for o in group if len(o["predicate"]) == 1]
            main = (plain or group)[0]
            node: dict = {"type": "minecraft:model", "model": main["model"]}
            entry = CmdEntry(path.stem, "legacy_override", value, node, path)
            extras = [o for o in group if o is not main]
            if extras:
                # Keep the extra models reachable so they are converted too; the definition needs hand editing.
                node["_legacy_variants"] = [{"type": "minecraft:model", "model": o["model"]} for o in extras]
                entry.notes.append(
                    "extra predicates " + ", ".join(sorted({k for o in extras for k in o["predicate"]} - {"custom_model_data"}))
                    + ": write this item's definition by hand")
            entries.append(entry)
    return entries


class PackConverter:
    def __init__(self, pack: Path, output: Path, namespace: str):
        self.pack = pack
        self.output = output
        self.namespace = namespace
        self.model_names: dict[str, str] = {}  # pack model id -> output name
        self.texture_targets: dict[str, str] = {}  # pack texture id -> output texture path
        self.missing_models: set[str] = set()
        self.warnings: list[str] = []

    # References -------------------------------------------------------------------------------------------

    @staticmethod
    def is_vanilla(namespace: str, path: str, roots: set[str]) -> bool:
        return namespace == "minecraft" and path.split("/", 1)[0] in roots

    def model_file(self, ref: str) -> Path:
        namespace, path = split_id(ref)
        return self.pack / "assets" / namespace / "models" / f"{path}.json"

    def collect_models(self, refs: list[str]) -> None:
        """Adds ``refs`` and the pack models they inherit from to :attr:`model_names` (named later)."""
        pending = list(refs)
        while pending:
            ref = self.normalise(pending.pop())
            if ref in self.model_names or ref in self.missing_models:
                continue
            namespace, path = split_id(ref)
            if self.is_vanilla(namespace, path, VANILLA_MODEL_ROOTS):
                continue
            file = self.model_file(ref)
            if not file.is_file():
                self.missing_models.add(ref)
                continue
            self.model_names[ref] = ""
            data = read_json(file)
            if isinstance(data, dict) and isinstance(data.get("parent"), str):
                pending.append(data["parent"])

    @staticmethod
    def normalise(ref: str) -> str:
        namespace, path = split_id(ref)
        return f"{namespace}:{path}"

    def assign_names(self) -> None:
        """Names each model after its file, prefixing parent folders (then the namespace) only where two models would
        otherwise share a name."""
        depth = dict.fromkeys(self.model_names, 1)

        def name_of(ref: str) -> str:
            namespace, path = split_id(ref)
            parts = path.split("/")
            if depth[ref] > len(parts):
                parts = [namespace, *parts]
            return to_snake_case("_".join(parts[-depth[ref]:]))

        while True:
            groups: dict[str, list[str]] = {}
            for ref in depth:
                groups.setdefault(name_of(ref), []).append(ref)
            clashing = [ref for refs in groups.values() if len(refs) > 1 for ref in refs]
            if not clashing:
                break
            grew = False
            for ref in clashing:
                if depth[ref] <= len(split_id(ref)[1].split("/")):
                    depth[ref] += 1
                    grew = True
            if not grew:
                raise ScriptError("these models only differ in punctuation and would get one name: "
                                  + ", ".join(sorted(clashing)))
        for ref in depth:
            self.model_names[ref] = name_of(ref)
            if depth[ref] > 1:
                self.warnings.append(f"name clash: {ref} is written as item/{self.model_names[ref]}")

    def model_id(self, ref: str) -> str:
        ref = self.normalise(ref)
        if ref in self.model_names:
            return f"{self.namespace}:item/{self.model_names[ref]}"
        return ref  # vanilla, or missing from the pack: left pointing where it pointed

    def texture_id(self, ref: str) -> str:
        if ref.startswith("#"):
            return ref
        namespace, path = split_id(ref)
        if self.is_vanilla(namespace, path, VANILLA_TEXTURE_ROOTS):
            return f"{namespace}:{path}"  # a vanilla sprite, even where the pack retextures it
        key = f"{namespace}:{path}"
        if key not in self.texture_targets:
            target = path if path.split("/", 1)[0] in ("item", "block") else f"item/{path}"
            taken = {t for k, t in self.texture_targets.items() if k != key}
            if target in taken:
                root, _, rest = target.partition("/")
                target = f"{root}/{namespace}/{rest}"
                self.warnings.append(f"texture clash: {key} is written as {self.namespace}:{target}")
            self.texture_targets[key] = target
        return f"{self.namespace}:{self.texture_targets[key]}"

    # Rewriting ---------------------------------------------------------------------------------------------

    def convert_model(self, ref: str) -> dict:
        data = copy.deepcopy(read_json(self.model_file(ref)))
        if not isinstance(data, dict):
            raise ScriptError(f"{self.model_file(ref)}: a model must be a JSON object")
        data.pop("overrides", None)
        if isinstance(data.get("parent"), str):
            data["parent"] = self.model_id(data["parent"])
        textures = data.get("textures")
        if isinstance(textures, dict):
            data["textures"] = {k: self.texture_id(v) if isinstance(v, str) else v for k, v in textures.items()}
        return data

    def convert_tree(self, node: Any) -> Any:
        """Rewrites an item model tree so that its models point at the converted ones."""
        if isinstance(node, list):
            return [self.convert_tree(child) for child in node]
        if not isinstance(node, dict):
            return node
        out = {}
        kind = strip_ns(node.get("type"))
        for key, value in node.items():
            if key == "_legacy_variants":
                continue
            if key == "model" and kind == "model" and isinstance(value, str):
                out[key] = self.model_id(value)
            elif key == "base" and kind == "special" and isinstance(value, str):
                out[key] = self.model_id(value)
            elif key == "type" and isinstance(value, str) and ":" not in value:
                out[key] = f"minecraft:{value}"
            else:
                out[key] = self.convert_tree(value)
        return out


def select_entries(entries: list[CmdEntry], base_items: list[str] | None, pattern: re.Pattern | None) -> list[CmdEntry]:
    selected = []
    seen = set()
    for entry in entries:
        if base_items and entry.base_item not in base_items:
            continue
        refs = entry.model_refs()
        if not refs:
            continue
        if pattern and not any(pattern.search(ref) for ref in refs):
            continue
        key = (entry.base_item, str(entry.key))
        if key in seen:
            continue  # the 26.x definition wins over a legacy override for the same value
        seen.add(key)
        selected.append(entry)
    return selected


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0],
                                     epilog="See the module docstring for the formats read and written.")
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK,
                        help=f"server resource pack root, holding assets/ (default: {relative(DEFAULT_PACK)})")
    parser.add_argument("--output", type=Path, default=MAIN_ASSETS,
                        help=f"assets folder of the target namespace (default: {relative(MAIN_ASSETS)})")
    parser.add_argument("--namespace", default="apeira", help="namespace of --output (default: %(default)s)")
    parser.add_argument("--base-item", action="append", metavar="ITEM",
                        help="only convert models of this vanilla item, such as paper; repeatable")
    parser.add_argument("--match", metavar="REGEX", help="only convert entries with a model id matching REGEX")
    parser.add_argument("--item-definitions", action="store_true",
                        help="also write items/<name>.json item definitions (datagen writes plain ones)")
    parser.add_argument("--copy-textures", action="store_true",
                        help="copy the textures the models use into <output>/textures/")
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST,
                        help=f"where to write the CustomModelData -> item report (default: {relative(DEFAULT_MANIFEST)})")
    parser.add_argument("--force", action="store_true", help="overwrite files that already exist")
    parser.add_argument("--dry-run", action="store_true", help="report what would be written, write nothing")
    parser.add_argument("-v", "--verbose", action="store_true", help="list every file")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    pack: Path = args.pack
    if not (pack / "assets").is_dir():
        raise ScriptError(f"{pack} is not a resource pack: it has no assets/ folder (pass --pack)")
    try:
        pattern = re.compile(args.match) if args.match else None
    except re.error as error:
        raise ScriptError(f"--match is not a valid regular expression: {error}") from error

    entries = select_entries(scan_item_definitions(pack) + scan_legacy_overrides(pack), args.base_item, pattern)
    if not entries:
        print("No CustomModelData models found" + (" for the given filters" if args.base_item or pattern else ""))
        return 0

    converter = PackConverter(pack, args.output, args.namespace)
    for entry in entries:
        converter.collect_models(entry.model_refs())
    converter.assign_names()

    written = skipped = 0
    prefix = "[dry run] " if args.dry_run else ""

    def emit(path: Path, data: Any) -> None:
        nonlocal written, skipped
        if path.exists() and not args.force:
            skipped += 1
            if args.verbose:
                print(f"{prefix}skip (exists) {relative(path)}")
            return
        write_json(path, data, dry_run=args.dry_run)
        written += 1
        if args.verbose:
            print(f"{prefix}write {relative(path)}")

    models_dir = args.output / "models" / "item"
    for ref, name in sorted(converter.model_names.items(), key=lambda item: item[1]):
        emit(models_dir / f"{name}.json", converter.convert_model(ref))

    manifest = []
    definitions: dict[str, CmdEntry] = {}
    for entry in entries:
        refs = entry.model_refs()
        main_ref = converter.normalise(refs[0])
        item = converter.model_names.get(main_ref) or to_snake_case(split_id(main_ref)[1].rsplit("/", 1)[-1])
        manifest.append({
            "base_item": f"minecraft:{entry.base_item}", "kind": entry.kind, "custom_model_data": entry.key,
            "item": f"{args.namespace}:{item}", "models": [converter.model_id(r) for r in refs],
            "source": relative(entry.source), "notes": entry.notes,
        })
        if main_ref in converter.missing_models:
            continue
        if item in definitions and definitions[item].node != entry.node:
            converter.warnings.append(f"items/{item}.json: {entry.base_item} and {definitions[item].base_item} "
                                      "use the same model differently; keeping the first")
        definitions.setdefault(item, entry)

    if args.item_definitions:
        for item, entry in sorted(definitions.items()):
            emit(args.output / "items" / f"{item}.json", {"model": converter.convert_tree(entry.node)})

    copied = 0
    if args.copy_textures:
        for source_id, target in sorted(converter.texture_targets.items()):
            namespace, path = split_id(source_id)
            source = pack / "assets" / namespace / "textures" / f"{path}.png"
            if not source.is_file():
                converter.warnings.append(f"texture {source_id} is missing from the pack")
                continue
            for suffix in (".png", ".png.mcmeta"):
                src = source.with_name(source.stem + suffix)
                dst = args.output / "textures" / f"{target}{suffix}"
                if not src.is_file():
                    continue
                if dst.exists() and not args.force:
                    skipped += 1
                    continue
                if not args.dry_run:
                    dst.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(src, dst)
                copied += 1
                if args.verbose:
                    print(f"{prefix}copy {relative(src)} -> {relative(dst)}")

    missing = sorted(converter.missing_models)
    if not args.dry_run:
        write_json(args.manifest, {"pack": str(pack), "entries": manifest, "missing_models": missing,
                                   "warnings": converter.warnings}, dry_run=False)

    for warning in converter.warnings:
        print(f"warning: {warning}")
    if missing:
        shown = missing if args.verbose else missing[:5]
        print(f"warning: {len(missing)} models are missing from the pack and were not converted: "
              + ", ".join(shown) + ("" if len(shown) == len(missing) else ", ... (see the manifest or --verbose)"))
    print(f"{prefix}{len(entries)} CustomModelData entries, {len(converter.model_names)} models, "
          f"{len(definitions) if args.item_definitions else 0} item definitions, {copied} texture files: "
          f"{written} written, {skipped} skipped because they exist (use --force)")
    if not args.copy_textures and converter.texture_targets:
        print(f"{len(converter.texture_targets)} textures are referenced; pass --copy-textures to copy them")
    if not args.dry_run:
        print(f"Manifest: {relative(args.manifest)}")
    return 0


if __name__ == "__main__":
    run(main)
