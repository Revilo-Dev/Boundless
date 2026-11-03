# Boundless: Quests

Boundless is a questing mod for Minecraft 1.21.1, built for modpacks that want quests to feel like part of the pack rather than an admin screen bolted onto it.

Players get a tidy quest book, clear progress tracking, optional objectives, pinned quests and rewards. Pack makers get an in-game editor for building everything from a simple chapter list to a full visual quest tree. You can work without hand-editing files, though the files are still there if that is your thing.


## For Players

- Open the quest book from its keybind, item or inventory button.
- Browse quests as a list or a visual tree.
- Search large questpacks and filter completed, rejected or locked quests.
- Follow item, submission, mob, advancement, effect, biome, dimension, XP and text-input objectives.
- Pin the quest you are working on so its progress stays visible.
- Claim item, XP, command, loot, advancement and toast rewards.
- Reject optional quests or restart repeatable ones when the pack allows it.
- Use quest completion scrolls to share progress in multiplayer.

## For creators

The quest editor is available to operators, create questpacks, manage assets and configs all in the operator tab

The GUI is configurable: text and icon scale, colours, category backgrounds, sticky headers, description wrapping and the location of the pinned quest HUD can all be adjusted.

### Questpack Editor

- Create list-based packs or visual quest trees.
- Create categories, sub-categories and quests.
- Format category and sub-category display titles without changing their generated IDs.
- Define dependencies, incompatibilities, optional paths and automatic completion.
- Reorder, duplicate, search and delete quests.
- Choose objectives like collect, kill, effects and other types easily from browsers.
- Add category backgrounds, quest-node styles, images, media, icons, emoji and background stickers to personalise questlines.
- Write alternate quest titles and descriptions for multiple languages.
- Quickly open a quest in the editor from its quest-book page with quick edit config.
- Export finished packs and keep up to five automatic backups per pack.

### FTB Quests Import

Boundless can import an existing FTB Quests questline from its quests folder or a chapter file. Titles, descriptions, dependencies, positions, tasks, rewards and available translations are carried across where possible. Anything that cannot be converted cleanly is marked on the affected quest and written to an import report.

Importing is best-effort—the two mods do not describe every task in the same way—but the original FTB files are never changed. That matters. Nobody enjoys discovering their only copy was the test copy.

## Multiplayer

The server owns quest progress and decides which questpacks and gameplay settings are active. Players receive the current quest data and progress when they join, while editor changes and reloads are synchronized for everyone.

Boundless should be installed on both the client and server. Operator tools and `/boundless` commands require operator permissions.

## Commands

| Command | Purpose |
|---|---|
| `/boundless reload` | Reload quest data and resync players. |
| `/boundless reset all [targets]` | Clear all quest progress. |
| `/boundless reset <id> [targets]` | Clear one quest's progress. |
| `/boundless complete all [targets]` | Mark every quest complete without rewards. |
| `/boundless complete <id> [targets]` | Mark one quest complete without rewards. |
| `/boundless redeem all [targets]` | Complete quests and grant their rewards. |
| `/boundless redeem <id> [targets]` | Complete one quest and grant its rewards. |
| `/boundless questpack list` | List available instance questpacks. |
| `/boundless questpack enable <id>` | Enable a questpack. |
| `/boundless questpack disable <id>` | Disable a questpack. |

If `[targets]` is left empty, the command affects the player running it.

## Configuration Reference

Most options are also available through the settings screen. Below list every entry in `boundless-common.toml`.

### Pack Selection

| Config | Default | Purpose |
|---|---:|---|
| `disabledQuestCategories` | `[]` | Category IDs hidden everywhere. |
| `appliedQuestPacks` | `[]` | Questpack IDs forced on by the server. |
| `disabledQuestPacks` | `[]` | Questpack IDs forced off by the server. |

### UI

