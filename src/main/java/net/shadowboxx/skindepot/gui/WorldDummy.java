package net.shadowboxx.skindepot.gui;

import java.io.File;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class WorldDummy extends World {
	static ISaveHandler saveHandlerIn = new SaveHandler(new File("skindepot"), "nowhere", true, null);
	static WorldSettings settings = new WorldSettings(0, GameType.NOT_SET, false, false, WorldType.FLAT);
	static WorldInfo info = new WorldInfo(settings, "SkinDepot");
	static WorldProvider providerIn = DimensionType.getById(0).createDimension();
	static Profiler profilerIn = new Profiler();
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
