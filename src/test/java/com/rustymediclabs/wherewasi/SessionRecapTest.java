package com.rustymediclabs.wherewasi;

import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionRecapTest
{
	@Test
	public void recordsOnlySessionGainsAndRoundTripsBySkillName()
	{
		int[] before = new int[Skill.values().length];
		before[Skill.WOODCUTTING.ordinal()] = 10000;
		before[Skill.ATTACK.ordinal()] = 500;
		int[] after = before.clone();
		after[Skill.WOODCUTTING.ordinal()] += 135;
		after[Skill.ATTACK.ordinal()] -= 5;
		SessionRecap recap = SessionRecap.capture(before, after, 1234);
		assertEquals(1, recap.gains.size());
		assertEquals(Integer.valueOf(135), recap.gains.get(Skill.WOODCUTTING));
		SessionRecap restored = SessionRecap.decode(recap.encode());
		assertEquals(recap.gains, restored.gains);
		assertEquals(1234, restored.endedAt);
		assertTrue(restored.tracked);
	}

	@Test
	public void safelyHandlesLegacyEmptyFutureAndMalformedRecords()
	{
		assertFalse(SessionRecap.legacy(1234).tracked);
		assertTrue(SessionRecap.decode("1234;").gains.isEmpty());
		assertEquals(Integer.valueOf(5), SessionRecap.decode("1234;FUTURE_SKILL=7,WOODCUTTING=5")
			.gains.get(Skill.WOODCUTTING));
		for (String value : new String[]{"broken", "0;", "1;ATTACK=-1", "1;ATTACK=no", "1;ATTACK=1,", "1;ATTACK"})
		{
			assertNull(value, SessionRecap.decode(value));
		}
		assertFalse(SessionRecap.experienceReady(null));
		assertFalse(SessionRecap.experienceReady(new int[]{0, 0}));
		assertTrue(SessionRecap.experienceReady(new int[]{0, 1154}));
	}

	@Test
	public void overlayUsesACompactPlainTextReminder()
	{
		assertEquals("Leave yourself a next step", WhereWasIPlugin.shortReminder("  "));
		assertEquals("Chop willows then bank", WhereWasIPlugin.shortReminder("Chop willows\nthen  bank"));
		assertTrue(WhereWasIPlugin.shortReminder("123456789012345678901234567890123456789012345678901234567890").endsWith("…"));
	}
}