| Config | Default | Purpose |
|---|---:|---|
| `UI.pinnedQuestHudPosition` | `bottom_left` | Pinned HUD corner: `top_left`, `top_right`, `bottom_left` or `bottom_right`. |
| `UI.hideQuestBookInInventory` | `false` | Hide the inventory quest-book button. |
| `UI.questBookInventoryButtonPosition` | `beside_recipe_book` | Place it beside the recipe book or above the offhand slot. |
| `UI.centerInventoryWithQuestPanel` | `true` | Centre the inventory and quest panel together. |
| `UI.hideCategoryHeader` | `false` | Hide the category banner. |
| `UI.filterDisplayMode` | `tabs` | Show filters as `tabs`, `buttons` or `hidden`. |
| `UI.disableCategories` | `false` | Remove category tabs and filtering. |
| `UI.showIdField` | `false` | Show editable IDs in the editor. |
| `UI.enableBuiltinQuestPack` | `true` | Enable the included Boundless questpack. |
| `UI.hideQuestWidgetIcons` | `false` | Hide icons in quest rows. |
| `UI.questTextScale` | `1.0` | Quest text scale, from `0.5` to `1.0`. |
| `UI.questIconScale` | `1.0` | Quest icon scale, from `0.5` to `1.0`. |
| `UI.enableQuestSearchBox` | `false` | Show search above the quest list. |
| `UI.expandTreeToggle` | `true` | Show the expand panel button on tree quest views. |
| `UI.customBackgrounds` | `true` | Show custom category backgrounds. |
| `UI.enableBackgroundThemes` | `true` | Allow category background themes on this client. |
| `UI.stickySubCategoryHeaders` | `true` | Keep the current sub-category heading visible. |
| `UI.stickyDescriptionHeaders` | `true` | Keep the quest title visible while reading. |
| `UI.enableDescriptionColors` | `true` | Allow colour formatting in descriptions. |
| `UI.questWidgetTextColor` | `FFFFFF` | Quest-row title colour. |
| `UI.descriptionTextColor` | `CFCFCF` | Default description colour. |
| `UI.descriptionTitleColor` | `FFFFFF` | Description title colour. |
| `UI.requiresTextColor` | `FF9F0F` | Requirement heading colour. |
| `UI.submitTextColor` | `1D9633` | Submit/input heading colour. |
| `UI.collectTextColor` | `1D9633` | Collection heading colour. |
| `UI.killTextColor` | `FF5555` | Kill heading colour. |
| `UI.effectTextColor` | `55FFFF` | Effect heading colour. |
| `UI.achieveTextColor` | `55FFFF` | Advancement heading colour. |
| `UI.observeTextColor` | `B46CFF` | Observation heading colour. |
| `UI.biomeTextColor` | `55CC55` | Biome heading colour. |
| `UI.dimensionTextColor` | `FF5555` | Dimension heading colour. |
| `UI.rewardTextColor` | `A8FFA8` | Reward heading and entry colour. |
| `UI.completedTextColor` | `55FF55` | Completed objective colour. |
| `UI.incompleteTextColor` | `FF5555` | Incomplete or invalid objective colour. |
| `UI.unlockTextColor` | `FFD700` | Category unlock colour. |
| `UI.enableDescriptionReadMore` | `true` | Collapse long descriptions behind Read More. |
| `UI.enableDescriptionTextWrapping` | `true` | Wrap descriptions to the panel width. |
| `UI.descriptionTextAlignment` | `left` | Use `left`, `center`, `right` or `adjust`. |
| `UI.enableQuestToasts` | `true` | Show quest-unlocked toasts. |

Colours use six-digit RGB hex values without `#`.

### Functionality

| Config | Default | Purpose |
|---|---:|---|
| `Functionality.disableQuestPinning` | `false` | Disable quest pinning and its HUD. |
| `Functionality.autoClaimQuestRewards` | `false` | Claim rewards as soon as a quest completes. |
| `Functionality.enableQuestRejecting` | `true` | Allow players to reject optional quests. |
| `Functionality.enableQuestScrolls` | `true` | Enable quest completion scrolls. |
| `Functionality.quickEdit` | `false` | Let operators edit a quest from the quest book. |
| `Functionality.multiLanguageQuests` | `true` | Use the client's matching quest translation. |
| `Functionality.preferredLanguage` | `client` | Choose a quest translation language or follow the client language. |
| `Functionality.questPackBackupInterval` | `5m` | Backup every `1m`, `5m`, `10m`, `30m` or turn it `off`. |

### Gameplay and Diagnostics

| Config | Default | Purpose |
|---|---:|---|
| `Gameplay.disableQuestBook` | `false` | Disable opening and obtaining the quest book. |
| `Gameplay.spawnWithQuestBook` | `false` | Give new players a quest book. |
| `Development.devMode` | `false` | Enable development diagnostics. Leave off for normal play. |
| `Development.debugQuestLoading` | `true` | Log quest loading timings while dev mode is on. |
| `Development.debugUiPerformance` | `true` | Log slow UI frames while dev mode is on. |
| `Development.debugNetworkTransfers` | `true` | Log questpack transfer details while dev mode is on. |
