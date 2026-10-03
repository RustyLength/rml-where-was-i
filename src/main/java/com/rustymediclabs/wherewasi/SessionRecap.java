package com.rustymediclabs.wherewasi;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;
import net.runelite.api.Skill;

/** Immutable checkpoint of XP gained while this plugin was enabled. */
final class SessionRecap
{
	final long endedAt;
	final Map<Skill, Integer> gains;
	final boolean tracked;

	private SessionRecap(long endedAt, Map<Skill, Integer> gains, boolean tracked)
	{
		this.endedAt = endedAt;
		EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
		copy.putAll(gains);
		this.gains = Collections.unmodifiableMap(copy);
		this.tracked = tracked;
	}

	static SessionRecap legacy(long time)
	{
		return new SessionRecap(time, Collections.emptyMap(), false);
	}

	static boolean experienceReady(int[] values)
	{
		if (values == null) { return false; }
		for (int value : values) { if (value > 0) { return true; } }
		return false;
	}

	static SessionRecap capture(int[] baseline, int[] current, long time)
	{
		Map<Skill, Integer> gains = new EnumMap<>(Skill.class);
		Skill[] skills = Skill.values();
		for (int i = 0; i < Math.min(skills.length, Math.min(baseline.length, current.length)); i++)
		{
			int gain = current[i] - baseline[i];
			if (gain > 0) { gains.put(skills[i], gain); }
		}
		return new SessionRecap(time, gains, true);
	}

	String encode()
	{
		StringJoiner values = new StringJoiner(",");
		gains.forEach((skill, gain) -> values.add(skill.name() + "=" + gain));
		return endedAt + ";" + values;
	}

	static SessionRecap decode(String text)
	{
		if (text == null) { return null; }
		String[] parts = text.split(";", -1);
		if (parts.length != 2) { return null; }
		try
		{
			long time = Long.parseLong(parts[0]);
			if (time <= 0) { return null; }
			Map<Skill, Integer> gains = new EnumMap<>(Skill.class);
			if (!parts[1].isEmpty())
			{
				for (String entry : parts[1].split(",", -1))
				{
					String[] pair = entry.split("=", -1);
					if (pair.length != 2) { return null; }
					int gain = Integer.parseInt(pair[1]);
					if (gain <= 0) { return null; }
					try { gains.put(Skill.valueOf(pair[0]), gain); }
					catch (IllegalArgumentException ignored) { /* Future skill: retain other known gains. */ }
				}
			}
			return new SessionRecap(time, gains, true);
		}
		catch (NumberFormatException ignored) { return null; }
	}
}
