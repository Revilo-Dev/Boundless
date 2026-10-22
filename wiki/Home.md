# Boundless Wiki

Boundless is a NeoForge questing mod with server-authoritative progress, data-driven quest packs, an in-game quest book, an operator editor, automatic pack backups, HUD pinning, completion scrolls, and configurable presentation.

## Documentation

- [Getting Started](Getting-Started) — install, open the book, progress, claim, reject, and repeat quests.
- [Quest Book Guide](Quest-Book-Guide) — navigation, search, filters, tree/list layouts, pinning, and scrolls.
- [Objectives and Rewards](Objectives-and-Rewards) — every supported objective and reward type with JSON examples.
- [Formatting, Colors, Icons, and Emojis](Formatting-Colors-Icons-and-Emojis) — description tokens, hex colors, icons, and Unicode behavior.
- [Quest Packs](Quest-Packs) — complete folder layout and category, sub-category, and quest schemas.
- [Backups and Recovery](Backups-and-Recovery) — automatic snapshots, retention, restore, replacement archives, and exports.
- [Using Questpacks on Servers](Server-Questpack-Deployment) — deployment and authority behavior.
- [Settings Guide](Settings-Guide) — every config option, default, range, and effect.
- [Commands](Commands) — exact operator command syntax.
- [Operator Tools](Operator-Tools) — editor workflow and safety practices.
- [Troubleshooting](Troubleshooting) — common data, progress, UI, and multiplayer issues.
- [Complete Wiki](Complete-Wiki) — the full documentation in one file.

## Feature Summary

- Quest book item, `]` keybind by default, and optional inventory button.
- List and tree quest-pack layouts with categories, sub-categories, dependencies, search, and filters.
- Twelve editor-supported objective types and seven editor-supported reward types.
- Optional/rejectable, repeatable, hidden-until-unlocked, inverse-dependency, and auto-claim quest behavior.
- Up to three persistent, world/server-specific pinned quests on the HUD.
- Per-pack automatic backups with restore support and safe replacement archives.
- Description colors, bold, italic, obfuscated text, animated highlight, wrapping, alignment, and scaling.
- In-game operator editor with item, tag, mob, and effect pickers; multi-select; duplication; ordering; tree placement; import/upload; export; and backups.

## Paths at a Glance

| Purpose | Path |
|---|---|
| Common configuration | `config/boundless-common.toml` |
| Quest packs | `config/boundless/questpacks/` |
| Automatic backups | `config/boundless/backups/questpacks/` |
| Exports | `config/boundless/exports/` |
| Client pin files | `config/boundless/pins/` |

This wiki documents the current multi-JSON format implemented in this repository.
