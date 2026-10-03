package com.rustymediclabs.wherewasi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Each non-empty note line is a task; duplicate lines have separate identities. */
final class JournalChecklist
{
	static final class Task
	{
		final String id;
		final String text;
		Task(String id, String text) { this.id = id; this.text = text; }
	}

	static List<Task> tasks(String note)
	{
		List<Task> result = new ArrayList<>();
		Map<String, Integer> occurrences = new HashMap<>();
		if (note == null) { return result; }
		for (String line : note.split("\\R"))
		{
			String text = line.trim();
			if (text.isEmpty()) { continue; }
			int occurrence = occurrences.merge(text, 1, Integer::sum);
			String id = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + ":" + occurrence;
			result.add(new Task(id, text));
		}
		return result;
	}

	static Set<String> decode(String value)
	{
		Set<String> result = new HashSet<>();
		if (value != null)
		{
			for (String id : value.split("\n")) { if (!id.isEmpty()) { result.add(id); } }
		}
		return result;
	}

	static String encode(Set<String> done)
	{
		return String.join("\n", new java.util.TreeSet<>(done));
	}

	static String next(String note, Set<String> done)
	{
		List<Task> tasks = tasks(note);
		for (Task task : tasks) { if (!done.contains(task.id)) { return task.text; } }
		return tasks.isEmpty() ? "Leave yourself a next step" : "All steps done — what's next?";
	}
}
