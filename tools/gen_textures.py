"""Generates Doole's Core & Crucible's textures by recolouring vanilla textures.

Run from the repo root after a Gradle build (the client jars come from the Loom cache):
    python tools/gen_textures.py
Output is committed. The recolouring functions are ported from Applied-Enchantments' tools/gen_resources.py:
tint_gray recolours grey metal pixels (tool heads, armor, ingots) to a tier colour and keeps coloured ones (handles);
hue_shift moves saturated pixels (template runes) to a colour. make_vibrant and add_shiny_edges then punch up the
freshly tinted pixels: richer saturation/contrast everywhere, plus faint deterministic edge noise on item icons
so tier colours read as worked metal/gem rather than a flat recolour.

To change a tier's colour, edit its entry in the colour tables below and run the script again.

Sources come from the 26.3 client jar (it has spears and copper gear); the 1.21.1 jar supplies the 1.21.1-only
armor layer format.
"""
import colorsys
import hashlib
import io
import os
import random
import zipfile
from pathlib import Path

from PIL import Image

HOME = Path(os.path.expanduser("~"))
JAR_26 = HOME / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
JAR_121 = HOME / ".gradle/caches/fabric-loom/1.21.1/minecraft-client.jar"
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/dooles_core_crucible/textures"

# Tier colours. Obsidian gear shares the obsidian alloy colour (Applied-Enchantments' 0x3B2754) so it reads as that
# alloy; Reinforced has its own steel-blue hue instead of netherite grey.
# Change a colour here and run the script to recolour that tier's items. Colours are 0xRRGGBB hex.
GEAR = {
    "bone": 0xE3DAC9,
    "flint": 0x3D3C3C,  # unused for the gear itself: flint gear takes vanilla flint's palette (palette_map)
    "emerald": 0x2BC46A,
    "obsidian": 0x3B2754,
    "reinforced": 0x2F5577,
}
ALLOY = {
    "iron": 0xD8B498,
    "emerald": 0x28B65A,
    "diamond": 0x4AEDD9,
    "obsidian": 0x3B2754,
    "netherite": 0x4D494D,
    "reinforced": 0x2F5577,
}
# Mace heads, one per tier the item exists in.
METAL = {
    "wooden": 0x9C7A48, "bone": 0xE3DAC9, "copper": 0xC06A4A, "iron": 0xD8D8D8, "emerald": 0x2BC46A,
    "diamond": 0x4AEDD9, "obsidian": 0x3B2754, "netherite": 0x4D494D, "reinforced": 0x2F5577,
}
# Bow and crossbow wood. Wood is the vanilla bow and crossbow.
LAUNCHER = {
    "bone": 0xE3DAC9, "flint": 0x5A5858, "copper": 0xC06A4A, "iron": 0xD8D8D8, "emerald": 0x2BC46A,
    "diamond": 0x4AEDD9, "obsidian": 0x3B2754, "netherite": 0x4D494D, "reinforced": 0x2F5577,
}
BOW_STATES = ["", "_pulling_0", "_pulling_1", "_pulling_2"]
CROSSBOW_STATES = ["_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework"]

TOOLS = ["pickaxe", "axe", "shovel", "hoe", "sword"]
# Shears: tint colours for Bone, Flint and Copper. Flint's is unused: it takes vanilla flint's palette
# (palette_map), like the flint gear above. Copper reuses the same copper hue as the mace head and bow/crossbow wood.
SHEARS = {"bone": 0xE3DAC9, "flint": 0x3D3C3C, "copper": 0xC06A4A}
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
MACE_TIERS = ["wooden", "copper", "iron", "emerald", "diamond", "obsidian", "netherite", "reinforced"]
MOD_ARMOR = ["emerald", "obsidian", "reinforced"]
BACKPORT_SPEARS = ["wooden", "copper", "iron", "diamond", "netherite", "golden"]  # 1.21.1 mod items, vanilla art
# Horse armor: the mod tiers get the gear colour; Copper and Netherite are 1.21.1 backports with vanilla art.
HORSE_ARMOR = ["emerald", "obsidian", "reinforced"]
BACKPORT_HORSE_ARMOR = ["copper", "netherite"]
# Wolf armor: every tier from Iron up; Copper is vanilla's armadillo scute armor.
WOLF_ARMOR = ["iron", "emerald", "diamond", "obsidian", "netherite", "reinforced"]

