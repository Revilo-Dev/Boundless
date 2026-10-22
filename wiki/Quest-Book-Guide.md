# Quest Book Guide

## Opening the UI

Open Boundless with the Quest Book item, the inventory button, or the **Open Quest Book** keybind (`]` by default). All three routes obey the relevant config options.

## Navigation and Layouts

Quest packs choose a `list` or `tree` layout in `boundless/pack.json`.

- **List:** categories filter the quest list; sub-category headers group and collapse quests.
- **Tree:** quests are placed at `tree_x`/`tree_y` coordinates with dependency lines and one of four node backgrounds.

The synthetic **All** category is always sorted first. A category can opt out of this view with `excludeFromAll`, and a category dependency can hide it until the named quest is redeemed.

## Search, Categories, and Filters

- Search matches quest names when `UI.enableQuestSearchBox` is enabled.
- `UI.filterDisplayMode` selects `tabs`, `buttons`, or `hidden` filter controls.
- `UI.disableCategories` removes category filtering entirely.
- `disabledQuestCategories` removes matching category IDs from questing, not just from the tab bar.
- Sticky sub-category headers and the sticky quest title are independently configurable.

## Quest Details

The details panel can show:

- Name, icon, formatted description, and dependency state.
- Every objective and live progress.
- Accepted alternatives for item or mob targets.
- Text-entry controls and manual check acknowledgements.
- Reward previews, including custom titles and icons.
- Claim, reject, repeat, pin/unpin, and scroll actions when eligible.

## Dependencies and Visibility

Normal dependencies unlock only after every dependency has been **claimed** at least once. A completed-but-unclaimed prerequisite does not count.

`lock_after_dependency: true` is inverse behavior: the quest is usable only before any listed dependency has ever been claimed. `hiddenUnderDependency: true` hides, rather than merely locks, a quest when its dependency rule is not satisfied.

## Optional and Repeatable Quests

- Only quests with `optional: true` can be rejected.
- Rejection changes the status to `REJECTED`; operators can reset it with `/boundless reset <id> [targets]`.
- A quest with `repeatable: true` can be restarted after redemption.
- Claim history is retained, so dependencies remain unlocked even while a repeatable quest begins another cycle.

## Pinning

- Use the pin icon in the sticky quest header.
- At most three quests are stored. Pinning a fourth removes the oldest.
- Pins persist in `config/boundless/pins/<world-or-server>.json` and are separated by singleplayer world name or multiplayer address.
- The HUD is hidden while another screen is open.
- Redeemed and rejected quests are removed automatically.
- HUD corner choices are `top_left`, `top_right`, `bottom_left`, and `bottom_right`.
- Logging out clears the active pin file in the current implementation; pinning can also be fully disabled.

## Completion Scrolls

When `Functionality.enableQuestScrolls` is enabled, a quest that has been claimed can create a scroll once per player/quest. The scroll action occupies the same header slot as pinning when it is available. Using the scroll follows the server-validated completion-scroll flow.

## Display Options

Text/icon scale, quest row icons, category headers, description wrapping/alignment, Read More, default colors, inline styles, and toast notifications are all configurable. See [Settings Guide](Settings-Guide) and [Formatting, Colors, Icons, and Emojis](Formatting-Colors-Icons-and-Emojis).
