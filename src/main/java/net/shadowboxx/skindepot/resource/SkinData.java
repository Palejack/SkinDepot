package net.shadowboxx.skindepot.resource;

import java.util.List;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

class SkinData {

    List<Skin> skins;
}

class Skin {

    String name;
    UUID uuid;
    String skin;

    public ResourceLocation getTexture() {
        return new ResourceLocation("skindepot", String.format("textures/skins/%s.png", skin));
    }
}
