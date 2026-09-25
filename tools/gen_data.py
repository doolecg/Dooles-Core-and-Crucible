"""Generates Doole's Core & Crucible's static data: lang, item models, tags, equipment assets, recipes, ore overrides.

Run from the repo root:
    python tools/gen_data.py           write the data (only files whose contents changed; stale files are deleted)
    python tools/gen_data.py --check   write nothing; exit 1 if the committed data is out of date
Output is committed. Mirrors the item list in ModItems / ModTiers / EquipType; GeneratedDataTest fails the build if
an item registered in Java is missing its lang entry, model or texture.

To change a recipe, name, tag or advancement, edit the tables in this file and run it again; don't edit the
generated JSON by hand, the next run overwrites it. Gear stats are not here: they live in the Java stat enums
(registry/) and the player's config/dooles_core_crucible.json.

Data that loads on every version goes to src/main/resources. Vanilla-format recipes differ by
version (1.21.1 writes ingredients as {"item": ...}, 26.x as plain ids), so they go to
src/main/versioned/<1.21.1|26>, which the Gradle builds add per target.
"""
import json
import sys
from pathlib import Path

SRC = Path(__file__).resolve().parent.parent / "src" / "main"
ROOT = SRC / "resources"
VERSIONED = SRC / "versioned"
ASSETS = ROOT / "assets" / "dooles_core_crucible"
DATA = ROOT / "data"
MOD = "dooles_core_crucible"

TIERS = ["wood", "bone", "flint", "copper", "iron", "emerald", "diamond", "obsidian", "netherite", "reinforced"]
PREFIX = {t: ("wooden" if t == "wood" else t) for t in TIERS}
LAUNCHERS = ["bow", "crossbow"]
RODS = ["fishing_rod"]
SHEARS = ["shears"]
TOOLS = ["pickaxe", "axe", "shovel", "hoe", "sword", "spear", "mace"] + LAUNCHERS + RODS + SHEARS
ARMOR = ["helmet", "chestplate", "leggings", "boots"]
SHIELDS = ["shield"]
BODY_ARMOR = ["horse_armor", "wolf_armor"]
# Gear that isn't a tool or player armor: shields, horse and wolf armor.
EXTRA = SHIELDS + BODY_ARMOR
ALLOY_TIERS = ["iron", "emerald", "diamond", "obsidian", "netherite", "reinforced"]
MOD_ARMOR = ["copper", "emerald", "obsidian", "reinforced"]  # copper only registered on 1.21.1
# If you change a draw time in LauncherStats.java, change it here too (it's used for the bow's pull animation).
# Bow draw ticks per tier, mirrors LauncherStats. Wood is the vanilla bow and crossbow.
DRAW_TICKS = {"bone": 19, "flint": 19, "copper": 18, "iron": 17, "emerald": 16, "diamond": 15, "obsidian": 15,
              "netherite": 14, "reinforced": 13}


def exists(tier, kind):
    """Whether the progression chain has a slot at all for this tier/kind combo (regardless of which item fills it)."""
    if kind in SHEARS:
        return tier in ("bone", "flint", "copper", "iron")
    if kind in ARMOR or kind in BODY_ARMOR:
        return TIERS.index(tier) >= TIERS.index("copper")
    if tier in ("bone", "flint"):
        return kind != "mace"
    return True


def availability(tier, kind):
    """'all', '1.21.1' (mod item only there), or None (vanilla item / not in chain)."""
    if not exists(tier, kind):
        return None
    if kind == "mace":
        return "all"
    if kind in LAUNCHERS or kind in RODS:
        return None if tier == "wood" else "all"
    if kind in SHEARS:
        return None if tier == "iron" else "all"  # Iron is the vanilla pair; there's no vanilla copper pair
    if kind in SHIELDS:
        return None if tier == "wood" else "all"  # Wood is the vanilla shield
    if kind == "wolf_armor":
        return None if tier == "copper" else "all"  # Copper is vanilla's armadillo wolf armor
    if kind == "horse_armor":
        if tier in ("iron", "diamond"):
            return None
        return "1.21.1" if tier in ("copper", "netherite") else "all"  # 26.x added copper and netherite horse armor
    if kind in ARMOR:
        if tier == "copper":
            return "1.21.1"
        return "all" if tier in MOD_ARMOR else None
    if tier in ("wood", "iron", "diamond", "netherite"):
        return "1.21.1" if kind == "spear" else None
    if tier == "copper":
        return "1.21.1"
    return "all"


def equipment():
    """(id, tier, kind, availability) for every mod equipment item."""
    out = []
    for tier in TIERS:
        for kind in TOOLS + SHIELDS + ARMOR + BODY_ARMOR:
            a = availability(tier, kind)
            if a:
                out.append((f"{PREFIX[tier]}_{kind}", tier, kind, a))
    return out


EQUIPMENT = equipment()
BACKPORTS = [("golden_spear", "1.21.1"), ("copper_nugget", "1.21.1")]

# Textures come from tools/gen_textures.py: one per item, at dooles_core_crucible:item/<id>.
def model_parent(item_id, kind):
    """Which vanilla item model an item's own model should extend."""
    if kind == "mace":
        return "handheld_mace"
    if kind in SHEARS:
        return "generated"  # vanilla shears use a flat icon, not a handheld model
    if kind in TOOLS or item_id == "golden_spear":
        return "handheld"
    return "generated"


def title(s):
    """Display name from an id; the Reinforced tier reads "Reinforced Netherite" (ids stay reinforced_*)."""
    out = " ".join(w.capitalize() for w in s.split("_"))
    return "Reinforced Netherite" + out[len("Reinforced"):] if out.startswith("Reinforced") else out


# Every generated file, path -> JSON text. Filled by write() and flushed to disk once by sync() at the end.
OUTPUT = {}


def write(path, obj):
    """Queues a JSON file for sync(). Nothing touches the disk until every generator has run."""
    OUTPUT[path] = json.dumps(obj, indent=2) + "\n"


def managed_files():
    """The JSON files this script owns on disk: everything under the data and versioned folders, and every asset
    except textures (those come from gen_textures.py)."""
    files = set()
    for folder in (DATA, VERSIONED, ROOT / "assets" / "minecraft"):
        files.update(folder.rglob("*.json"))
    for path in ASSETS.rglob("*.json"):
        if "textures" not in path.relative_to(ASSETS).parts:
            files.add(path)
    return files


def sync(check=False):
    """Writes queued files whose contents changed and deletes managed files that are no longer generated, so
    unchanged files keep their timestamps and removed items leave nothing behind. With check=True, only reports."""
    on_disk = managed_files()
    changed = []
    for path, text in OUTPUT.items():
        if path not in on_disk or path.read_text(encoding="utf-8") != text:
            changed.append(path)
    stale = sorted(on_disk - OUTPUT.keys())
    if check:
        for path in changed:
            print(f"out of date: {path.relative_to(SRC).as_posix()}")
        for path in stale:
            print(f"stale:       {path.relative_to(SRC).as_posix()}")
        return not (changed or stale)
    for path in changed:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(OUTPUT[path], encoding="utf-8", newline="\n")
    for path in stale:
        path.unlink()
    # Drop folders the deletions left empty, deepest first.
    for folder in sorted({p.parent for p in stale}, key=lambda d: len(d.parts), reverse=True):
        while folder != SRC and folder.exists() and not any(folder.iterdir()):
            folder.rmdir()
            folder = folder.parent
    print(f"{len(changed)} written, {len(stale)} deleted, {len(OUTPUT) - len(changed)} unchanged")
    return True


def all_items():
    """(id, display name, tier, kind) in the same order as ModItems.register."""
    items = []
    for t in ALLOY_TIERS:
        items.append((f"{t}_alloy_ingot", f"{title(t)} Alloy Ingot", None, None))
    for t in ALLOY_TIERS:
        if t != "netherite":  # the netherite template is vanilla's own, not one of ours
            items.append((f"{t}_upgrade_smithing_template", f"{title(t)} Upgrade Template", None, None))
    for item_id, tier, kind, _avail in EQUIPMENT:
        items.append((item_id, title(item_id), tier, kind))
    for item_id, _avail in BACKPORTS:
        items.append((item_id, title(item_id), None, None))
    return items


def entry(item_id, avail):
    """A tag entry for this item: a plain id when it's on every version, an optional id otherwise (1.21.1-only)."""
    full = f"{MOD}:{item_id}"
    return full if avail == "all" else {"id": full, "required": False}


def tag(values):
    """The JSON shape of a tag file."""
    return {"replace": False, "values": values}


