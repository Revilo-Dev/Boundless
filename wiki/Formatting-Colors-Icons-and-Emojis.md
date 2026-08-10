# Formatting, Colors, Icons, and Emojis

## Description Formatting Tokens

Quest descriptions recognize two-character tokens when `UI.enableDescriptionColors` is enabled. A token changes formatting from that point forward and is not displayed.

| Token | Effect | RGB |
|---|---|---|
| `/w` | Aqua | `55FFFF` |
| `/r` | Red | `FF5555` |
| `/g` | Green | `55FF55` |
| `/b` | Blue | `5555FF` |
| `/y` | Yellow | `FFFF55` |
| `/o` | Orange/gold | `FFAA00` |
| `/a` | Gray | `AAAAAA` |
| `/p` | Purple | `AA55FF` |
| `/l` | Toggle bold | — |
| `/i` | Toggle italic | — |
| `/e` | Toggle obfuscated/encrypted text | — |
| `/h` | Toggle animated highlight/shine | — |
| `/x` | Reset color and every style to defaults | configured default |

Example:

```text
/yWarning: /xBring /lthree diamonds/l to the altar. /hRare reward!/h
```

Tokens are case-insensitive. Style tokens toggle their state, so repeat `/l`, `/i`, `/e`, or `/h` to turn that style off. `/x` turns all styles off. When formatting is disabled, recognized tokens are stripped and the description uses `UI.descriptionTextColor`.

## Configurable Hex Colors

- `UI.questWidgetTextColor` controls normal quest-list titles.
- `UI.descriptionTextColor` is the description fallback and `/x` reset color.
- Values accept exactly six hexadecimal digits, with or without a leading `#` in the settings field; saved values are normalized without `#`.
- Locked quest names still use the built-in disabled gray rather than the configured quest-title color.

## Text Layout

- `UI.questTextScale`: `0.5`–`1.0` for list titles and detail text.
- `UI.questIconScale`: `0.5`–`1.0` for list/detail icons.
- `UI.enableDescriptionTextWrapping`: wrap to panel width or leave text unwrapped.
- `UI.descriptionTextAlignment`: `left`, `center`, `right`, or `adjust` (justified spacing).
- `UI.enableDescriptionReadMore`: collapses long descriptions behind a toggle.
- Explicit newline characters in the JSON string are preserved as line breaks.

## Icons

Quest, category, sub-category, command-reward, function-reward, loot-reward, and toast-reward icons normally use a namespaced item ID such as `minecraft:diamond`. Invalid or missing IDs fall back where the UI provides a fallback.

Quest icons may also refer to a texture resource path recognized by the UI. Texture paths do not resolve as item stacks, so places that require an item icon can use a fallback. Pack display icons use `boundless.icon_path` in `boundless/pack.json`, while `pack.png` is written by the editor for the pack itself.

Tree quest nodes support these `tree_background` values:

- `square`
- `rounded_square`
- `circle`
- `diamond`

Any other value becomes `square`.

## Item Tags and Components

Item objectives support:

- Item IDs: `minecraft:diamond`
- Item tags: `#minecraft:logs`
- Component-qualified item syntax using `{...}` or `[...]` after the ID

For tag targets, any item in the tag can match. For component-qualified targets, the stack must also match the serialized component data.

Item rewards support concrete item IDs and optional components. Do not use a tag as a reward entry because a tag does not identify the concrete item stack to grant.

## Emojis and Symbols

Boundless has no named emoji shortcode table and does not translate values such as `:star:`. Literal Unicode characters can be stored in JSON names/descriptions, but display depends on the active Minecraft font/resource packs and unsupported glyphs may render as boxes. The `/w`–`/x` sequences above are formatting tokens, not emojis.

For reliable cross-client presentation, prefer Minecraft item/texture icons and ordinary text. If a pack requires custom emoji-like glyphs, ship an appropriate resource pack/font and insert those literal characters in the quest text.
