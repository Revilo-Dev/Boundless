# Using Questpacks on Servers

This page shows the simple way to move questpacks between servers.

## Where questpacks go

Put questpacks in:

`config/boundless/questpacks/`

This is inside your server folder.

## Export a pack from client

1. Join world with operator permissions.
2. Open the Boundless Quest Editor.
3. Select the pack you want.
4. Click **Export**.
5. Boundless opens packs directory, copy this file.


## Use that pack on a server

1. Copy the exported pack to the server config folder directory.
2. Place it in `config/boundless/questpacks/`.
3. Start the server (or keep it running).
4. Run `/boundless reload`.
5. Make sure the pack is enabled.

## Quick check

- The pack appears in questpack tools/commands.
- Quests and categories appear in-game.
- Quest progress works as expected.

## If it does not show up

- Check the folder path again.
- Check that the pack is enabled.
- Run `/boundless reload` again.
