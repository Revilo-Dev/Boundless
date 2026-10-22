# Getting Started

## Requirements

Install Boundless and its required NeoForge/Minecraft dependencies on the game or server. Clients connecting to a Boundless server should use the matching mod version.

## Opening the Quest Book

Use any enabled entry point:

- Press `]` by default. Rebind **Open Quest Book** in Minecraft Controls.
- Use the `Quest Book` item. Its recipe is included with the mod.
- Click the quest button in the inventory when `UI.hideQuestBookInInventory` is `false`.

The keybind and item are blocked when `Gameplay.disableQuestBook` is `true`. New players receive a book only when `Gameplay.spawnWithQuestBook` is enabled.

## Quest Flow

1. Select a category and, for list packs, a sub-category.
2. Open an unlocked quest and read all objectives.
3. Complete **every** objective in the quest.
4. Claim the quest when it becomes `COMPLETED`, unless it auto-claims.
5. Receive its rewards and unlock dependent content.

Quest states are:

- `INCOMPLETE` — objectives are not all satisfied.
- `COMPLETED` — objectives are ready and rewards have not been claimed.
- `REDEEMED` — rewards were claimed.
- `REJECTED` — an optional quest was declined.

On multiplayer, progress and reward redemption are controlled by the server.

## Special Quest Behavior

- **Optional:** can be rejected. Rejected quests disappear from normal lists.
- **Repeatable:** after redemption, can be restarted for a new progress cycle.
- **Auto-complete:** actually means auto-claim; rewards are granted as soon as the quest is ready.
- **Hidden under dependency:** invisible until its dependency rule is satisfied.
- **Lock after dependency:** reverses normal dependency logic; the quest is available only while none of its listed dependencies has ever been claimed.
- **Multiple dependencies:** every listed dependency must satisfy the applicable dependency rule.

## Useful Player Features

- Search is available when `UI.enableQuestSearchBox` is enabled.
- Pin up to three quests from the quest detail header. The oldest pin is replaced by a fourth.
- A completed/rejected quest automatically leaves the pinned HUD.
- A previously claimed quest can create one completion scroll when scrolls are enabled.
- Long descriptions can use **Read More**, wrapping, alignment, color, and style tokens.

## First Checks

- No book access: check `Gameplay.disableQuestBook`, the inventory-button setting, and the keybind.
- No search box: enable `UI.enableQuestSearchBox`.
- Quest is locked: claim all normal dependencies; merely reaching `COMPLETED` is not enough.
- Manual pack edits do not appear: run `/boundless reload` as an operator.
