package com.ftwinston.Killer.ChunkKiller;

import java.util.List;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Helper;
import com.ftwinston.Killer.Option;
import com.ftwinston.Killer.PlayerFilter;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Material;

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
	public boolean isLocationProtected(Location l)
	{
		// ah crap, this ought to have a player parameter?
	}
	
	@Override
	public boolean isAllowedToRespawn(Player player) { return getOption(useSlaves).isEnabled(); }
	
	@Override
	public boolean useDiscreetDeathMessages() { return false; }
	
	@Override
	public Location getSpawnLocation(Player player)
	{
		// each player should spawn on their own chunk ... unless they've been defeated, in which case they should spawn on their master's chunk
		return Helper.getSafeSpawnLocationNear(getWorld(0).getSpawnLocation());
	}
	
	@Override
	public void gameStarted(boolean isNewWorlds)
	{
		if ( !isNewWorlds )
			; // this ... wouldn't work. Game mode ought to have a function for whether this is allowed at all??
		
		List<Player> players = getOnlinePlayers();
		
		// sort this list randomly, then make a new array of player names, for handling the player ORDER (for where their chunks should go, etc)
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
