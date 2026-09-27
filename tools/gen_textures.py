#!/usr/bin/env python3
"""Generates the mod's placeholder-free pixel art assets.

Requires Pillow (available in this dev environment). Run from the repository root:

    python3 tools/gen_textures.py

Outputs (all committed):
  * assets/creativecontainer/textures/block/creative_container.png  16x16 block texture
  * assets/creativecontainer/textures/gui/creative_container.png    396x222 screen background
  * assets/creativecontainer/textures/gui/slot.png                  18x18 slot cell
"""

from __future__ import annotations

import pathlib
import random

from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/creativecontainer/textures"

OUTLINE = (12, 13, 17, 255)
STEEL_DARK = (58, 62, 72, 255)
STEEL = (104, 110, 124, 255)
STEEL_LIGHT = (150, 157, 172, 255)
PANEL_DARK = (26, 28, 35, 255)
PANEL = (34, 37, 45, 255)
PANEL_LIGHT = (44, 48, 58, 255)
CYAN_DIM = (32, 128, 148, 255)
CYAN = (64, 214, 230, 255)
CYAN_BRIGHT = (176, 255, 255, 255)
VIOLET = (146, 96, 214, 255)


def noise(color, rng, amount=6):
    return (
        max(0, min(255, color[0] + rng.randint(-amount, amount))),
        max(0, min(255, color[1] + rng.randint(-amount, amount))),
        max(0, min(255, color[2] + rng.randint(-amount, amount))),
        color[3],
    )


def block_texture() -> Image.Image:
    """A steel machine face with a glowing cyan core: the container is a visible endgame block."""
    rng = random.Random(0xC0FFEE)
    img = Image.new("RGBA", (16, 16))
    px = img.load()

    for y in range(16):
        for x in range(16):
            # recessed inner area, slightly darker towards the bottom
            shade = PANEL_DARK
            if y >= 8:
                shade = (22, 24, 30, 255)
            px[x, y] = noise(shade, rng, 3)

    # steel frame ring
    for i in range(16):
        px[i, 1] = noise(STEEL, rng)
        px[i, 14] = noise(STEEL, rng)
        px[1, i] = noise(STEEL, rng)
        px[14, i] = noise(STEEL, rng)

    # brighter corner blocks of the frame
    for cx, cy in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[cx, cy] = noise(STEEL_LIGHT, rng, 4)

    # outline
    for i in range(16):
        px[i, 0] = OUTLINE
        px[i, 15] = OUTLINE
        px[0, i] = OUTLINE
        px[15, i] = OUTLINE

    # glowing core: a diamond of three shades
    for y in range(2, 14):
        for x in range(2, 14):
            distance = abs(x - 7.5) + abs(y - 7.5)
            if distance < 2.2:
                px[x, y] = CYAN_BRIGHT
            elif distance < 3.8:
                px[x, y] = CYAN
            elif distance < 5.0:
                px[x, y] = CYAN_DIM

    # violet corner studs inside the recess
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px[cx, cy] = VIOLET
    return img


def slot_texture() -> Image.Image:
    """One inventory cell; drawn by the screen on top of the background."""
    img = Image.new("RGBA", (18, 18), PANEL_DARK)
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, 17, 17), outline=STEEL_DARK)
    draw.line((1, 1, 16, 1), fill=STEEL_DARK)
    draw.line((1, 1, 1, 16), fill=STEEL_DARK)
    # a faint inner well so items read as "inside" the cell
    draw.rectangle((2, 2, 15, 15), outline=(20, 22, 28, 255))
    return img


def screen_background(width: int = 396, height: int = 222) -> Image.Image:
    """Two-panel layout: left = creative item browser, right = player inventory + pool."""
    img = Image.new("RGBA", (width, height), PANEL_DARK)
    draw = ImageDraw.Draw(img)

    # outer frame
    draw.rectangle((0, 0, width - 1, height - 1), outline=OUTLINE)
    draw.rectangle((1, 1, width - 2, height - 2), outline=STEEL_DARK)

    # left browser panel
    draw.rectangle((6, 4, 191, height - 5), fill=PANEL, outline=STEEL_DARK)
    # right panel (inventory + pool)
    draw.rectangle((199, 4, width - 7, height - 5), fill=PANEL, outline=STEEL_DARK)

    # accent bars
    draw.rectangle((7, 5, 190, 6), fill=CYAN_DIM)
    draw.rectangle((200, 5, width - 8, 6), fill=CYAN_DIM)

    # recessed areas for the two item grids and the pool
    draw.rectangle((14, 34, 180, 128), fill=PANEL_DARK, outline=STEEL_DARK)
    draw.rectangle((198, 146, 364, 200), fill=PANEL_DARK, outline=STEEL_DARK)

    # recessed area behind the player inventory
    draw.rectangle((198, 34, 364, 116), fill=PANEL_DARK, outline=STEEL_DARK)
    return img


def main() -> None:
    (ASSETS / "block").mkdir(parents=True, exist_ok=True)
    (ASSETS / "gui").mkdir(parents=True, exist_ok=True)

    block_texture().save(ASSETS / "block/creative_container.png")
    slot_texture().save(ASSETS / "gui/slot.png")
    screen_background().save(ASSETS / "gui/creative_container.png")
    print("wrote textures under", ASSETS.relative_to(ROOT))


if __name__ == "__main__":
    main()