def gen_assets():
    """Writes item models and equipment (armor texture) assets for every mod item. Returns the lang entries; they
    are written at the end, once tag_lang() can see every generated tag."""
    lang = {f"itemGroup.{MOD}": "Doole's Core & Crucible"}
    for item_id, name, tier, kind in all_items():
        lang[f"item.{MOD}.{item_id}"] = name
        if kind in LAUNCHERS:
            launcher_models(item_id, tier, kind)
            continue
        if kind in RODS:
            rod_models(item_id)
            continue
        if kind in SHIELDS:
            shield_models(item_id, tier)
            for colour in DYES:
                lang[f"item.{MOD}.{item_id}.{colour}"] = f"{title(colour)} {name}"
            continue
        write(ASSETS / "models" / "item" / f"{item_id}.json",
              {"parent": f"minecraft:item/{model_parent(item_id, kind)}", "textures": {"layer0": f"{MOD}:item/{item_id}"}})
        model = {"type": "minecraft:model", "model": f"{MOD}:item/{item_id}"}
        if kind == "spear" and tier not in (None,) and availability(tier, kind) == "all":
            # 26.x spears (the only ones these item definitions reach) use a 3D in-hand model, like vanilla's.
            write(ASSETS / "models" / "item" / f"{item_id}_in_hand.json",
                  {"parent": "minecraft:item/spear_in_hand", "textures": {"layer0": f"{MOD}:item/{item_id}_in_hand"}})
            write(ASSETS / "items" / f"{item_id}.json", {"model": {
                "type": "minecraft:select", "property": "minecraft:display_context",
                "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": model}],
                "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/{item_id}_in_hand"}},
                "swap_animation_scale": 1.95})
        else:
            write(ASSETS / "items" / f"{item_id}.json", {"model": model})
    lang.update(template_lang())
    lang.update(enchantment_lang())
    lang.update(tooltip_lang())
    lang.update(integration_lang())
    lang.update(advancement_lang())
    # The vanilla Netherite template now upgrades Obsidian gear with Netherite alloy.
    netherite = "item.minecraft.smithing_template.netherite_upgrade."
    write(ROOT / "assets" / "minecraft" / "lang" / "en_us.json", {
        netherite + "applies_to": "Obsidian Equipment, Obsidian Alloy Ingot",
        netherite + "ingredients": "Netherite Alloy Ingot, Netherite Ingot",
        netherite + "base_slot_description": "Add obsidian armor, weapon, tool or ingot",
        netherite + "additions_slot_description": "Add netherite alloy ingot or netherite ingot",
    })

    # 26.x equipment assets (1.21.1 uses ArmorMaterial.Layer and TieredHorse/WolfArmorItem in code): the worn
    # textures of the mod's armor, horse armor and wolf armor. Vanilla tiers only get the layers vanilla lacks.
    for tier in TIERS:
        texture = f"{MOD}:{tier}"
        layers = {}
        if availability(tier, "helmet") == "all":
            layers["humanoid"] = [{"texture": texture}]
            layers["humanoid_leggings"] = [{"texture": texture}]
        if availability(tier, "horse_armor") == "all":
            layers["horse_body"] = [{"texture": texture}]
        if availability(tier, "wolf_armor") == "all":
            layers["wolf_body"] = [{"texture": texture}]
        if layers:
            write(ASSETS / "equipment" / f"{tier}.json", {"layers": layers})
    return lang


def launcher_models(item_id, tier, kind):
    """Bow and crossbow models, copied from vanilla's. The base model carries 1.21.1's overrides (pull,
    pulling, charged, firework; registered in LauncherClient); 26.x reads the items/ definition instead."""
    models = ASSETS / "models" / "item"

    def ref(suffix=""):
        return f"{MOD}:item/{item_id}{suffix}"

    def model(suffix=""):
        return {"type": "minecraft:model", "model": ref(suffix)}

    if kind == "bow":
        states = ["_pulling_0", "_pulling_1", "_pulling_2"]
        overrides = [{"predicate": {"pulling": 1}, "model": ref("_pulling_0")},
                     {"predicate": {"pulling": 1, "pull": 0.65}, "model": ref("_pulling_1")},
                     {"predicate": {"pulling": 1, "pull": 0.9}, "model": ref("_pulling_2")}]
        # use_duration counts ticks; the scale makes 1.0 a full draw at the tier's draw time.
        definition = {"type": "minecraft:condition", "property": "minecraft:using_item",
                      "on_false": model(),
                      "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration",
                                  "scale": round(1 / DRAW_TICKS[tier], 6),
                                  "entries": [{"threshold": 0.65, "model": model("_pulling_1")},
                                              {"threshold": 0.9, "model": model("_pulling_2")}],
                                  "fallback": model("_pulling_0")}}
    else:
        states = ["_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework"]
        overrides = [{"predicate": {"pulling": 1}, "model": ref("_pulling_0")},
                     {"predicate": {"pulling": 1, "pull": 0.58}, "model": ref("_pulling_1")},
                     {"predicate": {"pulling": 1, "pull": 1.0}, "model": ref("_pulling_2")},
                     {"predicate": {"charged": 1}, "model": ref("_arrow")},
                     {"predicate": {"charged": 1, "firework": 1}, "model": ref("_firework")}]
        definition = {"type": "minecraft:select", "property": "minecraft:charge_type",
                      "cases": [{"when": "arrow", "model": model("_arrow")}, {"when": "rocket", "model": model("_firework")}],
                      "fallback": {"type": "minecraft:condition", "property": "minecraft:using_item",
                                   "on_false": model(),
                                   "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:crossbow/pull",
                                               "entries": [{"threshold": 0.58, "model": model("_pulling_1")},
                                                           {"threshold": 1.0, "model": model("_pulling_2")}],
                                               "fallback": model("_pulling_0")}}}
    write(models / f"{item_id}.json", {"parent": f"minecraft:item/{kind}", "textures": {"layer0": ref()},
                                       "overrides": overrides})
    for suffix in states:
        write(models / f"{item_id}{suffix}.json", {"parent": f"minecraft:item/{kind}", "textures": {"layer0": ref(suffix)}})
    write(ASSETS / "items" / f"{item_id}.json", {"model": definition})


def rod_models(item_id):
    """Fishing rod models: vanilla's fishing_rod/cast condition, copied for each tier. 1.21.1 uses an
    override on "cast"; 26.x reads the items/ definition instead. Both states parent to handheld_rod directly."""
    models = ASSETS / "models" / "item"

    def ref(suffix=""):
        return f"{MOD}:item/{item_id}{suffix}"

    def model(suffix=""):
        return {"type": "minecraft:model", "model": ref(suffix)}

    write(models / f"{item_id}.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": ref()},
                                       "overrides": [{"predicate": {"cast": 1}, "model": ref("_cast")}]})
    write(models / f"{item_id}_cast.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": ref("_cast")}})
    write(ASSETS / "items" / f"{item_id}.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
        "on_false": model(), "on_true": model("_cast")}})


# Banner base colours, for the shields' coloured names ("Red Iron Shield"), like vanilla's item.minecraft.shield.red.
DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black"]


def shield_models(item_id, tier):
    """Shield models. 1.21.1: models parented to vanilla's shield (built-in renderer) with their own
    "blocking" override; the item icon is only the particle texture. 26.x: vanilla's item definition, with the mod's
    shield renderer and the tier to draw."""
    models = ASSETS / "models" / "item"
    ref = f"{MOD}:item/{item_id}"
    write(models / f"{item_id}.json", {"parent": "minecraft:item/shield", "textures": {"particle": ref},
                                       "overrides": [{"predicate": {"blocking": 1}, "model": ref + "_blocking"}]})
    write(models / f"{item_id}_blocking.json", {"parent": "minecraft:item/shield_blocking", "textures": {"particle": ref}})
    renderer = {"type": f"{MOD}:shield", "tier": PREFIX[tier]}
    write(ASSETS / "items" / f"{item_id}.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:using_item",
        "on_false": {"type": "minecraft:special", "base": ref, "model": renderer},
        "on_true": {"type": "minecraft:special", "base": ref + "_blocking", "model": renderer},
        "transformation": {"left_rotation": [0.0, 0.0, 0.0, 1.0], "right_rotation": [0.0, 0.0, 0.0, 1.0],
                           "scale": [1.0, -1.0, -1.0], "translation": [0.0, 0.0, 0.0]}}})


