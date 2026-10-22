# Operator Tools

## Access

Boundless commands and server-side editor/config mutations require operator permission level `2`. Multiplayer progress and pack storage remain server-authoritative.

## Quest Settings

The settings screen groups options into All, Style, UI, and Features views. It supports direct six-digit color entry, cycling enumerated values/scales, per-tab reset, save/apply, and links to the Quest Editor. See [Settings Guide](Settings-Guide).

## Quest Editor Features

- Create and configure list/tree quest packs.
- Enable/disable the built-in pack and instance packs.
- Create, edit, duplicate, reorder, multi-select, and delete categories, sub-categories, and quests.
- Place tree quests visually and assign dependencies from the tree.
- Choose `square`, `rounded_square`, `circle`, or `diamond` tree nodes.
- Search quests and browse item, item-tag, inventory, mob, and effect suggestions.
- Add every supported completion/reward row type with validation.
- Format descriptions with the color/style toolbar and preview behavior.
- Upload full pack snapshots to a multiplayer server.
- View and restore recent automatic backups.
- Export a pack zip without overwriting older exports.
- Warn before discarding unsaved edits.

## Recommended Authoring Workflow

1. Create a normalized pack name and choose `list` or `tree`.
2. Create categories and decide category ordering/dependencies.
3. For a list pack, add sub-categories and default-open behavior.
4. Create quests with stable IDs and exact namespaced target IDs.
5. Add dependencies as individual IDs, not a comma-separated string.
6. Add completion targets, then rewards.
7. Test normal claim, auto-claim, submit consumption, repeat, reject, and dependency behavior.
8. Inspect the automatic backup list and export a release zip.
9. Deploy to a test server before production.

## Tree Authoring

Tree packs store `tree_x` and `tree_y` in each quest. In tree placement mode, left-click places a quest and right-clicking existing quests selects dependencies. Dependency lines are generated from quest IDs. Keep adequate spacing for titles and use node background shapes consistently.

## File Safety

- Editor JSON saves are written through a temporary file and replaced atomically when possible.
- Existing live packs are archived before full-directory replacement.
- Normal snapshots retain the five newest per pack.
- Deletion and bulk operations require confirmation/hold interactions in the editor.
- Do not treat automatic retention as source control; export releases and keep external backups.

## Security

Command and function rewards execute as the player with command permission level `4`. Only install trusted quest packs and review all `commands`, `functions`, loot tables, and uploaded pack contents.
