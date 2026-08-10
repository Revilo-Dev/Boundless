# Objectives and Rewards

A quest is ready only when its dependency rule passes and every completion target is satisfied. The editor writes the current format as `completion.complete`, but the loader also accepts several legacy shapes.

## Current Completion Format

```json
"completion": {
  "complete": [
    { "collect": "minecraft:diamond", "count": 3 },
    { "kill": "minecraft:zombie", "count": 5 }
  ]
}
```

## Objective Types

### Collect

```json
{ "collect": "minecraft:diamond", "count": 3 }
```

The player must obtain the required item count. Progress is permanently recorded up to the required amount for the current quest cycle, so later moving or using the items does not erase recorded progress.

Accepted alternatives use a primary display/match ID plus `acceptedItems`:

```json
{
  "collect": "minecraft:oak_log",
  "acceptedItems": ["minecraft:oak_log", "minecraft:birch_log", "#minecraft:logs"],
  "count": 16
}
```

### Submit

```json
{ "submit": "minecraft:diamond", "count": 3 }
```

Submit requires the items to be present when the quest is claimed and removes them on successful claim. All item/XP submissions are simulated first; nothing is removed if the player cannot pay every submission target. `acceptedItems` works as it does for collect targets and the required count may be distributed across accepted alternatives.

A legacy quest-level `"type": "submission"` or `"type": "submit"` makes its ordinary item targets behave as submit targets.

### Kill

```json
{ "kill": "minecraft:zombie", "count": 5 }
```

Counts server-observed kills. Alternatives use `acceptedMobs`:

```json
{
  "kill": "minecraft:zombie",
  "acceptedMobs": ["minecraft:zombie", "minecraft:husk", "minecraft:drowned"],
  "count": 10
}
```

### Advancement

```json
{ "advancement": "minecraft:story/mine_stone" }
```

The editor calls this type `achieve` but writes the `advancement` JSON key. It completes when the player has finished the named advancement.

### Effect

```json
{ "effect": "minecraft:speed" }
```

Completes once the player is detected with the named effect. The detection is permanently remembered for the quest cycle.

### Observe

```json
{ "observe": "minecraft:oak_log" }
```

Completes when the player looks at the target block, entity, or displayed item supported by observation detection. Dedicated servers accept the client observation report and validate it through the quest networking path. Completion is remembered for the cycle.

### Check

```json
{ "check": "I understand" }
```

Displays a manual acknowledgement objective. The player uses the quest UI to confirm it. Blank or boolean-like legacy values display `Understand`.

### Biome

```json
{ "biome": "minecraft:plains" }
```

Completes once the player visits the exact biome ID and remembers the visit for the cycle.

### Dimension

```json
{ "dimension": "minecraft:the_nether" }
```

Completes once the player enters the exact dimension ID and remembers it for the cycle.

### XP

```json
{ "xp": "points", "count": 100 }
```

Modes are `points` or `levels`. XP objectives are submission targets: the amount is verified and deducted when rewards are claimed. Level submission preserves the player's fractional progress inside the remaining level.

### LevelUP Level

```json
{ "levelup_level": 10 }
```

Requires the optional LevelUP integration and completes when the LevelUP level requirement is met. The editor only offers this type when LevelUP is present.

### Field Input

```json
{ "field": "BOUNDLESS", "field_text": "Enter the keyword" }
```

Adds a text field to the quest details. The player's trimmed input is matched case-insensitively against `field`; `field_text` is the hint. `fieldText` is accepted as a legacy alias.

## Objective Authoring Syntax in the Editor

The editor exposes: `collect`, `submit`, `kill`, `achieve`, `effect`, `observe`, `check`, `biome`, `dimension`, `xp`, optional `levelup`, and `field`. Counts follow the value. Alternative collect/submit/kill IDs are separated with `|`.

```text
collect minecraft:oak_log|minecraft:birch_log 16
kill minecraft:zombie|minecraft:husk 10
xp points 100
field "BOUNDLESS" "Enter the keyword"
```

## Reward Types

Rewards are granted server-side when the quest is redeemed. Inventory overflow is dropped at the player's position.

### Items

```json
"items": [
  { "item": "minecraft:diamond", "count": 2 }
]
```

An item reward may include item components. If `acceptedItems` contains alternatives, each of the `count` rolls randomly chooses one accepted entry.

### Experience

```json
"exp": "points",
"count": 100
```

Supported values are:

- `points` — vanilla experience points.
- `levels` — vanilla experience levels.
- `levelup` — LevelUP XP.
- `levelup_levels` — LevelUP levels.

The editor uses reward types `xp` and, when the integration is installed, `levelup`.

### Commands

```json
"commands": [
  {
    "command": "give @s minecraft:emerald 4",
    "icon": "minecraft:command_block",
    "title": "Emerald grant"
  }
]
```

Commands run as the claiming player with permission level `4` and suppressed command output. A leading `/` is optional and removed. `icon` and `title` affect the reward preview.

### Functions

```json
"functions": [
  {
    "function": "mypack:quest_reward",
    "icon": "minecraft:knowledge_book",
    "title": "Special reward"
  }
]
```

Runs the named Minecraft function as the claiming player. Supplying `function mypack:quest_reward` is also accepted; the duplicate prefix is normalized.

### Loot Tables

```json
"lootTables": [
  {
    "lootTable": "minecraft:chests/simple_dungeon",
    "icon": "minecraft:chest",
    "title": "Dungeon loot"
  }
]
```

Generates the loot table with the player and player position in a gift loot context. Generated stacks go to inventory or drop on overflow. The editor's `loot` reward currently serializes an equivalent `loot give @s loot ...` command; direct JSON supports native `lootTable`/`lootTables` entries.

### Advancements

```json
"advancements": [
  { "advancement": "minecraft:adventure/root" }
]
```

Awards every remaining criterion of the named advancement.

### Toasts

```json
"toasts": [
  {
    "title": "Secret Found",
    "description": "You discovered the chamber.",
    "icon": "minecraft:amethyst_shard"
  }
]
```

Sends a custom reward toast to the player. Quest-unlocked and reward toasts obey the client toast setting where applicable.

## Full Reward Example

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

## Claim Behavior

- `auto_complete` on a quest or its category immediately claims it when ready.
- `Functionality.autoClaimQuestRewards` globally applies the same behavior.
- Manual `/boundless complete` changes status without rewards; `/boundless redeem` force-completes and grants rewards.
- Reward and progress changes are authoritative on the server.
