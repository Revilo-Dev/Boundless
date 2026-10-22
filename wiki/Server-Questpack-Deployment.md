# Using Questpacks on Servers

## Server Location

Install unpacked quest packs in:

`<server>/config/boundless/questpacks/<pack-id>/`

The pack directory itself must directly contain `boundless/pack.json` and/or `pack.mcmeta`, plus `data/<namespace>/quests/`. Do not leave an extra nested `<pack-id>/<pack-id>/` level after extracting a zip.

## Deploy an Export

1. In the client/editor, select the pack and click **Export**.
2. Find the generated zip in `config/boundless/exports/`.
3. Stop the destination server for the safest filesystem deployment.
4. Extract the zip into `config/boundless/questpacks/<pack-id>/`.
5. Start the server.
6. Run `/boundless questpack list`.
7. If needed, run `/boundless questpack enable <pack-id>`.
8. Run `/boundless reload` after any live manual changes.
9. Join with a matching Boundless client and test progression/rewards.

## Multiplayer Editor Upload

An authorized client editor can zip the selected pack and upload it to the server in approximately 60 KB chunks. The server stages the upload, archives any current live pack, installs the completed upload, applies enabled state, reloads, and syncs players. Interrupted staging directories are recovered or hidden by pack discovery logic.

## Enable-State Rules

Server config overrides pack metadata:

1. `disabledQuestPacks` forces off.
2. `appliedQuestPacks` forces on if not forced off.
3. Otherwise the pack metadata default is used.

Use `/boundless questpack enable|disable` rather than editing both lists manually. The built-in pack has its own `UI.enableBuiltinQuestPack` switch.

## Server Authority

The server controls loaded definitions, progression, item/XP submission, status, reward delivery, scroll state, and pack toggles. It synchronizes definitions and progress to clients. Keep server and client versions aligned to avoid incompatible payloads or UI expectations.

## Updating a Live Pack

1. Export and retain the known-good version.
2. Make/test changes in a staging world or server.
3. Deploy through the editor upload or replace the server directory while stopped.
4. Run `/boundless reload`.
5. Verify category visibility, dependencies, existing player progress, submit targets, and privileged reward commands.
6. Confirm a timestamped backup exists in `config/boundless/backups/questpacks/<pack-id>/`.

## Validation Checklist

- `/boundless questpack list` shows the expected effective state.
- No JSON/load warnings appear in the server log.
- Categories/sub-categories are present and ordered.
- Locked quests unlock only after prerequisites are claimed.
- Objective progress updates on the server.
- Rewards are delivered once and overflow behavior is acceptable.
- Untrusted packs contain no unsafe command/function rewards.
