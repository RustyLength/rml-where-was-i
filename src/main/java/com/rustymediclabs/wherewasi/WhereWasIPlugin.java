package com.rustymediclabs.wherewasi;

import com.google.inject.Provides;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
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

@PluginDescriptor(
	name = "Where Was I?",
	description = "Remember where you left off — by Rusty Medic Labs",
	tags = {"notes", "journal", "reminder", "location", "rml"}
)
public class WhereWasIPlugin extends Plugin
{
	private static final String NOTE_KEY = "nextSteps";
	private static final String VISIT_KEY = "lastVisitV1";
	private static final long CHECKPOINT_MILLIS = 30_000;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ConfigManager configManager;
	@Inject private WhereWasIConfig config;

	private volatile boolean running;
	private volatile WhereWasIPanel panel;
	private volatile NavigationButton navigation;
	private volatile String activeProfile;
	private volatile LastVisit currentVisit;
	private long lastCheckpoint;

	@Provides
	WhereWasIConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(WhereWasIConfig.class);
	}

	@Override
	protected void startUp()
	{
		running = true;
		SwingUtilities.invokeLater(() ->
		{
			if (!running) { return; }
			panel = new WhereWasIPanel((profile, note) ->
				// Capture the account key instead of writing to whichever account is now logged in.
				// ConfigManager updates memory; RuneLite handles persistence.
				configManager.setConfiguration(WhereWasIConfig.GROUP, profile, NOTE_KEY, note));
			navigation = NavigationButton.builder()
				.tooltip("Where Was I? · Rusty Medic Labs")
				.icon(WhereWasIPanel.createIcon())
				.priority(8).panel(panel).build();
			clientToolbar.addNavigation(navigation);
			clientThread.invoke(this::captureVisit);
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		saveVisit();
		NavigationButton oldNavigation = navigation;
		if (oldNavigation != null) { clientToolbar.removeNavigation(oldNavigation); }
		navigation = null;
		panel = null;
		activeProfile = null;
		currentVisit = null;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		captureVisit();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			saveVisit();
			activeProfile = null;
			currentVisit = null;
			WhereWasIPanel target = panel;
			if (target != null) { SwingUtilities.invokeLater(target::showLoggedOut); }
		}
		// Hopping and loading are deliberately not treated as a new session.
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		saveVisit();
	}

	private void captureVisit()
	{
		WhereWasIPanel target = panel;
		if (!running || target == null || client.getGameState() != GameState.LOGGED_IN
			|| client.getAccountHash() == -1 || client.getAccountHash() == 0) { return; }
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null) { return; }
		// Create the RuneScape profile on a fresh RuneLite installation.
		if (configManager.getRSProfileKey() == null)
		{
			configManager.setRSProfileConfiguration(WhereWasIConfig.GROUP, "profileInitialized", true);
		}
		String profile = configManager.getRSProfileKey();
		if (profile == null) { return; }
		if (!Objects.equals(profile, activeProfile))
		{
			saveVisit();
			activeProfile = profile;
			currentVisit = null;
			lastCheckpoint = 0;
			String note = configManager.getConfiguration(WhereWasIConfig.GROUP, profile, NOTE_KEY);
			LastVisit previous = LastVisit.decode(configManager.getConfiguration(WhereWasIConfig.GROUP, profile, VISIT_KEY));
			String name = player.getName();
			SwingUtilities.invokeLater(() -> target.showAccount(profile, name, note, previous));
			if (config.welcomeMessage())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Where Was I? Your next steps and previous location are in the RML sidebar.", null);
			}
		}
		WorldPoint location = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		if (location == null) { return; }
		currentVisit = new LastVisit(location.getX(), location.getY(), location.getPlane(),
			client.getWorld(), System.currentTimeMillis());
		if (currentVisit.savedAt - lastCheckpoint >= CHECKPOINT_MILLIS)
		{
			saveVisit();
			lastCheckpoint = currentVisit.savedAt;
		}
	}

	private void saveVisit()
	{
		String profile = activeProfile;
		LastVisit visit = currentVisit;
		if (profile != null && visit != null)
		{
			configManager.setConfiguration(WhereWasIConfig.GROUP, profile, VISIT_KEY, visit.encode());
		}
	}
}
