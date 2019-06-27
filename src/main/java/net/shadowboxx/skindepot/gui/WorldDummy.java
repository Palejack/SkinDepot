package net.shadowboxx.skindepot.gui;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class WorldDummy extends World {
	static ISaveHandler saveHandlerIn = null;
	static WorldInfo info = null;
	static WorldProvider providerIn = null;
	static Profiler profilerIn = null;
	static boolean client = false;
	
	public WorldDummy() {
		super(saveHandlerIn, info, providerIn, profilerIn, client);
	}

	@Override
	protected IChunkProvider createChunkProvider() {
		return null;
	}

	@Override
	protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
		return false;
	}

}
