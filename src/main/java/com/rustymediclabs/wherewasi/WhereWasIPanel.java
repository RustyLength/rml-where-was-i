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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JButton;
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
	private final JButton mapPreview = new JButton("Preview available after your next visit");
	private final JTextArea mapHelp = textArea(2);
	private final MapAction openMap;
	private LastVisit displayedVisit;
	private EntranceTracker.Entry displayedEntrance;
	private final BiConsumer<String, String> saveNote;
	private String profile;
	private boolean loading;

	WhereWasIPanel(BiConsumer<String, String> saveNote)
	{
		this(saveNote, (profile, visit, entrance) -> { });
	}

	WhereWasIPanel(BiConsumer<String, String> saveNote, MapAction openMap)
	{
		this.saveNote = saveNote;
		this.openMap = openMap;
		setLayout(new GridBagLayout());
		setBackground(STONE);
		setBorder(BorderFactory.createEmptyBorder(12, 10, 14, 10));
		getScrollPane().setBorder(BorderFactory.createEmptyBorder());
		getScrollPane().getViewport().setBackground(STONE);

		JPanel heading = new JPanel(new BorderLayout(10, 0));
		heading.setBackground(STONE);
		JPanel titles = new JPanel(new BorderLayout(0, 4));
		titles.setBackground(STONE);
		titles.add(label("RUSTY MEDIC LABS", 10), BorderLayout.NORTH);
		titles.add(label("Where Was I?", 19), BorderLayout.CENTER);
		heading.add(titles, BorderLayout.CENTER);
		heading.add(new JLabel(new ImageIcon(createIcon())), BorderLayout.EAST);
		heading.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(101, 80, 50)),
			BorderFactory.createEmptyBorder(0, 0, 12, 0)));
		addRow(heading, 0);

		account.setEditable(false);
		account.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		account.setForeground(GOLD);
		addRow(section("YOUR CHARACTER", account), 1);
		previous.setEditable(false);
		JPanel location = new JPanel(new BorderLayout(0, 8));
		location.setOpaque(false);
		location.add(previous, BorderLayout.NORTH);
		mapPreview.setPreferredSize(new Dimension(0, MapSnapshot.SIZE));
		mapPreview.setMargin(new Insets(0, 0, 0, 0));
		mapPreview.setForeground(GOLD);
		mapPreview.setBackground(new Color(24, 23, 20));
		mapPreview.setBorder(BorderFactory.createLineBorder(new Color(101, 80, 50)));
		mapPreview.setToolTipText("Open the world map at your saved location");
		mapPreview.addActionListener(event ->
		{
			if (profile != null && displayedVisit != null)
			{
				mapHelp.setText("Opening world map…");
				openMap.open(profile, displayedVisit, displayedEntrance);
			}
		});
		location.add(mapPreview, BorderLayout.CENTER);
		mapHelp.setEditable(false);
		mapHelp.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		location.add(mapHelp, BorderLayout.SOUTH);
		addRow(section("LAST TIME YOU WERE HERE", location), 2);
		JScrollPane editor = new JScrollPane(note);
		editor.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editor.setPreferredSize(new Dimension(0, 170));
		editor.setBorder(BorderFactory.createLineBorder(new Color(101, 80, 50)));
		editor.getViewport().setBackground(new Color(32, 29, 25));
		JPanel notes = new JPanel(new BorderLayout(0, 8));
		notes.setOpaque(false);
		notes.add(editor, BorderLayout.CENTER);
		status.setForeground(GOLD);
		status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		notes.add(status, BorderLayout.SOUTH);
		addRow(section("MY NEXT STEPS", notes), 3);
		JTextArea help = textArea(3);
		help.setEditable(false);
		help.setBackground(STONE);
		help.setForeground(new Color(177, 163, 141));
		help.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		help.setText("A reminder for your next adventure. Notes save as you type; your location saves when you log out.");
		addRow(help, 4);
		note.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent event) { changed(); }
			@Override public void removeUpdate(DocumentEvent event) { changed(); }
			@Override public void changedUpdate(DocumentEvent event) { changed(); }
		});
		showLoggedOut();
	}

	private void addRow(java.awt.Component component, int row)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.insets = new Insets(row == 0 ? 0 : 12, 0, 0, 0);
		add(component, constraints);
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
		displayedVisit = visit;
		displayedEntrance = null;
		mapPreview.setIcon(null);
		mapPreview.setText(visit == null ? "No saved map yet" : "Open saved location on map");
		mapPreview.setEnabled(visit != null);
		mapPreview.setPreferredSize(new Dimension(0, 42));
		mapHelp.setText(visit == null ? "A terrain preview will be saved for your next visit."
			: "No terrain preview yet. Click for your saved coordinates.");
		account.setText(name);
		note.setText(text == null ? "" : text);
		note.setCaretPosition(0);
		note.setEnabled(true);
		previous.setText(visit == null ? "No previous visit yet.\nYour first visit will be saved automatically."
			: "World " + visit.world + " · Floor " + visit.plane
			+ "\nTile " + visit.x + ", " + visit.y + "\n" + TIME.format(Instant.ofEpochMilli(visit.savedAt)));
		status.setText("Notes ready");
		loading = false;
		revalidate();
		repaint();
	}

	void showLoggedOut()
	{
		loading = true;
		profile = null;
		displayedVisit = null;
		displayedEntrance = null;
		mapPreview.setIcon(null);
		mapPreview.setText("No account loaded");
		mapPreview.setEnabled(false);
		mapPreview.setPreferredSize(new Dimension(0, 42));
		mapHelp.setText("Your preview is saved separately for each character.");
		account.setText("Log in to load your account");
		previous.setText("Your saved location will appear here after you log in again.");
		note.setText("");
		note.setEnabled(false);
		status.setText("Waiting for your account");
		loading = false;
	}

	void showMap(String accountProfile, MapSnapshot snapshot)
	{
		if (!accountProfile.equals(profile)) { return; }
		displayedEntrance = snapshot.entrance;
		mapPreview.setText("");
		mapPreview.setIcon(new ImageIcon(snapshot.image));
		mapPreview.setPreferredSize(new Dimension(0, MapSnapshot.SIZE));
		mapHelp.setText(snapshot.entrance == null ? "Gold dot: saved tile. Click to open the map."
			: "Recorded entry: " + snapshot.entrance.name + ". Click for its entrance.");
		revalidate();
		repaint();
	}

	void mapStatus(String accountProfile, String text)
	{
		if (accountProfile != null && accountProfile.equals(profile)) { mapHelp.setText(text); }
	}

	interface MapAction
	{
		void open(String profile, LastVisit visit, EntranceTracker.Entry entrance);
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
