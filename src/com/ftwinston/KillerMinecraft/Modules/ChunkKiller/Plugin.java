package com.ftwinston.KillerMinecraft.Modules.ChunkKiller;

import org.bukkit.Material;

import com.ftwinston.KillerMinecraft.GameMode;
import com.ftwinston.KillerMinecraft.GameModePlugin;

public class Plugin extends GameModePlugin
{
	@Override
	public Material getMenuIcon() { return Material.GRASS; }
	
	@Override
	public String[] getDescriptionText() { return new String[] {"Each player has their own free-standing", "chunk of ground. Players compete", "to destroy other players' chunks."}; }
	
	@Override
	public GameMode createInstance()
	{
		return new ChunkKiller();
	}
}