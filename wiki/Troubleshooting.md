# Troubleshooting

## Quest Does Not Appear

- Confirm its category is not in `disabledQuestCategories`.
- Confirm its pack is enabled and the built-in pack switch is correct.
- Check `hiddenUnderDependency`, category `dependency`, and quest dependency state.
- Redeemed non-repeatable and rejected quests are intentionally hidden.
- Confirm the quest file is below `data/<namespace>/quests/` and not inside a category/sub-category metadata folder.

## Quest Is Locked

- Normal dependencies require **redeemed/previously claimed**, not merely completed, prerequisites.
- Every dependency in the array must pass.
- Missing quest IDs fail the quest dependency check.
- `lock_after_dependency: true` is inverse: claiming any listed dependency locks the quest.

## Objective Does Not Progress

- Use exact namespaced IDs (`minecraft:zombie`, not `Zombie`).
- `collect` permanently records observed inventory counts; `submit` requires current items and consumes them on claim.
- `xp` accepts only `points` or `levels` and is consumed on claim.
- `field` comparison trims and ignores case but otherwise expects the configured text.
- `check` requires manual acknowledgement.
- LevelUP objectives are available only when the integration is installed.
- Item component-qualified targets must match the stack's serialized components.
- On multiplayer, reproduce while connected to the authoritative server and check its log.

## Rewards Are Missing

- Ensure the quest was `REDEEMED`, not only force-completed with `/boundless complete`.
- Invalid/missing item IDs are skipped and logged.
- Inventory overflow drops at the player.
- Missing functions, loot tables, or advancements are skipped/fail without producing that reward.
- Review server logs for reward execution errors.
- LevelUP reward types require the integration.

## Pack Does Not Load

- Confirm `config/boundless/questpacks/<pack-id>/data/<namespace>/quests/` exists directly.
- Validate JSON syntax and types and prefer standard JSON. Fields named like `"//NOTE"` are merely ignored annotation properties, not actual comments.
- Confirm the namespace uses lowercase `a-z`, digits, `_`, `.`, or `-`.
- Check `disabledQuestPacks`, `appliedQuestPacks`, metadata `enabled`, and the built-in pack switch.
- Ensure the folder is not hidden/reserved (`.name`, `*.upload`, `*.tmp`, `*.temp`).
- Run `/boundless reload`.

## Category or Sub-Category Is Wrong

- Category files belong in `quests/categories/`.
- Use one supported sub-category folder spelling; `sub-category/` is recommended.
- Match quest `category` and `sub-category` IDs exactly.
- Quest ordering comes from source paths/filenames, so prefix filenames numerically.
- `UI.disableCategories` hides filtering; it does not delete category data.

## Formatting Looks Wrong

- Verify `UI.enableDescriptionColors` is enabled.
- Tokens use a forward slash: `/r`, `/l`, `/x`; Minecraft section-sign codes are not the Boundless syntax.
- Use `/x` to reset every active style.
- Validate configured colors as six hex digits.
- Unicode emoji/glyph display depends on the client's font/resource packs.

## Pinning Problems

- Check `Functionality.disableQuestPinning`.
- The HUD is intentionally hidden while any screen is open.
- Only three pins are retained; a fourth removes the oldest.
- Redeemed/rejected quests are automatically unpinned.
- Pin files are world/server-specific under `config/boundless/pins/` and the active file is cleared on logout in the current implementation.

## Editor or Backup Problems

- Save the current entry before navigating away, or choose Save in the unsaved-changes prompt.
- Verify write access to `config/boundless/`.
- Restore only through the selected pack's listed backup entries.
- If staging was interrupted, restart/reload so recovery logic can inspect hidden upload directories.
- For manual recovery, stop the game/server and copy a timestamped snapshot rather than editing it in place.

## Keybind or Inventory Button Missing

- The default key is `]`; check Minecraft control conflicts.
- `Gameplay.disableQuestBook` blocks the key and book item.
- `UI.hideQuestBookInInventory` hides the inventory button.
- The button position may be `beside_recipe_book` or `above_offhand_slot`.

## Getting Useful Logs

Enable `Development.devMode` only while diagnosing performance/load behavior, reproduce the issue, then disable it. Include the server log, affected pack files, exact quest ID, Boundless version, Minecraft/NeoForge version, and whether the issue is singleplayer or dedicated-server-only.
