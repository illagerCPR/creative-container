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

# ---- AE2-style light GUI palette (screen + slot only; the block face stays a dark machine) ----
GUI_OUTLINE = (26, 26, 26, 255)          # near-black outer frame
GUI_PANEL = (198, 198, 198, 255)         # the classic light container grey
GUI_EDGE_LIGHT = (255, 255, 255, 255)    # top/left bevel highlight
GUI_EDGE_DARK = (85, 85, 85, 255)        # bottom/right bevel shadow
GUI_WELL = (139, 139, 139, 255)          # recessed grid area
GUI_WELL_EDGE = (55, 55, 55, 255)        # recessed border
GUI_ACCENT = (139, 91, 214, 255)         # AE2 violet accent bar
GUI_ACCENT_DARK = (94, 60, 169, 255)


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
    """One inventory cell (18x18): a light recessed well with a vanilla-style bevel."""
    img = Image.new("RGBA", (18, 18), GUI_WELL)
    draw = ImageDraw.Draw(img)
    # outer hairline + inner bevel: dark top/left, light bottom/right reads as "recessed"
    draw.rectangle((0, 0, 17, 17), outline=GUI_WELL_EDGE)
    draw.line((1, 1, 16, 1), fill=GUI_EDGE_DARK)
    draw.line((1, 1, 1, 16), fill=GUI_EDGE_DARK)
    draw.line((2, 16, 16, 16), fill=GUI_EDGE_LIGHT)
    draw.line((16, 2, 16, 15), fill=GUI_EDGE_LIGHT)
    return img


def screen_background(width: int = 396, height: int = 222) -> Image.Image:
    """AE2-style light two-panel layout.

    Mirrors ``CreativeContainerLayout``: left = creative item browser (grid 16..178 x 40..202), right = the pool on
    top (grid 214..376 x 56..110) and the player inventory below (grid 214..376 x 126..202, hotbar flush at y=202),
    both centred on the right page so nothing hugs the page's left border.
    """
    img = Image.new("RGBA", (width, height), GUI_PANEL)
    draw = ImageDraw.Draw(img)

    # outer frame: black hairline, then a vanilla bevel (light top/left, dark bottom/right)
    draw.rectangle((0, 0, width - 1, height - 1), outline=GUI_OUTLINE)
    draw.line((1, 1, width - 2, 1), fill=GUI_EDGE_LIGHT)
    draw.line((1, 1, 1, height - 2), fill=GUI_EDGE_LIGHT)
    draw.line((1, height - 2, width - 2, height - 2), fill=GUI_EDGE_DARK)
    draw.line((width - 2, 2, width - 2, height - 2), fill=GUI_EDGE_DARK)

    # centre seam between the pages: a thin recessed groove at x = 193..197
    draw.rectangle((193, 6, 197, height - 7), fill=GUI_WELL, outline=GUI_WELL_EDGE)

    # violet accent bars on top of both pages
    draw.rectangle((7, 5, 190, 6), fill=GUI_ACCENT, outline=GUI_ACCENT_DARK)
    draw.rectangle((200, 5, width - 8, 6), fill=GUI_ACCENT, outline=GUI_ACCENT_DARK)

    # recessed wells: the browser grid, the pool grid, the amount field and the player inventory
    draw.rectangle((14, 38, 180, 204), fill=GUI_WELL, outline=GUI_WELL_EDGE)      # browser 9x9
    draw.rectangle((212, 54, 378, 112), fill=GUI_WELL, outline=GUI_WELL_EDGE)     # pool 9x3
    draw.rectangle((212, 40, 312, 54), fill=GUI_WELL, outline=GUI_WELL_EDGE)      # amount field (96x12 + bevel)
    draw.rectangle((212, 124, 378, 204), fill=GUI_WELL, outline=GUI_WELL_EDGE)    # inventory + hotbar
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
