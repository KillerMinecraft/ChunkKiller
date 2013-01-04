package com.ftwinston.Killer.ChunkKiller;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.GameModePlugin;
import com.ftwinston.Killer.Killer;

public class Plugin extends GameModePlugin
{
	public void onEnable()
	{
		Killer.registerGameMode(this);
	}
	
	@Override
	public GameMode createInstance()
	{
		return new ChunkKiller();
	}
	
	@Override
	public String[] getSignDescription()
	{
		return new String[] {
			"tbc",
			"",
			"",
			"",
			
			"",
			"",
			"",
			"",
			
			"",
			"",
			"",
			""
		};
	}
}