written = 0
unchanged = 0
_jars = {}


def rgb(c):
    """Splits a packed 0xRRGGBB int into its (red, green, blue) components."""
    return (c >> 16) & 255, (c >> 8) & 255, c & 255


def load(rel, jar=JAR_26):
    """Loads a vanilla texture (path relative to assets/minecraft/textures) out of a client jar, caching the open jar."""
    if jar not in _jars:
        if not jar.exists():
            raise SystemExit(f"Minecraft client jar not found at {jar}; run a Gradle build first.")
        _jars[jar] = zipfile.ZipFile(jar)
    return Image.open(io.BytesIO(_jars[jar].read(f"assets/minecraft/textures/{rel}"))).convert("RGBA")


def save(img, rel):
    """Writes an image under the mod's texture folder, skipping the write when the file already has these bytes."""
    global written, unchanged
    path = OUT / rel
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    data = buf.getvalue()
    if path.exists() and path.read_bytes() == data:
        unchanged += 1
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    written += 1


def pixels(img):
    """Every (r, g, b, a) pixel of an RGBA image, row by row."""
    px = img.load()
    return [px[x, y] for y in range(img.height) for x in range(img.width)]


def lum(r, g, b):
    """Perceived brightness of a colour, 0 (black) to 1 (white)."""
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0


def sat(r, g, b):
    """Colour saturation, 0 (grey) to 1 (fully saturated)."""
    return colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)[1]


def tint_gray(img, colour, sat_limit=0.2, mid=None, mask=None):
    """Recolour low-saturation (metal) pixels to `colour`, keeping their shading; leaves coloured pixels (handles).
    `mask(x, y)` limits the recolour to part of the image."""
    tr, tg, tb = rgb(colour)
    px = img.load()
    w, h = img.size

    # True for a pixel this function should recolour: visible, low-saturation, and inside the optional mask.
    def chosen(x, y):
        r, g, b, a = px[x, y]
        return a > 0 and sat(r, g, b) < sat_limit and (mask is None or mask(x, y))

    if mid is None:
        # No caller-supplied brightness: use the average brightness of the pixels we're about to recolour.
        vals = []
        for x in range(w):
            for y in range(h):
                if chosen(x, y):
                    vals.append(lum(*px[x, y][:3]))
        mid = sum(vals) / len(vals) if vals else 0.6
    out = img.copy()
    opx = out.load()
    for x in range(w):
        for y in range(h):
            if not chosen(x, y):
                continue
            r, g, b, a = px[x, y]
            f = lum(r, g, b) / mid
            if f <= 1.0:
                nr, ng, nb = tr * f, tg * f, tb * f
            else:
                k = min(1.0, (f - 1.0) / max(0.001, (1.0 / mid) - 1.0))
                nr, ng, nb = tr + (255 - tr) * k * 0.85, tg + (255 - tg) * k * 0.85, tb + (255 - tb) * k * 0.85
            opx[x, y] = (int(min(255, nr)), int(min(255, ng)), int(min(255, nb)), a)
    return out


def palette_map(img, source, sat_limit=0.2):
    """Recolour low-saturation (metal) pixels onto the shades of `source` (e.g. vanilla flint), darkest to darkest and
    lightest to lightest, so the result reads as that material; coloured pixels (handles) stay."""
    # The distinct grey shades used in the source material, darkest first.
    shade_set = set()
    for pixel in pixels(source):
        r, g, b, a = pixel
        if a > 0 and sat(r, g, b) < sat_limit:
            shade_set.add((r, g, b))
    shades = sorted(shade_set, key=lambda c: lum(*c))

    # The distinct grey shades used in our own image, darkest first.
    px = img.load()
    grey_set = set()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a > 0 and sat(r, g, b) < sat_limit:
                grey_set.add((r, g, b))
    greys = sorted(grey_set, key=lambda c: lum(*c))

    # Map each of our shades onto the source shade at the same relative position in its own list.
    to = {}
    for i, grey in enumerate(greys):
        index = round(i * (len(shades) - 1) / max(1, len(greys) - 1))
        to[grey] = shades[index]

    out = img.copy()
    opx = out.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a > 0 and (r, g, b) in to:
                opx[x, y] = to[(r, g, b)] + (a,)
    return out


