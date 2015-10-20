package com.ftwinston.KillerMinecraft.Modules.ChunkKiller;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.Helper;
import com.ftwinston.KillerMinecraft.Option;
import com.ftwinston.KillerMinecraft.WorldConfig;
import com.ftwinston.KillerMinecraft.Configuration.TeamInfo;
import com.ftwinston.KillerMinecraft.Configuration.ToggleOption;

public class ChunkKiller extends GameMode
{
	ToggleOption useSlaves, outlastYourOwnChunk;	
	
	String[] playerIndices;
	int[] playerScores;
	int[] slaveMasters;
	int chunkRows, chunkCols, chunksOnLastRow;
	static final int chunkCoreY = 63, chunkCoreX = 8, chunkCoreZ = 8, chunkSpacing = 3;
	static final Material coreMaterial = Material.EMERALD_BLOCK;
	
	@Override
	public int getMinPlayers() { return 2; } // one player on each team is our minimum
	
	@Override
	public Option[] setupOptions()
	{
		useSlaves = new ToggleOption("Losing players become slaves", true);
		outlastYourOwnChunk = new ToggleOption("Outlast your own chunk", false);
		
		ToggleOption.ensureOnlyOneEnabled(useSlaves, outlastYourOwnChunk);
		return new Option[] { useSlaves, outlastYourOwnChunk };
	}
	
	@Override
	public String getHelpMessage(int num, TeamInfo team)
	{
		switch ( num )
		{
			case 0:
			default:
				return null;
		}
	}
	
	@Override
	public boolean isLocationProtected(Location l, Player player)
	{
		// cores can only be affected directly by players (not by explosions or pistons)
		if ( player == null )
			return l.getBlock().getType() == coreMaterial;
		
		// if a player is a slave, their master's chunk is protected from them
		int index = getPlayerIndex(player);
		int slaveMaster = slaveMasters[index];
		
		if ( slaveMaster != -1 && l.getChunk() == getChunkByIndex(slaveMaster) )
			return true;
		
		return false;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerKilled(PlayerDeathEvent event)
	{	
		// you can respawn if your chunk is still alive, OR if slaves are enabled and you have a master
		int index = getPlayerIndex(event.getEntity());
		
		if (playerScores[index] <= 0 && (!useSlaves.isEnabled() || slaveMasters[index] == -1))
			Helper.makeSpectator(getGame(), event.getEntity());
	}

	@Override
	public boolean allowWorldGeneratorSelection() { return false; }
	
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
		playerScores = new int[playerIndices.length];
		slaveMasters = new int[playerIndices.length];
		for ( int i=0; i<playerIndices.length; i++ )
		{
			playerIndices[i] = players.get(i).getName();
			playerScores[i] = chunkCoreY;
			slaveMasters[i] = -1;
		}
		
		chunkRows = (int)Math.ceil(Math.sqrt(playerIndices.length));
		chunkCols = (int)Math.ceil((double)playerIndices.length/chunkRows);
		chunksOnLastRow = playerIndices.length % chunkRows;
		if ( chunksOnLastRow == 0 )
			chunksOnLastRow = chunkCols;
		
		world.setWorldType(WorldType.FLAT);
		world.setGenerator(new PlayerChunkGenerator(this));
	}
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		// each player should spawn on their own chunk ... unless they're a slave (having been defeated),
		// in which case they should spawn on their master's chunk. Assuming slavery is enabled.
		
		int index = getPlayerIndex(player);
		
		if ( playerScores[index] <= 0 && useSlaves.isEnabled() )
			index = slaveMasters[index];
		
		Chunk c = getChunkByIndex(index);
		Block spawn = c.getBlock(chunkCoreX, chunkCoreY, chunkCoreZ);
		
