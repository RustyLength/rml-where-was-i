package com.rustymediclabs.wherewasi;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WhereWasIConfig.GROUP)
public interface WhereWasIConfig extends Config
{
	String GROUP = "rml-where-was-i";

	@ConfigItem(
		keyName = "welcomeMessage",
		name = "Welcome-back message",
		description = "Show a chat reminder of your adventure journal when you log in"
	)
	default boolean welcomeMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "logoutReminder",
		name = "Logout journal reminder",
		description = "Open your journal after logout so you can leave a reminder; never delays logout"
	)
	default boolean logoutReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "screenReminder",
		name = "On-screen journal reminder",
		description = "Keep a movable reminder visible while playing, even with the sidebar closed"
	)
	default boolean screenReminder()
	{
		return true;
	}
}
