# Commands

All Boundless commands require permission level `2`. The root is `/boundless`.

## Reload

```text
/boundless reload
```

Force-reloads server quest definitions, then re-synchronizes every connected player. Use after external JSON changes, manual pack installation/removal, or config-based pack/category changes.

## Reset Progress

```text
/boundless reset <quest-id>
/boundless reset <quest-id> <targets>
/boundless reset all
/boundless reset all <targets>
```

- Without `targets`, the command targets the executing player; console execution therefore needs explicit targets.
- One-quest reset returns that quest to `INCOMPLETE` and refreshes its progress metadata.
- `reset all` clears the target players' saved Boundless quest progress broadly.
- Target syntax uses Minecraft's player entity argument, such as `@s`, `@a`, or a player name.

## Complete Without Rewards

```text
/boundless complete <quest-id>
/boundless complete <quest-id> <targets>
/boundless complete all
/boundless complete all <targets>
```

Force-sets eligible quest status to `COMPLETED` without granting rewards. It does not overwrite an already `REDEEMED` or `REJECTED` status.

## Redeem and Grant Rewards

```text
/boundless redeem <quest-id>
/boundless redeem <quest-id> <targets>
/boundless redeem all
/boundless redeem all <targets>
```

Force-completes and redeems quests, including item, command, function, loot-table, advancement, experience, and toast rewards. Existing `REDEEMED`/`REJECTED` quests are not redeemed again.

Unlike normal player claiming, the operator redeem path force-completes before calling redemption and is intended as an administrative override.

## Quest Pack Commands

```text
/boundless questpack list
/boundless questpack enable <pack-id>
/boundless questpack disable <pack-id>
```

- `list` scans directories in `config/boundless/questpacks/` and shows effective enabled state.
- `enable` and `disable` update the server's normalized config override lists, reload quest data, and sync connected players.
- The pack ID must name an existing directory.

## Not Implemented

There are no `/boundless toasts ...` commands in the current command tree. Toasts are controlled by `UI.enableQuestToasts` in settings/config.

## Safety

- Prefer a targeted quest/player command to `all` on production servers.
- Back up world progress and quest packs before bulk reset/redeem operations.
- Reward commands execute with elevated permission, so audit pack JSON before redeeming untrusted packs.
