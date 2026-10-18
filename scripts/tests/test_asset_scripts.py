"""Tests for the asset scripts. Run from the repository root with ``python -m unittest discover -s scripts/tests``."""

from __future__ import annotations

import contextlib
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import flatten_textures  # noqa: E402
import parse_blockbench_hitboxes as hitboxes  # noqa: E402
import process_server_pack  # noqa: E402
from asset_common import to_snake_case  # noqa: E402

PNG = b"\x89PNG\r\n\x1a\n"


def write(path: Path, data) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, bytes):
        path.write_bytes(data)
    else:
        path.write_text(json.dumps(data), encoding="utf-8")
    return path


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def quietly(main, argv) -> tuple[int, str]:
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
        code = main(argv)
    return code, out.getvalue()


class TempDirTest(unittest.TestCase):
    def setUp(self):
        self._dir = tempfile.TemporaryDirectory()
        self.root = Path(self._dir.name)

    def tearDown(self):
        self._dir.cleanup()


class SnakeCaseTest(unittest.TestCase):
    def test_names(self):
        self.assertEqual(to_snake_case("Platinum Sword-Glow 2"), "platinum_sword_glow_2")
        self.assertEqual(to_snake_case("platinumSword"), "platinum_sword")
        self.assertEqual(to_snake_case("HTMLParser"), "html_parser")
        self.assertEqual(to_snake_case("__already_snake__"), "already_snake")
        self.assertEqual(flatten_textures.snake_file_name("Big Leaf.PNG.mcmeta"), "big_leaf.png.mcmeta")


class ProcessServerPackTest(TempDirTest):
    def make_pack(self) -> Path:
        pack = self.root / "pack"
        assets = pack / "assets"
        # Legacy override style.
        write(assets / "minecraft/models/item/stick.json", {
            "parent": "item/handheld", "textures": {"layer0": "item/stick"},
            "overrides": [
                {"predicate": {"custom_model_data": 1}, "model": "custom/saplings/fir_sapling"},
                {"predicate": {"custom_model_data": 2}, "model": "custom/gear/shadow/sword"},
            ]})
        # 26.x item definition style, including a bow that pulls and a name clash ("sword").
        write(assets / "minecraft/items/bow.json", {"model": {
            "type": "minecraft:range_dispatch", "property": "minecraft:custom_model_data",
            "fallback": {"type": "minecraft:model", "model": "item/bow"},
            "entries": [
                {"threshold": 1, "model": {
                    "type": "condition", "property": "using_item",
                    "on_false": {"type": "model", "model": "custom/gear/platinum/platinum_bow"},
                    "on_true": {"type": "model", "model": "custom/gear/platinum/platinum_bow_pulling"}}},
                {"threshold": 2, "model": {"type": "model", "model": "custom/gear/toxic/sword"}},
            ]}})
        model = lambda texture: {"parent": "custom/base/handheld_big", "textures": {"layer0": texture}}  # noqa: E731
        write(assets / "minecraft/models/custom/saplings/fir_sapling.json",
              {"parent": "item/generated", "textures": {"layer0": "originrealms:block/saplings/fir"}})
        write(assets / "minecraft/models/custom/gear/shadow/sword.json", model("custom/gear/shadow/sword"))
        write(assets / "minecraft/models/custom/gear/toxic/sword.json", model("custom/gear/toxic/sword"))
        write(assets / "minecraft/models/custom/gear/platinum/platinum_bow.json", model("custom/gear/platinum/bow"))
        write(assets / "minecraft/models/custom/gear/platinum/platinum_bow_pulling.json",
              model("custom/gear/platinum/bow_pulling"))
        write(assets / "minecraft/models/custom/base/handheld_big.json", {"parent": "item/handheld",
                                                                         "display": {"gui": {"scale": [1, 1, 1]}}})
        write(assets / "originrealms/textures/block/saplings/fir.png", PNG)
        write(assets / "minecraft/textures/custom/gear/shadow/sword.png", PNG)
        return pack

    def run_script(self, *extra) -> tuple[int, str, Path]:
        out = self.root / "out"
        code, text = quietly(process_server_pack.main, [
            "--pack", str(self.make_pack()), "--output", str(out), "--manifest", str(self.root / "manifest.json"),
            *extra])
        return code, text, out

    def test_converts_both_formats(self):
        code, _, out = self.run_script("--item-definitions", "--copy-textures")
        self.assertEqual(code, 0)
        models = out / "models/item"
        self.assertEqual(sorted(p.stem for p in models.iterdir()), [
            "fir_sapling", "handheld_big", "platinum_bow", "platinum_bow_pulling", "shadow_sword", "toxic_sword"])
        self.assertEqual(load(models / "fir_sapling.json"), {
            "parent": "minecraft:item/generated", "textures": {"layer0": "apeira:block/saplings/fir"}})
        self.assertEqual(load(models / "shadow_sword.json")["parent"], "apeira:item/handheld_big")
        self.assertEqual(load(models / "shadow_sword.json")["textures"]["layer0"],
                         "apeira:item/custom/gear/shadow/sword")
        bow = load(out / "items/platinum_bow.json")["model"]
        self.assertEqual(bow["type"], "minecraft:condition")
        self.assertEqual(bow["on_true"]["model"], "apeira:item/platinum_bow_pulling")
        self.assertTrue((out / "textures/block/saplings/fir.png").is_file())
        self.assertTrue((out / "textures/item/custom/gear/shadow/sword.png").is_file())
        manifest = load(self.root / "manifest.json")
        values = {(e["base_item"], e["custom_model_data"]): e["item"] for e in manifest["entries"]}
        self.assertEqual(values[("minecraft:stick", 1)], "apeira:fir_sapling")
        self.assertEqual(values[("minecraft:bow", 2)], "apeira:toxic_sword")

    def test_dry_run_writes_nothing_and_existing_files_are_kept(self):
        _, _, out = self.run_script("--dry-run")
        self.assertFalse(out.exists())
        write(out / "models/item/fir_sapling.json", {"keep": True})
        self.run_script()
        self.assertEqual(load(out / "models/item/fir_sapling.json"), {"keep": True})
        self.run_script("--force")
        self.assertIn("parent", load(out / "models/item/fir_sapling.json"))

    def test_filters(self):
        _, _, out = self.run_script("--base-item", "stick", "--match", "saplings")
        self.assertEqual([p.stem for p in (out / "models/item").iterdir()], ["fir_sapling"])