def gen_tags():
    """Writes all item and block tags: repair materials, tool/armor grouping tags, enchantable tags, harvest levels."""
    item_tags = DATA / "minecraft" / "tags" / "item"
    mod_item_tags = DATA / MOD / "tags" / "item"
    block_tags = DATA / "minecraft" / "tags" / "block"
    mod_block_tags = DATA / MOD / "tags" / "block"

    # Repair materials, shared by tools and armor of a tier.
    repair = {"wood": ["#minecraft:planks"], "bone": ["minecraft:bone"], "flint": ["minecraft:flint"],
              "copper": ["minecraft:copper_ingot"]}
    for t in ALLOY_TIERS:
        repair[t] = [f"{MOD}:{t}_alloy_ingot"]
    for t, values in repair.items():
        write(mod_item_tags / "repair" / f"{t}.json", tag(values))

    # Every equipment item's tag entry, grouped by kind (pickaxe, sword, helmet, ...).
    by_kind = {}
    for kind in TOOLS + ARMOR + EXTRA:
        entries = []
        for item_id, _tier, item_kind, avail in EQUIPMENT:
            if item_kind == kind:
                entries.append(entry(item_id, avail))
        by_kind[kind] = entries
    spears = by_kind["spear"] + [entry("golden_spear", "1.21.1")]
    write(mod_item_tags / "spears.json", tag(spears))
    write(mod_item_tags / "maces.json", tag(by_kind["mace"]))
    write(mod_item_tags / "bows.json", tag(["minecraft:bow"] + by_kind["bow"]))
    write(mod_item_tags / "crossbows.json", tag(["minecraft:crossbow"] + by_kind["crossbow"]))
    # Convention tags, so other mods see the tiered launchers as bows and crossbows.
    common_tags = DATA / "c" / "tags" / "item" / "tools"
    write(common_tags / "bow.json", tag(by_kind["bow"]))
    write(common_tags / "crossbow.json", tag(by_kind["crossbow"]))
    write(common_tags / "ranged_weapon.json", tag(by_kind["bow"] + by_kind["crossbow"]))
    write(common_tags / "fishing_rod.json", tag(by_kind["fishing_rod"]))
    write(common_tags / "shear.json", tag(by_kind["shears"]))
    write(common_tags / "shield.json", tag(by_kind["shield"]))
    write(mod_item_tags / "shields.json", tag(["minecraft:shield"] + by_kind["shield"]))
    write(mod_item_tags / "wolf_armor.json", tag(["minecraft:wolf_armor"] + by_kind["wolf_armor"]))

    write(item_tags / "pickaxes.json", tag(by_kind["pickaxe"]))
    write(item_tags / "axes.json", tag(by_kind["axe"]))
    write(item_tags / "shovels.json", tag(by_kind["shovel"]))
    write(item_tags / "hoes.json", tag(by_kind["hoe"]))
    write(item_tags / "swords.json", tag(by_kind["sword"]))
    write(item_tags / "spears.json", tag([f"#{MOD}:spears"]))  # 26.x: lunge, durability, melee enchants
    for kind, vanilla in (("helmet", "head_armor"), ("chestplate", "chest_armor"),
                          ("leggings", "leg_armor"), ("boots", "foot_armor")):
        write(item_tags / f"{vanilla}.json", tag(by_kind[kind]))
    trimmable = []
    for kind in ARMOR:
        trimmable += by_kind[kind]
    write(item_tags / "trimmable_armor.json", tag(trimmable))

    ench = item_tags / "enchantable"
    write(ench / "mace.json", tag([f"#{MOD}:maces"]))
    write(ench / "weapon.json", tag([f"#{MOD}:maces"]))
    write(ench / "fire_aspect.json", tag([f"#{MOD}:maces", f"#{MOD}:spears"]))
    write(ench / "durability.json", tag([f"#{MOD}:maces", f"#{MOD}:spears"] + by_kind["bow"] + by_kind["crossbow"]
                                        + by_kind["fishing_rod"] + by_kind["shears"] + by_kind["shield"]))
    write(ench / "bow.json", tag(by_kind["bow"]))
    write(ench / "crossbow.json", tag(by_kind["crossbow"]))
    # Lure and Luck of the Sea both target this tag; vanilla only lists its own rod, so the mod rods join it here.
    write(ench / "fishing.json", tag(by_kind["fishing_rod"]))
    # Vanilla lists minecraft:shears directly in enchantable/mining, not through a group tag, like fishing_rod above.
    write(ench / "mining.json", tag(by_kind["shears"]))
    write(ench / "sharp_weapon.json", tag([f"#{MOD}:spears"]))
    # 1.21.1 has no spear tags: spears count as swords for sweeping, looting and knockback there.
    # The tag is unused on 26.x.
    write(ench / "sword.json", tag([f"#{MOD}:spears"]))

    # Harvest levels. Vanilla netherite keeps minecraft:incorrect_for_netherite_tool
    # (level 5); Reinforced (level 6) gets its own empty tag.
    needs = [f"#{MOD}:needs_obsidian_tool", f"#{MOD}:needs_netherite_tool", f"#{MOD}:needs_reinforced_tool"]
    for n in needs:
        write(mod_block_tags / f"{n.split(':')[1]}.json", tag([]))
    for vanilla in ("wooden", "stone", "copper", "iron", "diamond", "gold"):
        write(block_tags / f"incorrect_for_{vanilla}_tool.json", tag(needs))
    write(mod_block_tags / "incorrect_for_obsidian_tool.json", tag(needs[1:]))
    write(block_tags / "incorrect_for_netherite_tool.json", tag(needs[2:]))
    write(mod_block_tags / "incorrect_for_reinforced_tool.json", tag([]))


    write(mod_item_tags / "alloy_ingots.json", tag([f"{MOD}:{t}_alloy_ingot" for t in ALLOY_TIERS]))


# ---------------------------------------------------------------- recipes

# Recipe tables. To change how something is crafted, edit these and run the script. In the patterns, X is the
# tier's material (CRAFT_MATERIAL) and # is a stick; other letters are explained next to each table.
# Crafted gear: Bone, Flint and Copper pieces are crafted new, with vanilla tool shapes.
CRAFT_MATERIAL = {"bone": "minecraft:bone", "flint": "minecraft:flint", "copper": "minecraft:copper_ingot"}
TOOL_PATTERNS = {"pickaxe": ["XXX", " # ", " # "], "axe": ["XX", "X#", " #"], "shovel": ["X", "#", "#"],
                 "hoe": ["XX", " #", " #"],
                 "sword": ["X", "X", "#"], "spear": ["  X", " # ", "#  "]}
# Bows and crossbows: vanilla's shapes with the material added (bow: the empty centre; crossbow: in place
# of the iron ingot). S = string, T = tripwire hook.
LAUNCHER_PATTERNS = {"bow": [" #S", "#XS", " #S"], "crossbow": ["#X#", "STS", " # "]}
# Fishing rod: vanilla's shape with the tier material at the bottom middle. S = string.
ROD_PATTERN = ["  #", " #S", "#XS"]
# Shears: vanilla's own diagonal shape, just with the tier material instead of iron ingots.
SHEARS_PATTERN = [" X", "X "]
# Shield: vanilla's shape with the tier material in place of the iron ingot. W = planks.
SHIELD_PATTERN = ["WXW", "WWW", " W "]
# Alloys: tier -> (base ingot, raw addition). Templates are crafted from the raw addition.
# If you change an alloy here, change the matching row in compat/ForgingDisplays.java so JEI shows the same recipe.
ALLOYS = {
    "iron": ("minecraft:copper_ingot", "minecraft:iron_ingot"),
    "emerald": (f"{MOD}:iron_alloy_ingot", "minecraft:emerald"),
    "diamond": (f"{MOD}:emerald_alloy_ingot", "minecraft:diamond"),
    "obsidian": (f"{MOD}:diamond_alloy_ingot", "minecraft:crying_obsidian"),
    "netherite": (f"{MOD}:obsidian_alloy_ingot", "minecraft:netherite_ingot"),
    "reinforced": (f"{MOD}:netherite_alloy_ingot", f"{MOD}:obsidian_alloy_ingot"),
}
# Template materials where they differ from the alloy's addition: tier -> (craft centre, clone material).
TEMPLATE_MATERIAL = {"reinforced": ("minecraft:nether_star", f"{MOD}:obsidian_alloy_ingot")}


