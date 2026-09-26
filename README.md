# Doole's Core & Crucible

This is a Minecraft mod that changes how you get better tools, weapons and armour.

You start with wooden gear and work your way up through bone, flint, copper, iron, emerald, diamond, obsidian, netherite and finally reinforced netherite. From iron onwards, you upgrade your gear at the smithing table. Your gear keeps its name and its enchantments when you upgrade it, and comes out fully repaired.

The current version is **0.3.1**. See the [changelog](CHANGELOG.md) for every version, or the [0.3.1 release notes](release-notes/0.3.1.md).

## What's in it

- Nine tiers of tools, weapons, spears, maces and armour
- Bows, crossbows, fishing rods, shears, shields, horse armour and wolf armour for each tier
- Alloy ingots and upgrade templates, plus a catalyst slot in the smithing table for the top tiers
- 15 new enchantments, including Vein Resonance for mining whole veins and tunnels at once
- Ore veins that replace the normal ore blobs
- A recipe book for the smithing table, an advancements tab, and a tooltip that shows an item's stats when you hold shift
- Its own creative tab (with every enchanted book), and its items also appear in the vanilla Tools, Combat and Ingredients tabs

## Getting started

1. Punch some trees and make wooden tools as normal. You can also make a wooden mace from a log placed on top of a stick.
2. Stone tools can't be crafted in this mod. Instead, get bones from skeletons or flint from gravel, and use them to make bone or flint tools. These work like stone tools.
3. Find some copper and make copper tools and copper armour. Copper is the last tier you craft at a crafting table.
4. Look in chests for an Iron Upgrade Template. They turn up in dungeons, in village weaponsmith, toolsmith and armourer chests, and sometimes in village houses, mineshafts, strongholds, desert and jungle temples, pillager outposts, woodland mansions, shipwrecks and ruined portals.
5. Use the template to make iron alloy ingots, then use those to upgrade your copper gear to iron at the smithing table. From here on, every tier is an upgrade of the one before.

Making an alloy ingot and doing an upgrade both use up a template, so you will need more than one. Look for more in chests, or copy one (see Upgrade templates below). Copying needs netherrack, which you can find at ruined portals before you go to the Nether.

Iron and diamond tools and armour can't be crafted the normal way any more, and the normal diamond to netherite upgrade has been removed. You have to go up the chain.

Hold shift while looking at any piece of gear to see its stats, what tier comes next and what you need for the upgrade. The advancements tab (press L) also walks you through the whole chain.

## How upgrading works

Every upgrade from copper to iron and above is done at the smithing table. You need three things:

- the upgrade template for the new tier
- the piece of gear you want to upgrade, from the tier below
- an alloy ingot for the new tier

The template is used up. The new item keeps its name, enchantments and armour trim, and comes out at full durability. For example, a half-worn iron pickaxe becomes a brand-new emerald pickaxe.

The top three tiers also need a catalyst. A small extra slot appears in the smithing table when your item needs one:

| Upgrading to | Catalyst |
|---|---|
| Obsidian | Ghast Tear |
| Netherite | Wither Skeleton Skull |
| Reinforced | Nether Star |

The catalyst is used up as well.

The order is: copper, iron, emerald, diamond, obsidian, netherite, reinforced. Bone and flint gear can't be upgraded. You need to make copper gear first.

## Recipes

### Bone, flint and copper gear

These are made at a crafting table. They use the same shapes as normal Minecraft items, with bone, flint or a copper ingot as the material.

- Pickaxe, axe, shovel, hoe, sword and spear: the normal tool shapes, with sticks for the handle.
- Bow: the normal bow shape (sticks and string), with the material in the empty middle square.
- Crossbow: the normal crossbow shape, with the material where the iron ingot would go.
- Fishing rod: the normal fishing rod shape, with the material in the bottom middle square.
- Shears: two of the material placed diagonally, like normal shears.
- Shield: the normal shield shape, with the material where the iron ingot would go.

There is no bone or flint mace or armour.

### Copper mace and copper horse armour

- Copper mace: a block of copper on top of a stick.
- Copper horse armour: seven copper ingots in the same shape as leather horse armour.