class FlattenTexturesTest(TempDirTest):
    def setUp(self):
        super().setUp()
        self.assets = self.root / "assets/apeira"
        self.textures = self.assets / "textures"

    def flatten(self, *extra) -> tuple[int, str]:
        return quietly(flatten_textures.main, ["--assets", str(self.assets), *extra])

    def test_flattens_renames_and_updates_models(self):
        write(self.textures / "item/Custom/Minerals/Platinum Sword.png", PNG)
        write(self.textures / "item/Custom/Minerals/Platinum Sword.png.mcmeta", {"animation": {}})
        write(self.textures / "block/misc/FirSapling.png", PNG)
        write(self.assets / "models/item/platinum_sword.json",
              {"textures": {"layer0": "apeira:item/Custom/Minerals/Platinum Sword", "other": "minecraft:item/stick"}})
        code, _ = self.flatten()
        self.assertEqual(code, 0)
        files = sorted(p.relative_to(self.textures).as_posix() for p in self.textures.rglob("*") if p.is_file())
        self.assertEqual(files, ["block/fir_sapling.png", "item/platinum_sword.png", "item/platinum_sword.png.mcmeta"])
        self.assertFalse((self.textures / "item/Custom").exists())
        self.assertEqual(load(self.assets / "models/item/platinum_sword.json")["textures"],
                         {"layer0": "apeira:item/platinum_sword", "other": "minecraft:item/stick"})

    def test_collisions_are_left_in_place(self):
        write(self.textures / "item/shadow/sword.png", PNG)
        write(self.textures / "item/toxic/Sword.png", PNG)
        write(self.textures / "item/helmet/cap.png", PNG)
        code, text = self.flatten()
        self.assertEqual(code, 1)
        self.assertIn("collision", text)
        self.assertTrue((self.textures / "item/shadow/sword.png").is_file())
        self.assertTrue((self.textures / "item/toxic/Sword.png").is_file())
        self.assertTrue((self.textures / "item/cap.png").is_file())

    def test_prefix_resolves_collisions(self):
        write(self.textures / "item/shadow/sword.png", PNG)
        write(self.textures / "item/toxic/sword.png", PNG)
        write(self.textures / "item/sword.png", PNG)
        code, _ = self.flatten("--on-collision", "prefix")
        self.assertEqual(code, 0)
        names = sorted(p.name for p in (self.textures / "item").iterdir())
        self.assertEqual(names, ["shadow_sword.png", "sword.png", "toxic_sword.png"])

    def test_dry_run_changes_nothing(self):
        write(self.textures / "item/a/B.png", PNG)
        self.flatten("--dry-run")
        self.assertTrue((self.textures / "item/a/B.png").is_file())


