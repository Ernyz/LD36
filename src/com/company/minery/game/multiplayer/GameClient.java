package com.company.minery.game.multiplayer;

import com.badlogic.gdx.utils.Array;
import com.company.minery.Constants;
import com.company.minery.game.Game;
import com.company.minery.game.GameUpdate;
import com.company.minery.game.multiplayer.messages.ClientAssignmentMessage;
import com.company.minery.game.multiplayer.messages.ImpulseMessage;
import com.company.minery.game.multiplayer.messages.ObjectMessage;
import com.company.minery.game.multiplayer.messages.PlayerMessage;
import com.company.minery.game.multiplayer.messages.SpearMessage;
import com.company.minery.game.multiplayer.messages.WorldStateMessage;
import com.company.minery.game.player.PhysicalObject;
import com.company.minery.game.player.Player;
import com.company.minery.game.player.Player.MovementDirection;
import com.company.minery.game.player.Spear;
import com.company.minery.utils.AssetResolution;
import com.company.minery.utils.kryonet.Client;
import com.company.minery.utils.kryonet.Connection;
import com.company.minery.utils.kryonet.Listener;

public final class GameClient implements GameEndpoint {

	private final Game game;
	private final Client client;
	private final GameUpdate worldUpdate = new GameUpdate(this);
	private final Runnable disconnectCallback;
	private final Array<Object> receivedObjects = new Array<Object>();
	
	public GameClient(final Game game,
							final Runnable disconnectCallback) {
		
		this.game = game;
		this.disconnectCallback = disconnectCallback;
		
		client = new Client();
		client.addListener(new Listener() {
			@Override
			public void connected(final Connection connection) {
				System.out.println("connected");
			}
			
			@Override
			public void disconnected(final Connection connection) {
				if(disconnectCallback != null) {
					disconnectCallback.run();
				}
			}
			
			@Override
			public void received(final Connection connection, 
								 final Object object) {
				
				receivedObjects.add(object);
			}
			
			@Override
			public void idle(final Connection connection) {
			}
		});
		Multiplayer.register(client);
	}
	
	public void begin(final String serverIpAddress, 
					  final int tcpPort, 
					  final int udpPort) {
		
		client.start();
		
		try {
			client.connect(10000, serverIpAddress, tcpPort, udpPort);
		}
		catch(final Exception ex) {
			ex.printStackTrace();
			if(disconnectCallback != null) {
				disconnectCallback.run();
			}
		}
		
		game.currentMap().physicalObjects.clear();
		game.players.clear();
	}
	
	@Override
	public void end() {
		client.close();
	}
	
	@Override
	public void update(final float deltaTime) {
		final boolean requestsJump = game.localPlayer().requestsJump;
		final Player.MovementDirection movementDirection = game.localPlayer().movementDirection;
		
		synchronized(this) {
			final Array<Object> receivedObjects = this.receivedObjects;
			final int n = receivedObjects.size;
			
			for(int i = 0; i < n; i += 1) {
				final Object object = receivedObjects.get(i);
				
				if(object instanceof WorldStateMessage) {
					final WorldStateMessage worldState = (WorldStateMessage) object;
					
					final PlayerMessage[] players = worldState.players;
					final SpearMessage[] spears = worldState.spears;
					
					final AssetResolution dataResolution = Constants.RESOLUTION_LIST[worldState.resolutionIndex];
					final float scale = dataResolution.calcScale() / game.assets.resolution.calcScale();
					
					for(int ii = 0; ii < players.length; ii += 1) {
						final PlayerMessage message = players[ii];
						
						boolean found = false;
						
						for(int iii = 0; iii < game.players.size; iii += 1) {
							final Player player = game.players.get(iii);
							
							if(player.uid == message.uid) {
								setPlayerState(player, message);
								found = true;
								break;
							}
						}
						
						if(!found) {
							final Player player = new Player(false, message.uid);
							player.applyAppearance(game.assets);
							game.players.add(player);
							game.currentMap().physicalObjects.add(player);
							setPlayerState(player, message);
						}
					}
					
					for(int ii = 0; ii < spears.length; ii += 1) {
						final SpearMessage message = spears[ii];
						
						boolean found = false;
						
						for(int iii = 0; iii < game.spears.size; iii += 1) {
							final Spear spear = game.spears.get(iii);
							
							if(spear.uid == message.uid) {
								setSpearState(spear, message);
								found = true;
								break;
							}
						}
						
						if(!found) {
							final Spear spear = new Spear(message.uid);
							spear.applyAppearance(game.assets);
							game.spears.add(spear);
							game.currentMap().physicalObjects.add(spear);
							setSpearState(spear, message);
						}
					}
					
					// TODO: handle removal
				}
				else if(object instanceof ClientAssignmentMessage) {
					final ClientAssignmentMessage clientAssignment = (ClientAssignmentMessage) object;
					
					// TODO: load map here
					
					final Player localPlayer = new Player(true, clientAssignment.playerUidAssignment);
					localPlayer.applyAppearance(game.assets);
					game.setLocalPlayer(localPlayer);
					game.inputTranslator.setListener(localPlayer);
					game.currentMap().physicalObjects.add(localPlayer);
					game.players.add(localPlayer);
				
					final AssetResolution dataResolution = Constants.RESOLUTION_LIST[clientAssignment.resolutionIndex];
					final float scale = dataResolution.calcScale() / game.assets.resolution.calcScale();
					
					localPlayer.x = clientAssignment.x * scale;
					localPlayer.y = clientAssignment.y * scale;
				}
			}
			
			receivedObjects.clear();
		}
		
		worldUpdate.update(deltaTime, game);
		
		if(client.isConnected()) {
			final ImpulseMessage impulseMessage = new ImpulseMessage();
			
			impulseMessage.jumpFlag = requestsJump;
			impulseMessage.movementFlag = (byte) movementDirection.id;
			impulseMessage.messageTime = System.currentTimeMillis();
			
			client.sendUDP(impulseMessage);
		}
	}
	
	private void setPlayerState(final Player player, final PlayerMessage message) {
		setObjectState(player, message);
		player.flip(message.flip);
		player.hasWeapon = message.hasWeapon;
		player.requestsAttack = message.requestsAttack;
	}
	
	private void setSpearState(final Spear spear, final SpearMessage message) {
		setObjectState(spear, message);
		spear.lastRotation = message.lastRotation;
	}
	
	private void setObjectState(final PhysicalObject object, 
							  	final ObjectMessage message) {
		
		object.requestsJump = message.requestsJump;
		object.isInAir = message.isJumping;
		object.velocityX = message.velocityX;
		object.velocityY = message.velocityY;
		object.movementDirection = MovementDirection.valueOf(message.movementDirection);
		object.velocityX = message.velocityX;
		object.velocityY = message.velocityY;
		object.x = message.x;
		object.y = message.y;
	}
	
}