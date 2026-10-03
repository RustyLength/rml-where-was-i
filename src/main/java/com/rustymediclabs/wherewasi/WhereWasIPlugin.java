package com.rustymediclabs.wherewasi;

import com.google.inject.Provides;
import java.util.Objects;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.SpritePixels;
import net.runelite.api.WidgetNode;
import net.runelite.api.Constants;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.Filepath;

@Slf4j
@PluginDescriptor(
	name = "Where Was I?",
	internalName = "rml-where-was-i",
	description = "Remember where you left off — by Rusty Medic Labs",
	tags = {"notes", "journal", "reminder", "location", "rml"}
)
public class WhereWasIPlugin extends Plugin
{
	private static final String NOTE_KEY = "nextSteps";
	private static final String VISIT_KEY = "lastVisitV1";
	private static final long CHECKPOINT_MILLIS = 30_000;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ConfigManager configManager;
	@Inject private WhereWasIConfig config;
	@Inject private ScheduledExecutorService executor;
	@Inject private WorldMapPointManager mapPoints;

	private volatile boolean running;
	private volatile WhereWasIPanel panel;
	private volatile NavigationButton navigation;
	private volatile String activeProfile;
	private volatile LastVisit currentVisit;
	private long lastCheckpoint;
	private final EntranceTracker entrances = new EntranceTracker();
	private volatile CurrentMap currentMap;
	private int[] terrain;
	private int terrainWidth;
	private int terrainHeight;
	private int terrainPlane = -1;
	private boolean terrainDirty = true;
	private long terrainRetryAfter;
	private WorldMapPoint savedMapPoint;
	private WorldPoint pendingMapTarget;
	private long mapRequestUntil;
	private volatile long session;
	private CompletableFuture<Void> mapIo = CompletableFuture.completedFuture(null);

