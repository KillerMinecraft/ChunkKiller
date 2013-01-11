package com.ftwinston.Killer.ChunkKiller;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Option;
import com.ftwinston.Killer.WorldConfig;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

public class ChunkKiller extends GameMode
{
	public static final int useSlaves = 0;	
	@Override
	public int getMinPlayers() { return 2; } // one player on each team is our minimum
	
	@Override
	public Option[] setupOptions()
	{
		Option[] options = {
			new Option("Losing players become slaves", true)
		};
		
		return options;
	}
	
	@Override
	public String getHelpMessage(int num, int team)
	{
		switch ( num )
		{
			case 0:
			default:
				return null;
		}
	}
	
	@Override
	public boolean teamAllocationIsSecret() { return false; }
	
	@Override
	public boolean isLocationProtected(Location l, Player p)
	{
		// true if i'm a slave and this is my master's chunk, or if its nobody's chunk and the warm-up period hasn't expired
		return false;
	}
	
	@Override
	public boolean isAllowedToRespawn(Player player) { return getOption(useSlaves).isEnabled(); }
	
	@Override
	public boolean useDiscreetDeathMessages() { return false; }
	
	@Override
	public Environment[] getWorldsToGenerate() { return new Environment[] { Environment.NORMAL }; }
	
	@Override
	public void beforeWorldGeneration(int worldNumber, WorldConfig world)
	{
		// get a list of all players, in a random order
		List<Player> players = getOnlinePlayers();
		Collections.shuffle(players);
		
		// set up an array of player names, so we can easily determine the index (and thus the chunk) of any given player
		playerIndices = new String[players.size()];
		for ( int i=0; i<playerIndices.length; i++ )
			playerIndices[i] = players.get(i).getName();
		
		chunkRows = (int)Math.ceil(Math.sqrt(playerIndices.length));
		chunkCols = (int)Math.ceil((double)playerIndices.length/chunkRows);
		chunksOnLastRow = playerIndices.length % chunkRows;
		if ( chunksOnLastRow == 0 )
			chunksOnLastRow = chunkCols;
		
		world.setWorldType(WorldType.FLAT);
		world.setGenerator(new PlayerChunkGenerator());
	}
	
	public class PlayerChunkGenerator extends ChunkGenerator
	{
		int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, lastRowChunk;
		public PlayerChunkGenerator()
		{
			chunkMinX = 0; chunkMaxX = (chunkRows-1) * chunkSpacing;
			chunkMinZ = 0; chunkMaxZ = (chunkCols-1) * chunkSpacing;
			lastRowChunk = (chunksOnLastRow-1) * chunkSpacing;
		}
		
		@Override
		public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes)
		{
			if ( x < chunkMinX || x > chunkMaxX || z < chunkMinZ || z > chunkMaxZ || x % chunkSpacing != 0 || z % chunkSpacing != 0 || (x == chunkMaxX && z > lastRowChunk) )
				return new byte[1][];
			
			// a player chunk goes here
			final int numChunkSections = 4;
			byte[][] data = new byte[numChunkSections][];
			
			// stone
			for ( int i=0; i<numChunkSections; i++ )
			{
				data[i] = new byte[4096];
				for ( int j=0; j<4096; j++ )
					data[i][j] = 1;
			}

			// bedrock
			int i = 0;
			for ( int j=0; j<256; j++ )
				data[i][j] = 7;
			
			// dirt
			i = numChunkSections - 1;
			for ( int j=2048; j<3840; j++ )
				data[i][j] = 3;
			
			// grass
			for ( int j=3840; j<4096; j++ )
				data[i][j] = 2;
			
			return data;
		}
		
		void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
	        if (result[y >> 4] == null) {
	            result[y >> 4] = new byte[4096];
	        }
	        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
	    }
	}
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		// each player should spawn on their own chunk ... unless they've been defeated, in which case they should spawn on their master's chunk

		Chunk c = getChunkForPlayer(player);
		
		int x = (c.getX() << 4) + 7, z = (c.getZ() << 4) + 7;
		System.out.println("x = " + x + ", z = " + z);
		return c.getWorld().getHighestBlockAt(x, z).getRelative(BlockFace.UP).getLocation();
	}
	
	String[] playerIndices;
	int chunkRows, chunkCols, chunksOnLastRow, chunkSpacing = 3;
	
	@Override
	public void gameStarted(boolean isNewWorlds)
	{
		if ( !isNewWorlds )
			; // this ... wouldn't work. Game mode ought to have a function for whether this is allowed at all??
	}
	
	public int getPlayerIndex(Player player)
	{
		for ( int i=0; i< playerIndices.length; i++ )
			if ( player.getName().equals(playerIndices[i]) )
				return i;
		return 0;
	}
	
	public Chunk getChunkForPlayer(Player player)
	{
		int index = getPlayerIndex(player);
		int row = index / chunkRows, col = index % chunkRows;
		return getWorld(0).getChunkAt(row * chunkSpacing, col * chunkSpacing);
	}
	
	@Override
	public void gameFinished()
	{
		
	}
	
	@Override
	public void playerJoinedLate(Player player, boolean isNewPlayer)
	{
		if ( !isNewPlayer )
			return;
		
		// make them be a spectator
	}
	
	@Override
	public void playerKilledOrQuit(OfflinePlayer player)
	{
		if ( hasGameFinished() )
			return;
		
		// see if there are one (or fewer) un-defeated players left. If that's the case, call finishGame();
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		return getSpawnLocation(player);
	}
}