def hue_shift(img, colour, min_sat=0.25, brightness=1.0):
    """Move saturated pixels (template runes) to the hue/saturation of `colour`, keeping their value."""
    th, ts, tv = colorsys.rgb_to_hsv(*[v / 255 for v in rgb(colour)])
    px = img.load()
    out = img.copy()
    opx = out.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if ss < min_sat:
                continue
            nv = min(1.0, vv * brightness * (0.55 + 0.45 * tv / 0.9))
            ns = min(1.0, max(ss, 0.35) * (0.4 + 0.6 * ts) / 0.9)
            nr, ng, nb = colorsys.hsv_to_rgb(th, ns, nv)
            opx[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return out


def mace_head(x, y):
    """True for pixels on the mace's head (the part to recolour), false for the handle."""
    return y <= 7 or x >= 8


def tint_colours(img, colour, palette, mid, mask=None):
    """Recolour the pixels whose colour is in `palette` (the vanilla wood shades) to `colour`, like tint_gray; `mid` is
    the palette's mean brightness, so every state of one item gets the same shades."""
    out = img.copy()
    px, opx = img.load(), out.load()
    tr, tg, tb = rgb(colour)
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a == 0 or (r, g, b) not in palette or (mask and not mask(x, y)):
                continue
            f = lum(r, g, b) / mid
            if f <= 1.0:
                nr, ng, nb = tr * f, tg * f, tb * f
            else:
                k = min(1.0, (f - 1.0) / max(0.001, (1.0 / mid) - 1.0))
                nr, ng, nb = tr + (255 - tr) * k * 0.85, tg + (255 - tg) * k * 0.85, tb + (255 - tb) * k * 0.85
            opx[x, y] = (int(min(255, nr)), int(min(255, ng)), int(min(255, nb)), a)
    return out


def wood_palette(img):
    """The distinct saturated (wood) colours used in the image, e.g. a bow's or fishing rod's shades."""
    colours = set()
    for pixel in pixels(img):
        r, g, b, a = pixel
        if a > 0 and sat(r, g, b) >= 0.25:
            colours.add((r, g, b))
    return colours


def not_nocked_arrow(x, y):
    """The drawn bow's arrow runs down the main diagonal; it keeps its vanilla shaft."""
    return abs(x - y) > 1


# How visible the edge noise is overall: 0.3 keeps the pattern but at 30% of its original brightness change.
NOISE_SCALE = 0.3

# How strongly add_shiny_edges shines, by tier: metals and gems get the full glint, bone and flint are matte so
# only get a little, and plain wood (the mace head on the wooden tier) gets none. A tier not listed here (a new
# one added later) defaults to the full metal/gem effect.
EDGE_STRENGTH = {"wooden": 0.0, "bone": 0.35, "flint": 0.35}


def edge_strength(tier):
    """How strongly add_shiny_edges should shine for this tier (1.0 = full effect)."""
    return EDGE_STRENGTH.get(tier, 1.0)


def make_vibrant(before, after, sat_boost=1.25, contrast=1.12):
    """Makes the freshly tinted pixels richer: `before` is the source image and `after` is the tinted result, so
    comparing them tells us which pixels the recolour actually changed (the tier-coloured ones) versus which it
    left alone (wooden handles, bowstrings, fishing line, transparent background) - only the changed ones are
    touched. Saturation is boosted by `sat_boost` and a touch of contrast is added around mid-grey, but hue is
    never changed, so it still reads as the same tier colour, just less washed out.
    """
    bpx, apx = before.load(), after.load()
    w, h = after.size
    out = after.copy()
    opx = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = apx[x, y]
            if a == 0 or (r, g, b) == bpx[x, y][:3]:
                continue  # untouched by the recolour step; leave it exactly as it was
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            ss = min(1.0, ss * sat_boost)
            vv = min(1.0, max(0.0, 0.5 + (vv - 0.5) * contrast))
            nr, ng, nb = colorsys.hsv_to_rgb(hh, ss, vv)
            opx[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return out


def add_shiny_edges(before, after, name, strength=1.0):
    """Adds faint deterministic noise around a tinted region, the way worn metal catches the light in a Minecraft
    icon. `before`/`after` are compared the same way as in make_vibrant to find the tinted pixels; the ones that
    touch a transparent or untinted pixel (4-neighbourhood) are the edge. Roughly a third of the edge pixels get a
    little brighter, biased toward the top and left (Minecraft item art is lit from the top-left); a few
    bottom/right edge pixels get a little darker instead, for contrast. NOISE_SCALE keeps all of it subtle, and
    there are no near-white glints. `name` (the output file name) seeds the randomness through an md5 hash, not
    Python's salted hash(), so the same file gets exactly the same noise on every run. `strength` scales the whole
    effect down for matte tiers and off for wood.
    """
    if strength <= 0:
        return after
    rng = random.Random(hashlib.md5(name.encode()).hexdigest())
    bpx, apx = before.load(), after.load()
    w, h = after.size

    def is_tinted(x, y):
        if x < 0 or y < 0 or x >= w or y >= h:
            return False
        r, g, b, a = apx[x, y]
        return a > 0 and (r, g, b) != bpx[x, y][:3]

    out = after.copy()
    opx = out.load()
    for y in range(h):
        for x in range(w):
            if not is_tinted(x, y):
                continue
            top_left = not is_tinted(x, y - 1) or not is_tinted(x - 1, y)
            bottom_right = not is_tinted(x, y + 1) or not is_tinted(x + 1, y)
            if not (top_left or bottom_right):
                continue  # interior pixel, not on the silhouette
            roll = rng.random()
            r, g, b, a = apx[x, y]
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            amount = strength * NOISE_SCALE
            if top_left and roll < 0.6 * strength:
                rng.random()  # used to pick near-white glints; still drawn so the rest of the pattern stays put
                vv = min(1.0, vv + 0.30 * amount)  # a faint bright fleck
                ss = max(0.0, ss - 0.15 * amount)
            elif bottom_right and roll < 0.15 * strength:
                vv = max(0.0, vv * (1 - 0.22 * amount))  # a faint shadow, for contrast
                ss = min(1.0, ss + 0.08 * amount)
            else:
                continue
            nr, ng, nb = colorsys.hsv_to_rgb(hh, ss, vv)
            opx[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return out


def make():
    """Generates every mod item's textures by recolouring the matching vanilla texture, tier by tier."""
    # Gear: tools and spears for every mod tier, armor for the mod armor tiers. Flint takes vanilla flint's shades.
    flint = load("item/flint.png")
    for tier, colour in GEAR.items():
        recolour = (lambda img: palette_map(img, flint)) if tier == "flint" else (lambda img, c=colour: tint_gray(img, c))
        strength = edge_strength(tier)
        for tool in TOOLS:
            src = load(f"item/iron_{tool}.png")
            tinted = make_vibrant(src, recolour(src))
            save(add_shiny_edges(src, tinted, f"{tier}_{tool}", strength), f"item/{tier}_{tool}.png")
        src = load("item/iron_spear.png")
        tinted = make_vibrant(src, recolour(src))
        save(add_shiny_edges(src, tinted, f"{tier}_spear", strength), f"item/{tier}_spear.png")
        src = load("item/iron_spear_in_hand.png")
        tinted = make_vibrant(src, recolour(src))
        save(add_shiny_edges(src, tinted, f"{tier}_spear_in_hand", strength), f"item/{tier}_spear_in_hand.png")

    # Shears: the whole blade is grey metal, so this is the same recolour as the tools above, just on shears.png.
    for tier, colour in SHEARS.items():
        recolour = (lambda img: palette_map(img, flint)) if tier == "flint" else (lambda img, c=colour: tint_gray(img, c))
        strength = edge_strength(tier)
        src = load("item/shears.png")
        tinted = make_vibrant(src, recolour(src))
        save(add_shiny_edges(src, tinted, f"{tier}_shears", strength), f"item/{tier}_shears.png")

    for tier in MOD_ARMOR:
        colour = GEAR[tier]
        strength = edge_strength(tier)
        for piece in ARMOR:
            src = load(f"item/iron_{piece}.png")
            tinted = make_vibrant(src, tint_gray(src, colour, sat_limit=0.35))
            save(add_shiny_edges(src, tinted, f"{tier}_{piece}", strength), f"item/{tier}_{piece}.png")
        # Worn: 26.x equipment layers and 1.21.1 armor layers (same UV layout). Vibrance only, no edge noise: these
        # are seen small and at a distance on the player model, where pixel-noise would read as static, not a shine.
        src = load("entity/equipment/humanoid/iron.png")
        save(make_vibrant(src, tint_gray(src, colour, sat_limit=0.35)), f"entity/equipment/humanoid/{tier}.png")
        src = load("entity/equipment/humanoid_leggings/iron.png")
        save(make_vibrant(src, tint_gray(src, colour, sat_limit=0.35)), f"entity/equipment/humanoid_leggings/{tier}.png")
        src = load("models/armor/iron_layer_1.png", JAR_121)
        save(make_vibrant(src, tint_gray(src, colour, sat_limit=0.35)), f"models/armor/{tier}_layer_1.png")
        src = load("models/armor/iron_layer_2.png", JAR_121)
        save(make_vibrant(src, tint_gray(src, colour, sat_limit=0.35)), f"models/armor/{tier}_layer_2.png")

    # Maces: the grey head takes the tier colour; the handle stays.
    for tier in MACE_TIERS:
        src = load("item/mace.png")
        tinted = make_vibrant(src, tint_gray(src, METAL[tier], sat_limit=0.3, mask=mace_head))
        save(add_shiny_edges(src, tinted, f"{tier}_mace", edge_strength(tier)), f"item/{tier}_mace.png")

    # 1.21.1 backports of 26.x items keep their vanilla art.
    for tool in TOOLS:
        save(load(f"item/copper_{tool}.png"), f"item/copper_{tool}.png")
    for piece in ARMOR:
        save(load(f"item/copper_{piece}.png"), f"item/copper_{piece}.png")
    save(load("entity/equipment/humanoid/copper.png"), "models/armor/copper_layer_1.png")
    save(load("entity/equipment/humanoid_leggings/copper.png"), "models/armor/copper_layer_2.png")
    for tier in BACKPORT_SPEARS:
        save(load(f"item/{tier}_spear.png"), f"item/{tier}_spear.png")
    save(load("item/copper_nugget.png"), "item/copper_nugget.png")

    # Alloys and templates.
    ingot = load("item/iron_ingot.png")
    for tier, colour in ALLOY.items():
        tinted = make_vibrant(ingot, tint_gray(ingot, colour, sat_limit=0.35))
        save(add_shiny_edges(ingot, tinted, f"{tier}_alloy_ingot", edge_strength(tier)), f"item/{tier}_alloy_ingot.png")
    template = load("item/netherite_upgrade_smithing_template.png")
    for tier, colour in ALLOY.items():
        if tier != "netherite":
            # Vibrance only: the rune stays a flat inlay rather than picking up a shiny edge.
            save(make_vibrant(template, hue_shift(template, colour, min_sat=0.2, brightness=1.2)),
                 f"item/{tier}_upgrade_smithing_template.png")

    # Bows and crossbows: the wood takes the tier colour; string, iron fittings, loaded arrows and rockets stay.
    for kind, base, states in (("bow", "bow", BOW_STATES), ("crossbow", "crossbow_standby", CROSSBOW_STATES)):
        palette = wood_palette(load(f"item/{base}.png"))
        mid = sum(lum(*c) for c in palette) / len(palette)
        for tier, colour in LAUNCHER.items():
            strength = edge_strength(tier)
            src = load(f"item/{base}.png")
            tinted = make_vibrant(src, tint_colours(src, colour, palette, mid))
            save(add_shiny_edges(src, tinted, f"{tier}_{kind}", strength), f"item/{tier}_{kind}.png")
            for state in states:
                if state == "":
                    continue
                mask = not_nocked_arrow if kind == "bow" else None
                src = load(f"item/{kind}{state}.png")
                tinted = make_vibrant(src, tint_colours(src, colour, palette, mid, mask))
                save(add_shiny_edges(src, tinted, f"{tier}_{kind}{state}", strength), f"item/{tier}_{kind}{state}.png")

    # Shields: the wooden boards take the tier colour, like the bow wood; the iron rim and boss stay. The
    # two textures go into vanilla's shield atlas; the item texture is only the particle (the renderer draws the model).
    for state in ("shield_base", "shield_base_nopattern"):
        img = load(f"entity/shield/{state}.png")
        palette = wood_palette(img)
        mid = sum(lum(*c) for c in palette) / len(palette)
        for tier, colour in LAUNCHER.items():
            tinted = make_vibrant(img, tint_colours(img, colour, palette, mid))
            save(tinted, f"entity/shield/{tier}{state[len('shield'):]}.png")
            if state == "shield_base_nopattern":
                save(shield_icon(tinted), f"item/{tier}_shield.png")

    # Horse armor: the grey metal takes the tier colour, like the player armor. Worn textures are 26.x equipment layers;
    # 1.21.1's TieredHorseArmorItem points at the same files.
    for tier in HORSE_ARMOR:
        colour = GEAR[tier]
        src = load("item/iron_horse_armor.png")
        tinted = make_vibrant(src, tint_gray(src, colour, sat_limit=0.35))
        save(add_shiny_edges(src, tinted, f"{tier}_horse_armor", edge_strength(tier)), f"item/{tier}_horse_armor.png")
        src = load("entity/equipment/horse_body/iron.png")
        save(make_vibrant(src, tint_gray(src, colour, sat_limit=0.35)), f"entity/equipment/horse_body/{tier}.png")
    for tier in BACKPORT_HORSE_ARMOR:
        save(load(f"item/{tier}_horse_armor.png"), f"item/{tier}_horse_armor.png")
        save(load(f"entity/equipment/horse_body/{tier}.png"), f"entity/equipment/horse_body/{tier}.png")

    # Wolf armor: the armadillo scutes take the tier colour. No dye overlay: tiered wolf armor isn't dyeable.
    for kind, rel in (("item", "item/wolf_armor.png"), ("worn", "entity/equipment/wolf_body/armadillo_scute.png")):
        img = load(rel)
        palette = wood_palette(img)
        mid = sum(lum(*c) for c in palette) / len(palette)
        for tier in WOLF_ARMOR:
            tinted = make_vibrant(img, tint_colours(img, METAL[tier], palette, mid))
            if kind == "item":
                save(add_shiny_edges(img, tinted, f"{tier}_wolf_armor", edge_strength(tier)), f"item/{tier}_wolf_armor.png")
            else:
                save(tinted, f"entity/equipment/wolf_body/{tier}.png")

    # Fishing rods: the same wood tint as bows/crossbows; the reel and line stay untinted.
    for state in ("fishing_rod", "fishing_rod_cast"):
        img = load(f"item/{state}.png")
        palette = wood_palette(img)
        mid = sum(lum(*c) for c in palette) / len(palette)
        for tier, colour in LAUNCHER.items():
            tinted = make_vibrant(img, tint_colours(img, colour, palette, mid))
            save(add_shiny_edges(img, tinted, f"{tier}_{state}", edge_strength(tier)), f"item/{tier}_{state}.png")


def darker_palette(palette):
    """A trim palette for a trim on armor of its own colour, like vanilla's iron_darker: the palette moved about three
    shades down (read from 1.5 steps in), with the shades past its darkest end extrapolated by darkening that end."""
    colours = [palette.getpixel((x, 0)) for x in range(palette.width)]
    out = Image.new("RGBA", palette.size)
    for i in range(palette.width):
        t = 1.5 + i
        last = len(colours) - 1
        if t <= last:
            a, b = colours[int(t)], colours[min(int(t) + 1, last)]
            f = t - int(t)
            c = tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3))
        else:
            c = tuple(round(v * 0.72 ** (t - last)) for v in colours[last][:3])
        out.putpixel((i, 0), c + (255,))
    return out


def make_trim_palettes():
    """Palettes for a trim on mod armor of the same colour (see armor_trims() in gen_data.py). Emerald's is made here;
    Copper's is vanilla 26.x's own, for the 1.21.1 Copper armor. 1.21.1 and 26.2 read palettes from
    trims/color_palettes/, 26.3 from palettes/trim/."""
    emerald = darker_palette(load("trims/color_palettes/emerald.png", JAR_121))
    save(emerald, "trims/color_palettes/emerald_darker.png")
    save(emerald, "palettes/trim/emerald_darker.png")
    save(load("palettes/trim/copper_darker.png"), "trims/color_palettes/copper_darker.png")


def shield_icon(texture):
    """A 16×16 item texture from a shield texture's front face (12×22 at 1,1 in the ShieldModel layout), squeezed to
    fit. Only used as the shield's break particle: the item itself is drawn by the shield renderer."""
    face = texture.crop((1, 1, 13, 23)).resize((9, 16), Image.NEAREST)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.paste(face, (4, 0))
    return icon


def make_icon():
    """Mod icon: the Reinforced pickaxe, upscaled."""
    icon = tint_gray(load("item/iron_pickaxe.png"), GEAR["reinforced"]).resize((128, 128), Image.NEAREST)
    icon.save(OUT.parent.parent.parent / "icon.png")


if __name__ == "__main__":
    make()
    make_trim_palettes()
    make_icon()
    print(f"{written} textures written, {unchanged} unchanged, in {OUT}")
