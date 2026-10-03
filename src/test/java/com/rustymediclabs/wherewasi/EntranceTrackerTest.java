package com.rustymediclabs.wherewasi;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class EntranceTrackerTest
{
	@Test
	public void recordsObservedEntryAndClearsItAfterLeaving()
	{
		EntranceTracker tracker = new EntranceTracker();
		assertNull(tracker.update(new WorldPoint(2743, 3154, 0)));
		assertEquals("brimhaven-n", tracker.update(new WorldPoint(2713, 9564, 0)).id);
		assertEquals("brimhaven-n", tracker.update(new WorldPoint(2700, 9512, 0)).id);
		assertNull(tracker.update(new WorldPoint(2743, 3154, 0)));
	}

	@Test
	public void startingUndergroundOrTeleportingDoesNotInventAnEntrance()
	{
		EntranceTracker tracker = new EntranceTracker();
		assertNull(tracker.update(new WorldPoint(2700, 9512, 0)));
		tracker.reset();
		tracker.update(new WorldPoint(2743, 3154, 0));
		assertNull(tracker.update(new WorldPoint(3200, 9600, 0)));
		tracker.reset();
		tracker.update(new WorldPoint(2743, 3154, 0));
		tracker.update(new WorldPoint(2713, 9564, 0));
		assertNull(tracker.update(new WorldPoint(3200, 9600, 0)));
	}
}
