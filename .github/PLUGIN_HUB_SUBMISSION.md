# Plugin Hub submission

Title: Add Where Was I? adventure journal

## Pull request description

Where Was I? helps players resume their plans with a journal saved separately for each RuneScape character. It provides activity and supplies notes, tick-off next steps, a movable overlay showing the first unfinished step, optional login/logout sidebar reminders, and the latest 20 session XP recaps.

Existing notes remain intact. Logout is never delayed or intercepted; the player can update the journal on the login screen. World hops keep the same XP session. All data uses RuneLite configuration storage. The plugin makes no third-party requests and does not automate game input or actions.

Validation: Java 11 build and unit tests pass in GitHub Actions. The author has tested checklist progression, normal logout, XP recaps, moving the overlay, notes and ticks surviving logout and a full restart, and expanded session history. Main/iron isolation and all settings toggles are awaiting final author confirmation.

BSD-2-Clause licensed. Uses the standard Plugin Hub build with no additional runtime dependencies.

Generated-by: OpenAI Codex

## Manifest

In a branch of your fork of `runelite/plugin-hub`, create `plugins/where-was-i` with:

```properties
repository=https://github.com/RustyLength/rml-where-was-i.git
commit=FULL_TESTED_40_CHARACTER_COMMIT_SHA
```

Use the latest tested commit at submission time. The manifest is the only change to the Plugin Hub fork. Submit against `runelite/plugin-hub:master` and check the Plugin Hub build/review results.

## Final in-game checks

- Switch main → iron → main: notes, ticks and history must stay with each character.
- Disable On-screen journal reminder: overlay disappears; enable it again: it returns.
- Disable Open journal on login: logging in does not open the journal; enable it: it opens again.
- Disable Logout journal reminder: logout does not open the journal; enable it: it opens after logout without delaying logout.

Update the validation paragraph after these checks are confirmed. Do not describe this plugin as available in the Plugin Hub until the maintainers approve and merge the submission.
