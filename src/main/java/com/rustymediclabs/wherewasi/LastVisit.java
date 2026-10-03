package com.rustymediclabs.wherewasi;

/** Version-one saved location, independent of the client and Swing. */
final class LastVisit
{
	final int x, y, plane, world;
	final long savedAt;

	LastVisit(int x, int y, int plane, int world, long savedAt)
	{
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.world = world;
		this.savedAt = savedAt;
	}

	String encode()
	{
		return x + "," + y + "," + plane + "," + world + "," + savedAt;
	}

	static LastVisit decode(String value)
	{
		if (value == null) { return null; }
		String[] parts = value.split(",", -1);
		if (parts.length != 5) { return null; }
		try
		{
			LastVisit visit = new LastVisit(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Long.parseLong(parts[4]));
			return visit.x >= 0 && visit.y >= 0 && visit.plane >= 0 && visit.plane <= 3
				&& visit.world > 0 && visit.savedAt > 0 ? visit : null;
		}
		catch (NumberFormatException ignored) { return null; }
	}
}
