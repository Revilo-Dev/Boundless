# Boundless Complete Wiki

This is the complete single-file manual for the current Boundless codebase. Boundless is a NeoForge questing mod with server-authoritative progress, data-driven packs, an in-game editor, backups, HUD pinning, completion scrolls, and configurable presentation.

## Contents

1. [Paths and Access](#paths-and-access)
2. [Player Guide](#player-guide)
3. [Quest Rules](#quest-rules)
4. [Every Objective Type](#every-objective-type)
5. [Every Reward Type](#every-reward-type)
6. [Pinning and Scrolls](#pinning-and-scrolls)
7. [Formatting, Colors, Icons, and Emojis](#formatting-colors-icons-and-emojis)
8. [Quest Pack Format](#quest-pack-format)
9. [Backups and Recovery](#backups-and-recovery)
10. [All Configuration Options](#all-configuration-options)
11. [Commands](#commands)
12. [Operator Editor](#operator-editor)
13. [Server Deployment](#server-deployment)
14. [Troubleshooting](#troubleshooting)

## Paths and Access

| Purpose | Path |
|---|---|
| Common config | `config/boundless-common.toml` |
| Instance quest packs | `config/boundless/questpacks/` |
| Automatic pack backups | `config/boundless/backups/questpacks/` |
| Exported zip files | `config/boundless/exports/` |
| Client pin state | `config/boundless/pins/` |

Open the quest book with its item, the inventory button, or **Open Quest Book** (`]` by default). The key is changed in Minecraft Controls. `Gameplay.disableQuestBook` blocks item/key access; `UI.hideQuestBookInInventory` only hides the inventory button.

Boundless commands and authoritative editor/config actions require permission level `2`.

## Player Guide

### Basic Flow

1. Open the quest book.
2. Select a category and, in list packs, a sub-category.
3. Open an unlocked quest.
4. Complete every objective.
5. Claim the quest unless it auto-claims.
6. Receive rewards and unlock dependent quests/categories.

### States

- `INCOMPLETE`: objectives are not all satisfied.
- `COMPLETED`: ready, but not claimed.
- `REDEEMED`: claimed and rewards delivered.
- `REJECTED`: an optional quest was declined.

The server controls states, objective submission, rewards, scroll state, and quest definitions in multiplayer.

### Layout and Navigation

Packs use `list` or `tree` layout. List packs show grouped quest rows and collapsible sub-category headers. Tree packs use per-quest coordinates, dependency lines, and shaped nodes.

Search is visible only when `UI.enableQuestSearchBox` is enabled. Filters may render as `tabs`, `buttons`, or `hidden`. Categories can be globally hidden as navigation (`UI.disableCategories`) or specific category IDs can be fully disabled (`disabledQuestCategories`).

### Special Actions

- Optional quests can be rejected and then disappear from normal lists.
- Repeatable quests can restart after redemption.
- Manual `check` objectives require acknowledgement in the detail UI.
- Field objectives provide a text box.
- Eligible claimed quests can create a completion scroll once.
- Up to three active quests can be pinned to the HUD.

## Quest Rules

### Dependencies

`dependencies` accepts one string or an array of quest IDs. Normal dependencies pass only after **every** named quest has been claimed at least once. `COMPLETED` without claiming does not unlock dependents. Missing IDs fail quest dependency checks.

`lock_after_dependency: true` inverts the rule: the quest is available only while none of its listed dependencies has ever been claimed. `hiddenUnderDependency: true` makes a failed dependency rule hide the quest instead of merely leaving it locked.

Category `dependency` hides/locks the category until that quest is `REDEEMED`; a missing category dependency fails open.

### Flags

- `optional`: enables rejection.
- `repeatable`: enables restart after redemption.
- `auto_complete`: auto-claims when ready, despite the historical name.
- Category `auto_complete`: auto-claims ready quests in that category.
- Global `Functionality.autoClaimQuestRewards`: auto-claims all ready quests.
- `hiddenUnderDependency`: hides until the dependency rule passes.
- `lock_after_dependency`: inverse dependency rule.

### Status Visibility

Rejected quests are hidden. Redeemed non-repeatable quests are hidden. Repeatable redeemed quests remain available for restart. A disabled category removes its quests from loading/progression.

## Every Objective Type

The current editor writes objectives inside:

```json
"completion": {
  "complete": [
    { "collect": "minecraft:diamond", "count": 3 }
  ]
}
```

All listed targets must pass.

### `collect`

```json
{ "collect": "minecraft:diamond", "count": 3 }
```

Permanently records observed inventory progress up to the requirement for that quest cycle. Item IDs, `#namespace:tag` values, and component-qualified items are supported.

Alternatives:

```json
{
  "collect": "minecraft:oak_log",
  "acceptedItems": ["minecraft:oak_log", "minecraft:birch_log", "#minecraft:logs"],
  "count": 16
}
```

### `submit`

```json
{ "submit": "minecraft:diamond", "count": 3 }
```

Requires current inventory and consumes matching items on successful claim. Every item/XP submission is simulated first, so partial payment is not taken if another submission target cannot be paid. Supports `acceptedItems` alternatives.

Legacy quest `type` values `submission` or `submit` make ordinary item targets consuming submissions.

### `kill`

```json
{ "kill": "minecraft:zombie", "count": 5 }
```

Counts server-observed entity kills. Alternatives use `acceptedMobs`.

### `advancement`

```json
{ "advancement": "minecraft:story/mine_stone" }
```

Passes when the player has completed the named advancement. The editor labels this type `achieve`.

### `effect`

```json
{ "effect": "minecraft:speed" }
```

Passes once the effect is detected and remembers it for the cycle.

### `observe`

```json
{ "observe": "minecraft:oak_log" }
```

Passes when supported block/entity/displayed-item observation detects the exact target and remembers it.

### `check`

```json
{ "check": "I understand" }
```

Creates a manual acknowledgement. Blank/legacy boolean-like values use `Understand`.

### `biome`

```json
{ "biome": "minecraft:plains" }
```

Remembers visiting the exact biome for the cycle.

### `dimension`

```json
{ "dimension": "minecraft:the_nether" }
```

Remembers entering the exact dimension for the cycle.

### `xp`

```json
{ "xp": "points", "count": 100 }
```

Modes: `points` or `levels`. XP targets are consuming submissions deducted at claim. Level deductions preserve fractional progress in the remaining level.

### `levelup_level`

```json
{ "levelup_level": 10 }
```

Requires the optional LevelUP integration and checks its level. The editor shows `levelup` only when that mod is available.

### `field`

```json
{ "field": "BOUNDLESS", "field_text": "Enter the keyword" }
```

Compares trimmed text case-insensitively. `field_text` is the hint; `fieldText` is an accepted legacy alias.

### Editor Objective Names

The editor exposes `collect`, `submit`, `kill`, `achieve`, `effect`, `observe`, `check`, `biome`, `dimension`, `xp`, optional `levelup`, and `field`. Alternative IDs for collect/submit/kill use `|`, and counts follow the value:

```text
collect minecraft:oak_log|minecraft:birch_log 16
kill minecraft:zombie|minecraft:husk 10
xp points 100
field "BOUNDLESS" "Enter the keyword"
```

There is no current `stat` objective type.

## Every Reward Type

Rewards live under `reward` and are delivered server-side on redemption. Inventory overflow drops at the player.

### Items

```json
"items": [{ "item": "minecraft:diamond", "count": 2 }]
```

Item components are supported. If an item reward has multiple `acceptedItems`, each unit randomly selects an entry.

### Experience

```json
"exp": "points",
"count": 100
```

Values: `points`, `levels`, `levelup`, or `levelup_levels`. The latter two require LevelUP.

### Commands

```json
"commands": [{
  "command": "give @s minecraft:emerald 4",
  "icon": "minecraft:command_block",
  "title": "Emerald grant"
}]
```

Runs as the claiming player with permission `4` and suppressed output. A leading `/` is removed. Audit untrusted packs carefully.

### Functions

```json
"functions": [{
  "function": "mypack:quest_reward",
  "icon": "minecraft:knowledge_book",
  "title": "Special reward"
}]
```

Runs the Minecraft function as the claiming player. A supplied leading `/` or `function` word is normalized.

### Loot Tables

```json
"lootTables": [{
  "lootTable": "minecraft:chests/simple_dungeon",
  "icon": "minecraft:chest",
  "title": "Dungeon loot"
}]
```

Generates native loot in a gift context with the player and position. The editor's `loot` row currently serializes an equivalent command reward.

### Advancements

```json
"advancements": [{ "advancement": "minecraft:adventure/root" }]
```

Awards all remaining criteria of the named advancement.

### Toasts

```json
"toasts": [{
  "title": "Secret Found",
  "description": "You discovered the chamber.",
  "icon": "minecraft:amethyst_shard"
}]
```

Sends a custom reward toast.

### Complete Reward Object

```json
"reward": {
  "items": [{ "item": "minecraft:diamond", "count": 2 }],
  "commands": [{ "command": "say @s completed a quest", "icon": "minecraft:command_block", "title": "Announcement" }],
  "functions": [{ "function": "mypack:reward", "icon": "minecraft:knowledge_book", "title": "Function" }],
  "lootTables": [{ "lootTable": "minecraft:chests/simple_dungeon", "icon": "minecraft:chest", "title": "Loot" }],
  "advancements": [{ "advancement": "minecraft:adventure/root" }],
  "toasts": [{ "title": "Complete", "description": "Reward claimed", "icon": "minecraft:diamond" }],
  "exp": "points",
  "count": 100
}
```

`/boundless complete` does not grant rewards. `/boundless redeem` force-completes and grants them.

## Pinning and Scrolls

### Pins

- Use the pin icon in the sticky quest header.
- Capacity is three; a fourth removes the oldest.
- State is separated by singleplayer world name or multiplayer server address.
- Files live at `config/boundless/pins/<world-or-server>.json`.
- The HUD is hidden while another screen is open.
- Redeemed/rejected quests are removed automatically.
- Position: `top_left`, `top_right`, `bottom_left`, or `bottom_right`.
- `Functionality.disableQuestPinning` disables the system.
- The current implementation clears the active pin file when logging out.

The HUD displays up to three target summaries per pinned quest with item/fallback icons and progress.

### Completion Scrolls

When `Functionality.enableQuestScrolls` is true, a previously claimed quest can create one scroll per player/quest. When eligible, the scroll button replaces the pin button in the same header slot. Creation/use is server-validated.

## Formatting, Colors, Icons, and Emojis

### Description Tokens

| Token | Effect | RGB |
|---|---|---|
| `/w` | Aqua | `55FFFF` |
| `/r` | Red | `FF5555` |
| `/g` | Green | `55FF55` |
| `/b` | Blue | `5555FF` |
| `/y` | Yellow | `FFFF55` |
| `/o` | Orange/gold | `FFAA00` |
| `/a` | Gray | `AAAAAA` |
| `/p` | Purple | `AA55FF` |
| `/l` | Toggle bold | — |
| `/i` | Toggle italic | — |
| `/e` | Toggle obfuscated text | — |
| `/h` | Toggle animated highlight | — |
| `/x` | Reset color and all styles | configured default |

Example:

```text
/yWarning: /xBring /lthree diamonds/l. /hRare reward!/h
```

Tokens are case-insensitive. Repeating a style token toggles it off; `/x` resets everything. If formatting is disabled, recognized tokens are stripped.

### Hex and Layout

- `UI.questWidgetTextColor`: normal quest row title RGB.
- `UI.descriptionTextColor`: default/reset description RGB.
- Both require six hex digits; the settings field accepts a leading `#`.
- Text/icon scales range `0.5`–`1.0`.
- Description alignment values are `left`, `center`, `right`, and `adjust`.
- Wrapping, Read More, sticky headers, and row icons are independently configurable.

### Icons and Tree Shapes

Use namespaced item IDs such as `minecraft:diamond` for quest/category/reward icons. Texture-resource icons may work where the UI renders a texture, but do not resolve to item stacks. Tree backgrounds are `square`, `rounded_square`, `circle`, or `diamond`; invalid values become `square`.

Item targets support item IDs, `#item:tags`, and component suffixes in `{...}` or `[...]` form. Item rewards require a concrete item ID (optionally with components), not a tag.

### Emojis

Boundless has no shortcode registry and does not translate `:emoji_name:`. Literal Unicode may be stored, but rendering depends on client fonts/resource packs and unsupported characters may show as boxes. For reliable emoji-like visuals, ship a custom font resource pack or use item/texture icons.

## Quest Pack Format

### Folder Layout

```text
config/boundless/questpacks/<pack-id>/
├─ pack.png
├─ boundless/pack.json
└─ data/<namespace>/quests/
   ├─ categories/<category>.json
   ├─ sub-category/<sub-category>.json
   └─ <quest>.json
```

Quest files may be nested. Supported sub-category folder aliases are `subcategories`, `sub_category`, `subcategory`, and `sub-category`. The loader also understands legacy roots without `data/`, but the shown structure is recommended.

### Pack Metadata

```json
{
  "pack": {
    "pack_format": 0,
    "description": "My Boundless Quest Pack"
  },
  "boundless": {
    "icon_path": "minecraft:diamond",
    "layout": "list",
    "enabled": true
  }
}
```

`layout` is `list` or `tree`. Invalid values become `list`. `boundless/pack.json` takes precedence over legacy `pack.mcmeta` metadata.

Editor pack names become lowercase, spaces become `-`, and unsupported characters are removed. Names beginning `.` or ending `.upload`, `.tmp`, or `.temp` are reserved. Namespaces match `[a-z0-9_.-]+`.

### Category JSON

```json
{
  "id": "main",
  "name": "Main Progression",
  "icon": "minecraft:grass_block",
  "order": 1,
  "exclude_from_all": false,
  "dependency": "intro_complete",
  "auto_complete": false
}
```

Defaults: name=`id`, icon=`minecraft:book`, order=`0`, flags=false. Categories sort by order then name. **All** is synthetic and first. `exclude_from_all` hides this category from **All** only.

### Sub-Category JSON

```json
{
  "id": "getting_started",
  "category": "main",
  "name": "Getting Started",
  "icon": "minecraft:wooden_pickaxe",
  "order": 1,
  "default_open": true
}
```

`category` can scope the same sub-ID. `defaultOpen` is accepted as an alias. Undefined referenced sub-categories receive generated metadata.

### Quest JSON

```json
{
  "id": "mine_diamonds",
  "name": "Diamonds!",
  "icon": "minecraft:diamond",
  "description": "/bFind /lthree/l diamonds./x",
  "category": "main",
  "sub-category": "getting_started",
  "dependencies": ["iron_age"],
  "optional": false,
  "repeatable": false,
  "auto_complete": false,
  "hiddenUnderDependency": false,
  "lock_after_dependency": false,
  "tree_x": 120,
  "tree_y": 80,
  "tree_background": "diamond",
  "completion": {
    "complete": [{ "collect": "minecraft:diamond", "count": 3 }]
  },
  "reward": {
    "items": [{ "item": "minecraft:emerald", "count": 2 }]
  }
}
```

Quest field defaults/aliases:

| Field | Default / Alias |
|---|---|
| `id` | filename without `.json` |
| `name` | `id` |
| `icon` | `boundless:quest_book` |
| `description` | empty |
| `category` | `misc` |
| `sub-category` | empty; aliases `sub_category`, `subCategory` |
| `dependencies` | empty; one string or array, never comma-separated text |
| `optional`, `repeatable` | `false` |
| `auto_complete` | `false`; alias `autoComplete` |
| `hiddenUnderDependency` | `false`; alias `hidden_under_dependency` |
| `lock_after_dependency` | `false`; alias `lockAfterDependency` |
| `type` | `collection`; legacy `submit`/`submission` behavior |
| `tree_x`, `tree_y` | `-1` |
| `tree_background` | `square` |

Quest source paths/filenames control deterministic list ordering; use `01-`, `02-`, etc. Tree layout uses coordinates. An empty completion has no failing target, so use `check` if acknowledgement is intended.

### Enable-State Priority

1. `disabledQuestPacks` forces off.
2. Otherwise `appliedQuestPacks` forces on.
3. Otherwise `boundless.enabled` is used.
4. Otherwise legacy `pack.mcmeta` Boundless enabled state is used.
5. Otherwise enabled is the default.

The built-in namespace also obeys `UI.enableBuiltinQuestPack`.

## Backups and Recovery

Normal snapshots live at `config/boundless/backups/questpacks/<pack-id>/<timestamp>-<reason>/`. The five newest normal snapshots per pack are retained. Folders beginning `_` are excluded from pruning.

Full pack replacement archives the previous live directory under `<pack-id>/_replaced/<timestamp>-<reason>/`. These archives are separate from five-snapshot retention. Failed replacement attempts try to restore the archived live directory.

Editor JSON writes use a `.saving` temporary file and atomic replacement when supported. Uploads/restores use staged directories, and pack discovery attempts to recover completed interrupted uploads.

To restore through the editor: select pack → pack options → **Recent Backups** → choose timestamp → confirm. Boundless validates that the snapshot is inside that pack's backup root, stages it, archives live content, swaps, and reloads.

Exports are independent zip archives in `config/boundless/exports/`. Existing names are not overwritten; `-1`, `-2`, etc. are added. Use exports/version control for long-term history.

Manual restore: stop game/server, copy the live pack elsewhere, copy a timestamped snapshot into the live pack directory, verify no extra nesting, restart, and `/boundless reload`.

## All Configuration Options

### Root

| Key | Default | Effect |
|---|---|---|
| `disabledQuestCategories` | `[]` | Fully disables matching category IDs. |
| `appliedQuestPacks` | `[]` | Normalized pack IDs forced on. |
| `disabledQuestPacks` | `[]` | Normalized pack IDs forced off; wins over applied. |

### UI

| Key | Default / Range | Effect |
|---|---|---|
| `UI.pinnedQuestHudPosition` | `bottom_left` | `top_left`, `top_right`, `bottom_left`, `bottom_right`. |
| `UI.hideQuestBookInInventory` | `false` | Hides inventory quest button. |
| `UI.questBookInventoryButtonPosition` | `beside_recipe_book` | Or `above_offhand_slot`. |
| `UI.centerInventoryWithQuestPanel` | `true` | Centers inventory + quest panel together. |
| `UI.hideCategoryHeader` | `false` | Hides category banner. |
| `UI.filterDisplayMode` | `tabs` | `tabs`, `buttons`, or `hidden`. |
| `UI.disableCategories` | `false` | Removes category filtering/navigation. |
| `UI.enableBuiltinQuestPack` | `true` | Enables shipped Boundless quests. |
| `UI.hideQuestWidgetIcons` | `false` | Hides quest row icons. |
| `UI.questTextScale` | `1.0` / `0.5`–`1.0` | Scales quest/detail text. |
| `UI.questIconScale` | `1.0` / `0.5`–`1.0` | Scales list/detail icons. |
| `UI.enableQuestSearchBox` | `false` | Shows quest-name search. |
| `UI.stickySubCategoryHeaders` | `true` | Sticky active list group. |
| `UI.stickyDescriptionHeaders` | `true` | Sticky quest detail title. |
| `UI.enableDescriptionColors` | `true` | Enables Boundless formatting tokens. |
| `UI.questWidgetTextColor` | `FFFFFF` | Six-digit quest-title RGB. |
| `UI.descriptionTextColor` | `CFCFCF` | Default/reset description RGB. |
| `UI.enableDescriptionReadMore` | `true` | Collapses long descriptions. |
| `UI.enableDescriptionTextWrapping` | `true` | Wraps detail text. |
| `UI.descriptionTextAlignment` | `left` | `left`, `center`, `right`, `adjust`. |
| `UI.enableQuestToasts` | `true` | Enables quest-unlocked toasts. |

### Functionality

| Key | Default | Effect |
|---|---|---|
| `Functionality.disableQuestPinning` | `false` | Disables pin button/HUD/state. |
| `Functionality.autoClaimQuestRewards` | `false` | Auto-redeems ready quests. |
| `Functionality.enableQuestScrolls` | `true` | Enables completion scroll flow. |

### Gameplay

| Key | Default | Effect |
|---|---|---|
| `Gameplay.disableQuestBook` | `false` | Blocks item and keybind opening. |
| `Gameplay.spawnWithQuestBook` | `false` | Gives new players a book. |

### Development

| Key | Default | Effect |
|---|---|---|
| `Development.devMode` | `false` | Rate-limited performance diagnostics. |

Fixed, non-configurable limits: five normal backups per pack and three pinned quests. Save settings in-game; after external pack edits use `/boundless reload`. On multiplayer, server authority wins for authoritative behavior.

## Commands

All require permission `2`.

```text
/boundless reload

/boundless reset <quest-id> [targets]
/boundless reset all [targets]

/boundless complete <quest-id> [targets]
/boundless complete all [targets]

/boundless redeem <quest-id> [targets]
/boundless redeem all [targets]

/boundless questpack list
/boundless questpack enable <pack-id>
/boundless questpack disable <pack-id>
```

Omitted targets mean the executing player; console must provide targets. `reset` clears status/progress, `complete` grants no rewards, and `redeem` grants rewards. Pack enable/disable writes override lists, reloads, and syncs players. There are no current `/boundless toasts` commands.

## Operator Editor

The editor supports:

- Creating list/tree packs and editing metadata/enabled state.
- Category, sub-category, and quest create/edit/duplicate/reorder/delete.
- Multi-select and guarded deletion.
- Visual tree placement and dependency selection.
- Item, tag, inventory, mob, and effect pickers/suggestions.
- Every editor objective and reward row type.
- Description format toolbar.
- Unsaved-changes prompts.
- Multiplayer zipped/chunked pack upload.
- Recent backup restore.
- Non-overwriting zip export.

Recommended flow: create pack → categories → sub-categories/tree plan → quests → dependencies → objectives → rewards → test every special behavior → inspect backup → export → stage on a server.

Command/function rewards run with elevated permission. Only install trusted packs.

## Server Deployment

1. Export a tested pack from the editor.
2. Stop the destination server for manual filesystem deployment.
3. Extract directly into `<server>/config/boundless/questpacks/<pack-id>/`.
4. Start the server.
5. Run `/boundless questpack list`.
6. Enable if necessary.
7. Run `/boundless reload` after live external changes.
8. Test with matching client/server Boundless versions.

An authorized multiplayer editor can instead upload the pack in chunks. The server stages it, archives the current live directory, installs, applies enabled state, reloads, and syncs players.

Validate categories, dependency claims, existing progress, submissions, reward commands, and backups after every deployment.

## Troubleshooting

### Missing or Locked Quest

- Check pack/built-in enabled state and `disabledQuestCategories`.
- Check category and quest IDs exactly.
- Claim normal prerequisites; completion alone is insufficient.
- Remember `lock_after_dependency` is inverse.
- Rejected and redeemed non-repeatable quests are hidden.

### No Objective Progress

- Use exact namespaced IDs.
- Distinguish permanent `collect` from consuming `submit`.
- XP mode must be `points` or `levels` and is consumed on claim.
- A `check` needs acknowledgement.
- Field input trims/ignores case only.
- LevelUP types require the mod.
- Component-qualified item targets require matching components.
- Check the authoritative server log.

### Missing Rewards

- Confirm `REDEEMED`, not only `/boundless complete`.
- Invalid items are skipped/logged; overflow drops.
- Verify functions, loot tables, advancements, and LevelUP integration exist.
- Check reward command errors in server logs.

### Pack Not Loading

- Verify folder nesting and `data/<namespace>/quests/`.
- Validate JSON and prefer standard JSON. Annotation properties such as `"//NOTE"` are simply ignored fields, not actual comments.
- Check override lists/metadata enabled state.
- Avoid reserved staging folder names.
- Run `/boundless reload`.

### Formatting or Pins

- Use `/r`-style tokens, not section-sign codes.
- Use six-digit hex colors and `/x` to reset.
- Emoji display depends on client font resources.
- Pin HUD hides while screens are open and removes completed/rejected quests.
- Capacity is three and the current pin file clears on logout.

### Backups

- Confirm write permission under `config/boundless/`.
- Restore only a listed snapshot for the selected pack.
- Restart/reload after interrupted staging.
- Stop the server for manual directory recovery.

For diagnostics, temporarily enable `Development.devMode`, reproduce once, then disable it. Provide the server log, affected pack, quest ID, mod/Minecraft/NeoForge versions, and environment type in a bug report.
