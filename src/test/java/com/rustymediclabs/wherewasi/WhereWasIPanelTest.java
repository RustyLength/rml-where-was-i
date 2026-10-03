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
