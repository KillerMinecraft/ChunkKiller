package com.ftwinston.Killer.ChunkKiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class PlayerChunkGenerator extends ChunkGenerator
{
	int chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, lastRowChunk;
	public PlayerChunkGenerator(ChunkKiller game)
	{
		chunkMinX = 0; chunkMaxX = (game.chunkRows-1) * ChunkKiller.chunkSpacing;
		chunkMinZ = 0; chunkMaxZ = (game.chunkCols-1) * ChunkKiller.chunkSpacing;
		lastRowChunk = (game.chunksOnLastRow-1) * ChunkKiller.chunkSpacing;
	}
	
	@Override
	public List<BlockPopulator> getDefaultPopulators(World world)
	{
		List<BlockPopulator> list = new ArrayList<BlockPopulator>();
		list.add(new DetailPopulator());
		return list;
	}
	
	@Override
	public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes)
	{
		if ( isEmptyChunk(x, z) )
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
	
	private boolean isEmptyChunk(int x, int z)
	{
		return x < chunkMinX || x > chunkMaxX || z < chunkMinZ || z > chunkMaxZ || x % ChunkKiller.chunkSpacing != 0 || z % ChunkKiller.chunkSpacing != 0 || (x == chunkMaxX && z > lastRowChunk);
	}
	
	private Block getRandomBlock(Chunk c, Random r, int yMin, int yMax)
	{
		Block b = null;
		for ( int i=0; i<3; i++ )
		{
			int x = 1 + r.nextInt(13), z = 1 + r.nextInt(13);
			if ( x >= ChunkKiller.chunkCoreX )
				x++;
			if ( z >= ChunkKiller.chunkCoreZ )
				z++;
			b = c.getBlock(x, yMin + r.nextInt(yMax-yMin+1), z);
			if ( b.getType() == Material.STONE )
				break;
		}
		return b;
	}
	
	public class DetailPopulator extends BlockPopulator
	{
		@Override
		public void populate(World w, Random r, Chunk c)
		{
			if ( isEmptyChunk(c.getX(), c.getZ()) )
				return;
			
			r.setSeed(w.getSeed()); // ensure each chunk generates the same
			
			for ( int y=0;y<=ChunkKiller.maxCoreY; y++ )
				c.getBlock(ChunkKiller.chunkCoreX, y, ChunkKiller.chunkCoreZ).setTypeId(ChunkKiller.coreMaterial);
			
			int num = 5; // 3 clumps of gravel, each 3x3
			for ( int i=0; i<num; i++ )
			{
				Block b = getRandomBlock(c, r, 40, 62);
				for ( int j=0; j<3; j++ )
				{
					b.setType(Material.GRAVEL);
					b.getRelative(BlockFace.NORTH).setType(Material.GRAVEL);
					b.getRelative(BlockFace.EAST).setType(Material.GRAVEL);
					b.getRelative(BlockFace.SOUTH).setType(Material.GRAVEL);
					b.getRelative(BlockFace.WEST).setType(Material.GRAVEL);
					b.getRelative(BlockFace.NORTH_EAST).setType(Material.GRAVEL);
					b.getRelative(BlockFace.NORTH_WEST).setType(Material.GRAVEL);
					b.getRelative(BlockFace.SOUTH_EAST).setType(Material.GRAVEL);
					b.getRelative(BlockFace.SOUTH_WEST).setType(Material.GRAVEL);
					b = b.getRelative(BlockFace.DOWN);
				}
			}
			
			num = r.nextInt(4) + 2; // 2-5 veins of 2-4 diamonds
			for ( int i=0; i<num; i++ )
			{
				Block b = getRandomBlock(c, r, 2, 20);
				b.setType(Material.DIAMOND_ORE);
				if ( r.nextInt(3) != 1 )	
					b.getRelative(r.nextBoolean() ? BlockFace.NORTH : BlockFace.SOUTH).setType(Material.DIAMOND_ORE);
				if ( r.nextInt(3) != 1 )						
					b.getRelative(r.nextBoolean() ? BlockFace.EAST : BlockFace.WEST).setType(Material.DIAMOND_ORE);
				if ( r.nextInt(3) != 1 )						
					b.getRelative(r.nextBoolean() ? BlockFace.UP : BlockFace.DOWN).setType(Material.DIAMOND_ORE);
			}
			
			num = r.nextInt(3) + 5; // 5-7 veins of up to 8 iron
			for ( int i=0; i<num; i++ )
			{
				Block b = getRandomBlock(c, r, 20, 44);
				BlockFace f1 = r.nextBoolean() ? BlockFace.NORTH : BlockFace.SOUTH, f2 = r.nextBoolean() ? BlockFace.EAST : BlockFace.WEST;
				for ( int j=0; j<2; j++ )					
				{
					b.setType(Material.IRON_ORE);
					if ( r.nextInt(7) != 1 )	
						b.getRelative(f1).setType(Material.IRON_ORE);
					if ( r.nextInt(7) != 1 )						
						b.getRelative(f2).setType(Material.IRON_ORE);
					if ( r.nextInt(7) != 1 )						
						b.getRelative(f1).getRelative(f2).setType(Material.IRON_ORE);
					b = b.getRelative(r.nextBoolean() ? BlockFace.UP : BlockFace.DOWN);
				}
			}
			
			num = r.nextInt(4) + 5; // 5-8 veins of up to 8 coal
			for ( int i=0; i<num; i++ )
			{
				Block b = getRandomBlock(c, r, 32, 48);
				BlockFace f1 = r.nextBoolean() ? BlockFace.NORTH : BlockFace.SOUTH, f2 = r.nextBoolean() ? BlockFace.EAST : BlockFace.WEST;
				for ( int j=0; j<2; j++ )					
				{
					b.setType(Material.COAL_ORE);
					if ( r.nextInt(7) != 1 )	
						b.getRelative(f1).setType(Material.COAL_ORE);
					if ( r.nextInt(7) != 1 )						
						b.getRelative(f2).setType(Material.COAL_ORE);
					if ( r.nextInt(7) != 1 )						
						b.getRelative(f1).getRelative(f2).setType(Material.COAL_ORE);
					b = b.getRelative(r.nextBoolean() ? BlockFace.UP : BlockFace.DOWN);
				}
			}
			
			num = r.nextInt(4) + 2; // 2-5 veins of up to 8 redstone
			for ( int i=0; i<num; i++ )
			{
				Block b = getRandomBlock(c, r, 32, 48);
				BlockFace f1 = r.nextBoolean() ? BlockFace.NORTH : BlockFace.SOUTH, f2 = r.nextBoolean() ? BlockFace.EAST : BlockFace.WEST;
				for ( int j=0; j<2; j++ )					
				{
					b.setType(Material.REDSTONE_ORE);
					if ( r.nextInt(4) != 1 )	
						b.getRelative(f1).setType(Material.REDSTONE_ORE);
					if ( r.nextInt(4) != 1 )						
						b.getRelative(f2).setType(Material.REDSTONE_ORE);
					if ( r.nextInt(4) != 1 )						
						b.getRelative(f1).getRelative(f2).setType(Material.REDSTONE_ORE);
					b = b.getRelative(r.nextBoolean() ? BlockFace.UP : BlockFace.DOWN);
				}
			}
			
			w.generateTree(c.getBlock(1 + r.nextInt(7), ChunkKiller.maxCoreY+1, 1 + r.nextInt(7)).getLocation(), TreeType.BIRCH);
			w.generateTree(c.getBlock(9 + r.nextInt(6), ChunkKiller.maxCoreY+1, 1 + r.nextInt(7)).getLocation(), TreeType.JUNGLE);
			w.generateTree(c.getBlock(1 + r.nextInt(7), ChunkKiller.maxCoreY+1, 9 + r.nextInt(6)).getLocation(), TreeType.REDWOOD);
			
			// water in the remaining corner .. with some wheat growing next to it, for the chickens
			
			Block water = c.getBlock(10 + r.nextInt(4), ChunkKiller.maxCoreY, 10 + r.nextInt(4));
			water.setType(Material.WATER);
			water.getRelative(1, 0, 0).setType(Material.WATER);
			water.getRelative(0, 0, 1).setType(Material.WATER);
			water.getRelative(1, 0, 1).setType(Material.WATER);
			
			Block b = water.getRelative(2, 0, 0); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(2, 0, 1); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(0, 0, 2); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(1, 0, 2); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(2, 0, 2); b.setType(Material.SOIL); b.setData((byte)0x8);
			
			b = water.getRelative(2, 1, 0); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(2, 1, 1); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(0, 1, 2); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(1, 1, 2); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(2, 1, 2); b.setType(Material.CROPS); b.setData((byte)0x7);
			
			
			// chickens, for food & feathers
			w.spawnEntity(c.getBlock(9, ChunkKiller.maxCoreY + 1, 14).getLocation(), EntityType.CHICKEN);
			w.spawnEntity(c.getBlock(14, ChunkKiller.maxCoreY + 1, 9).getLocation(), EntityType.CHICKEN);
			w.spawnEntity(c.getBlock(9, ChunkKiller.maxCoreY + 1, 11).getLocation(), EntityType.CHICKEN);
			w.spawnEntity(c.getBlock(11, ChunkKiller.maxCoreY + 1, 9).getLocation(), EntityType.CHICKEN);
		}
	}
}