		return Helper.findSpaceForPlayer(new Location(c.getWorld(), spawn.getX(), spawn.getY() + 1, spawn.getZ()));
	}
	
	@Override
	public void gameStarted()
	{

	}
	
	public int getPlayerIndex(Player player)
	{
		for ( int i=0; i< playerIndices.length; i++ )
			if ( player.getName().equals(playerIndices[i]) )
				return i;
		return 0;
	}
	
	public Chunk getChunkByIndex(int index)
	{
		int row = index % chunkRows, col = index / chunkRows;
		return getWorld(0).getChunkAt(row * chunkSpacing, col * chunkSpacing);
	}
	
	public int getIndexOfChunk(Chunk c)
	{
		return (c.getX() / chunkSpacing) * chunkCols + (c.getZ() / chunkSpacing);
	}
	
	public int getIndexByName(String name)
	{
		for ( int i=0; i<playerIndices.length; i++ )
			if ( name.equals(playerIndices[i]) )
				return i;
		return -1;
	}
	
	@Override
	public void gameFinished()
	{
		
	}
	
	@Override
	public void playerJoinedLate(Player player)
	{
		// make them be a spectator
		Helper.makeSpectator(getGame(), player);
	}
	
	@Override
	public void playerQuit(OfflinePlayer player)
	{
		if ( hasGameFinished() )
			return;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		int index = getPlayerIndex(event.getEntity());
		if ( slaveMasters[index] != -1 )
			index = slaveMasters[index];
		Chunk c = getChunkByIndex(index);
		PlayerInventory inv = event.getEntity().getInventory();
		
		ItemStack items[] = inv.getContents();
		inv.clear();
		
		World w = event.getEntity().getWorld();
		
		Block b = c.getBlock(1, chunkCoreY, 1);
		int minX = b.getX(), minZ = b.getZ();
		b = c.getBlock(15, chunkCoreY, 15);
		int maxX = b.getX(), maxZ = b.getZ();
		
		if ( minX > maxX )
		{
			int tmp = minX;
			minX = maxX;
			maxX = tmp;
		}
		
		if ( minZ > maxZ )
		{
			int tmp = minZ;
			minZ = maxZ;
			maxZ = tmp;
		}
		
		for ( ItemStack item : items )
		{
			if (item == null)
				continue;
			
			int x = minX + random.nextInt(14), z = minZ + random.nextInt(14);
			Location loc = new Location(w, x, w.getHighestBlockYAt(x,  z) + 1, z);
			w.dropItem(loc, item);
		}
	}
	
	@Override
	public Location getCompassTarget(Player player)
	{
		return getSpawnLocation(player);
	}
	
	@Override
	public int getMonsterSpawnLimit(int quantity)
	{
		switch ( quantity )
		{
		case 0:
			return 0;
		case 1:
			return 5;
		case 2:
		default:
			return 10;
		case 3:
			return 15;
		case 4:
			return 25;
		}
	}
	
	@Override
	public int getAnimalSpawnLimit(int quantity)
	{
		switch ( quantity )
		{
		case 0:
			return 0;
		case 1:
			return 2;
		case 2:
		default:
			return 4;
		case 3:
			return 7;
		case 4:
			return 10;
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event)
    {
		Block b = event.getBlock();
		if (b.getType() != coreMaterial)
			return;
		
    	event.setExpToDrop(50);
		
		Chunk c = b.getChunk();
		
		int index = getIndexOfChunk(c);
		Player victimPlayer = Helper.getPlayer(playerIndices[index]);
		if ( victimPlayer != null )
			victimPlayer.playSound(victimPlayer.getLocation(), Sound.ANVIL_LAND, 1, 0);

    	int scoreRemaining = --playerScores[index];
    	
    	int yToRemove = chunkCoreY - scoreRemaining;
		
		// remove levels of this chunk, from the bottom up
		for ( int x=0; x<16; x++ )
			for ( int z=0; z<16; z++ )
			{
				Block remove = c.getBlock(x,yToRemove,z);
				if ( remove != b )
					remove.setType(Material.AIR);
			}
		
		// this chunk's player hasn't yet been defeated, don't continue
		if (scoreRemaining > 0)
		{
			event.setCancelled(true);
			return;
		}
		
		Player killerPlayer = event.getPlayer();

		if ( killerPlayer != null )
			broadcastMessage(ChatColor.RED + playerIndices[index] + "'s chunk was destroyed by " + killerPlayer.getName());
		else
			broadcastMessage(ChatColor.RED + playerIndices[index] + "'s chunk was destroyed");
		
		// check for game end
		int numRemaining = 0, remainingIndex = 0;
		for ( int i=0; i<playerScores.length; i++ )
			if ( playerScores[i] > 0 )
			{
				numRemaining++;
				remainingIndex = i;
			}
		
		if ( numRemaining == 1 )
		{
			broadcastMessage(ChatColor.YELLOW + "Only one chunk remaining - " + playerIndices[remainingIndex] + " wins the game!");
			finishGame();
		}
		else if ( numRemaining == 0 )
		{
			broadcastMessage(ChatColor.YELLOW + "All chunks have been destroyed, game drawn");
			finishGame();
		}
		else if ( useSlaves.isEnabled() )
		{// this player becomes a slave of the player who defeated them, as do any slaves THEY may have
			int newMaster = killerPlayer == null ? -1 : getIndexByName(killerPlayer.getName());
			slaveMasters[index] = newMaster;
			victimPlayer.sendMessage(ChatColor.YELLOW + "You are now a slave of " + killerPlayer.getName());
			killerPlayer.sendMessage(ChatColor.YELLOW + victimPlayer.getName() + " is now your slave");
			
			for ( int i=0; i<slaveMasters.length; i++ )
				if ( slaveMasters[i] == index )
				{
					slaveMasters[i] = newMaster;
					Player slave = Helper.getPlayer(playerIndices[i]);
					if ( slave != null )
					{
						slave.sendMessage(ChatColor.YELLOW + "You are now a slave of " + killerPlayer.getName());
						killerPlayer.sendMessage(ChatColor.YELLOW + slave.getName() + " is now your slave");
					}
				}
		}
		else if ( victimPlayer != null && !outlastYourOwnChunk.isEnabled() )
			victimPlayer.setHealth(0); // this player should die, now
    }
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event)
    {
    	if ( event.getBlock().getType() == coreMaterial )
			event.setCancelled(true);
    }
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent event)
	{
		if ( !(event.getEntity() instanceof Player) )
			return;

		Player attacker = Helper.getAttacker(event);
		if ( attacker == null )
			return;
					
		Player victim = (Player)event.getEntity();
		
		int victimIndex = getPlayerIndex(victim);
		int attackerIndex = getPlayerIndex(victim);
		if ( victimIndex == -1 || attackerIndex == -1 )
			return;
		
		if ( attackerIndex == slaveMasters[victimIndex] )
		{// master attacking slave, instagib & stop them being a slave (force spectator)
			event.setDamage(100);
			slaveMasters[victimIndex] = -1;
		}
		else if ( victimIndex == slaveMasters[attackerIndex] )	
		{// slave attacking master
			event.setCancelled(true);
		}
	}
}