class HitboxTest(TempDirTest):
    def shapes(self, path: Path, *extra) -> str:
        code, text = quietly(hitboxes.main, [str(path), *extra])
        self.assertEqual(code, 0)
        return text

    def test_merges_and_drops(self):
        boxes, dropped = hitboxes.simplify([
            hitboxes.Box(0, 0, 0, 8, 2, 16), hitboxes.Box(8, 0, 0, 16, 2, 16),  # two halves of a slab
            hitboxes.Box(2, 0, 2, 4, 1, 4),  # inside the slab
            hitboxes.Box(0, 2, 0, 16, 2, 16),  # flat plane
        ], 0)
        self.assertEqual(boxes, [hitboxes.Box(0, 0, 0, 16, 2, 16)])
        self.assertEqual(dropped, 1)

    def test_turning(self):
        box = hitboxes.Box(0, 0, 0, 16, 16, 4)  # against the north side
        self.assertEqual(box.turned(1), hitboxes.Box(12, 0, 0, 16, 16, 16))  # east
        self.assertEqual(box.turned(2), hitboxes.Box(0, 0, 12, 16, 16, 16))  # south
        self.assertEqual(box.turned(3), hitboxes.Box(0, 0, 0, 4, 16, 16))  # west

    def test_java_model(self):
        model = write(self.root / "Small Table.json", {"elements": [
            {"from": [0, 14, 0], "to": [16, 16, 16]},
            {"from": [7, 0, 7], "to": [9, 14, 9]},
            {"from": [6, 0, 6], "to": [10, 14, 10], "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}},
        ]})
        text = self.shapes(model)
        self.assertIn("SMALL_TABLE_SHAPE = Shapes.or(", text)
        self.assertIn("Block.box(0, 14, 0, 16, 16, 16)", text)
        self.assertIn("Block.box(5.1716, 0, 5.1716, 10.8284, 14, 10.8284)", text)
        self.assertNotIn("Block.box(7, 0, 7", text)  # inside the rotated post

    def test_bbmodel_hitbox_group_and_offset(self):
        model = write(self.root / "chair.bbmodel", {
            "meta": {"model_format": "free"},
            "elements": [
                {"uuid": "seat", "type": "cube", "from": [-8, 0, -8], "to": [8, 8, 8], "origin": [0, 0, 0]},
                {"uuid": "detail", "type": "cube", "from": [-1, 8, -1], "to": [1, 20, 1], "origin": [0, 0, 0]},
                {"uuid": "hb", "type": "cube", "from": [-8, 0, -8], "to": [8, 10, 0], "origin": [0, 0, 0],
                 "visibility": False},
            ],
            "outliner": [{"name": "model", "uuid": "g1", "children": ["seat", "detail"]},
                         {"name": "Hitbox", "uuid": "g2", "children": ["hb"]}]})
        text = self.shapes(model, "--facings")
        self.assertIn("CHAIR_SHAPE_NORTH = Block.box(0, 0, 0, 16, 10, 8);", text)
        self.assertIn("CHAIR_SHAPE_EAST = Block.box(8, 0, 0, 16, 10, 16);", text)
        everything = self.shapes(model, "--group", "model", "--bounds")
        self.assertIn("Block.box(0, 0, 0, 16, 20, 16)", everything)

    def test_errors(self):
        model = write(self.root / "flat.json", {"parent": "item/generated"})
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(hitboxes.ScriptError):
                hitboxes.main([str(model)])
        output = write(self.root / "out.java", b"keep")
        with self.assertRaises(hitboxes.ScriptError):
            hitboxes.main([str(model), "-o", str(output)])


if __name__ == "__main__":
    unittest.main()
