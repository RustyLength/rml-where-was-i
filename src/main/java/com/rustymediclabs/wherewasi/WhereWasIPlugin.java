package com.rustymediclabs.wherewasi;

import com.google.inject.Provides;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Where Was I?",
	internalName = "where-was-i",
	description = "Pick up where you left off with an account-specific adventure journal",
	tags = {"notes", "journal", "reminder", "goals", "rml"}
)
public class WhereWasIPlugin extends Plugin
{
	static final String NEXT_STEPS = "nextSteps";
	static final String ACTIVITY = "currentActivityV1";
	static final String SUPPLIES = "rememberSuppliesV1";
	static final String DONE = "completedStepsV1";
	static final String UPDATED = "journalUpdatedV1";
	private static final String HISTORY = "sessionHistoryV1";
	private static final String RECAP_KEY = "sessionRecapV1";
	private static final long CHECKPOINT_MILLIS = 30_000;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ConfigManager configManager;
	@Inject private WhereWasIConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private JournalReminderOverlay reminderOverlay;
	private volatile String reminderText = "Leave yourself a next step";
	private volatile boolean running;
	private volatile WhereWasIPanel panel;
	private volatile NavigationButton navigation;
	private volatile String activeProfile;
	private volatile SessionRecap currentRecap;
	private volatile long session;
	private int[] baselineXp;
	private long lastCheckpoint;
	private long sessionId;
	private volatile SessionHistory history = SessionHistory.decode(null);

	@Provides
	WhereWasIConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(WhereWasIConfig.class);
	}

	@Override
	protected void startUp()
	{
		running = true;
		overlayManager.add(reminderOverlay);
		SwingUtilities.invokeLater(() ->
		{
			if (!running) { return; }
			panel = new WhereWasIPanel((profile, key, text) ->
				// Explicit profile keeps post-logout edits attached to the correct character.
				{
					configManager.setConfiguration(WhereWasIConfig.GROUP, profile, key, text);
					configManager.setConfiguration(WhereWasIConfig.GROUP, profile, UPDATED, System.currentTimeMillis());
					if (Objects.equals(profile, activeProfile) && (NEXT_STEPS.equals(key) || DONE.equals(key)))
					{
						updateReminder(profile);
					}
				});
			navigation = NavigationButton.builder().tooltip("Where Was I?")
				.icon(WhereWasIPanel.createIcon()).priority(8).panel(panel).build();
			clientToolbar.addNavigation(navigation);
			clientThread.invoke(this::captureSession);
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		overlayManager.remove(reminderOverlay);
		saveRecap();
		session++;
		NavigationButton old = navigation;
		if (old != null) { clientToolbar.removeNavigation(old); }
		navigation = null;
		panel = null;
		activeProfile = null;
		currentRecap = null;
		baselineXp = null;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		captureSession();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGIN_SCREEN || activeProfile == null) { return; }
		saveRecap();
		SessionRecap ended = currentRecap;
		SessionHistory endedHistory = history;
		String profile = activeProfile;
		activeProfile = null;
		currentRecap = null;
		baselineXp = null;
		long endedSession = ++session;
		WhereWasIPanel target = panel;
		boolean remind = config.logoutReminder();
		SwingUtilities.invokeLater(() ->
		{
			if (!running || panel != target || target == null || session != endedSession) { return; }
			target.showSessionEnded(profile, ended, remind);
			target.showHistory(endedHistory);
			if (remind && navigation != null) { clientToolbar.openPanel(navigation); }
		});
		// World hops and loading screens deliberately keep the same session.
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		// Cached data only: no blocking calls or game interaction during close.
		saveRecap();
	}

	private void captureSession()
	{
		WhereWasIPanel target = panel;
		if (!running || target == null || client.getGameState() != GameState.LOGGED_IN
			|| client.getAccountHash() == -1 || client.getAccountHash() == 0) { return; }
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null) { return; }
		int[] experience = client.getSkillExperiences();
		if (!SessionRecap.experienceReady(experience)) { return; }
		if (configManager.getRSProfileKey() == null)
		{
			configManager.setRSProfileConfiguration(WhereWasIConfig.GROUP, "profileInitialized", true);
		}
		String profile = configManager.getRSProfileKey();
		if (profile == null) { return; }
		if (!Objects.equals(profile, activeProfile))
		{
			saveRecap();
			activeProfile = profile;
			baselineXp = experience.clone();
			sessionId = System.currentTimeMillis();
			history = SessionHistory.decode(read(profile, HISTORY));
			currentRecap = null;
			lastCheckpoint = 0;
			long accountSession = ++session;
			String activity = read(profile, ACTIVITY);
			String steps = read(profile, NEXT_STEPS);
			updateReminder(profile);
			String completed = read(profile, DONE);
			long updated = updatedAt(read(profile, UPDATED));
			String supplies = read(profile, SUPPLIES);
			SessionRecap saved = SessionRecap.decode(read(profile, RECAP_KEY));
			if (saved == null)
			{
				LastVisit legacy = LastVisit.decode(read(profile, "lastVisitV1"));
				if (legacy != null) { saved = SessionRecap.legacy(legacy.savedAt); }
			}
			if (history.entries().isEmpty() && saved != null && saved.tracked)
			{
				history = history.with(saved.endedAt, saved);
			}
			SessionHistory previousHistory = history;
			boolean openOnLogin = config.loginRecap();
			SessionRecap previous = saved;
			String name = player.getName();
			SwingUtilities.invokeLater(() ->
			{
				if (running && panel == target && session == accountSession)
				{
					target.showAccount(profile, name, activity, steps, supplies, previous, completed, updated);
					target.showHistory(previousHistory);
					if (openOnLogin && navigation != null) { clientToolbar.openPanel(navigation); }
				}
			});
			if (config.welcomeMessage())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Where Was I? Your adventure journal is ready in the sidebar.", null);
			}
		}
		currentRecap = SessionRecap.capture(baselineXp, experience, System.currentTimeMillis());
		if (currentRecap.endedAt - lastCheckpoint >= CHECKPOINT_MILLIS)
		{
			saveRecap();
			lastCheckpoint = currentRecap.endedAt;
		}
	}

	private void updateReminder(String profile)
	{
		reminderText = shortReminder(JournalChecklist.next(read(profile, NEXT_STEPS),
			JournalChecklist.decode(read(profile, DONE))));
	}

	static long updatedAt(String value)
	{
		try { return value == null ? 0 : Math.max(0, Long.parseLong(value)); }
		catch (NumberFormatException ignored) { return 0; }
	}

	String getReminderText()
	{
		return reminderText;
	}

	static String shortReminder(String text)
	{
		if (text == null || text.trim().isEmpty()) { return "Leave yourself a next step"; }
		String line = text.replaceAll("\\s+", " ").trim();
		return line.length() > 48 ? line.substring(0, 45) + "…" : line;
	}

	private String read(String profile, String key)
	{
		return configManager.getConfiguration(WhereWasIConfig.GROUP, profile, key);
	}

	private void saveRecap()
	{
		String profile = activeProfile;
		SessionRecap recap = currentRecap;
		if (profile != null && recap != null)
		{
			configManager.setConfiguration(WhereWasIConfig.GROUP, profile, RECAP_KEY, recap.encode());
			history = history.with(sessionId, recap);
			configManager.setConfiguration(WhereWasIConfig.GROUP, profile, HISTORY, history.encode());
		}
	}
}