def removed_recipes():
    """Vanilla recipes replaced by the chain -> their recipe-advancement folder.
    Names missing on a version (spears on 1.21.1) are harmless: the file is skipped.
    Copper gear stays craftable: Copper is the last crafted tier."""
    out = {}
    for material in ("iron", "diamond"):
        for kind in ("pickaxe", "axe", "shovel", "hoe"):
            out[f"{material}_{kind}"] = "tools"
        for kind in ("sword", "spear"):
            out[f"{material}_{kind}"] = "combat"
        for kind in ARMOR:
            out[f"{material}_{kind}"] = "combat"
    for kind in ("pickaxe", "axe", "shovel", "hoe"):
        out[f"stone_{kind}"] = "tools"
        out[f"netherite_{kind}_smithing"] = "tools"
    for kind in ("sword", "spear"):
        out[f"stone_{kind}"] = "combat"
    for kind in ["sword", "spear"] + ARMOR:
        out[f"netherite_{kind}_smithing"] = "combat"
    out["netherite_horse_armor_smithing"] = "combat"  # 26.x: Diamond -> Netherite horse armor skips Obsidian
    return out


# Skips the file on both loaders. NeoForge 26.x has no neoforge:false (it's neoforge:never),
# so use conditions that exist on every version.
DISABLED = {
    "neoforge:conditions": [{"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "minecraft"}}],
    "fabric:load_conditions": [{"condition": "fabric:not", "value": {"condition": "fabric:true"}}],
}


def ingredient(fmt, value):
    """One ingredient in the given format. value: item id, '#tag', or a list of those."""
    if isinstance(value, list):
        return [ingredient(fmt, v) for v in value]
    if fmt == "26":
        return value
    return {"tag": value[1:]} if value.startswith("#") else {"item": value}


def shaped(fmt, pattern, key, result, count=1, category="misc"):
    """The JSON for a shaped crafting recipe (a fixed grid pattern of ingredients)."""
    return {"type": "minecraft:crafting_shaped", "category": category,
            "key": {k: ingredient(fmt, v) for k, v in key.items()},
            "pattern": pattern, "result": {"count": count, "id": result}}


def shapeless(fmt, ingredients, result, count=1, category="misc"):
    """The JSON for a shapeless crafting recipe (any arrangement of ingredients)."""
    return {"type": "minecraft:crafting_shapeless", "category": category,
            "ingredients": [ingredient(fmt, i) for i in ingredients], "result": {"count": count, "id": result}}


def gen_recipes():
    """Writes smithing recipes, alloy recipes, the disabled vanilla recipes and the per-version crafting recipes."""
    recipes = DATA / MOD / "recipe"

    # Smithing upgrades: Copper -> Iron and above.
    for tier in ALLOY_TIERS:
        prev = TIERS[TIERS.index(tier) - 1]
        for kind in TOOLS + ARMOR + EXTRA:
            if exists(tier, kind) and exists(prev, kind):
                write(recipes / "smithing" / f"{PREFIX[tier]}_{kind}.json",
                      {"type": f"{MOD}:progression_smithing", "slot": kind, "tier": tier})

    for tier, (base, addition) in ALLOYS.items():
        write(recipes / "alloy" / f"{tier}.json",
              {"type": f"{MOD}:alloy_smithing", "tier": tier, "base": base, "addition": addition})

    for name, folder in removed_recipes().items():
        write(DATA / "minecraft" / "recipe" / f"{name}.json", DISABLED)
        write(DATA / "minecraft" / "advancement" / "recipes" / folder / f"{name}.json", DISABLED)

    for fmt in ("1.21.1", "26"):
        gen_versioned_recipes(fmt, VERSIONED / fmt / "data" / MOD / "recipe")

    # Recipe-book unlocks, like vanilla's: each recipe unlocks when you first hold its key ingredient.
    gen_unlocks(recipes, DATA / MOD / "advancement" / "recipes")
    for fmt in ("1.21.1", "26"):
        gen_unlocks(VERSIONED / fmt / "data" / MOD / "recipe", VERSIONED / fmt / "data" / MOD / "advancement" / "recipes")


# Crafting ingredients that don't say what a recipe is about.
FILLER = {"minecraft:stick", "minecraft:paper", "minecraft:feather", "minecraft:netherrack", "minecraft:string",
          "minecraft:tripwire_hook"}


def unlock_item(recipe):
    """The ingredient whose pickup unlocks a recipe in the recipe book. Smithing recipes unlock tier by
    tier: gear upgrades with the tier's alloy ingot, alloys with the previous tier's ingot (their base), so the book
    never shows a tier the player hasn't reached."""
    kind = recipe["type"]
    if kind == f"{MOD}:progression_smithing":
        return f"{MOD}:{recipe['tier']}_alloy_ingot"
    if kind == f"{MOD}:alloy_smithing":
        return recipe["base"]
    values = recipe["key"].values() if "key" in recipe else recipe["ingredients"]
    ids = []
    for v in values:
        if isinstance(v, dict):  # 1.21.1 format
            v = "#" + v["tag"] if "tag" in v else v["item"]
        ids.append(v)
    return next((i for i in ids if i not in FILLER), ids[0])


def gen_unlocks(recipes, out):
    """Writes a recipe-unlock advancement for every recipe already queued under `recipes`, except special recipes
    (banner on a shield), which never show in the recipe book."""
    for path in sorted(p for p in OUTPUT if p.is_relative_to(recipes)):
        recipe = json.loads(OUTPUT[path])
        if recipe["type"].startswith("minecraft:crafting_special"):
            continue
        name = path.relative_to(recipes).with_suffix("").as_posix()
        rid = f"{MOD}:{name}"
        write(out / f"{name}.json", {
            "parent": "minecraft:recipes/root",
            # Vanilla also ORs in a recipe_unlocked criterion, but its field is "recipe" up to 26.2 and "recipes"
            # on 26.3; the ingredient alone works everywhere and the advancement still completes once.
            "criteria": {"has_ingredient": has([unlock_item(recipe)])},
            "rewards": {"recipes": [rid]},
        })


def template_id(tier):
    """The upgrade template's registry id for this tier (the Netherite one is vanilla's own)."""
    if tier == "netherite":
        return "minecraft:netherite_upgrade_smithing_template"
    return f"{MOD}:{tier}_upgrade_smithing_template"


def gen_versioned_recipes(fmt, out):
    """Writes the crafting recipes whose ingredient format differs by version (templates, crafted gear, backports)."""
    # Template chain: the Iron template is dungeon loot (TemplateLoot); every later template is the one
    # below it, 7 Paper and the tier's material. T comes before X in the key, so the previous template unlocks it.
    chain = ["PTP", "PXP", "PPP"]
    tiers = list(ALLOYS)
    for i, (tier, (_, material)) in enumerate(ALLOYS.items()):
        template = template_id(tier)
        centre, clone = TEMPLATE_MATERIAL.get(tier, (material, material))
        if i > 0:
            write(out / "template" / f"{tier}.json",
                  shaped(fmt, chain, {"P": "minecraft:paper", "T": template_id(tiers[i - 1]), "X": centre}, template))
        if tier == "netherite":
            continue  # vanilla clone recipe
        write(out / "template" / f"{tier}_clone.json",
              shaped(fmt, ["#S#", "#C#", "###"], {"#": clone, "S": template, "C": "minecraft:netherrack"},
                     template, count=2))
    # The mace path starts at the mod's wooden mace; the copper mace is a copper block above a stick.
    write(out / "wooden_mace.json", shaped(fmt, ["L", "#"], {"L": "#minecraft:logs", "#": "minecraft:stick"},
                                           f"{MOD}:wooden_mace", category="equipment"))
    write(out / "tool" / "copper_mace.json", shaped(fmt, ["C", "#"], {"C": "minecraft:copper_block", "#": "minecraft:stick"},
                                                    f"{MOD}:copper_mace", category="equipment"))
    # Crafted gear: Bone and Flint tools and spears with sticks, like vanilla stone tools. Copper tools
    # and spears are vanilla recipes on 26.x, so they're only written for the 1.21.1 backports.
    for tier in ("bone", "flint", "copper"):
        for kind, pattern in TOOL_PATTERNS.items():
            result = item_id(tier, kind, fmt)
            if result is None or not result.startswith(MOD + ":"):
                continue
            write(out / "tool" / f"{PREFIX[tier]}_{kind}.json",
                  shaped(fmt, pattern, {"X": CRAFT_MATERIAL[tier], "#": "minecraft:stick"}, result, category="equipment"))
        for kind, pattern in LAUNCHER_PATTERNS.items():
            key = {"X": CRAFT_MATERIAL[tier], "#": "minecraft:stick", "S": "minecraft:string"}
            if kind == "crossbow":
                key["T"] = "minecraft:tripwire_hook"
            write(out / "tool" / f"{tier}_{kind}.json",
                  shaped(fmt, pattern, key, f"{MOD}:{tier}_{kind}", category="equipment"))
        write(out / "tool" / f"{tier}_fishing_rod.json",
              shaped(fmt, ROD_PATTERN, {"X": CRAFT_MATERIAL[tier], "#": "minecraft:stick", "S": "minecraft:string"},
                     f"{MOD}:{tier}_fishing_rod", category="equipment"))
        # Shears have no vanilla copper pair to re-enable on 26.x (unlike the tools above), so this is written for
        # every version.
        write(out / "tool" / f"{tier}_shears.json",
              shaped(fmt, SHEARS_PATTERN, {"X": CRAFT_MATERIAL[tier]}, f"{MOD}:{tier}_shears", category="equipment"))
        write(out / "tool" / f"{tier}_shield.json",
              shaped(fmt, SHIELD_PATTERN, {"W": "#minecraft:planks", "X": CRAFT_MATERIAL[tier]}, f"{MOD}:{tier}_shield",
                     category="equipment"))
    # Copper horse armor starts the horse armor chain: leather horse armor's shape in copper ingots. It's
    # the vanilla item on 26.x (loot only there) and a mod item on 1.21.1.
    write(out / "tool" / "copper_horse_armor.json",
          shaped(fmt, ["X X", "XXX", "X X"], {"X": "minecraft:copper_ingot"}, item_id("copper", "horse_armor", fmt),
                 category="equipment"))
    if fmt == "26":
        # 26.x's banner-on-shield recipe is data and names its shield, so every tier gets its own copy. 1.21.1's is
        # hard-coded to Items.SHIELD; ShieldDecorationRecipeMixin widens it instead.
        for tier in TIERS:
            shield = item_id(tier, "shield", fmt)
            if shield.startswith(MOD + ":"):
                write(out / "shield_decoration" / f"{tier}.json", {
                    "type": "minecraft:crafting_special_shielddecoration", "banner": "#minecraft:banners",
                    "result": {"id": shield}, "target": shield})

    if fmt != "1.21.1":
        return
    # 1.21.1 backports: vanilla-equivalent recipes for items 26.x ships.
    spear = ["  X", " # ", "#  "]
    write(out / "wooden_spear.json", shaped(fmt, spear, {"X": "#minecraft:planks", "#": "minecraft:stick"},
                                            f"{MOD}:wooden_spear", category="equipment"))
    write(out / "golden_spear.json", shaped(fmt, spear, {"X": "minecraft:gold_ingot", "#": "minecraft:stick"},
                                            f"{MOD}:golden_spear", category="equipment"))
    armor = {"helmet": ["XXX", "X X"], "chestplate": ["X X", "XXX", "XXX"],
             "leggings": ["XXX", "X X", "X X"], "boots": ["X X", "X X"]}
    for kind, pattern in armor.items():
        write(out / f"copper_{kind}.json", shaped(fmt, pattern, {"X": "minecraft:copper_ingot"},
                                                  f"{MOD}:copper_{kind}", category="equipment"))
    write(out / "copper_nugget.json", shapeless(fmt, ["minecraft:copper_ingot"], f"{MOD}:copper_nugget", count=9))
    write(out / "copper_ingot_from_nuggets.json",
          shaped(fmt, ["###", "###", "###"], {"#": f"{MOD}:copper_nugget"}, "minecraft:copper_ingot"))


def template_lang():
    """Tooltip and slot text for the SmithingTemplateItems (see TemplateItems)."""
    out = {}

    def add(name, applies_to, ingredients, base_slot, additions_slot):
        key = f"item.{MOD}.smithing_template.{name}."
        out[key + "applies_to"] = applies_to
        out[key + "ingredients"] = ingredients
        out[key + "base_slot_description"] = base_slot
        out[key + "additions_slot_description"] = additions_slot

    for tier, (base, addition) in ALLOYS.items():
        if tier == "netherite":
            continue
        prev = title(TIERS[TIERS.index(tier) - 1])
        raw = title(addition.split(":")[1])
        extra = " + Nether Star catalyst" if tier == "reinforced" else ""
        add(f"{tier}_upgrade", f"{prev} Equipment, {title(base.split(':')[1])}",
            f"{title(tier)} Alloy Ingot, {raw}{extra}",
            f"Add {prev.lower()} armor, weapon, tool or ingot",
            f"Add {title(tier).lower()} alloy ingot or {raw.lower()}")
    return out


# ---------------------------------------------------------------- enchantments

# name -> (title, max level, anvil cost, weight, slots, supported items, extras, description)
# To rebalance an enchantment (max level, cost, how often it's offered), edit its row here and run the script; it
# rewrites data/dooles_core_crucible/enchantment/<name>.json. The description is the English text players see.
ENCHANTMENTS = {
    "vein_resonance": ("Vein Resonance", 3, 4, 2, ["mainhand"], None, {},
                       "Hold the Vein Resonance key (Left Alt) while mining to break a whole shape; scroll to pick one. "
                       "More shapes at higher levels."),
    "luminous_ward": ("Luminous Ward", 1, 2, 3, ["any"], None, {}, "Leaves light behind in dark places."),
    "cavernous_echo": ("Cavernous Echo", 2, 4, 2, ["armor"], "#minecraft:enchantable/armor", {},
                       "Taking damage in the dark reveals nearby ores."),
    "geode_cracker": ("Geode Cracker", 3, 4, 2, ["mainhand"], None, {"exclusive_set": "minecraft:silk_touch"},
                      "More gems from ores."),
    "tectonic_pulse": ("Tectonic Pulse", 1, 2, 5, ["mainhand"], None, {}, "Also breaks the sand and gravel above."),
    "thermal_tempering": ("Thermal Tempering", 2, 4, 2, ["any"], "#minecraft:enchantable/durability",
                          {"exclusive_set": "minecraft:mending"}, "Self-repairs in fire, lava or on magma."),
    "kinetic_resonance": ("Kinetic Resonance", 3, 4, 2, ["mainhand"], None,
                          {"primary_items": "#minecraft:enchantable/sharp_weapon"},
                          "Repeated hits on one target deal up to +50%."),
    "soul_syphon": ("Soul Syphon", 1, 8, 1, ["mainhand"], "#minecraft:enchantable/weapon", {"effects": {
        "minecraft:mob_experience": [{"effect": {"type": "minecraft:multiply", "factor": 2.0}}],
        "minecraft:item_damage": [{"effect": {"type": "minecraft:set", "value": 0.0}}]}},
                    "Double kill XP; hits cost XP, not durability."),
    "volatile_payload": ("Volatile Payload", 3, 8, 1, ["mainhand"], None, {}, "Fully drawn arrows explode on impact."),
    "enders_grasp": ("Ender's Grasp", 1, 4, 2, ["mainhand"], None, {}, "Drops go straight to your inventory."),
    "aegis_reflection": ("Aegis Reflection", 2, 4, 2, ["chest", "hand"], None, {},
                         "May reflect projectiles; always while blocking."),
    "molten_tread": ("Molten Tread", 2, 4, 2, ["feet"], "#minecraft:enchantable/foot_armor",
                     {"exclusive_set": "#minecraft:exclusive_set/boots"},
                     "Walk on lava as temporary magma, safe from its heat."),
    "soul_tether": ("Soul Tether", 1, 4, 2, ["mainhand"], "#minecraft:enchantable/weapon", {},
                    "Struck foes can't teleport for 10 seconds."),
    "gravitic_anchor": ("Gravitic Anchor", 1, 4, 2, ["legs", "feet"], None, {},
                        "Immune to knock-ups, explosion launches and Levitation."),
    "phalanx_ward": ("Phalanx Ward", 2, 4, 2, ["hand"], None, {}, "Blocking briefly shields nearby allies."),
}
# The longer explanation JEI shows on each enchantment's books (jei.<mod>.info.enchantment.<name>). Keep these in step
# with what enchant/EnchantmentHooks and enchant/VeinMining actually do.
ENCHANTMENT_INFO = {
    "vein_resonance": "Mining tools. Hold the Vein Resonance key (Left Alt by default) while mining to break a whole "
                      "shape at once; scroll while holding it to pick the shape. Level I: Seam (every touching block "
                      "of the same ore or log) and Bore (a 1-wide hole). Level II adds Facet (a 3x3 face). Level III "
                      "adds Gallery (a 3x3 tunnel), Drift (a walkable 1x2 tunnel), Rise (stairs up) and Descent "
                      "(stairs down). Reach is 4, 8 or 12 blocks by level; higher-tier tools break more blocks. "
                      "Stops before the tool would break.",
    "luminous_ward": "Armor and mining tools. In dark places, a tool leaves an invisible light where it breaks a "
                     "block, and armor places one at your feet (1 durability each). The lights stay behind.",
    "cavernous_echo": "Armor. Taking damage in the dark makes ores within 8 blocks per level glow through walls for "
                      "6 seconds, for you only. Up to 64 ores; once every 6 seconds.",
    "geode_cracker": "Pickaxes. Gem drops (diamond, emerald, lapis, quartz, amethyst shards, prismarine crystals) "
                     "get 0 to 1 extra item per level. Can't be combined with Silk Touch. Doesn't affect items "
                     "spilled from containers.",
    "tectonic_pulse": "Pickaxes and shovels. Breaking a block also breaks the sand, gravel and other falling blocks "
                      "stacked above it, up to 64 high.",
    "thermal_tempering": "Anything with durability. While you're on fire, in lava or standing on magma, the item "
                         "repairs 2 durability per level every second. Can't be combined with Mending.",
    "kinetic_resonance": "Swords, axes, maces and spears. Each hit in a row on the same target within 3 seconds adds "
                         "10% melee damage, up to 30%, 40% or 50% by level. A new target or a pause resets it.",
    "soul_syphon": "Weapons. Mobs you kill drop double experience. The weapon takes no durability damage; each hit "
                   "costs you 2 experience points instead, or half a heart when you have none.",
    "volatile_payload": "Bows and crossbows. Fully drawn arrows (and every crossbow bolt) explode where they land or "
                        "hit. The blast hurts mobs but never breaks blocks, and grows with level.",
    "enders_grasp": "Mining tools and weapons. Blocks you mine and mobs you kill drop straight into your inventory; "
                    "what doesn't fit lands at your feet.",
    "aegis_reflection": "Chestplates and shields. Each level gives a 15% chance to send an incoming projectile back. "
                        "While blocking with an Aegis Reflection shield, projectiles always bounce back.",
    "molten_tread": "Boots. Lava under you turns to magma for 5 to 8 seconds so you can walk across, in a wider "
                    "circle at higher levels. Magma doesn't hurt you. It melts back once nobody is standing on it.",
    "soul_tether": "Weapons. Anything you hit can't teleport for 10 seconds: no ender pearls, chorus fruit or "
                   "enderman teleports.",
    "gravitic_anchor": "Leggings and boots. You can't be knocked upward by hits, mace smashes, sonic booms or "
                       "explosions, and Levitation has no effect. Sideways knockback still applies.",
    "phalanx_ward": "Shields. Blocking a hit raises a ward for 3 seconds per level. Your teammates, your pets and "
                    "players attacked by mobs within 3 blocks plus 1 per level take no damage; each hit it stops "
                    "costs the shield 1 durability.",
}

# Mod enchantable tags (supported_items = None above). Version-neutral: 1.21.1 lacks enchantable/melee_weapon.
# Which items each enchantment can go on (item tags).
ENCHANTABLE = {
    "vein_resonance": ["#minecraft:enchantable/mining"],
    "luminous_ward": ["#minecraft:enchantable/armor", "#minecraft:enchantable/mining"],
    "geode_cracker": ["#minecraft:pickaxes"],
    "tectonic_pulse": ["#minecraft:pickaxes", "#minecraft:shovels"],
    "kinetic_resonance": ["#minecraft:enchantable/sharp_weapon", f"#{MOD}:maces", "minecraft:mace", f"#{MOD}:spears",
                          {"id": "#minecraft:enchantable/melee_weapon", "required": False}],
    "volatile_payload": ["#minecraft:enchantable/bow", "#minecraft:enchantable/crossbow"],
    "enders_grasp": ["#minecraft:enchantable/mining", "#minecraft:enchantable/weapon"],
    "aegis_reflection": ["#minecraft:chest_armor", f"#{MOD}:shields"],
    "gravitic_anchor": ["#minecraft:leg_armor", "#minecraft:foot_armor"],
    "phalanx_ward": [f"#{MOD}:shields"],
}
VEIN_SHAPES = {"seam": "Seam", "bore": "Bore", "facet": "Facet", "gallery": "Gallery", "drift": "Drift", "rise": "Rise", "descent": "Descent"}


def enchantment(name, fmt=None):
    """The JSON for one enchantment definition."""
    _, max_level, anvil, weight, slots, supported, extras, _ = ENCHANTMENTS[name]
    out = {"anvil_cost": anvil, "description": {"translate": f"enchantment.{MOD}.{name}"},
           "max_cost": {"base": 50, "per_level_above_first": 10}, "max_level": max_level,
           "min_cost": {"base": 15, "per_level_above_first": 10}, "slots": slots,
           "supported_items": supported or f"#{MOD}:enchantable/{name}", "weight": weight}
    out.update(extras)
    if name == "gravitic_anchor":
        # 1.21.2 dropped the "generic." prefix from attribute ids.
        attribute = ("minecraft:explosion_knockback_resistance" if fmt == "26"
                     else "minecraft:generic.explosion_knockback_resistance")
        out["effects"] = {"minecraft:attributes": [{"id": f"{MOD}:enchantment.gravitic_anchor", "attribute": attribute,
                                                    "amount": 1.0, "operation": "add_value"}]}
    return out


def gen_enchantments():
    """Writes every enchantment's definition, its enchantable-items tag, and the shared ore/loot tags they need."""
    folder = DATA / MOD / "enchantment"
    for name in ENCHANTMENTS:
        if name == "gravitic_anchor":
            for fmt in ("1.21.1", "26"):
                write(VERSIONED / fmt / "data" / MOD / "enchantment" / f"{name}.json", enchantment(name, fmt))
        else:
            write(folder / f"{name}.json", enchantment(name))
    for name, values in ENCHANTABLE.items():
        write(DATA / MOD / "tags" / "item" / "enchantable" / f"{name}.json", tag(values))
    ids = [f"{MOD}:{n}" for n in ENCHANTMENTS]
    for t in ("in_enchanting_table", "non_treasure", "on_random_loot", "tradeable"):
        write(DATA / "minecraft" / "tags" / "enchantment" / f"{t}.json", tag(ids))

    write(DATA / MOD / "tags" / "block" / "ores.json", tag([
        {"id": "#c:ores", "required": False}, "#minecraft:coal_ores", "#minecraft:copper_ores", "#minecraft:iron_ores",
        "#minecraft:gold_ores", "#minecraft:redstone_ores", "#minecraft:lapis_ores", "#minecraft:diamond_ores",
        "#minecraft:emerald_ores", "minecraft:nether_quartz_ore", "minecraft:nether_gold_ore", "minecraft:ancient_debris"]))
    write(DATA / MOD / "tags" / "block" / "vein_mineable.json", tag([
        f"#{MOD}:ores", "#minecraft:logs", "minecraft:glowstone", "minecraft:amethyst_cluster"]))
    write(DATA / MOD / "tags" / "item" / "geode_cracker_drops.json", tag([
        {"id": "#c:gems", "required": False}, "minecraft:diamond", "minecraft:emerald", "minecraft:lapis_lazuli",
        "minecraft:quartz", "minecraft:amethyst_shard", "minecraft:prismarine_crystals"]))


def enchantment_lang():
    """Lang entries for the enchantments, the vein-mining key/HUD text and the vein shape names."""
    out = {}
    for name, info in ENCHANTMENTS.items():
        out[f"enchantment.{MOD}.{name}"] = info[0]
        out[f"enchantment.{MOD}.{name}.desc"] = info[-1]
    out[f"key.{MOD}.vein_mine"] = "Vein Resonance"
    out[f"hud.{MOD}.vein.count"] = "%s blocks"
    out[f"message.{MOD}.tethered"] = "A Soul Tether holds you in place"
    for key, value in VEIN_SHAPES.items():
        out[f"vein_shape.{MOD}.{key}"] = value
    return out


# ---------------------------------------------------------------- tooltip

# The italic line at the bottom of each tier's tooltip.
FLAVOR = {
    "wood": "Rough-hewn, but it gets the job started.",
    "bone": "Scavenged from the fallen, sharpened with patience.",
    "flint": "Knapped to a keen, brittle edge.",
    "copper": "The first metal of many.",
    "iron": "Honest steel, forged from the first alloy.",
    "emerald": "Traded for, then forged into something better.",
    "diamond": "Hard enough to outlast the mountain.",
    "obsidian": "Cooled from the heart of a volcano.",
    "netherite": "Forged in fire that never dies.",
    "reinforced": "A masterwork forged through progressive alloying.",
}


def tooltip_lang():
    """Lang entries for the item tooltip: labels, tier flavor text and tier display names."""
    k = f"tooltip.{MOD}."
    out = {
        k + "hold_shift": "Hold [%s] for Detailed Stats",
        k + "shift": "Shift",
        k + "durability": "Durability",
        k + "mining_speed": "Mining Speed",
        k + "attack_damage": "Attack Damage",
        k + "harvest_level": "Harvest Level",
        k + "till_area": "Till Area",
        k + "harvest_bonus": "Harvest Bonus",
        k + "replants": ", replants",
        k + "tier": "Tier",
        k + "arrow_damage": "Arrow Damage",
        k + "armor_pierce": "Armor Pierce",
        k + "draw_time": "Draw Time",
        k + "charge_time": "Charge Time",
        k + "lure": "Lure",
        k + "luck": "Luck",
        k + "reel_strength": "Reel Strength",
        k + "cast_distance": "Cast Distance",
        k + "raise_time": "Raise Time",
        k + "axe_disable": "Axe Disable",
        k + "knockback_resist": "Knockback Resist",
        k + "side_cover": "Side Cover",
        k + "seconds": "%ss",
        k + "header.progression": "--- Progression ---",
        k + "next_tier": "Next Tier",
        k + "requires": "Requires",
        k + "or": " or ",
        k + "max_level": "Max Level Reached",
        k + "not_applicable": "N/A",
        k + "crafted_new": "a new Copper piece (crafting table)",
    }
    for key, text in FLAVOR.items():
        out[k + "flavor." + key] = f"\"{text}\""
    for t in TIERS:
        out[f"tier.{MOD}.{t}"] = title(t)
    return out


# ---------------------------------------------------------------- integrations

def integration_lang():
    """Lang entries for JEI/EMI integration text and the smithing recipe book's filter tooltip."""
    j = f"jei.{MOD}."
    enchantments = {j + "info.enchantment." + name: text for name, text in ENCHANTMENT_INFO.items()}
    enchantments[j + "info.enchantment.max_level"] = "Max level: %s"
    return enchantments | {
        j + "crucible_smithing": "Doole's Core & Crucible",
        j + "info.alloy_ingot": "Made in a smithing table from the tier's upgrade template, the previous tier's "
                                "ingot and the tier's raw material. Upgrades the previous tier's gear in a smithing "
                                "table.",
        j + "info.upgrade_template": "The Iron template is found in dungeon chests. Every later template is crafted "
                                     "from the one below it, 7 Paper and the tier's material (the Netherite template "
                                     "is also bastion loot). Clone it with 7 of that material and a Netherrack. Used "
                                     "up by every upgrade.",
        j + "info.catalyst": "Smithing catalyst: goes in the extra smithing-table slot. Ghast Tear for Obsidian "
                             "gear, Wither Skeleton Skull for Netherite gear, Nether Star for Reinforced Netherite gear "
                             "and the Reinforced Netherite alloy. Used up by the craft.",
        # Smithing recipe book: the filter button's tooltip, like vanilla's "Showing Smeltable".
        f"gui.{MOD}.recipebook.toggleRecipes.smithable": "Showing Smithable",
    }


def tag_lang():
    """Names for the mod's item tags (tag.item.<namespace>.<path>, slashes as dots), which recipe viewers show and
    Fabric warns about when missing. Built from the tag files queued so far, so a new tag can't be left unnamed."""
    tags = DATA / MOD / "tags" / "item"
    out = {}
    for path in OUTPUT:
        if tags not in path.parents:
            continue
        parts = path.relative_to(tags).with_suffix("").parts
        if parts[0] == "enchantable":
            name = f"{ENCHANTMENTS[parts[1]][0]} Enchantable"
        elif parts[0] == "repair":
            name = f"{title(parts[1])} Repair Materials"
        else:
            name = title(parts[-1])
        out[f"tag.item.{MOD}." + ".".join(parts)] = name
    return dict(sorted(out.items()))


# ---------------------------------------------------------------- advancements
# A "Doole's Core & Crucible" tab in the vanilla advancements screen, that walks the whole gear chain. Item ids differ
# per version (copper gear and spears are vanilla on 26.x) and 26.3 stops the server on a bad advancement, so the
# files are written per version into src/main/versioned/<fmt>.

ADV_TOOLS = ["pickaxe", "axe", "shovel", "hoe", "sword", "spear", "mace"]


def item_id(tier, kind, fmt):
    """Registry id of a chain slot on the given version, or None when the slot doesn't exist."""
    if not exists(tier, kind):
        return None
    a = availability(tier, kind)
    if a == "all" or (a == "1.21.1" and fmt == "1.21.1"):
        return f"{MOD}:{PREFIX[tier]}_{kind}"
    if kind in ("spear",) and fmt == "1.21.1":
        return None
    if kind in LAUNCHERS or kind in RODS:
        return f"minecraft:{kind}"  # Wood: the vanilla bow, crossbow and fishing rod
    if kind in SHEARS:
        return "minecraft:shears"  # Iron: the vanilla pair (no tier prefix, unlike iron_pickaxe etc.)
    if kind in SHIELDS:
        return "minecraft:shield"  # Wood: the vanilla shield
    if kind == "wolf_armor":
        return "minecraft:wolf_armor"  # Copper: vanilla's armadillo wolf armor
    return f"minecraft:{PREFIX[tier]}_{kind}"


def has(items, **extra):
    """An advancement criterion that fires when the player's inventory contains any one of `items`."""
    # Vanilla wants a single id when there's only one, or a list when there's more than one.
    item_condition = items[0] if len(items) == 1 else items
    predicate = {"items": item_condition}
    predicate.update(extra)
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [predicate]}}


