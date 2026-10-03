package com.rustymediclabs.wherewasi;

import org.junit.Test;
import static org.junit.Assert.*;

public class LastVisitTest
{
	@Test
	public void persistedVisitKeepsInteriorCoordinatesAndTimestamp()
	{
		LastVisit saved = LastVisit.decode(new LastVisit(3200, 9800, 2, 503, 123456789L).encode());
		assertNotNull(saved);
		assertEquals(3200, saved.x);
		assertEquals(9800, saved.y);
		assertEquals(2, saved.plane);
		assertEquals(503, saved.world);
		assertEquals(123456789L, saved.savedAt);
	}

	@Test
	public void missingOrDamagedHistoryDoesNotPreventLogin()
	{
		String[] invalid = {null, "", "bad", "1,2,3,4", "1,2,3,4,not-a-time",
			"1,2,4,503,1000", "-1,2,0,503,1000", "1,2,0,503,-1"};
		for (String value : invalid) { assertNull(LastVisit.decode(value)); }
	}
}
