package com.rustymediclabs.wherewasi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

final class WhereWasIPanel extends PluginPanel
{
	private static final Color GOLD = new Color(230, 183, 92);
	private static final Color STONE = new Color(47, 39, 31);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
		.withZone(ZoneId.systemDefault());
	private final JTextArea account = textArea(1);
	private final JTextArea previous = textArea(4);
	private final JTextArea note = textArea(9);
	private final JLabel status = new JLabel();
	private final BiConsumer<String, String> saveNote;
	private String profile;
	private boolean loading;

	WhereWasIPanel(BiConsumer<String, String> saveNote)
	{
		this.saveNote = saveNote;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(STONE);
		setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
		add(label("RUSTY MEDIC LABS", 11));
		add(label("Where Was I?", 23));
		account.setEditable(false);
		add(section("ACCOUNT", account));
		previous.setEditable(false);
		add(section("PREVIOUS VISIT", previous));
		JScrollPane editor = new JScrollPane(note);
		editor.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editor.setBorder(BorderFactory.createLineBorder(new Color(101, 80, 50)));
		add(section("MY NEXT STEPS", editor));
		status.setForeground(GOLD);
		status.setFont(status.getFont().deriveFont(11f));
		add(section("", status));
		JTextArea help = textArea(3);
		help.setEditable(false);
		help.setText("Notes save as you type. Location saves every 30 seconds and when you log out.");
		add(help);
		note.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent event) { changed(); }
			@Override public void removeUpdate(DocumentEvent event) { changed(); }
			@Override public void changedUpdate(DocumentEvent event) { changed(); }
		});
		showLoggedOut();
	}

	private void changed()
	{
		if (!loading && profile != null)
		{
			saveNote.accept(profile, note.getText());
			status.setText("Saved for this account");
		}
	}

	void showAccount(String profile, String name, String text, LastVisit visit)
	{
		loading = true;
		this.profile = profile;
		account.setText(name);
		note.setText(text == null ? "" : text);
		note.setCaretPosition(0);
		note.setEnabled(true);
		previous.setText(visit == null ? "No previous visit yet.\nYour first visit will be saved automatically."
			: "Tile " + visit.x + ", " + visit.y + " · Floor " + visit.plane
			+ "\nWorld " + visit.world + "\n" + TIME.format(Instant.ofEpochMilli(visit.savedAt)));
		status.setText("Notes ready");
		loading = false;
		revalidate();
		repaint();
	}

	void showLoggedOut()
	{
		loading = true;
		profile = null;
		account.setText("Log in to load your account");
		previous.setText("Your saved location will appear here after you log in again.");
		note.setText("");
		note.setEnabled(false);
		status.setText("Waiting for your account");
		loading = false;
	}

	private static JTextArea textArea(int rows)
	{
		JTextArea area = new JTextArea(rows, 16);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBackground(new Color(32, 29, 25));
		area.setForeground(new Color(232, 225, 210));
		area.setCaretColor(GOLD);
		area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		return area;
	}

	private static JLabel label(String text, int size)
	{
		JLabel label = new JLabel(text);
		label.setForeground(GOLD);
		label.setFont(new Font(Font.SERIF, Font.BOLD, size));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel section(String title, java.awt.Component content)
	{
		JPanel section = new JPanel(new BorderLayout(0, 7));
		section.setBackground(STONE);
		section.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
		section.add(label(title, 11), BorderLayout.NORTH);
		section.add(content, BorderLayout.CENTER);
		section.setAlignmentX(LEFT_ALIGNMENT);
		return section;
	}

	static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = icon.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(STONE);
			graphics.fillRoundRect(1, 1, 22, 22, 5, 5);
			graphics.setColor(GOLD);
			graphics.drawRoundRect(1, 1, 21, 21, 5, 5);
			graphics.setFont(new Font(Font.SERIF, Font.BOLD, 22));
			graphics.drawString("?", 6, 20);
		}
		finally { graphics.dispose(); }
		return icon;
	}
}
