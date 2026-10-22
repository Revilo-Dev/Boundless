# Settings Guide

Boundless registers a common NeoForge config at `config/boundless-common.toml`. The in-game settings screen exposes the player-facing options and sends authoritative changes to the server when permitted.

## Root Options

| Key | Default | Effect |
|---|---|---|
| `disabledQuestCategories` | `[]` | Category IDs removed from loading, visibility, and progression. Matching is case-insensitive after trimming. |
| `appliedQuestPacks` | `[]` | Pack IDs explicitly forced on. IDs are normalized to lowercase. |
| `disabledQuestPacks` | `[]` | Pack IDs explicitly forced off; this list wins over `appliedQuestPacks`. |

## UI Options

| Key | Default / Values | Effect |
|---|---|---|
| `UI.pinnedQuestHudPosition` | `bottom_left` | Pinned HUD corner: `top_left`, `top_right`, `bottom_left`, or `bottom_right`. Invalid runtime values fall back to `bottom_left`. |
| `UI.hideQuestBookInInventory` | `false` | Hides the inventory quest button. |
| `UI.questBookInventoryButtonPosition` | `beside_recipe_book` | `beside_recipe_book` or `above_offhand_slot`. |
| `UI.centerInventoryWithQuestPanel` | `true` | Centers the inventory and its open quest panel as one combined layout. |
| `UI.hideCategoryHeader` | `false` | Hides the category banner above quest content. |
| `UI.filterDisplayMode` | `tabs` | Filter controls: `tabs`, `buttons`, or `hidden`. |
| `UI.disableCategories` | `false` | Removes category tabs/filtering; does not equal `disabledQuestCategories`. |
| `UI.enableBuiltinQuestPack` | `true` | Loads the quests shipped in the `boundless` namespace. |
| `UI.hideQuestWidgetIcons` | `false` | Removes icons from quest rows. |
| `UI.questTextScale` | `1.0` (`0.5`–`1.0`) | Scales quest list titles and description/objective/reward text. |
| `UI.questIconScale` | `1.0` (`0.5`–`1.0`) | Scales quest list and detail icons. |
| `UI.enableQuestSearchBox` | `false` | Displays quest-name search above the quest list. |
| `UI.stickySubCategoryHeaders` | `true` | Keeps the active sub-category header visible while a list scrolls. |
| `UI.stickyDescriptionHeaders` | `true` | Keeps the quest title/header fixed above long details. |
| `UI.enableDescriptionColors` | `true` | Parses `/w`, `/r`, `/l`, and other Boundless formatting tokens. When off, tokens are stripped. |
| `UI.questWidgetTextColor` | `FFFFFF` | Six-digit RGB color for normal quest row titles. A leading `#` is accepted by validation. |
| `UI.descriptionTextColor` | `CFCFCF` | Six-digit default description RGB color and `/x` reset color. |
| `UI.enableDescriptionReadMore` | `true` | Collapses long descriptions behind Read More/Less. |
| `UI.enableDescriptionTextWrapping` | `true` | Wraps descriptions to the detail panel width. |
| `UI.descriptionTextAlignment` | `left` | `left`, `center`, `right`, or `adjust` (justified). |
| `UI.enableQuestToasts` | `true` | Shows quest-unlocked toast notifications. |

## Functionality Options

| Key | Default | Effect |
|---|---|---|
| `Functionality.disableQuestPinning` | `false` | Disables the pin button, saved pins, and pinned HUD. |
| `Functionality.autoClaimQuestRewards` | `false` | Automatically redeems every quest as soon as it becomes ready. Quest/category `auto_complete` also forces auto-claim. |
| `Functionality.enableQuestScrolls` | `true` | Allows eligible completion scroll creation and use. |

## Gameplay Options

| Key | Default | Effect |
|---|---|---|
| `Gameplay.disableQuestBook` | `false` | Blocks quest book item use and the keybind. |
| `Gameplay.spawnWithQuestBook` | `false` | Gives new players a Quest Book on spawn. |

## Development Option

| Key | Default | Effect |
|---|---|---|
| `Development.devMode` | `false` | Enables rate-limited Boundless performance diagnostics in the log. Keep off during normal play. |

## Related Fixed Values

- Automatic normal backup retention is fixed at five snapshots per quest pack; it is not a TOML option.
- Pin capacity is fixed at three quests.
- The default quest-book key is `]` and is changed in Minecraft Controls, not in the TOML file.

## Authority and Pack State

On multiplayer, the server is authoritative for quest content, progress, rewards, pack enable state, and shared behavior. The client receives server quest/config snapshots. Local visual behavior is applied immediately by the settings screen where appropriate, but server policy wins for authoritative features.

## Applying Changes

- Changes saved through the settings screen are persisted and the UI is refreshed.
- After manual TOML or quest-pack edits, use `/boundless reload` for quest content and reconnect/restart if a NeoForge config value has not been picked up.
- If a pack metadata `enabled` value appears ignored, check `appliedQuestPacks` and `disabledQuestPacks` first.