def gear(tier, fmt, kinds=ADV_TOOLS):
    """The registry ids that exist for this tier, across the given kinds (tools, armor, launchers, ...)."""
    ids = []
    for kind in kinds:
        item = item_id(tier, kind, fmt)
        if item:
            ids.append(item)
    return ids


def gen_advancements():
    """Writes the "Doole's Core & Crucible" advancement tab, one set of files per version (item ids differ by version)."""
    for fmt in ("1.21.1", "26"):
        out = VERSIONED / fmt / "data" / MOD / "advancement"
        # 26.x takes a texture asset id, 1.21.1 a texture path.
        bg = "minecraft:block/deepslate_tiles" if fmt == "26" else "minecraft:textures/block/deepslate_tiles.png"
        adv = []  # (name, parent, icon, criteria, requirements ("any"/"all"), frame, xp)

        def add(name, parent, icon, criteria, mode="any", frame="task", xp=0, hidden=False):
            adv.append((name, parent, icon, criteria, mode, frame, xp, hidden))

        # Every tiered launcher (bow/crossbow) id, across every tier, for the "fletcher" advancement.
        launcher_ids = []
        for t in TIERS:
            for i in gear(t, fmt, LAUNCHERS):
                if i.startswith(MOD + ":"):
                    launcher_ids.append(i)
        # Every tiered fishing rod id, across every tier, for the "angler" advancement.
        rod_ids = []
        for t in TIERS:
            for i in gear(t, fmt, RODS):
                if i.startswith(MOD + ":"):
                    rod_ids.append(i)
        # Every tiered shield id, for "shield_bearer"; tiered horse and wolf armor (Iron and up), for "barding".
        shield_ids = []
        body_ids = []
        for t in TIERS:
            shield_ids += [i for i in gear(t, fmt, SHIELDS) if i.startswith(MOD + ":")]
            if TIERS.index(t) >= TIERS.index("iron"):
                body_ids += gear(t, fmt, BODY_ARMOR)
        # Bone and Flint tool/armor ids, for the "first_upgrade" advancement.
        tier_one_ids = []
        for t in ("bone", "flint"):
            tier_one_ids += gear(t, fmt)
        # Obsidian tools plus Obsidian armor, for the "obsidian" advancement.
        obsidian_armor_ids = []
        for k in ARMOR:
            item = item_id("obsidian", k, fmt)
            if item:
                obsidian_armor_ids.append(item)
        obsidian_ids = gear("obsidian", fmt) + obsidian_armor_ids
        # One criterion per enchantment (as an item, and as an enchanted book), for "arcane_forging". All the
        # "_item" criteria come first, then all the "_book" ones, matching the order the advancement used to list
        # them in (it doesn't affect the game, just keeps the generated JSON stable between runs).
        enchantment_criteria = {}
        for n in ENCHANTMENTS:
            enchantment_criteria[f"{n}_item"] = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
                {"predicates": {"minecraft:enchantments": [{"enchantments": f"{MOD}:{n}"}]}}]}}
        for n in ENCHANTMENTS:
            enchantment_criteria[f"{n}_book"] = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
                {"predicates": {"minecraft:stored_enchantments": [{"enchantments": f"{MOD}:{n}"}]}}]}}

        add("root", None, "minecraft:smithing_table", {"wooden": has(["minecraft:wooden_pickaxe", "minecraft:wooden_axe",
                                                                       "minecraft:wooden_sword"])})
        add("first_upgrade", "root", f"{MOD}:bone_pickaxe", {"tier_one": has(tier_one_ids)})
        add("copper_age", "first_upgrade", item_id("copper", "pickaxe", fmt), {"copper": has(gear("copper", fmt))}, xp=10)
        add("copper_armor", "copper_age", item_id("copper", "chestplate", fmt),
            {k: has([item_id("copper", k, fmt)]) for k in ARMOR}, mode="all", xp=20)
        add("fletcher", "first_upgrade", f"{MOD}:bone_bow", {"launcher": has(launcher_ids)})
        add("full_quiver", "fletcher", f"{MOD}:reinforced_crossbow",
            {k: has([item_id("reinforced", k, fmt)]) for k in LAUNCHERS}, mode="all", frame="challenge", xp=100)
        add("angler", "first_upgrade", f"{MOD}:bone_fishing_rod", {"rod": has(rod_ids)})
        add("shield_bearer", "first_upgrade", f"{MOD}:bone_shield", {"shield": has(shield_ids)})
        add("first_alloy", "copper_age", f"{MOD}:iron_alloy_ingot", {"ingot": has([f"{MOD}:iron_alloy_ingot"])}, xp=10)
        add("iron_forged", "first_alloy", "minecraft:iron_pickaxe", {"iron": has(gear("iron", fmt))}, xp=20)
        add("iron_armor", "iron_forged", "minecraft:iron_chestplate",
            {k: has([item_id("iron", k, fmt)]) for k in ARMOR}, mode="all", frame="goal", xp=30)
        add("arcane_forging", "iron_forged", "minecraft:enchanted_book", enchantment_criteria, xp=30)
        add("barding", "iron_forged", "minecraft:iron_horse_armor", {"body_armor": has(body_ids)}, xp=20)
        add("emerald", "iron_forged", f"{MOD}:emerald_pickaxe", {"emerald": has(gear("emerald", fmt))}, xp=30)
        add("diamond", "emerald", "minecraft:diamond_pickaxe", {"diamond": has(gear("diamond", fmt))}, xp=40)
        add("obsidian", "diamond", f"{MOD}:obsidian_pickaxe", {"obsidian": has(obsidian_ids)}, frame="goal", xp=50)
        add("netherite_template", "obsidian", "minecraft:netherite_upgrade_smithing_template",
            {"template": has(["minecraft:netherite_upgrade_smithing_template"])}, xp=40)
        add("netherite", "netherite_template", "minecraft:netherite_pickaxe", {"netherite": has(gear("netherite", fmt))}, xp=60)
        add("reinforced_alloy", "netherite", f"{MOD}:reinforced_alloy_ingot",
            {"ingot": has([f"{MOD}:reinforced_alloy_ingot"])}, xp=50)
        add("reinforced", "reinforced_alloy", f"{MOD}:reinforced_pickaxe", {"reinforced": has(gear("reinforced", fmt))},
            frame="goal", xp=100)
        add("reinforced_armor", "reinforced", f"{MOD}:reinforced_chestplate",
            {k: has([item_id("reinforced", k, fmt)]) for k in ARMOR}, mode="all", frame="challenge", xp=200)

        # Turn each queued advancement into its JSON file.
        for name, parent, icon, criteria, mode, frame, xp, hidden in adv:
            names = list(criteria)
            data = {}
            if parent:
                data["parent"] = f"{MOD}:{parent}"
            display = {"icon": {"id": icon} if fmt == "26" else {"id": icon, "count": 1},
                       "title": {"translate": f"advancements.{MOD}.{name}.title"},
                       "description": {"translate": f"advancements.{MOD}.{name}.description"},
                       "frame": frame}
            if parent is None:
                display.update({"background": bg, "show_toast": False, "announce_to_chat": False})
            if hidden:
                display["hidden"] = True
            data["display"] = display
            data["criteria"] = criteria
            data["requirements"] = [names] if mode == "any" else [[n] for n in names]
            if xp:
                data["rewards"] = {"experience": xp}
            write(out / f"{name}.json", data)


