#!/usr/bin/env python3
"""Draws 16x16 placeholder textures for models whose textures do not exist yet.

Every ``<namespace>:block/...`` and ``<namespace>:item/...`` texture that a model in ``--models`` refers to (by
default the generated models of apeira-main) is drawn into ``--assets``/textures when the file is missing, so new
blocks and items show a tinted placeholder in game instead of the magenta and black "missing" checkerboard.
Block textures are a brick pattern, item textures a gem shape on a transparent background; the colour comes from
the texture name, so each placeholder looks different and stays the same between runs.

Real textures simply replace the placeholders: the script never overwrites a file unless given ``--force``.

Example::

    python scripts/make_placeholder_textures.py --dry-run
    python scripts/make_placeholder_textures.py
"""

from __future__ import annotations

import argparse
import colorsys
import hashlib
import struct
import zlib
from pathlib import Path

from asset_common import MAIN_ASSETS, REPO_ROOT, ScriptError, read_json, relative, run, split_id

#: Models written by the apeira-main data run.
GENERATED_MODELS = REPO_ROOT / "apeira-main" / "src" / "generated" / "resources" / "assets" / "apeira" / "models"

SIZE = 16
Pixel = tuple[int, int, int, int]
TRANSPARENT: Pixel = (0, 0, 0, 0)


def model_textures(models: Path, namespace: str) -> list[str]:
    """Texture paths (``block/stone``) in ``namespace`` that the models under ``models`` refer to, sorted."""
    if not models.is_dir():
        raise ScriptError(f"{models} is not a folder")
    found: set[str] = set()
    for path in sorted(models.rglob("*.json")):
        textures = read_json(path).get("textures", {})
        for value in textures.values():
            if not isinstance(value, str) or value.startswith("#"):
                continue  # a reference to another texture variable of the model
            texture_namespace, texture = split_id(value)
            if texture_namespace == namespace and texture.split("/")[0] in ("block", "item"):
                found.add(texture)
    return sorted(found)


def base_colour(name: str) -> tuple[float, float, float]:
    """A hue, saturation and value picked from ``name``, so every texture gets its own stable colour."""
    digest = hashlib.sha256(name.encode("utf-8")).digest()
    return digest[0] / 255, 0.35 + digest[1] / 255 * 0.3, 0.6 + digest[2] / 255 * 0.25


def shade(hsv: tuple[float, float, float], value_scale: float) -> Pixel:
    hue, saturation, value = hsv
    red, green, blue = colorsys.hsv_to_rgb(hue, saturation, max(0.0, min(1.0, value * value_scale)))
    return round(red * 255), round(green * 255), round(blue * 255), 255


def block_pixels(name: str) -> list[list[Pixel]]:
    """Four rows of bricks with darker mortar lines, offset by half a brick on every other row."""
    hsv = base_colour(name)
    brick, light, mortar = shade(hsv, 1.0), shade(hsv, 1.15), shade(hsv, 0.6)
    pixels = []
    for y in range(SIZE):
        row = []
        offset = 0 if (y // 4) % 2 == 0 else 4
        for x in range(SIZE):
            if y % 4 == 3 or (x + offset) % 8 == 7:
                row.append(mortar)
            elif y % 4 == 0 or (x + offset) % 8 == 0:
                row.append(light)  # highlight on each brick's top and left edge
            else:
                row.append(brick)
        pixels.append(row)
    return pixels


def item_pixels(name: str) -> list[list[Pixel]]:
    """A diamond-shaped gem with a dark outline and a lighter upper half."""
    hsv = base_colour(name)
    outline, light, dark = shade(hsv, 0.45), shade(hsv, 1.2), shade(hsv, 0.9)
    centre = (SIZE - 1) / 2
    pixels = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            distance = abs(x - centre) + abs(y - centre)
            if distance > 7.5:
                row.append(TRANSPARENT)
            elif distance > 6.5:
                row.append(outline)
            else:
                row.append(light if y < centre else dark)
        pixels.append(row)
    return pixels


def encode_png(pixels: list[list[Pixel]]) -> bytes:
    """Encodes rows of RGBA pixels as an 8-bit RGBA PNG."""
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def placeholder(texture: str) -> bytes:
    """The PNG for ``texture`` (``block/<name>`` or ``item/<name>``)."""
    draw = block_pixels if texture.startswith("block/") else item_pixels
    return encode_png(draw(texture))


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--models", type=Path, default=GENERATED_MODELS,
                        help="models whose textures to check (default: %(default)s)")
    parser.add_argument("--assets", type=Path, default=MAIN_ASSETS,
                        help="assets folder the textures go in (default: %(default)s)")
    parser.add_argument("--namespace", default="apeira", help="namespace of --assets (default: %(default)s)")
    parser.add_argument("--force", action="store_true", help="redraw textures that already exist")
    parser.add_argument("--dry-run", action="store_true", help="print the textures without writing anything")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    textures = args.assets / "textures"
    written = 0
    for texture in model_textures(args.models, args.namespace):
        target = textures / f"{texture}.png"
        if target.exists() and not args.force:
            continue
        print(f"{'would draw' if args.dry_run else 'drew'} {relative(target)}")
        if not args.dry_run:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(placeholder(texture))
        written += 1
    print(f"{written} placeholder texture(s) {'to draw' if args.dry_run else 'drawn'}")
    return 0


if __name__ == "__main__":
    run(main)
