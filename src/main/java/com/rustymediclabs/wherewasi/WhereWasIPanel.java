package com.rustymediclabs.wherewasi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.HashSet;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;
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
	private final JTextArea activity = textArea(2);
	private final JTextArea steps = textArea(5);
	private final JTextArea supplies = textArea(2);
	private final JTextArea recap = textArea(3);
	private final JTextArea prompt = textArea(2);
	private final JLabel status = new JLabel();
	private final JPanel checklist = new JPanel();
	private final JTextArea historyText = textArea(12);
	private final JScrollPane historyScroll = editor(historyText, 180);
	private final JButton historyToggle = new JButton("Show session history");
	private Set<String> completed = new HashSet<>();
	private final JournalWriter writer;
	private String profile;
	private boolean loading;

	WhereWasIPanel(JournalWriter writer)
	{
		this.writer = writer;
		setLayout(new GridBagLayout());
		setBackground(STONE);
		setBorder(BorderFactory.createEmptyBorder(12, 10, 14, 10));
		getScrollPane().setBorder(BorderFactory.createEmptyBorder());
		getScrollPane().getViewport().setBackground(STONE);
		JPanel heading = new JPanel(new BorderLayout(10, 0));
		heading.setBackground(STONE);
		heading.add(label("Where Was I?", 19), BorderLayout.CENTER);
		heading.add(new JLabel(new ImageIcon(createIcon())), BorderLayout.EAST);
		heading.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(101, 80, 50)),
			BorderFactory.createEmptyBorder(0, 0, 12, 0)));
		addRow(heading, 0);
		account.setEditable(false);
		account.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		account.setForeground(GOLD);
		addRow(section("YOUR CHARACTER", account), 1);
		prompt.setEditable(false);
		prompt.setForeground(GOLD);
		addRow(prompt, 2);
		addRow(section("I WAS WORKING ON", editor(activity, 65)), 3);
		JPanel nextSteps = new JPanel(new BorderLayout(0, 8));
		nextSteps.setOpaque(false);
		nextSteps.add(editor(steps, 100), BorderLayout.NORTH);
		checklist.setLayout(new BoxLayout(checklist, BoxLayout.Y_AXIS));
		checklist.setOpaque(false);
		nextSteps.add(checklist, BorderLayout.CENTER);
		JTextArea hint = textArea(1);
		hint.setEditable(false);
		hint.setText("One step per line. Tick it off below.");
		hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		nextSteps.add(hint, BorderLayout.SOUTH);
		addRow(section("MY NEXT STEPS", nextSteps), 4);
		addRow(section("DON'T FORGET", editor(supplies, 65)), 5);
		status.setForeground(GOLD);
		status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		addRow(status, 6);
		recap.setEditable(false);
		addRow(section("LAST SESSION", recap), 7);
		JTextArea credit = textArea(1);
		credit.setEditable(false);
		credit.setBackground(STONE);
		credit.setForeground(new Color(151, 137, 115));
		credit.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		credit.setText("Designed by Rusty Medic Labs");
		JPanel historyPanel = new JPanel(new BorderLayout(0, 8));
		historyPanel.setOpaque(false);
		historyText.setEditable(false);
		historyScroll.setVisible(false);
		historyToggle.setName("sessionHistoryToggle");
		historyToggle.setForeground(GOLD);
		historyToggle.addActionListener(event ->
		{
			boolean show = !historyScroll.isVisible();
			historyScroll.setVisible(show);
			historyToggle.setText(show ? "Hide session history" : "Show session history");
			revalidate();
		});
		historyPanel.add(historyToggle, BorderLayout.NORTH);
		historyPanel.add(historyScroll, BorderLayout.CENTER);
		addRow(historyPanel, 8);
		addRow(credit, 9);
		listen(activity, WhereWasIPlugin.ACTIVITY);
		listen(steps, WhereWasIPlugin.NEXT_STEPS);
		listen(supplies, WhereWasIPlugin.SUPPLIES);
		showLoggedOut();
	}

	private static JScrollPane editor(JTextArea text, int height)
	{
		JScrollPane scroll = new JScrollPane(text);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(0, height));
		scroll.setBorder(BorderFactory.createLineBorder(new Color(101, 80, 50)));
		return scroll;
	}

	private void listen(JTextArea text, String key)
	{
		text.setName(key);
		text.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent event) { changed(); }
			@Override public void removeUpdate(DocumentEvent event) { changed(); }
			@Override public void changedUpdate(DocumentEvent event) { changed(); }
			private void changed()
			{
				if (!loading && profile != null)
				{
					writer.save(profile, key, text.getText());
					showUpdated(System.currentTimeMillis());
					if (WhereWasIPlugin.NEXT_STEPS.equals(key)) { rebuildChecklist(); }
				}
			}
		});
	}

	private void addRow(java.awt.Component component, int row)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(row == 0 ? 0 : 10, 0, 0, 0);
		add(component, constraints);
	}

	void showAccount(String profile, String name, String activityText, String stepsText,
		String suppliesText, SessionRecap previous)
	{
		showAccount(profile, name, activityText, stepsText, suppliesText, previous, null, 0);
	}

	void showAccount(String profile, String name, String activityText, String stepsText,
		String suppliesText, SessionRecap previous, String done, long updated)
	{
		loading = true;
		this.profile = profile;
		completed = JournalChecklist.decode(done);
		account.setText(name);
		load(activity, activityText);
		load(steps, stepsText);
		load(supplies, suppliesText);
		prompt.setText("Pick up where you left off. Your journal saves as you type.");
		showRecap(previous);
		showUpdated(updated);
		rebuildChecklist();
		loading = false;
	}

	private static void load(JTextArea editor, String text)
	{
		editor.setText(text == null ? "" : text);
		editor.setCaretPosition(0);
		editor.setEnabled(true);
	}

	void showSessionEnded(String accountProfile, SessionRecap ended, boolean remind)
	{
		if (!accountProfile.equals(profile)) { return; }
		showRecap(ended);
		// Keep this explicit profile and its editors available on the login screen.
		prompt.setText(remind ? "Before you go: leave your future self a reminder. You are already logged out."
			: "Logged out. You can still update this character's journal.");
	}

	void showLoggedOut()
	{
		loading = true;
		profile = null;
		completed.clear();
		checklist.removeAll();
		showHistory(SessionHistory.decode(null));
		account.setText("Log in to load your character");
		for (JTextArea text : new JTextArea[]{activity, steps, supplies})
		{
			text.setText("");
			text.setEnabled(false);
		}
		prompt.setText("Your journal is saved separately for each character.");
		showRecap(null);
		status.setText("Waiting for your character");
		loading = false;
	}

	private void rebuildChecklist()
	{
		checklist.removeAll();
		for (JournalChecklist.Task task : JournalChecklist.tasks(steps.getText()))
		{
			JPanel row = new JPanel(new BorderLayout(5, 0));
			row.setOpaque(false);
			JCheckBox check = new JCheckBox();
			check.setName(task.id);
			check.setOpaque(false);
			check.setSelected(completed.contains(task.id));
			JTextArea title = textArea(1);
			title.setEditable(false);
			title.setText(task.text);
			title.setForeground(check.isSelected() ? new Color(151, 137, 115) : GOLD);
			check.addActionListener(event ->
			{
				if (profile == null || loading) { return; }
				if (check.isSelected()) { completed.add(task.id); }
				else { completed.remove(task.id); }
				writer.save(profile, WhereWasIPlugin.DONE, JournalChecklist.encode(completed));
				title.setForeground(check.isSelected() ? new Color(151, 137, 115) : GOLD);
				showUpdated(System.currentTimeMillis());
			});
			row.add(check, BorderLayout.WEST);
			row.add(title, BorderLayout.CENTER);
			checklist.add(row);
		}
		revalidate();
		repaint();
	}

	void showHistory(SessionHistory history)
	{
		StringBuilder text = new StringBuilder();
		for (SessionHistory.Entry entry : history.entries())
		{
			if (text.length() > 0) { text.append("\n\n"); }
			text.append(recapText(entry.recap));
		}
		historyText.setText(text.length() == 0 ? "Your saved sessions will appear here (up to 20)." : text.toString());
		historyText.setCaretPosition(0);
	}

	private void showUpdated(long time)
	{
		status.setText(updatedLabel(time, System.currentTimeMillis()));
	}

	static String updatedLabel(long time, long now)
	{
		if (time <= 0) { return "Date recorded after your next edit"; }
		LocalDate date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
		long days = Math.max(0, ChronoUnit.DAYS.between(date, today));
		if (days == 0) { return "Updated today, " + DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
			.format(Instant.ofEpochMilli(time)); }
		return "Updated " + days + (days == 1 ? " day ago" : " days ago");
	}

	private void showRecap(SessionRecap previous)
	{
		if (previous == null)
		{
			recap.setText("Your first session recap will appear after you log out.");
			return;
		}
		recap.setText(recapText(previous));
	}

	private static String recapText(SessionRecap previous)
	{
		StringBuilder text = new StringBuilder(TIME.format(Instant.ofEpochMilli(previous.endedAt)));
		if (!previous.tracked) { text.append("\nXP tracking starts with this update."); }
		else if (previous.gains.isEmpty()) { text.append("\nNo XP gains recorded."); }
		else
		{
			previous.gains.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
				.forEach(entry -> text.append("\n").append(entry.getKey().getName()).append(": +")
					.append(String.format(Locale.UK, "%,d", entry.getValue())).append(" XP"));
		}
		return text.toString();
	}

	interface JournalWriter
	{
		void save(String profile, String key, String text);
	}

	private static JTextArea textArea(int rows)
	{
		JTextArea area = new JTextArea(rows, 0);
		area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
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
		section.setBackground(new Color(32, 29, 25));
		section.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(76, 61, 43)),
			BorderFactory.createEmptyBorder(10, 9, 10, 9)));
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
			graphics.setColor(new Color(26, 24, 21));
			graphics.fillRoundRect(0, 0, 24, 24, 5, 5);
			graphics.setColor(new Color(104, 73, 39));
			graphics.fillRoundRect(4, 2, 17, 20, 3, 3);
			graphics.setColor(GOLD);
			graphics.drawRoundRect(4, 2, 17, 20, 3, 3);
			graphics.drawLine(7, 3, 7, 21);
			graphics.setColor(new Color(237, 218, 175));
			graphics.drawLine(10, 9, 18, 9);
			graphics.drawLine(10, 12, 18, 12);
			graphics.drawLine(10, 15, 16, 15);
			graphics.setColor(new Color(155, 53, 43));
			graphics.fillRect(14, 2, 4, 5);
		}
		finally { graphics.dispose(); }
		return icon;
	}
}