	@Provides
	WhereWasIConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(WhereWasIConfig.class);
	}

	@Override
	protected void startUp()
	{
		running = true;
		SwingUtilities.invokeLater(() ->
		{
			if (!running) { return; }
			panel = new WhereWasIPanel((profile, note) ->
				// Capture the account key instead of writing to whichever account is now logged in.
				// ConfigManager updates memory; RuneLite handles persistence.
				configManager.setConfiguration(WhereWasIConfig.GROUP, profile, NOTE_KEY, note),
				(profile, visit, entrance) -> clientThread.invoke(() -> openMap(profile, visit, entrance)));
			navigation = NavigationButton.builder()
				.tooltip("Where Was I?")
				.icon(WhereWasIPanel.createIcon())
				.priority(8).panel(panel).build();
			clientToolbar.addNavigation(navigation);
			clientThread.invoke(this::captureVisit);
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		saveVisit();
		clearMapPoint();
		session++;
		NavigationButton oldNavigation = navigation;
		if (oldNavigation != null) { clientToolbar.removeNavigation(oldNavigation); }
		navigation = null;
		panel = null;
		activeProfile = null;
		currentVisit = null;
		currentMap = null;
		terrain = null;
		terrainDirty = true;
		entrances.reset();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		captureVisit();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		// A map can open between server ticks; follow the local UI lifecycle.
		centrePendingMap();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			saveVisit();
			clearMapPoint();
			session++;
			activeProfile = null;
			currentVisit = null;
			currentMap = null;
			terrain = null;
			terrainDirty = true;
			entrances.reset();
			WhereWasIPanel target = panel;
			if (target != null) { SwingUtilities.invokeLater(target::showLoggedOut); }
		}
		if (event.getGameState() == GameState.LOADING) { terrainDirty = true; }
		// Hopping and loading are deliberately not treated as a new session.
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		saveVisit();
	}

	private void captureVisit()
	{
		WhereWasIPanel target = panel;
		if (!running || target == null || client.getGameState() != GameState.LOGGED_IN
			|| client.getAccountHash() == -1 || client.getAccountHash() == 0) { return; }
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null) { return; }
		// Create the RuneScape profile on a fresh RuneLite installation.
		if (configManager.getRSProfileKey() == null)
		{
			configManager.setRSProfileConfiguration(WhereWasIConfig.GROUP, "profileInitialized", true);
		}
		String profile = configManager.getRSProfileKey();
		if (profile == null) { return; }
		if (!Objects.equals(profile, activeProfile))
		{
			saveVisit();
			activeProfile = profile;
			currentVisit = null;
			currentMap = null;
			terrain = null;
			terrainDirty = true;
			entrances.reset();
			clearMapPoint();
			long accountSession = ++session;
			lastCheckpoint = 0;
			String note = configManager.getConfiguration(WhereWasIConfig.GROUP, profile, NOTE_KEY);
			LastVisit previous = LastVisit.decode(configManager.getConfiguration(WhereWasIConfig.GROUP, profile, VISIT_KEY));
			String name = player.getName();
			SwingUtilities.invokeLater(() ->
			{
				if (running && panel == target && session == accountSession)
				{
					target.showAccount(profile, name, note, previous);
				}
			});
			queueMapIo(() -> loadMap(profile, previous, target, accountSession));
			if (config.welcomeMessage())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Where Was I? Open the sidebar to view your next steps and previous location.", null);
			}
		}
		WorldPoint location = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		if (location == null) { return; }
		currentVisit = new LastVisit(location.getX(), location.getY(), location.getPlane(),
			client.getWorld(), System.currentTimeMillis());
		EntranceTracker.Entry entrance = entrances.update(location);
		if (terrainDirty || terrainPlane != location.getPlane()
			|| (terrain == null && System.currentTimeMillis() >= terrainRetryAfter))
		{
			try
			{
				SpritePixels map = client.drawInstanceMap(location.getPlane());
				if (map == null || map.getPixels() == null) { throw new IllegalStateException("Terrain sprite unavailable"); }
				terrain = map.getPixels().clone();
				terrainWidth = map.getWidth();
				terrainHeight = map.getHeight();
			}
			catch (RuntimeException error)
			{
				terrain = null;
				terrainRetryAfter = System.currentTimeMillis() + 5_000;
				log.debug("Could not capture Where Was I terrain", error);
				mapStatus("Terrain preview couldn't be captured. Check the RuneLite log.");
			}
			terrainPlane = location.getPlane();
			terrainDirty = false;
		}
		currentMap = terrain == null ? null : new CurrentMap(currentVisit, entrance, terrain,
			terrainWidth, terrainHeight, player.getLocalLocation().getSceneX(), player.getLocalLocation().getSceneY());
		if (currentVisit.savedAt - lastCheckpoint >= CHECKPOINT_MILLIS)
		{
			saveVisit();
			lastCheckpoint = currentVisit.savedAt;
		}
	}

	private void saveVisit()
	{
		String profile = activeProfile;
		LastVisit visit = currentVisit;
		if (profile != null && visit != null)
		{
			configManager.setConfiguration(WhereWasIConfig.GROUP, profile, VISIT_KEY, visit.encode());
			CurrentMap map = currentMap;
			if (map != null && map.visit == visit)
			{
				queueMapIo(() -> writeMap(profile, map));
			}
		}
	}

	private synchronized void queueMapIo(Runnable work)
	{
		// RuneLite owns this executor. Serialise our work without blocking the client or EDT.
		mapIo = mapIo.handle((result, error) -> null).thenRunAsync(work, executor);
	}

	private Filepath mapFile(String profile) throws IOException
	{
		Filepath directory = getPluginDirectory();
		directory.createDirectories();
		return directory.joinSegment(UUID.nameUUIDFromBytes(profile.getBytes(StandardCharsets.UTF_8)) + ".map");
	}

	private void writeMap(String profile, CurrentMap map)
	{
		try
		{
			Filepath file = mapFile(profile);
			Filepath temporary = getPluginDirectory().joinSegment("pending.map");
			MapSnapshot snapshot = new MapSnapshot(map.visit, map.entrance,
				MapSnapshot.crop(map.pixels, map.width, map.height, map.sceneX, map.sceneY));
			try (OutputStream output = temporary.openOutputStream()) { snapshot.write(output); }
			try { temporary.moveTo(file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
			catch (java.nio.file.AtomicMoveNotSupportedException ignored)
			{
				temporary.moveTo(file, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException | RuntimeException error)
		{
			log.debug("Could not save Where Was I map preview", error);
			previewStatus(profile, "Terrain preview couldn't be saved. Check the RuneLite log.");
		}
	}

	private void loadMap(String profile, LastVisit previous, WhereWasIPanel target, long accountSession)
	{
		if (previous == null) { return; }
		try
		{
			Filepath file = mapFile(profile);
			if (!file.exists())
			{
				previewStatus(profile, "No terrain preview saved yet. Log out and back in after visiting a location.");
				rebuildPreview(profile, previous, target, accountSession);
				return;
			}
			if (file.size() > 256_000) { throw new IOException("Saved preview exceeds size limit"); }
			MapSnapshot snapshot;
			try (InputStream input = file.openInputStream()) { snapshot = MapSnapshot.read(input); }
			if (snapshot.visit.x != previous.x || snapshot.visit.y != previous.y || snapshot.visit.plane != previous.plane)
			{
				previewStatus(profile, "The terrain preview belongs to an earlier checkpoint. A new preview will be saved this visit.");
				rebuildPreview(profile, previous, target, accountSession);
				return; // Never put a marker over terrain saved at a different location.
			}
			clientThread.invoke(() ->
			{
				if (!running || session != accountSession || !profile.equals(activeProfile)) { return; }
				LastVisit visit = currentVisit;
				if (visit != null) { entrances.restore(snapshot, new WorldPoint(visit.x, visit.y, visit.plane)); }
				setMapPoint(previous, snapshot.entrance);
				SwingUtilities.invokeLater(() ->
				{
					if (running && panel == target && session == accountSession) { target.showMap(profile, snapshot); }
				});
			});
		}
		catch (IOException | RuntimeException error)
		{
			log.debug("Could not load Where Was I map preview", error);
			previewStatus(profile, "Terrain preview couldn't be loaded. Check the RuneLite log.");
			rebuildPreview(profile, previous, target, accountSession);
		}
	}

	private void rebuildPreview(String profile, LastVisit previous, WhereWasIPanel target, long accountSession)
	{
		clientThread.invoke(() ->
		{
			if (!running || session != accountSession || panel != target || !profile.equals(activeProfile)
				|| client.getGameState() != GameState.LOGGED_IN || client.isInInstancedRegion()
				|| terrain == null || terrainPlane != previous.plane) { return; }
			LocalPoint point = LocalPoint.fromWorld(client, new WorldPoint(previous.x, previous.y, previous.plane));
			if (point == null || point.getSceneX() < 0 || point.getSceneY() < 0
				|| point.getSceneX() >= Constants.SCENE_SIZE || point.getSceneY() >= Constants.SCENE_SIZE) { return; }
			// Initial installs can rebuild the saved tile only while its real terrain is loaded.
			// Do not invent a preview for another region or an instance.
			MapSnapshot snapshot = new MapSnapshot(previous, null,
				MapSnapshot.crop(terrain, terrainWidth, terrainHeight, point.getSceneX(), point.getSceneY()));
			setMapPoint(previous, null);
			SwingUtilities.invokeLater(() ->
			{
				if (running && panel == target && session == accountSession)
				{
					target.showMap(profile, snapshot);
					target.mapStatus(profile, "Preview rebuilt from this area's terrain. Click to open the map.");
				}
			});
		});
	}

	private void setMapPoint(LastVisit visit, EntranceTracker.Entry entrance)
	{
		clearMapPoint();
		savedMapPoint = new WorldMapPoint(mapTarget(visit, entrance), WhereWasIPanel.createMapPin());
		savedMapPoint.setName(entrance == null ? "Where Was I? Saved location" : "Where Was I? " + entrance.name);
		savedMapPoint.setTooltip(savedMapPoint.getName());
		savedMapPoint.setImagePoint(new net.runelite.api.Point(12, 30));
		savedMapPoint.setJumpOnClick(true);
		mapPoints.add(savedMapPoint);
	}

	private static WorldPoint mapTarget(LastVisit visit, EntranceTracker.Entry entrance)
	{
		return entrance == null ? new WorldPoint(visit.x, visit.y, 0) : entrance.point;
	}

	private void clearMapPoint()
	{
		if (savedMapPoint != null) { mapPoints.remove(savedMapPoint); savedMapPoint = null; }
		pendingMapTarget = null;
	}

	private void openMap(String profile, LastVisit visit, EntranceTracker.Entry entrance)
	{
		if (!running || !profile.equals(activeProfile) || client.getGameState() != GameState.LOGGED_IN) { return; }
		setMapPoint(visit, entrance);
		pendingMapTarget = mapTarget(visit, entrance);
		mapRequestUntil = System.currentTimeMillis() + 10_000;
		if (!isMapOpen())
		{
			// The orb container owns the menu listener; WORLDMAP is its graphic.
			Widget globe = client.getWidget(InterfaceID.Orbs.ORB_WORLDMAP);
			if (globe == null || globe.getOnOpListener() == null)
			{
				globe = client.getWidget(InterfaceID.Orbs.WORLDMAP);
			}
			if (globe != null && globe.getOnOpListener() != null)
			{
				// Run the globe's local UI listener. No mouse/key injection or server action.
				client.createScriptEventBuilder(globe.getOnOpListener()).setSource(globe).setOp(1).build().run();
			}
		}
		centrePendingMap();
	}

	private void centrePendingMap()
	{
		if (pendingMapTarget == null) { return; }
		Widget widget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		WorldMap map = client.getWorldMap();
		// Check the attached interface, rather than a hidden flag on its cached graphic.
		if (isMapOpen() && widget != null && map != null && map.getWorldMapData() != null)
		{
			boolean onMap = map.getWorldMapData().surfaceContainsPosition(pendingMapTarget.getX(), pendingMapTarget.getY());
			if (onMap) { map.setWorldMapPositionTarget(pendingMapTarget); }
			mapStatus(onMap ? "Gold pin marks your saved location" : "This dungeon is not on this map. No recorded entrance yet.");
			pendingMapTarget = null;
		}
		else if (System.currentTimeMillis() > mapRequestUntil)
		{
			mapStatus("Open the map using the globe, then click the preview again.");
			pendingMapTarget = null;
		}
	}

	private boolean isMapOpen()
	{
		if (client.getComponentTable() == null) { return false; }
		for (WidgetNode node : client.getComponentTable())
		{
			if (node.getId() == InterfaceID.WORLDMAP) { return true; }
		}
		return false;
	}

	private void mapStatus(String text)
	{
		WhereWasIPanel target = panel;
		String profile = activeProfile;
		if (target != null) { SwingUtilities.invokeLater(() -> target.mapStatus(profile, text)); }
	}

	private void previewStatus(String profile, String text)
	{
		WhereWasIPanel target = panel;
		long accountSession = session;
		if (target != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				if (running && panel == target && session == accountSession) { target.mapStatus(profile, text); }
			});
		}
	}

	private static final class CurrentMap
	{
		final LastVisit visit;
		final EntranceTracker.Entry entrance;
		final int[] pixels;
		final int width, height, sceneX, sceneY;
		CurrentMap(LastVisit visit, EntranceTracker.Entry entrance, int[] pixels, int width, int height, int sceneX, int sceneY)
		{
			this.visit = visit;
			this.entrance = entrance;
			this.pixels = pixels;
			this.width = width;
			this.height = height;
			this.sceneX = sceneX;
			this.sceneY = sceneY;
		}
	}
}
