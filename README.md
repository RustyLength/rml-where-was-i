# Where Was I? — Rusty Medic Labs

An account-specific reminder sidebar for RuneLite. This first development version provides:

- A folded-map sidebar badge for Where Was I?, with a small Rusty Medic Labs designer credit in the footer.
- A next-steps note that saves as you type, separately for each RuneScape profile.
- The previous saved tile coordinates, floor, world and time.
- A saved terrain preview with a gold location dot; click to open and centre the world map.
- A gold pin on the world map, anchored at the saved tile.
- A location checkpoint every 30 seconds and on logout, client close or plugin disable.
- An optional login message pointing you to the sidebar.

RuneLite's ConfigManager stores notes and visit metadata; terrain previews are local files. The plugin sends no HTTP requests. Normal RuneLite profile sync settings apply to notes and visit metadata. Notes belong to a RuneScape profile, including its game mode, and do not depend on a display name. The previous-visit card stays fixed during a session and refreshes on the next login. World hopping does not start a new session. A forced process termination can lose changes since RuneLite's last disk flush.

## Run locally

Use Java 11 and run `./gradlew run` (`.\gradlew.bat run` on Windows). Jagex-account users should follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Manual acceptance check

1. Log in, enable **Where Was I?**, and open its folded-map sidebar button.
2. Type a note, log out and back in. Confirm the note, previous location, world and time.
3. Walk to another location, then close RuneLite normally. Relaunch and verify the last location.
4. Switch accounts; verify each has its own note and that the login screen disables editing.
5. Hop worlds; verify your note and previous-visit card stay unchanged.
6. Disable/re-enable the plugin and verify there is only one sidebar button and the note survives.
7. Turn the welcome message off and verify a new login produces no plugin chat reminder.

`./gradlew build` runs offline model/Swing checks; these do not verify in-game behaviour.

## Planned next stages

The approved OSRS character artwork, inventory/equipment snapshots and session history are still planned.

Terrain previews are stored locally in RuneLite's plugin data directory using Filepath, separately for each account. File operations and PNG encoding run off the client thread. Terrain is rendered once after a scene load or floor change, rather than scanned every tick. The image and its coordinates are stored together. The sidebar remains fixed to the previous visit during the current session.

If a preview is missing on an initial install, it can be rebuilt for the saved tile when that tile is in the currently loaded ordinary scene. Other regions and instances are not guessed. Capture, storage and loading failures show a message in the sidebar and diagnostic details in the RuneLite debug log.

Recorded dungeon entries initially support Brimhaven north/south, Taverley, Lumbridge Swamp Caves, Edgeville main/shed and Catacombs of Kourend. The plugin must observe you walking through an entrance; logging in underground or teleporting there does not guess an entrance. The preview shows the actual dungeon tile; clicking targets the recorded surface entrance. Unsupported dungeon coordinates may not be present on the world map.

Map acceptance checks: after updating, log in, log out and back in to create the first preview. Verify the gold dot matches the saved terrain and click it with the world map closed, then open. Walk elsewhere and hop worlds: the previous preview should remain fixed. Close normally and relaunch; verify persistence and switch accounts to check isolation. For an entrance test, enter Brimhaven from the surface while the plugin is enabled, log out inside, return, and click the preview: verify the surface entrance is marked. Starting underground without an observed entry must not invent one.

Not yet published to the Plugin Hub. Not affiliated with Jagex or RuneLite.
