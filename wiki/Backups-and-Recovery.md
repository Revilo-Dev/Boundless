# Backups and Recovery

## Automatic Backup Location

Per-pack backups are stored under:

`config/boundless/backups/questpacks/<pack-id>/`

Normal snapshots use timestamped folders such as:

`yyyy-MM-dd_HH-mm-ss-<reason>`

The editor shows these under **Recent Backups** in pack options.

## When Backups Are Created

Boundless snapshots packs around editor and storage operations, including saved/updated content, enable-state changes, replacement/upload operations, and restores. JSON writes use a temporary `.saving` file and then an atomic replace when the filesystem supports it.

## Retention

- The normal snapshot limit is fixed at **5 per pack**.
- Newest timestamped snapshots are retained.
- Folders whose names start with `_` are excluded from normal snapshot pruning.

## Replacement Archives

When a live pack directory is replaced, the prior directory is moved under:

`config/boundless/backups/questpacks/<pack-id>/_replaced/<timestamp>-<reason>/`

These replacement archives are separate from the five normal snapshots. If a replacement fails, Boundless attempts to move the archived live pack back into place.

Individual files displaced by some operations may also be archived beneath `_replaced` while preserving their relative path.

## Restoring in the Editor

1. Open **Quest Settings** as an operator.
2. Open **Quest Editor** and select the pack.
3. Open pack options and find **Recent Backups**.
4. Click a backup, verify the timestamp, and confirm restore.
5. Boundless stages a copy, archives the current live pack, swaps in the selected snapshot, and reloads quest data.

Only snapshots located under the selected pack's backup root are accepted.

## Staging Recovery

Uploads and restores use hidden staging folders. At pack discovery/load time, Boundless attempts to recover completed uploads left by an interrupted older operation. Directories beginning with `.` or ending in `.upload`, `.tmp`, or `.temp` are hidden from the normal pack list.

## Exports Are Separate

The editor's **Export** button creates zip files in:

`config/boundless/exports/`

Exports are portable archives, not part of automatic retention. Existing exports are never overwritten; a numeric suffix is added. Keep exports in external backups or version control for long-term history.

## Manual Recovery

If the UI cannot restore a pack:

1. Stop the server/game before moving directories.
2. Copy the broken live pack elsewhere.
3. Copy a chosen timestamped snapshot into `config/boundless/questpacks/<pack-id>/`.
4. Confirm that `data/<namespace>/quests/` and metadata are directly inside that directory, not nested in another copy of `<pack-id>`.
5. Start the server/game and run `/boundless reload`.

Do not edit the only backup copy in place.
