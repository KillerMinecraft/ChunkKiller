package com.ftwinston.KillerMinecraft.Modules.ChunkKiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
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

		// dirt
		int i = numChunkSections - 1;
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
	
	private int getRandomCoord(Random r, int distanceFromEdge)
	{
		int i = distanceFromEdge + r.nextInt(15 - distanceFromEdge - distanceFromEdge);
		if ( i >= ChunkKiller.chunkCoreX )
			i++;
		return i;
	}
	
	private int getIntInRange(Random r, int min, int max)
	{
		return min + r.nextInt(max-min+1);
	}
	
	private Block getRandomBlock(Chunk c, Random r, int yMin, int yMax, int distanceFromEdge)
	{
		Block b = null;
		for ( int i=0; i<3; i++ )
		{
			int x = getRandomCoord(r, distanceFromEdge), z = getRandomCoord(r, distanceFromEdge);
			b = c.getBlock(x, getIntInRange(r, yMin, yMax), z);
			if ( b.getType() == Material.STONE )
				break;
		}
		return b;
	}
	
	public class DetailPopulator extends BlockPopulator
	{
		@SuppressWarnings("deprecation")
		@Override
		public void populate(World w, Random r, Chunk c)
		{
			if ( isEmptyChunk(c.getX(), c.getZ()) )
				return;
			
			r.setSeed(w.getSeed()); // ensure each chunk generates the same
			
			// lava
			int num = 4, bx, by, bz;
			for ( int i=0; i<num; i++ )
			{
				int hSize = r.nextInt(3), vSize = r.nextInt(3);
				bx = getRandomCoord(r, hSize+1); by = getIntInRange(r, 2, 44); bz = getRandomCoord(r, hSize+1);
				for ( int x=bx-hSize; x<=bx+(hSize == 0 ? 1 : hSize); x++ )
					for ( int z=bz-hSize; z<=bz+(hSize == 0 ? 1 : hSize); z++ )
						for ( int y=by; y<=by+vSize; y++)
							c.getBlock(x, y, z).setType(Material.LAVA);
			}
			
			// spider spawner, skeleton spawner
			bx = getRandomCoord(r, 3); by = getIntInRange(r, 2, 44); bz = getRandomCoord(r, 3);
			for ( int x=bx-2; x<=bx+2; x++ )
				for ( int z=bz-2; z<=bz+2; z++ )
					for ( int y=by; y<=by+2; y++)
						c.getBlock(x, y, z).setType(Material.AIR);
			Block b = c.getBlock(bx, by, bz);
			b.setType(Material.MOB_SPAWNER);
			CreatureSpawner spawner = (CreatureSpawner)b.getState();
			spawner.setSpawnedType(EntityType.SPIDER);
			
			bx = getRandomCoord(r, 3); by = getIntInRange(r, 2, 44); bz = getRandomCoord(r, 3);
			for ( int x=bx-2; x<=bx+2; x++ )
				for ( int z=bz-2; z<=bz+2; z++ )
					for ( int y=by; y<=by+2; y++)
						c.getBlock(x, y, z).setType(Material.AIR);
			b = c.getBlock(bx, by, bz);
			b.setType(Material.MOB_SPAWNER);
			spawner = (CreatureSpawner)b.getState();
			spawner.setSpawnedType(EntityType.SKELETON);
			
			num = 5; // clumps of gravel, each 3x3x3
			for ( int i=0; i<num; i++ )
			{
				b = getRandomBlock(c, r, 40, 62, 2);
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
			
			num = 4; // clumps of sand, each 3x3x1
			for ( int i=0; i<num; i++ )
			{
				b = getRandomBlock(c, r, 46, 62, 2);
				b.setType(Material.SAND);
				b.getRelative(BlockFace.NORTH).setType(Material.SAND);
				b.getRelative(BlockFace.EAST).setType(Material.SAND);
				b.getRelative(BlockFace.SOUTH).setType(Material.SAND);
				b.getRelative(BlockFace.WEST).setType(Material.SAND);
				b.getRelative(BlockFace.NORTH_EAST).setType(Material.SAND);
				b.getRelative(BlockFace.NORTH_WEST).setType(Material.SAND);
				b.getRelative(BlockFace.SOUTH_EAST).setType(Material.SAND);
				b.getRelative(BlockFace.SOUTH_WEST).setType(Material.SAND);
			}
			
			num = r.nextInt(4) + 4; // 4-7 veins of 2-4 diamonds
			for ( int i=0; i<num; i++ )
			{
				b = getRandomBlock(c, r, 1, 20, 1);
				b.setType(Material.DIAMOND_ORE);
				if ( r.nextInt(3) != 1 )	
					b.getRelative(r.nextBoolean() ? BlockFace.NORTH : BlockFace.SOUTH).setType(Material.DIAMOND_ORE);
				if ( r.nextInt(3) != 1 )						
					b.getRelative(r.nextBoolean() ? BlockFace.EAST : BlockFace.WEST).setType(Material.DIAMOND_ORE);
				if ( r.nextInt(3) != 1 )						
					b.getRelative(r.nextBoolean() ? BlockFace.UP : BlockFace.DOWN).setType(Material.DIAMOND_ORE);
			}
			
			num = r.nextInt(3) + 9; // 9-11 veins of up to 8 iron
			for ( int i=0; i<num; i++ )
			{
				b = getRandomBlock(c, r, 20, 44, 1);
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
				b = getRandomBlock(c, r, 32, 48, 1);
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
				b = getRandomBlock(c, r, 32, 48, 1);
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
			
			w.generateTree(c.getBlock(1 + r.nextInt(7), ChunkKiller.chunkCoreY+1, 1 + r.nextInt(7)).getLocation(), TreeType.BIRCH);
			w.generateTree(c.getBlock(9 + r.nextInt(6), ChunkKiller.chunkCoreY+1, 1 + r.nextInt(7)).getLocation(), TreeType.SMALL_JUNGLE);
			w.generateTree(c.getBlock(1 + r.nextInt(7), ChunkKiller.chunkCoreY+1, 9 + r.nextInt(6)).getLocation(), TreeType.REDWOOD);
			
			// water in the remaining corner .. with some wheat growing next to it, for the chickens
			Block water = c.getBlock(10 + r.nextInt(4), ChunkKiller.chunkCoreY, 10 + r.nextInt(4));
			water.setType(Material.STATIONARY_WATER);
			water.getRelative(1, 0, 0).setType(Material.STATIONARY_WATER);
			water.getRelative(0, 0, 1).setType(Material.STATIONARY_WATER);
			water.getRelative(1, 0, 1).setType(Material.STATIONARY_WATER);
			
			b = water.getRelative(2, 0, 0); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(2, 0, 1); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(0, 0, 2); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(1, 0, 2); b.setType(Material.SOIL); b.setData((byte)0x8);
			b = water.getRelative(2, 0, 2); b.setType(Material.SOIL); b.setData((byte)0x8);
			
			b = water.getRelative(2, 1, 0); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(2, 1, 1); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(0, 1, 2); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(1, 1, 2); b.setType(Material.CROPS); b.setData((byte)r.nextInt(7));
			b = water.getRelative(2, 1, 2); b.setType(Material.CROPS); b.setData((byte)0x7);
			
			// the core
			c.getBlock(ChunkKiller.chunkCoreX, ChunkKiller.chunkCoreY, ChunkKiller.chunkCoreZ).setType(ChunkKiller.coreMaterial);
			
			// chickens, for food & feathers
			w.spawnEntity(c.getBlock(9, ChunkKiller.chunkCoreY + 1, 14).getLocation(), EntityType.CHICKEN);
			w.spawnEntity(c.getBlock(14, ChunkKiller.chunkCoreY + 1, 9).getLocation(), EntityType.CHICKEN);
			w.spawnEntity(c.getBlock(9, ChunkKiller.chunkCoreY + 1, 11).getLocation(), EntityType.CHICKEN);
			w.spawnEntity(c.getBlock(11, ChunkKiller.chunkCoreY + 1, 9).getLocation(), EntityType.CHICKEN);
		}
	}
}
