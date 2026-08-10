# Quest Packs

## Storage and Folder Layout

Instance quest packs live in:

`config/boundless/questpacks/<pack-id>/`

Recommended structure:

```text
<pack-id>/
├─ pack.png
├─ boundless/
│  └─ pack.json
└─ data/
   └─ <namespace>/
      └─ quests/
         ├─ categories/
         │  └─ <category>.json
         ├─ sub-category/
         │  └─ <sub-category>.json
         └─ <quest>.json
```

Quest JSON files may be nested below `quests/`; file paths provide deterministic list ordering. Category and sub-category metadata files are read from their designated folders and are not treated as quests.

Supported sub-category folder spellings are `subcategories`, `sub_category`, `subcategory`, and `sub-category`. Use `sub-category` for consistency with the editor and bundled data.

The loader also understands legacy layouts without `data/`, but new packs should use the structure above.

## Pack Metadata

The editor writes `boundless/pack.json`:

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

| Field | Meaning |
|---|---|
| `pack.description` | Display description in the editor. |
| `boundless.icon_path` | Optional pack icon resource/item identifier. |
| `boundless.layout` | `list` or `tree`; invalid values normalize to `list`. |
| `boundless.enabled` | Pack's default enabled state. |

If `boundless/pack.json` is absent, legacy `pack.mcmeta` metadata is read. Config lists override metadata: `disabledQuestPacks` wins first, then `appliedQuestPacks`, then the metadata default.

## Pack and Namespace Naming

- Editor-created pack folder names are lowercase.
- Spaces become `-`; characters outside `a-z`, `0-9`, `_`, `.`, and `-` are removed.
- Names beginning with `.` or ending `.upload`, `.tmp`, or `.temp` are invalid/reserved.
- Namespaces must match `[a-z0-9_.-]+`.
- IDs should be stable, unique, lowercase identifiers without spaces.

## Category Schema

`data/<namespace>/quests/categories/<file>.json`:

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

| Field | Default | Meaning |
|---|---:|---|
| `id` | required | Category ID referenced by quests. |
| `name` | `id` | Display name. |
| `icon` | `minecraft:book` | Display icon. |
| `order` | `0` | Category sort order, then name. |
| `exclude_from_all` | `false` | Omit the category's quests from the synthetic **All** tab. |
| `dependency` | empty | Hide/lock the category until this quest is redeemed. Missing dependency IDs fail open. |
| `auto_complete` | `false` | Auto-claim ready quests in this category. `autoComplete` is a supported alias. |

## Sub-Category Schema

`data/<namespace>/quests/sub-category/<file>.json`:

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

| Field | Default | Meaning |
|---|---:|---|
| `id` | required | Value used by quest `sub-category`. |
| `category` | empty | Optionally scopes duplicate sub-category IDs to one category. |
| `name` | `id` | Header display name. |
| `icon` | `minecraft:book` | Header icon. |
| `order` | `0` | Metadata value; current list construction primarily follows source filename/path order. |
| `default_open` | `true` | Whether the group begins expanded. `defaultOpen` is accepted. |

If quests reference an undefined sub-category, Boundless creates display metadata automatically from the ID.

## Quest Schema

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
    "complete": [
      { "collect": "minecraft:diamond", "count": 3 }
    ]
  },
  "reward": {
    "items": [{ "item": "minecraft:emerald", "count": 2 }]
  }
}
```

### Quest Fields

| Field | Default | Meaning |
|---|---:|---|
| `id` | filename | Unique quest ID. If omitted, the JSON filename without `.json` is used. |
| `name` | `id` | Display title. The editor requires a nonblank name. |
| `icon` | `boundless:quest_book` | Item/texture icon identifier. |
| `description` | empty | Player text with optional formatting tokens. |
| `category` | `misc` | Category ID. |
| `sub-category` | empty | List grouping. Aliases: `sub_category`, `subCategory`. |
| `dependencies` | empty | One string or an array of quest IDs. Do not use a comma-separated string. |
| `optional` | `false` | Allows player rejection. |
| `repeatable` | `false` | Allows a new cycle after redemption. |
| `auto_complete` | `false` | Auto-claims when ready. Alias: `autoComplete`. |
| `hiddenUnderDependency` | `false` | Hides while dependency logic fails. Alias: `hidden_under_dependency`. |
| `lock_after_dependency` | `false` | Inverts dependency logic; available only before listed dependencies are ever claimed. Alias: `lockAfterDependency`. |
| `type` | `collection` | Legacy quest mode; `submission`/`submit` converts item targets into consuming submissions. |
| `tree_x`, `tree_y` | `-1` | Tree coordinates; the editor writes them for placed tree nodes. |
| `tree_background` | `square` | `square`, `rounded_square`, `circle`, or `diamond`. |
| `completion` | empty | All objective targets. See [Objectives and Rewards](Objectives-and-Rewards). |
| `reward` | empty | Rewards granted on claim. See [Objectives and Rewards](Objectives-and-Rewards). |

An empty completion list has no unsatisfied targets, so once dependencies pass the quest can become ready. Use an explicit `check` objective when player acknowledgement is intended.

## Ordering

- Categories sort by numeric `order`, then display name; **All** is first.
- Quest list ordering uses the quest resource/source path, so numeric filename prefixes such as `01-`, `02-`, and `03-` are reliable.
- Tree packs use explicit coordinates rather than list order.

## Import, Upload, Export, and Reload

- The editor's folder button opens the Boundless pack location for manual import.
- Multiplayer editor saves can zip and upload the full pack to the server in chunks; operator permission is required by the server handlers.
- Export writes a new zip to `config/boundless/exports/`, adding `-1`, `-2`, and so on rather than overwriting an existing export.
- After external edits or file copies, run `/boundless reload`.

## Enable-State Priority

For a pack ID, Boundless resolves state in this order:

1. An entry in `disabledQuestPacks` disables it.
2. Otherwise, an entry in `appliedQuestPacks` enables it.
3. Otherwise, `boundless.enabled` in `boundless/pack.json` is used.
4. Otherwise, the legacy `pack.mcmeta` Boundless enabled flag is used.
5. Otherwise, the pack defaults to enabled.

The built-in `boundless` namespace additionally obeys `UI.enableBuiltinQuestPack`.
