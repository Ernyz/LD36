package com.company.minery.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.company.minery.Constants;
import com.company.minery.game.map.Layer;
import com.company.minery.game.map.Map;
import com.company.minery.game.map.Tile;
import com.company.minery.game.map.Tiles;
import com.company.minery.game.multiplayer.GameEndpoint;
import com.company.minery.game.player.PhysicalObject;
import com.company.minery.game.player.Player;
import com.company.minery.game.player.Player.MovementDirection;
import com.company.minery.game.player.Spear;

public final class GameUpdate {

	private final GameEndpoint gameClient;
	private final Vector2 tmpVector = new Vector2();
	
	public GameUpdate(final GameEndpoint gameClient) {
		this.gameClient = gameClient;
	}
	
	private Map map;
	
	public void update(final float deltaTime,
					   final Game game) {

		final Map map = game.currentMap();
		this.map = map;
		
		final float tileWidth = map.tileWidth;
		final float tileHeight = map.tileHeight;
		
		final float maxFallSpeed = -tileHeight * Constants.MAX_FALL_SPEED;
		final float gravity = tileHeight * Constants.GRAVITY;
		
		final Array<PhysicalObject> physicalObjects = map.physicalObjects;

		final Vector2 tmpVector = this.tmpVector;
		
		final Layer mainLayer = map.mainLayer;
		final Tiles mainLayerTiles = mainLayer.tiles;
		final byte[] mainLayerByteTiles = mainLayerTiles.tiles;
		final Tile[] mainLayerTileset = mainLayerTiles.tileset;
		final int mainLayerWidth = mainLayerTiles.width;
		final int mainLayerHeight = mainLayerTiles.height;
		
		final float maxPlayerVelocityX = Constants.RUN_SPEED * tileWidth;
		final float maxPlayerVelocityY = Constants.JUMP_HEIGHT * tileHeight;
		
		final float maxSpearVelocity = Constants.JUMP_HEIGHT * 2f * tileHeight;
		
		for(int i = 0; i < physicalObjects.size; i += 1) {
			final PhysicalObject object = physicalObjects.get(i);
			
			// ************************************
			// UPDATE OBJECT
			// ************************************
			{
				//update pawn FlipX
				final MovementDirection dir = object.movementDirection;

				if(object instanceof Player) {
					final Player player = (Player) object;
					
					if(dir == MovementDirection.Left) {
						((Player) object).flip(true);
					} 
					else if(dir == MovementDirection.Right) {
						((Player) object).flip(false);
					}
					
					if(player.requestsAttack && player.hasWeapon) {
						final Spear spear = new Spear();
						spear.applyAppearance(game.assets);
						
						final float spawnPointX = player.x + player.width / 2f;
						final float spawnPointY = player.y + player.height / 2f;
						
						tmpVector.set(player.attackX - spawnPointX, player.attackY - spawnPointY);
						final float angle = tmpVector.angle() - 90f;
						
						tmpVector.set(0, maxSpearVelocity);
						tmpVector.rotate(angle);
						
						spear.x = spawnPointX;
						spear.y = spawnPointY;
						spear.velocityX = tmpVector.x;
						spear.velocityY = tmpVector.y;
						spear.movementDirection = spear.velocityX < 0 ? MovementDirection.Left : MovementDirection.Right;
						
						game.spears.add(spear);
						map.physicalObjects.add(spear);
						
						object.animationTimer = 0f;
						player.hasWeapon = false;
					}
					
					player.requestsAttack = false;
				}
				
				// update pawn jump animation
				boolean jumpStart = object.requestsJump;
				boolean inAir = object.isInAir;
				jumpStart = jumpStart && !inAir;
				
				if(jumpStart) {
					object.animationTimer = 0f;
				}
				
				final float animationTimer = object.animationTimer;
				
				//if(inAir && pawn.jumpAnimation() != null) {
					/*pawn.jumpAnimation().mix(
							skeleton, 
							animationTimer, 
							animationTimer + deltaTime, 
							false, 
							null,
							0.3f);*/
				//}
				
				// update pawn idle or run animation if not in air
				if(!inAir) {
					/*if(!pawn.isRunning) {
						pawn.idleAnimation().apply(
								skeleton, 
								animationTimer, 
								animationTimer + deltaTime, 
								true, 
								null);
					}
					else {
						pawn.runAnimation().apply(
								skeleton, 
								animationTimer, 
								animationTimer + deltaTime, 
								true, 
								null);
					}*/
				}
				
				object.animationTimer = animationTimer + deltaTime;
				//skeleton.updateWorldTransform();
			}
			
			// ************************************
			// SET/UPDATE VELOCITY
			// ************************************
			
			if(object instanceof Player) {
				object.velocityX = object.movementDirection.mul * maxPlayerVelocityX;
			}
			
			{
				if(object.velocityX != 0f) {
					object.isRunning = true;
				}
				else {
					object.isRunning = false;
				}
				
				if(object.requestsJump) {
					object.requestsJump = false;
					
					if(!object.isInAir) {
						object.isJumping = true;
						object.isInAir = true;
						object.velocityY = maxPlayerVelocityY;
					}
				}
				else {
					if(!(object instanceof Spear && object.movementDirection == MovementDirection.Idle)) {
						float velY = object.velocityY - gravity * deltaTime;
						
						if(velY < maxFallSpeed) {
							velY = maxFallSpeed;
						}
						object.velocityY = velY;
					}
				}
			}
			
			final float pawnX;
			final float pawnY;
			final float pawnWidth = object.width;
			final float pawnHeight = object.height;
			
			// ************************************
			// APPLY MOVEMENT/COLLISIONS
			// ************************************
			{
				final float currentX = object.x;
				final float currentY = object.y;
				
				final float velocityX = object.velocityX;
				final float velocityY = object.velocityY;
				
				final float normVelocityX = velocityX * deltaTime;
				final float normVelocityY = velocityY * deltaTime;
				
				final int segmentsX = 8; // TODO: calculate by the movement amount
				final int segmentsY = 8; // TODO: calculate by the movement amount
				final int segments = Math.max(segmentsX, segmentsY);
				
				float xMod = 1f;
				float yMod = 1f;
				
				{
					final float segmentStep = 1f / segments;
					for(int ii = segments; ii >= 1; ii -= 1) {
						final float mod = (segmentStep * ii);
						xMod = mod;
						yMod = mod;
						
						final float x = currentX + normVelocityX * xMod;
						final float y = currentY + normVelocityY * yMod;
						
						if(!checkPawnCollision(object, x, y, x + pawnWidth, y + pawnHeight, map)) {
							break;
						}
						else {
							xMod = 0f;
							yMod = 0f;
						}
					}
				}
				
				{
					final float y = currentY + normVelocityY * yMod;
					final float segmentStep = 1f / segmentsX;
					for(int ii = segments; ii >= 1; ii -= 1) {
						final float mod = (segmentStep * ii);
						xMod = mod;
						
						final float x = currentX + normVelocityX * xMod;
						
						if(!checkPawnCollision(object, x, y, x + pawnWidth, y + pawnHeight, map)) {
							break;
						}
						else {
							xMod = 0f;
						}
					}
				}
				
				{
					final float x = currentX + normVelocityX * xMod;
					final float segmentStep = 1f / segmentsY;
					for(int ii = segments; ii >= 1; ii -= 1) {
						final float mod = (segmentStep * ii);
						yMod = mod;
						
						final float y = currentY + normVelocityY * yMod;
						
						if(!checkPawnCollision(object, x, y, x + pawnWidth, y + pawnHeight, map)) {
							object.isInAir = true;
							break;
						}
						else {
							yMod = 0f;
						}
					}
				}
				
				if(object instanceof Spear) {
					if(xMod != 1 || yMod != 1) {
						object.velocityX = 0;
						object.velocityY = 0;
						object.movementDirection = MovementDirection.Idle;
					}
				}
				
				pawnX = currentX + normVelocityX * xMod;
				pawnY = currentY + normVelocityY * yMod;
				
				object.x = pawnX;
				object.y = pawnY;

				// If hits sides
				if(xMod == 0) {
					object.isRunning = false;
				}
				
				// If hits the ground or top.
				if(yMod == 0f) {
					// If velocity is less than 0 this means that pawn has hit the ground.
					if(normVelocityY < 0f) {
						object.isInAir = false;
					}
					object.velocityY = 0f;
				}
				
				if(object.velocityY <= 0f) {
					object.isJumping = false;
				}
			}
		}
	}
	
	private boolean checkPawnCollision(final PhysicalObject object, 
									   final float pawnX, 
									   final float pawnY, 
									   final float pawnRight, 
									   final float pawnTop,
									   final Map map) {
		
		final float tileWidth = map.tileWidth;
		final float tileHeight = map.tileHeight;
		
		final Tiles tiles = map.mainLayer.tiles;
		final byte[] tileIndexes = tiles.tiles;
		final int tilesWidth = tiles.width;
		
		final int tileLeft = (int)(pawnX / tileWidth);
		final int tileBottom = (int)(pawnY / tileHeight);
		final int tileRight = (int)(pawnRight / tileWidth);
		final int tileTop = (int)(pawnTop / tileHeight);
		
		for(int yi = tileBottom; yi <= tileTop; yi += 1) {
			for(int xi = tileLeft; xi <= tileRight; xi += 1) {
				final byte tileIndex = tileIndexes[yi * tilesWidth + xi];
				
				if(tileIndex == -128) {
					continue;
				}
				
				return true;
			}
		}
		
		return false;
	}
	
}
