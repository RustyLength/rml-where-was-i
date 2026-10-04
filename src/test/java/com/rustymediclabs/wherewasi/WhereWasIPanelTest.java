package com.rustymediclabs.wherewasi;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JScrollPane;
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

	@Test
	public void tickingTasksIsSavedPerCharacterWithoutRewritingTheNote() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> writes = new ArrayList<>();
			WhereWasIPanel panel = new WhereWasIPanel((profile, key, text) -> writes.add(profile + ":" + key + ":" + text));
			String note = "Bank\nGet food";
			String id = JournalChecklist.tasks(note).get(0).id;
			panel.showAccount("main", "Main", "", note, "", null, null, 0);
			findCheck(panel, id).doClick();
			assertEquals("main:completedStepsV1:" + id, writes.get(0));
			assertEquals(note, findEditor(panel, WhereWasIPlugin.NEXT_STEPS).getText());
			int count = writes.size();
			panel.showAccount("iron", "Iron", "", note, "", null, null, 0);
			assertFalse(findCheck(panel, id).isSelected());
			panel.showAccount("main", "Main", "", note, "", null, id, 0);
			assertTrue(findCheck(panel, id).isSelected());
			assertEquals(count, writes.size());
			panel.showSessionEnded("main", null, true);
			findCheck(panel, id).doClick();
			assertEquals("main:completedStepsV1:", writes.get(writes.size() - 1));
		});
	}

	private static JCheckBox findCheck(Container container, String id)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JCheckBox && id.equals(child.getName())) { return (JCheckBox) child; }
			if (child instanceof Container)
			{
				JCheckBox found = findCheck((Container) child, id);
				if (found != null) { return found; }
			}
		}
		return null;
	}

	@Test
	public void expandingHistoryKeepsEditorsAndHistoryReadableAtSidebarWidth() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			WhereWasIPanel panel = new WhereWasIPanel((profile, key, text) -> { });
			panel.showAccount("iron", "BudgieFE", "WC Willows", "Get 40 WC\ntest plug in", "", null);
			panel.showHistory(SessionHistory.decode("1|100;WOODCUTTING=1283\n2|200;"));
			int collapsedHeight = panel.getPreferredSize().height;
			JButton toggle = (JButton) findNamed(panel, "sessionHistoryToggle");
			toggle.doClick();
			assertTrue(panel.getPreferredSize().height >= collapsedHeight + 180);
			// PluginPanel is laid out at its natural height inside RuneLite's scroll pane.
			panel.setSize(225, panel.getPreferredSize().height);
			layoutTree(panel);
			JScrollPane history = (JScrollPane) findNamed(panel, "sessionHistoryScroll");
			assertEquals(180, history.getHeight());
			assertTrue(findEditor(panel, WhereWasIPlugin.ACTIVITY).getHeight() >= 40);
			assertTrue(findCheck(panel, JournalChecklist.tasks("Get 40 WC").get(0).id).getHeight() > 0);
			toggle.doClick();
			assertFalse(history.isVisible());
		});
	}

	private static void layoutTree(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container) { layoutTree((Container) child); }
		}
	}

	private static Component findNamed(Container container, String name)
	{
		for (Component child : container.getComponents())
		{
			if (name.equals(child.getName())) { return child; }
			if (child instanceof Container)
			{
				Component found = findNamed((Container) child, name);
				if (found != null) { return found; }
			}
		}
		return null;
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
