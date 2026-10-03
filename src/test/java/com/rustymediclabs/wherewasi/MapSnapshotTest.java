package com.rustymediclabs.wherewasi;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class MapSnapshotTest
{
	@Test
	public void roundTripKeepsTerrainCoordinatesAndEntranceTogether() throws Exception
	{
		BufferedImage terrain = new BufferedImage(MapSnapshot.SIZE, MapSnapshot.SIZE, BufferedImage.TYPE_INT_RGB);
		terrain.setRGB(10, 20, 0x345678);
		MapSnapshot map = new MapSnapshot(new LastVisit(2700, 9512, 0, 379, 12345),
			EntranceTracker.byId("brimhaven-n"), terrain);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		map.write(bytes);
		MapSnapshot restored = MapSnapshot.read(new ByteArrayInputStream(bytes.toByteArray()));
		assertEquals(map.visit.encode(), restored.visit.encode());
		assertEquals("brimhaven-n", restored.entrance.id);
		assertEquals(0xff345678, restored.image.getRGB(10, 20));
	}

	@Test
	public void cropUsesExtendedScenePaddingAndNorthAtTheTop()
	{
		int width = 736;
		int[] pixels = new int[width * width];
		// 40 extended tiles each side. Scene (50,60) has pixel centre (362,334).
		pixels[314 * width + 342] = 0x123456;
		BufferedImage crop = MapSnapshot.crop(pixels, width, width, 50, 60);
		assertEquals(0xff123456, crop.getRGB(MapSnapshot.SIZE / 2 - 20, MapSnapshot.SIZE / 2 - 20));
		assertEquals(0xffe6b75c, crop.getRGB(MapSnapshot.SIZE / 2, MapSnapshot.SIZE / 2));
	}

	@Test(expected = IOException.class)
	public void rejectWrongImageDimensionsBeforeDecoding() throws Exception
	{
		MapSnapshot map = new MapSnapshot(new LastVisit(1, 2, 0, 301, 1), null,
			new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		map.write(bytes);
		MapSnapshot.read(new ByteArrayInputStream(bytes.toByteArray()));
	}
}
