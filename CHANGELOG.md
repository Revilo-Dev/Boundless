# Boundless: Quests Changelog

## 1.21.1-neo-12.1 — 2026-11-10

Update 12.1 focuses on questbook navigation, tree editing, sub-categories, editor consistency, formatting, icon selection, localization, and a large collection of UI fixes.

### Quest Book

- Added quest search to both the standalone questbook and inventory quest panel.
- Added the search icon inside the quest search field with corrected text alignment and spacing.
- Added mouse drag panning to tree layouts in the inventory questbook, matching the editor.
- Added right-click navigation in player tree views. Right-clicking empty tree space moves that point directly to the center of the view.
- Added `+` and numpad `+` shortcuts to zoom in.
- Added `-` and numpad `-` shortcuts to zoom out.
- Added `=` as a shortcut to center the current tree.
- Tree categories now center automatically when first opened.
- Tree zoom and pan are preserved when leaving and reopening a category.
- Selected quests remain selected when the questbook refreshes or rebuilds its widgets.
- Quest details now refresh immediately after returning from Quick Edit.
- Returning from Quick Edit reopens the quest that was being edited instead of returning to an unrelated screen.
- Fixed inventory questbook drag and release events not reaching the tree widget.
- Fixed a softlock configuration where both the questbook item/keybind and inventory quest panel could become inaccessible. Disabling the questbook item now keeps inventory access available.
- Removed sticky sub-category headings so headings no longer overlap scrolled quest content.
- Quest list titles now render their authored color and style formatting.
- Category tooltips now render formatted names without exposing raw formatting tokens.
- Quest tree tooltips render above inventory items and other foreground content.
- Quest node icons are vertically centered more accurately.
- Item, custom asset, emoji, and component-bearing quest icons now render consistently on quest nodes.

### Tree Views

- Added Shift-drag snapping for quest nodes.
- Shift-dragging can align nodes horizontally or vertically with nearby nodes.
- Shift-dragging can constrain movement to clean horizontal, vertical, or 45-degree angles.
- Added a second Move tool tooltip line explaining that Shift enables snapping.
- Selecting the tree background now clears the selected quest node.
- Dependency lines now highlight white while hovered instead of requiring a click.
- Reworked dependency connector rendering so 90-degree corners remain continuous.
- Fixed square artifacts and mismatched joins at dependency line corners.
- Improved connector outlines and inner-line alignment in both player and editor trees.
- Added tree selection synchronization when a quest is opened from another view.
- Improved tooltip depth for editor tree nodes, dependency prompts, and import warnings.

### Quick Edit

- Quick Edit now preserves the source quest and category.
- Exiting Quick Edit with Escape or Back returns directly to the edited quest.
- Quest data is hot-reloaded before the quest page is restored.
- Updated quest names, descriptions, objectives, rewards, icons, and tree selection are visible immediately after leaving Quick Edit.
- Fixed reload paths that returned to the tree without reopening the original quest.

### Categories and Sub-categories

- Reworked sub-category rows to use the standard Boundless quest widget appearance.
- Sub-category rows now show their icon before the formatted display name.
- Clicking a sub-category now opens its dedicated editor screen instead of expanding an inline editor underneath the row.
- Creating a sub-category now opens the same dedicated editor screen.
- Centered the compact `+` button used to create sub-categories.
- Fixed the sub-category display field so it can be focused, selected, typed in, and formatted normally.
- Sub-category display fields are now single-line, use larger text, and vertically center their contents.
- Added color and style formatting support to category and sub-category display names.
- Added translation support for sub-category display names.
- Added icon selection for sub-categories.
- Added duplicate display-ID validation for new sub-categories.
- Existing sub-category IDs remain immutable when editing their display name.
- Removed the redundant sub-category settings section from tree-category properties.
- Replaced the sub-category delete control with the standard reject/delete texture.
- Corrected sub-category creation, deletion, reordering, translation, and icon-picker behavior.

### Display Text and Formatting

- Quest, category, and sub-category display fields now use larger text.
- Single-line display text is vertically centered instead of top-aligned.
- Display fields support text selection and highlighting.
- Formatting tokens are interpreted visually inside display fields instead of being shown as raw `/r`, `/w`, and similar codes.
- Added formatting controls below a display field only while that field is focused.
- Separated title formatting controls from description controls.
- Focusing a display field no longer moves or detaches the description emoji, undo, or redo controls.
- Description formatting controls remain attached to the description editor.
- Display fields remain single-line while descriptions retain multiline editing.
- Translation title rows use the same single-line behavior.
- Translation delete controls now scale the complete 20×20 delete texture into their 16×16 controls without cropping.
- Replaced translation red-X text with the standard delete icon.

### Quest Objectives, Dependencies, and Rewards

