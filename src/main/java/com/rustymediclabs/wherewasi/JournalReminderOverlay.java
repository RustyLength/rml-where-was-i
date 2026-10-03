package com.rustymediclabs.wherewasi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

/** Local journal cue only; never intercepts logout or game input. */
final class JournalReminderOverlay extends OverlayPanel
{
	private final Client client;
	private final WhereWasIConfig config;
	private final WhereWasIPlugin plugin;

	@Inject
	JournalReminderOverlay(Client client, WhereWasIConfig config, WhereWasIPlugin plugin)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(220, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.screenReminder() || client.getGameState() != GameState.LOGGED_IN) { return null; }
		panelComponent.getChildren().add(LineComponent.builder().left("Where Was I?")
			.leftColor(new Color(230, 183, 92)).build());
		panelComponent.getChildren().add(LineComponent.builder().left(plugin.getReminderText()).build());
		panelComponent.getChildren().add(LineComponent.builder().left("Before logout: update your journal")
			.leftColor(new Color(177, 163, 141)).build());
		return super.render(graphics);
	}
}
