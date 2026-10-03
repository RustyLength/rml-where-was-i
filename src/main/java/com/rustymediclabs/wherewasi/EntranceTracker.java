package com.rustymediclabs.wherewasi;

import net.runelite.api.coords.WorldPoint;

/** Records an observed transition near a known entrance, never projects a dungeon onto the surface. */
final class EntranceTracker
{
	// Entrance coordinates from RuneLite's worldmap/DungeonLocation.java.
	// Only these ordinary surface-to-underground transitions are supported initially.
	private static final Entry[] ENTRIES = {
		new Entry("brimhaven-n", "Brimhaven Dungeon (north)", 2743, 3154),
		new Entry("brimhaven-s", "Brimhaven Dungeon (south)", 2759, 3062),
		new Entry("taverley", "Taverley Dungeon", 2883, 3397),
		new Entry("lumbridge-swamp", "Lumbridge Swamp Caves", 3168, 3172),
		new Entry("edgeville", "Edgeville Dungeon", 3096, 3469),
		new Entry("edgeville-shed", "Edgeville Dungeon (shed)", 3115, 3452),
		new Entry("catacombs", "Catacombs of Kourend", 1636, 3673)
	};
	private WorldPoint previous;
	private Entry entry;

	Entry update(WorldPoint point)
	{
		if (point.getY() < 6400) { entry = null; }
		else if (previous != null)
		{
			if (previous.getY() < 6400)
			{
				entry = null;
				for (Entry candidate : ENTRIES)
				{
					if (previous.getPlane() == 0 && candidate.point.distanceTo2D(previous) <= 6
						&& Math.abs(point.getX() - previous.getX()) <= 128
						&& Math.abs(point.getY() - 6400 - previous.getY()) <= 128)
					{
						entry = candidate;
						break;
					}
				}
			}
			else if (point.distanceTo2D(previous) > 128) { entry = null; }
		}
		previous = point;
		return entry;
	}

	void restore(MapSnapshot snapshot, WorldPoint current)
	{
		if (entry != null) { return; }
		previous = current;
		entry = snapshot != null && current.getY() >= 6400
			&& new WorldPoint(snapshot.visit.x, snapshot.visit.y, snapshot.visit.plane).distanceTo2D(current) <= 64
			? snapshot.entrance : null;
	}

	void reset() { previous = null; entry = null; }

	static Entry byId(String id)
	{
		for (Entry entry : ENTRIES) { if (entry.id.equals(id)) { return entry; } }
		return null;
	}

	static final class Entry
	{
		final String id;
		final String name;
		final WorldPoint point;
		Entry(String id, String name, int x, int y)
		{
			this.id = id;
			this.name = name;
			this.point = new WorldPoint(x, y, 0);
		}
	}
}