- Reworked objective, dependency, and reward rows for consistent compact sizing.
- Unfocused completion and reward rows remain single-line and expand only while actively editing multiline content.
- Dependency rows remain single-line.
- Changing an entry type now clears the previous value and selected item/component data.
- Restored the objective type label `Advance`.
- Added localized labels for Collect, Submit, Kill, Advance, Effect, Observe, Check, Biome, Dimension, XP, LevelUP, Input, Item, Command, Loot table, and Toast entry types.
- Replaced text `X` controls for dependencies, completion entries, and rewards with the standard reject/delete texture.
- Correctly scales the complete 20×20 delete texture into each 16×16 row control.
- Correctly scales the complete 20×20 dependency lock texture into its 16×16 control.
- Fixed lock buttons stretching to the full height of expanded rows.
- Dependency input rows now use the full field width instead of a separate icon box.
- Dependency icons render inside the beginning of the text field.
- Dependency lock behavior is stored per dependency instead of forcing one shared state for every dependency.
- XP and LevelUP amount selectors use compact value-width dropdowns rather than stretching to the type selector width.
- Added a smaller yellow Unifont down-arrow to make dropdown fields clearer.
- Right-aligned dropdown values consistently.
- Added item-browser selection for Command and Toast icons.
- Added clearer localized placeholders for every supported completion and reward type.

### Icons and Item Browser

- Added an Inventory tab to the icon picker.
- Inventory icon selection preserves item components and NBT data.
- Quest icons can now store and render component-bearing item stacks.
- Added support for emoji and custom icons on quest nodes, not only registered items.
- Added dedicated picker tabs for items, item groups, item tags, mobs, effects, icons, block textures, full block textures, inventory items, and custom assets.
- Added multi-select support where supported by an entry type.
- Added an icon for the Custom Assets tab using `icons/style-icon.png`.
- Improved picker tab sizing, selected states, tooltips, filtering, and empty-state messages.
- Fixed icon rendering in editor rows, quest lists, player trees, and editor trees.

### Quest Media and Assets

- Added image previews to the quest media selector.
- Added a remove-media control to clear media from a quest description.
- Media selection uses the standard `Browse` label.
- Quest media supports static images and looping GIFs.
- Video files are not supported in this release.
- Improved the Asset Manager layout and Boundless panel styling.
- Added localized upload, rename, delete, loading, validation, usage, dimension, and empty-state messages.
- Added validation for unsupported extensions, duplicate names, invalid names, and non-square emoji images.
- Added separate Assets, Media, and Emojis views and filters.
- Updated the mod icon.
- Removed obsolete duplicated colored-theme textures.

### Background Editing

- Removed the redundant category-background Browse button because the background icon box already opens the picker.
- Expanded the background mode selector into the space freed by the removed button.
- Kept tile, custom tile, and full-image background selection available through the background field.
- Background selection clears the active quest-node highlight for a clearer preview.

### Settings and Localization

- Renamed the quest `Flags` section to `Options` and moved it below Rewards.
- Added settings search.
- Added localized settings names and descriptions for custom backgrounds, sticky description headers, quest rejection, and Quick Edit.
- Added localized color settings for description titles, requirement headings, objective types, rewards, completion state, and category unlocks.
- Localized editor labels, tooltips, placeholders, dialogs, validation messages, asset-manager text, tree actions, and sticker-editor text added in this update.
- Removed remaining hard-coded strings from the affected editor and asset-management paths.
- Updated the configuration documentation for questbook access behavior.
- Removed the obsolete sticky sub-category header setting.

### UI and Stability Fixes

- Standardized delete and lock controls around their 20×20 source textures without cropping or stretching.
- Corrected dropdown arrow size, color, and vertical position.
- Corrected quest search text alignment.
- Improved compact field alignment throughout the editor.
- Fixed description controls floating when a display field receives focus.
- Fixed title formatting controls disappearing after field conversion.
- Fixed sub-category display fields failing to receive mouse and keyboard input.
- Fixed category names leaking formatting codes into tooltips.
- Fixed selected quest state being lost during questbook rebuilds.
- Fixed quest details failing to refresh after editor reloads.
- Fixed player tree panning only working in the editor.
- Fixed background selection leaving a quest node highlighted.
- Fixed incomplete dependency connector corners.
- Fixed component and non-item icons failing to render on quest nodes.
- Cleaned and rebuilt the release output to ensure all `QuestDetailsPanel` nested classes are included in the mod jar.

### Technical Notes

- Added focused checks for dependency encoding, editor entry types, and tree snapping.
- Added client mouse-drag, mouse-release, and key events for the inventory quest panel.
- Improved tree-view state persistence and quest selection restoration.
- Added reusable search-field rendering for questbook views.
- Added localization keys for all new user-facing controls and messages.
- Version updated to `1.21.1-neo-12.1`.

