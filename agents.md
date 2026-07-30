# Boundless Agent Notes

## Quest-Pack Authority Rules

- Treat quest-pack definitions and player quest progress as separate data systems.
- Synchronizing quest-pack definitions must not overwrite valid player progress unless the new structure makes that progress invalid.
- Use stable IDs for quest packs, categories, quests, objectives, and rewards where needed. Never key progress by list order, GUI order, display title, or file order.

## Authority By Environment

- Singleplayer: the local configuration folder is authoritative. Enabled local quest packs must load into every singleplayer world, including existing worlds.
- LAN: the host configuration is authoritative for the whole session. Joining clients must use the host’s enabled quest-pack definitions and must ignore their own local versions for that session.
- Dedicated server: the server configuration is authoritative. Clients must use server-provided quest-pack definitions for validation, progression, rewards, dependencies, and display.

## Remote Definition Rules

- Remote quest-pack definitions on LAN clients and dedicated-server clients are temporary session data.
- Never permanently copy remote quest-pack definitions into the client configuration folder.
- Never replace local quest-pack files with host/server versions.
- Never modify the client’s enabled-pack configuration because of a LAN/server connection.
- On disconnect, clear temporary remote definitions, revision metadata, and session-only caches, then restore the normal local registry.

## Progress Rules

- Singleplayer progress must persist per world and restore on reopen.
- LAN progress must persist on the host authority and reconnect to the host-provided quest-pack definition by stable IDs.
- Dedicated-server progress must persist on the server and restore on reconnect. Client caches must not be the only source of multiplayer progress.
- Claimed rewards must not become claimable again after reconnect unless a structural migration explicitly requires it.

## Runtime Editing And Sync

- Runtime quest-pack edits on dedicated servers are server-authoritative.
- Validate edited quest-pack definitions before replacing the active authoritative version.
- After an accepted edit, update the server-side stored definition, refresh in-memory authoritative data, advance a revision/hash, and synchronize the new definition to all affected clients.
- Prefer full authoritative snapshots over partial updates when correctness is more important than bandwidth.
- All connected clients on the same authority must converge on the same quest-pack revision.
- Clients must not continue using stale quest-pack data after the authority accepts an edit.

## Validation Requirements

- Validate pack IDs, unique quest IDs, unique category IDs, objective validity, dependency validity, circular dependency safety, reward validity, revision/hash generation, serialization/deserialization, and safe progress mapping before accepting a synchronized or edited pack.
- An invalid edited pack must not replace the last valid authoritative server version.

## Non-Negotiable Invariants

- Singleplayer uses locally enabled quest packs.
- LAN uses the host’s enabled quest packs.
- Dedicated servers use server-enabled quest packs.
- Joining clients cannot override authoritative LAN/server definitions.
- Remote quest-pack definitions are temporary on clients.
- Receiving a remote quest pack must not overwrite client configuration files.
- Player progress is separate from quest-pack definitions.
- LAN and server progress must survive disconnect/reconnect.
- Runtime server edits must synchronize to every connected client.
- All clients on the same authority must use the same quest-pack revision.
- Authority validates progression, completion, dependencies, and rewards.
- Client displays must reflect the authority’s current definition and progress state.
