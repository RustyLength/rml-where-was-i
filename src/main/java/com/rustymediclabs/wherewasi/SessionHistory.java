package com.rustymediclabs.wherewasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/** Stable session IDs let checkpoints replace the same session instead of duplicating it. */
final class SessionHistory
{
	static final int LIMIT = 20;
	static final class Entry
	{
		final long id;
		final SessionRecap recap;
		Entry(long id, SessionRecap recap) { this.id = id; this.recap = recap; }
	}
	private final List<Entry> entries;

	private SessionHistory(List<Entry> entries) { this.entries = Collections.unmodifiableList(entries); }
	List<Entry> entries() { return entries; }

	SessionHistory with(long id, SessionRecap recap)
	{
		List<Entry> result = new ArrayList<>();
		result.add(new Entry(id, recap));
		for (Entry entry : entries)
		{
			if (entry.id != id && result.size() < LIMIT) { result.add(entry); }
		}
		return new SessionHistory(result);
	}

	String encode()
	{
		StringJoiner result = new StringJoiner("\n");
		for (Entry entry : entries) { result.add(entry.id + "|" + entry.recap.encode()); }
		return result.toString();
	}

	static SessionHistory decode(String text)
	{
		List<Entry> result = new ArrayList<>();
		if (text != null)
		{
			for (String line : text.split("\n"))
			{
				String[] parts = line.split("\\|", 2);
				if (parts.length != 2) { continue; }
				try
				{
					long id = Long.parseLong(parts[0]);
					SessionRecap recap = SessionRecap.decode(parts[1]);
					if (id > 0 && recap != null && result.stream().noneMatch(e -> e.id == id) && result.size() < LIMIT)
					{
						result.add(new Entry(id, recap));
					}
				}
				catch (NumberFormatException ignored) { /* Skip damaged entries, retain valid history. */ }
			}
		}
		return new SessionHistory(result);
	}
}
