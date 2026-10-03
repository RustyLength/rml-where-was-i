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
		description = "Show a chat reminder of the RML sidebar when you log in"
	)
	default boolean welcomeMessage()
	{
		return true;
	}
}
