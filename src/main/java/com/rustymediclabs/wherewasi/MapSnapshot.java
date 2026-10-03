package com.rustymediclabs.wherewasi;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.runelite.api.Constants;

/** One record keeps the terrain, marker and coordinates together. */
final class MapSnapshot
{
	private static final String FORMAT = "rml-map-v1";
	static final int SIZE = 192;
	final LastVisit visit;
	final EntranceTracker.Entry entrance;
	final BufferedImage image;

	MapSnapshot(LastVisit visit, EntranceTracker.Entry entrance, BufferedImage image)
	{
		this.visit = visit;
		this.entrance = entrance;
		this.image = image;
	}

	static BufferedImage crop(int[] pixels, int width, int height, int sceneX, int sceneY)
	{
		// drawInstanceMap includes the extended scene, with north at the top.
		int paddingX = (width - Constants.SCENE_SIZE * 4) / 2;
		int paddingY = (height - Constants.SCENE_SIZE * 4) / 2;
		int centreX = paddingX + sceneX * 4 + 2;
		int centreY = height - paddingY - (sceneY * 4 + 2);
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < SIZE; y++)
		{
			int sourceY = centreY - SIZE / 2 + y;
			for (int x = 0; x < SIZE; x++)
			{
				int sourceX = centreX - SIZE / 2 + x;
				if (sourceX >= 0 && sourceX < width && sourceY >= 0 && sourceY < height)
				{
					image.setRGB(x, y, pixels[sourceY * width + sourceX]);
				}
			}
		}
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(Color.BLACK);
			graphics.fillOval(SIZE / 2 - 5, SIZE / 2 - 5, 11, 11);
			graphics.setColor(new Color(230, 183, 92));
			graphics.fillOval(SIZE / 2 - 3, SIZE / 2 - 3, 7, 7);
		}
		finally { graphics.dispose(); }
		return image;
	}

	void write(OutputStream stream) throws IOException
	{
		DataOutputStream header = new DataOutputStream(stream);
		header.writeUTF(FORMAT);
		header.writeUTF(visit.encode());
		header.writeUTF(entrance == null ? "" : entrance.id);
		header.flush();
		// These streams never create ImageIO temporary files outside Filepath.
		try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(stream))
		{
			if (!ImageIO.write(image, "png", output)) { throw new IOException("PNG writer unavailable"); }
		}
	}

	static MapSnapshot read(InputStream stream) throws IOException
	{
		DataInputStream header = new DataInputStream(stream);
		if (!FORMAT.equals(header.readUTF())) { throw new IOException("Unknown map format"); }
		LastVisit visit = LastVisit.decode(header.readUTF());
		EntranceTracker.Entry entrance = EntranceTracker.byId(header.readUTF());
		if (visit == null) { throw new IOException("Invalid saved coordinates"); }
		try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(stream))
		{
			javax.imageio.ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
			try
			{
				reader.setInput(input);
				if (reader.getWidth(0) != SIZE || reader.getHeight(0) != SIZE)
				{
					throw new IOException("Invalid preview size");
				}
				return new MapSnapshot(visit, entrance, reader.read(0));
			}
			finally { reader.dispose(); }
		}
	}
}
