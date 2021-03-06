package com.company.minery.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.company.minery.App;
import com.company.minery.Constants;
import com.company.minery.game.GameAssets.TextureRegionExt;
import com.company.minery.game.map.Generator;
import com.company.minery.game.map.Map;
import com.company.minery.game.map.MapLocation;
import com.company.minery.game.multiplayer.GameClient;
import com.company.minery.game.player.InputTranslator;
import com.company.minery.game.player.Player;
import com.company.minery.game.player.Spear;
import com.company.minery.utils.AssetResolution;

public final class Game implements Disposable {
	
	private final GameClient client;
	
	public final GameAssets assets;
	
	private Player localPlayer; /**/ public Player localPlayer() { return localPlayer; }
	public final Array<Player> players = new Array<Player>();
	public final Array<Spear> spears = new Array<Spear>();
	
	public final InputTranslator inputTranslator;
	
	private float lastSizeScale;
	public boolean playing;
	
	public float messageTimer;
	public TextureRegionExt message;
	
	private Map currentMap; /**/ public Map currentMap() { return currentMap; }
	
	public final App app;
	
	public Game(final App app) {
		this.app = app;
		this.assets = new GameAssets();

		client = new GameClient(this);
		
		inputTranslator = new InputTranslator(this);
		inputTranslator.setMovementKeys(Keys.A, Keys.D, Keys.W);
	}
	
	public void setLocalPlayer(final Player localPlayer) {
		this.localPlayer = localPlayer;
	}
	
	public void begin() {
		this.message = null;
		this.messageTimer = 0f;
		
		localPlayer = new Player(true);
		
		inputTranslator.setListener(localPlayer);
		
		players.clear();
		spears.clear();
		
		players.add(localPlayer);
		
		currentMap = Generator.generateTestMap(assets);
		
		final MapLocation startLocation = currentMap.findLocationByName("p1_start");
		
		localPlayer.x = startLocation.x + startLocation.width / 2f - localPlayer.width / 2f;
		localPlayer.y = startLocation.y;
		
		
		setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), assets.resolution);
		
		lastSizeScale = assets.resolution.calcScale();
		
		currentMap.setScale(lastSizeScale * Constants.PIXELART_SCALE);
		
		if(!client.begin(Constants.SERVER_IP, Constants.DEFAULT_TCP_PORT, Constants.DEFAULT_UDP_PORT)) {
			exit();
			return;
		}
		
		currentMap.physicalObjects.add(localPlayer);
	}
	
	public void end() {
		client.end();
	}
	
	public void setSize(final float width, 
						final float height,
						final AssetResolution assetResolution) {
		
		assets.rescale(assetResolution);
		
		// TODO: apply appearances
		for(int i = 0; i < players.size; i += 1) {
			players.get(i).applyAppearance(assets);
		}
		
		if(playing) {
			final float sizeScale = assets.resolution.calcScale();
			final float sizeRescale = sizeScale / lastSizeScale;
			lastSizeScale = sizeScale;

			currentMap.setScale(sizeRescale);
			currentMap.assetLoader.load(currentMap, assets);
		}
	}
	
	private void limitView() {
		final float screenWidth = Gdx.graphics.getWidth();
		final float screenHeight = Gdx.graphics.getHeight();
		final float mapWidth = currentMap.tileWidth * currentMap.mainLayer.tiles.width;
		final float mapHeight = currentMap.tileHeight * currentMap.mainLayer.tiles.height;
		
		float viewX = currentMap.viewX;
		float viewY = currentMap.viewY;
		
		if(viewX < 0f) {
			viewX = 0f;
		}
		if(viewX + screenWidth > mapWidth) {
			viewX = mapWidth - screenWidth;
		}
		
		if(viewY < 0f) {
			viewY = 0f;
		}
		if(viewY + screenHeight > mapHeight) {
			viewY = mapHeight - screenHeight;
		}
		
		currentMap.setViewPosition(viewX, viewY);
	}
	
	private void updateView(final float delta) {
		// Adjust the view to player
		{
			final Map currentMap = this.currentMap;
			final float viewX = currentMap.viewX;
			final float viewY = currentMap.viewY;
			
			final float targetViewX = localPlayer.x + localPlayer.width / 2f - Gdx.graphics.getWidth() / 2f;
			final float targetViewY = localPlayer.y + localPlayer.height / 2f - Gdx.graphics.getHeight() / 2f;
			
			final float percent = delta / Constants.CAMERA_FOLLOW_SPEED;
			
			currentMap.setViewPosition(
					viewX + (targetViewX - viewX) * percent, 
					viewY + (targetViewY - viewY) * percent);
		}
		
		limitView();
	}

	public void update(final float delta) {
		if(message != null) {
			messageTimer += delta;
			
			if(messageTimer >= Constants.MESSAGE_TIME) {
				if(Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Keys.ANY_KEY)) {
					if(message != assets.fightLabel) {
						message = null;
						exit();
						return;
					}
					
					message = null;
				}
			}
		}
		
		inputTranslator.update();
		client.update(delta);
		updateView(delta);
	}
	
	public void exit() {
		playing = false;
		app.setScreen(app.menuScreen);
	}
	
	@Override
	public void dispose() {
		assets.dispose();
	}
	
}