After copper, maces, horse armour and wolf armour are upgraded at the smithing table like everything else. Copper wolf armour is the normal armadillo scute wolf armour.

### Alloy ingots

Alloy ingots are made at the smithing table. Each one needs the upgrade template for its tier, the ingot from the tier below and a raw material. The template is used up.

| Alloy ingot | Base slot | Addition slot |
|---|---|---|
| Iron | Copper ingot | Iron ingot |
| Emerald | Iron alloy ingot | Emerald |
| Diamond | Emerald alloy ingot | Diamond |
| Obsidian | Diamond alloy ingot | Crying obsidian |
| Netherite | Obsidian alloy ingot | Netherite ingot |
| Reinforced | Netherite alloy ingot | Obsidian alloy ingot |

The reinforced alloy ingot also needs a Nether Star as its catalyst.

### Upgrade templates

You find the Iron Upgrade Template in chests. Every other template is crafted from the one before it:

```
Paper      Old template   Paper
Paper      Material       Paper
Paper      Paper          Paper
```

| New template | Old template | Material |
|---|---|---|
| Emerald | Iron | Emerald |
| Diamond | Emerald | Diamond |
| Obsidian | Diamond | Crying obsidian |
| Netherite (the normal one) | Obsidian | Netherite ingot |
| Reinforced | Netherite | Nether Star |

You can also copy a template, just like the normal netherite template. Put the template in the top middle, netherrack in the centre, and fill the other seven squares with the material. This gives you two templates.

| Template | Material for copying |
|---|---|
| Iron | Iron ingot |
| Emerald | Emerald |
| Diamond | Diamond |
| Obsidian | Crying obsidian |
| Netherite | Uses the normal copying recipe |
| Reinforced | Obsidian alloy ingot |

## Vein Resonance

Vein Resonance goes on mining tools. Hold **Left Alt** while mining to break a whole shape at once, and scroll while holding it to pick the shape. You can change the key in Controls.

| Level | Shapes |
|---|---|
| I | Seam (every touching block of the same ore or log), Bore (a 1-wide hole) |
| II | adds Facet (a 3×3 face) |
| III | adds Gallery (a 3×3 tunnel), Drift (a 1×2 tunnel you can walk through), Rise (stairs going up), Descent (stairs going down) |

Shapes reach 4, 8 or 12 blocks depending on the level. Better tools break more blocks at once, up to 48 with reinforced netherite. It stops before your tool breaks.

## Versions

The mod works on Minecraft 1.21.1, 26.2 and 26.3, with either Fabric or NeoForge. On Fabric you also need Fabric API.

JEI is not needed, but if you have it installed, the mod adds its smithing recipes to it and explains each of its enchantments on the enchanted books.

## Changing things

The first time you start the game, the mod makes a file called `config/dooles_core_crucible.json`. You can change tool, armour, bow, fishing rod, shears and shield stats in it, as well as how big the ore veins are and how often upgrade templates turn up in chests. Restart the game after you change it. If you delete the file, everything goes back to the default settings.

Recipes, item names, tags and advancements are made by the script `tools/gen_data.py`. To change them, edit the script and run it again with `python tools/gen_data.py`.

Textures are made by `tools/gen_textures.py`, which recolours the normal Minecraft textures. Build the mod once first, then run `python tools/gen_textures.py`.

The enchantments are also made by `tools/gen_data.py`, which writes them to `src/main/resources/data/dooles_core_crucible/enchantment/` (Gravitic Anchor's differs by version, so it is in `src/main/versioned/`). The script also holds the enchantment descriptions JEI shows.

Modpack makers can also change any recipe or tag with a datapack.

## Building the mod

You need Java 21 for Minecraft 1.21.1 and Java 25 for 26.2 and 26.3. Gradle can download these for you. To build every version, run:

```
./gradlew build
```

The finished jar files go in `versions/<version>-<loader>/build/libs/`, and a copy of each is put straight into `versions/`.

To make a release, change `mod.version` in `stonecutter.properties.toml`, then run:

```
./gradlew build buildAndCollect
```

This puts every jar, with its sources jar, in `build/libs/<mod version>/`. Write the release notes in `release-notes/<mod version>.md`.

## Licence

This mod uses the MIT licence. See the [LICENSE](LICENSE) file.