# Vanilla overworld ore placements and the configured feature each one places. Ores generate as veins instead
# (worldgen/VeinKind), so every one is overridden to place nothing. The feature reference is kept, which
# makes the override load on every version, and the biomes that list the placement still tell VeinKind where to go.
ORE_PLACEMENTS = {
    "ore_coal_upper": "ore_coal", "ore_coal_lower": "ore_coal_buried",
    "ore_iron_upper": "ore_iron", "ore_iron_middle": "ore_iron", "ore_iron_small": "ore_iron_small",
    "ore_copper": "ore_copper_small", "ore_copper_large": "ore_copper_large",
    "ore_gold": "ore_gold_buried", "ore_gold_lower": "ore_gold_buried", "ore_gold_extra": "ore_gold",
    "ore_redstone": "ore_redstone", "ore_redstone_lower": "ore_redstone",
    "ore_lapis": "ore_lapis", "ore_lapis_buried": "ore_lapis_buried",
    "ore_diamond": "ore_diamond_small", "ore_diamond_medium": "ore_diamond_medium",
    "ore_diamond_large": "ore_diamond_large", "ore_diamond_buried": "ore_diamond_buried",
    "ore_emerald": "ore_emerald",
}


def gen_worldgen():
    """Switches off vanilla's blob ore placements; the Java vein generator replaces them."""
    for name, feature in ORE_PLACEMENTS.items():
        write(DATA / "minecraft" / "worldgen" / "placed_feature" / f"{name}.json",
              {"feature": f"minecraft:{feature}", "placement": [{"type": "minecraft:count", "count": 0}]})


