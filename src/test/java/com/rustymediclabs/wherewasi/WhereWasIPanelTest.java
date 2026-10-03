package com.rustymediclabs.wherewasi;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class WhereWasIPanelTest
{
	@Test
	public void journalEditsStayWithTheirCharacterIncludingAfterLogout() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> writes = new ArrayList<>();
			WhereWasIPanel panel = new WhereWasIPanel((profile, key, text) ->
				writes.add(profile + ":" + key + ":" + text));
			JTextArea steps = findEditor(panel, WhereWasIPlugin.NEXT_STEPS);
			JTextArea activity = findEditor(panel, WhereWasIPlugin.ACTIVITY);
			JTextArea supplies = findEditor(panel, WhereWasIPlugin.SUPPLIES);
			assertFalse(steps.isEnabled());
			panel.showAccount("main", "BudgieMS", "Slayer", "Original note\nexactly as saved", "Antifire", null);
			assertTrue(writes.isEmpty());
			assertEquals("Original note\nexactly as saved", steps.getText());
			activity.append(" task");
			assertEquals("main:currentActivityV1:Slayer task", writes.get(writes.size() - 1));
			supplies.append(" and food");
			assertEquals("main:rememberSuppliesV1:Antifire and food", writes.get(writes.size() - 1));
			int count = writes.size();
			panel.showSessionEnded("main", SessionRecap.legacy(1), true);
			assertEquals(count, writes.size());
			assertTrue(steps.isEnabled());
			steps.append(" tomorrow");
			assertEquals("main:nextSteps:Original note\nexactly as saved tomorrow", writes.get(writes.size() - 1));
			count = writes.size();
			panel.showAccount("iron", "BudgieFE", "Woodcutting", "Iron reminder", "Axe", null);
			assertEquals(count, writes.size());
			// A stale logout callback must not change the new character's journal.
			panel.showSessionEnded("main", SessionRecap.legacy(1), true);
			assertEquals("Iron reminder", steps.getText());
			steps.append(" next");
			assertEquals("iron:nextSteps:Iron reminder next", writes.get(writes.size() - 1));
			count = writes.size();
			panel.showLoggedOut();
			assertEquals(count, writes.size());
			assertFalse(steps.isEnabled());
		});
	}

	private static JTextArea findEditor(Container container, String key)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextArea && key.equals(child.getName())) { return (JTextArea) child; }
			if (child instanceof Container)
			{
				JTextArea found = findEditor((Container) child, key);
				if (found != null) { return found; }
			}
		}
		return null;
	}
}
