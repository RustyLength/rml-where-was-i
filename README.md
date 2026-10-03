# Where Was I?

An account-specific adventure journal for RuneLite, designed by Rusty Medic Labs.

Leave yourself three reminders: **I was working on**, **My next steps**, and **Don't forget** (supplies, gear, or anything else). Existing next-step notes are preserved, and each RuneScape character has its own journal.

A small **on-screen reminder** shows your next step even when the sidebar is closed. Hold **Alt** and drag it using RuneLite's standard overlay controls. Turn off **On-screen journal reminder** in plugin settings to hide it.

After normal logout, **Logout journal reminder** opens the journal with a gentle prompt. Logout is never blocked or delayed. You can still edit the character's journal on the login screen; the next login loads the appropriate character's saved notes. Turn off the logout setting to stop automatically opening the sidebar.

**Last session** shows XP gained by skill while the plugin was enabled. World hops keep the same session; disabling and enabling the plugin starts another session. XP is checkpointed every 30 seconds and saved on logout and normal client close. Force-closing RuneLite may lose recent changes. The old saved-location timestamp is retained for the first recap; the map preview and pin have been removed.

All journal data uses RuneLite configuration storage. No third-party requests or uploads.

## Development

Java 11-compatible plugin with the official example-plugin Gradle structure.

```powershell
git pull
.\gradlew.bat run --console=plain
```

On Linux/macOS use `./gradlew run`. For Jagex Accounts, follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Never share account credentials.

## Manual checks

1. Confirm your existing next-step note remains. Add an activity and supplies.
2. Close the sidebar: the overlay should still show the next step. Hold Alt and drag it, then test hiding it through plugin settings.
3. Gain a little XP, then log out normally. Logout should proceed immediately, the journal should open, and the recap should show the XP gained.
4. Edit a reminder while logged out, then log back in: the edit should remain. Switch between main and iron characters: journals should stay separate.
5. World-hop: the journal and current session should remain. Disable the logout reminder and confirm normal logout doesn't open the sidebar.
6. Close and relaunch normally to check persistence and the previous session recap.

Build and unit tests do not establish in-game correctness; these checks require the user's confirmation. This plugin is not yet published to the Plugin Hub.
