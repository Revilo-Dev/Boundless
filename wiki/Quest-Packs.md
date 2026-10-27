# Quest Packs

## Pack Location

Quest packs are loaded from:

`config/boundless/questpacks/`

Each pack should be a folder with a valid data layout.

## Multi-JSON Structure (Current)

Boundless uses the multi-file data structure:

`data/<namespace>/quests/`

Inside that:

- `categories/*.json`
- `sub-category/*.json` (also supports related subcategory directory variants)
- quest files directly under `quests/*.json`

## Metadata

Pack metadata is read from `pack.mcmeta` with Boundless fields such as enabled state and optional icon path.

## Enable/Disable

You can enable/disable packs using:

- In-game editor toggles (operator use)
- `/boundless questpack` commands

After state changes, Boundless reloads quest data.

## Importing and Exporting

The in-game editor supports:

- Opening import directory
- Exporting a quest pack zip

## Quest Media

Quest JSON may include a `media` filename. The editor copies PNG, JPEG, and animated GIF files into `config/boundless/media/`; media is displayed below the quest description. Animated GIFs loop and include a restart control.

## Tree Stickers

Tree category JSON may contain a `stickers` array. Each entry has `type` (`text` or `icon`), `value`, `x`, and `y`. In the tree editor, right-click an empty position to add text or choose an item/custom emoji icon.

## Reloading After Manual Edits

If you edit files outside the game:

`/boundless reload`

This refreshes quest data from disk.
