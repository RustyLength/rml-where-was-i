# Where Was I? — Rusty Medic Labs

An account-specific reminder sidebar for RuneLite. This first development version provides:

- A gold question-mark sidebar button and RML branding.
- A next-steps note that saves as you type, separately for each RuneScape profile.
- The previous saved tile coordinates, floor, world and time.
- A location checkpoint every 30 seconds and on logout, client close or plugin disable.
- An optional login message pointing you to the sidebar.

RuneLite's ConfigManager stores the data. The plugin sends no HTTP requests. Normal RuneLite profile sync settings apply. Notes belong to a RuneScape profile, including its game mode, and do not depend on a display name. The previous-visit card stays fixed during a session and refreshes on the next login. World hopping does not start a new session. A forced process termination can lose changes since RuneLite's last disk flush.

## Run locally

Use Java 11 and run `./gradlew run` (`.\gradlew.bat run` on Windows). Jagex-account users should follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Manual acceptance check

1. Log in, enable **Where Was I?**, and open the gold **?** sidebar button.
2. Type a note, log out and back in. Confirm the note, previous location, world and time.
3. Walk to another location, then close RuneLite normally. Relaunch and verify the last location.
4. Switch accounts; verify each has its own note and that the login screen disables editing.
5. Hop worlds; verify your note and previous-visit card stay unchanged.
6. Disable/re-enable the plugin and verify there is only one sidebar button and the note survives.
7. Turn the welcome message off and verify a new login produces no plugin chat reminder.

`./gradlew build` runs offline model/Swing checks; these do not verify in-game behaviour.

## Planned next stages

The approved OSRS character artwork, mini-map preview, clickable world map and dungeon entrance mapping are not implemented in this first version. Inventory/equipment snapshots and session history will follow after the initial account/location checks.

Not yet published to the Plugin Hub. Not affiliated with Jagex or RuneLite.
