package com.rustymediclabs.wherewasi;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class JournalFeaturesTest
{
	@Test
	public void tasksPreserveDuplicatesAndCompletedStepsAcrossReordering()
	{
		List<JournalChecklist.Task> tasks = JournalChecklist.tasks("Bank\n\nGet food\nBank");
		assertEquals(3, tasks.size());
		assertNotEquals(tasks.get(0).id, tasks.get(2).id);
		Set<String> done = JournalChecklist.decode(null);
		done.add(tasks.get(0).id);
		assertEquals("Get food", JournalChecklist.next("Bank\nGet food\nBank", done));
		assertEquals("Get food", JournalChecklist.next("Get food\nBank", done));
		assertEquals(done, JournalChecklist.decode(JournalChecklist.encode(done)));
		done.add(tasks.get(1).id);
		done.add(tasks.get(2).id);
		assertEquals("All steps done — what's next?", JournalChecklist.next("Bank\nGet food\nBank", done));
		assertEquals("Leave yourself a next step", JournalChecklist.next("", done));
	}

	@Test
	public void checkpointsReplaceOneSessionAndHistoryIsBounded()
	{
		SessionRecap first = SessionRecap.decode("100;WOODCUTTING=10");
		SessionRecap later = SessionRecap.decode("200;WOODCUTTING=20");
		SessionHistory history = SessionHistory.decode(null).with(1, first).with(1, later);
		assertEquals(1, history.entries().size());
		assertEquals(200, history.entries().get(0).recap.endedAt);
		for (int i = 2; i <= 30; i++) { history = history.with(i, later); }
		assertEquals(20, history.entries().size());
		assertEquals(30, history.entries().get(0).id);
		SessionHistory restored = SessionHistory.decode(history.encode());
		assertEquals(history.encode(), restored.encode());
		assertEquals(1, SessionHistory.decode("broken\n1|100;\n1|200;\n-1|100;").entries().size());
	}

	@Test
	public void updateAgeDoesNotInventDatesForExistingNotes()
	{
		assertEquals(0, WhereWasIPlugin.updatedAt("bad"));
		assertEquals(0, WhereWasIPlugin.updatedAt("-1"));
		assertEquals("Date recorded after your next edit", WhereWasIPanel.updatedLabel(0, 100));
		long time = java.time.ZonedDateTime.of(2026, 10, 1, 12, 0, 0, 0, java.time.ZoneId.systemDefault())
			.toInstant().toEpochMilli();
		assertEquals("Updated 5 days ago", WhereWasIPanel.updatedLabel(time, time + 5 * 86400000L));
		assertTrue(WhereWasIPanel.updatedLabel(time, time).startsWith("Updated today"));
	}
}
