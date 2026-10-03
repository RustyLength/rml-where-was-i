package com.rustymediclabs.wherewasi;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class WhereWasIPanelTest
{
	@Test
	public void aLateMapForAnotherAccountCannotReplaceTheDisplayedVisit() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> opened = new ArrayList<>();
			WhereWasIPanel panel = new WhereWasIPanel((key, value) -> { },
				(key, visit, entrance) -> opened.add(key + ":" + visit.x + ":" + (entrance == null ? "" : entrance.id)));
			LastVisit main = new LastVisit(2700, 9512, 0, 301, 1);
			LastVisit iron = new LastVisit(3087, 3236, 0, 379, 2);
			MapSnapshot mainMap = new MapSnapshot(main, EntranceTracker.byId("brimhaven-n"),
				new BufferedImage(MapSnapshot.SIZE, MapSnapshot.SIZE, BufferedImage.TYPE_INT_RGB));
			panel.showAccount("main", "Main", "", main);
			panel.showMap("main", mainMap);
			JButton button = findButton(panel);
			button.doClick();
			assertEquals("main:2700:brimhaven-n", opened.get(0));
			panel.showAccount("iron", "Iron", "", iron);
			panel.showMap("main", mainMap);
			assertNull(button.getIcon());
			button.doClick();
			assertEquals("iron:3087:", opened.get(1));
			panel.showLoggedOut();
			assertFalse(button.isEnabled());
		});
	}

	private static JButton findButton(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JButton) { return (JButton) child; }
			if (child instanceof Container)
			{
				JButton found = findButton((Container) child);
				if (found != null) { return found; }
			}
		}
		return null;
	}

	@Test
	public void switchingAccountsDoesNotCopyOrEraseAnotherAccountsNote() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			List<String> writes = new ArrayList<>();
			WhereWasIPanel panel = new WhereWasIPanel((key, value) -> writes.add(key + ":" + value));
			JTextArea editor = findEditor(panel);
			assertNotNull(editor);
			assertFalse(editor.isEnabled());
			panel.showAccount("rsprofile.main", "BudgieMS", "Main plan", null);
			assertTrue(writes.isEmpty());
			assertEquals("Main plan", editor.getText());
			editor.append(" tomorrow");
			assertEquals("rsprofile.main:Main plan tomorrow", writes.get(writes.size() - 1));
			int beforeSwitch = writes.size();
			panel.showAccount("rsprofile.iron", "BudgieFE", "Iron plan", null);
			assertEquals(beforeSwitch, writes.size());
			assertEquals("Iron plan", editor.getText());
			editor.append(" next");
			assertEquals("rsprofile.iron:Iron plan next", writes.get(writes.size() - 1));
			int beforeLogout = writes.size();
			panel.showLoggedOut();
			assertEquals(beforeLogout, writes.size());
			assertFalse(editor.isEnabled());
			assertEquals("", editor.getText());
		});
	}

	private static JTextArea findEditor(Container container)
	{
		for (Component child : container.getComponents())
		{
			if (child instanceof JTextArea && ((JTextArea) child).isEditable()) { return (JTextArea) child; }
			if (child instanceof Container)
			{
				JTextArea found = findEditor((Container) child);
				if (found != null) { return found; }
			}
		}
		return null;
	}
}
