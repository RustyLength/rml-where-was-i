package com.rustymediclabs.wherewasi;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WhereWasIPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhereWasIPlugin.class);
		RuneLite.main(args);
	}
}