# Advancement titles and descriptions shown in the advancements screen.
ADVANCEMENT_TEXT = {
    "root": ("Doole's Core & Crucible", "Gear is forged tier by tier now. Start with wooden tools"),
    "first_upgrade": ("Start of the Chain", "Craft a Bone or Flint tool"),
    "copper_age": ("The Copper Age", "Craft a Copper tool or weapon"),
    "copper_armor": ("Clad in Copper", "Wear the start of the armor chain: all four Copper pieces"),
    "fletcher": ("Fletcher", "Craft a tiered bow or crossbow"),
    "full_quiver": ("Full Quiver", "Hold a Reinforced Netherite bow and crossbow"),
    "angler": ("Gone Fishing", "Craft a tiered fishing rod"),
    "shield_bearer": ("Shield Bearer", "Craft a tiered shield"),
    "barding": ("Barding", "Forge Iron or better armor for a horse or a wolf"),
    "first_alloy": ("The First Alloy", "Forge an Iron Alloy Ingot in a smithing table"),
    "iron_forged": ("Forged, Not Crafted", "Smith Copper gear up to Iron with a template and an alloy"),
    "iron_armor": ("Iron Clad", "Hold a full set of Iron armor"),
    "arcane_forging": ("Arcane Forging", "Obtain one of the Doole's Core & Crucible enchantments"),
    "emerald": ("Traded Up", "Forge Emerald gear"),
    "diamond": ("Diamonds, the Hard Way", "Smith Emerald gear up to Diamond"),
    "obsidian": ("Volcanic Glass", "Forge Obsidian gear, with a Ghast Tear as the catalyst"),
    "netherite_template": ("Bastion Loot", "Find or craft the Netherite Upgrade Template"),
    "netherite": ("No Shortcuts", "Smith Obsidian gear up to Netherite"),
    "reinforced_alloy": ("Twin Alloys", "Fuse Netherite and Obsidian alloys with a Nether Star"),
    "reinforced": ("Reinforced", "Forge Reinforced Netherite gear, with a Nether Star as the catalyst"),
    "reinforced_armor": ("Unbreakable", "Hold a full set of Reinforced Netherite armor"),
}


def advancement_lang():
    """Lang entries for the advancement titles and descriptions."""
    out = {}
    for name, (t, d) in ADVANCEMENT_TEXT.items():
        out[f"advancements.{MOD}.{name}.title"] = t
        out[f"advancements.{MOD}.{name}.description"] = d
    return out


if __name__ == "__main__":
    check = "--check" in sys.argv[1:]
    lang = gen_assets()
    gen_tags()
    gen_recipes()
    gen_enchantments()
    gen_advancements()
    gen_worldgen()
    write(ASSETS / "lang" / "en_us.json", lang | tag_lang())
    if not sync(check):
        sys.exit("Generated data is out of date: run python tools/gen_data.py")
    print(f"{len(all_items())} items")
