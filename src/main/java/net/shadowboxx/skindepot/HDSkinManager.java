package net.shadowboxx.skindepot;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SkinManager.SkinAvailableCallback;
import net.minecraft.util.ResourceLocation;
import net.shadowboxx.skindepot.resource.SkinResourceManager;

public final class HDSkinManager implements IResourceManagerReloadListener {

    public static final HDSkinManager INSTANCE = new HDSkinManager();
    private static final ResourceLocation LOADING = new ResourceLocation("LOADING");

    private String gatewayUrl = "skindepot.shadowboxx.net/skinmanager";
    private String skinUrl = "skindepot.shadowboxx.net";
    private boolean enabled = true;

    private Map<UUID, Map<Type, MinecraftProfileTexture>> profileTextures = Maps.newHashMap();
    private Map<UUID, Map<Type, ResourceLocation>> skinCache = Maps.newHashMap();
    private List<ISkinModifier> skinModifiers = Lists.newArrayList();

    private SkinResourceManager resources = new SkinResourceManager();
    private ExecutorService executor = Executors.newCachedThreadPool();

    public HDSkinManager() {
    }

    public Optional<ResourceLocation> getSkinLocation(GameProfile profile1, final Type type, boolean loadIfAbsent) {
        if (!enabled)
            return Optional.empty();

        ResourceLocation skin = this.resources.getPlayerTexture(profile1, type);
        if (skin != null)
            return Optional.of(skin);

        // try to recreate a broken gameprofile
        // happens when server sends a random profile with skin and displayname
        Property prop = Iterables.getFirst(profile1.getProperties().get("textures"), null);
        if (prop != null && Strings.isNullOrEmpty(prop.getValue())) {
            JsonObject obj = new Gson().fromJson(new String(Base64.decodeBase64(prop.getValue())), JsonObject.class);
            // why are plugins sending a json null?
            if (obj != null) {
                String name = null;
                // this should be optional
                if (obj.has("profileName")) {
                    name = obj.get("profileName").getAsString();
                }
                // this is required
                if (obj.has("profileId")) {
                    UUID uuid = UUIDTypeAdapter.fromString(obj.get("profileId").getAsString());
                    profile1 = new GameProfile(uuid, name);
                }
            }
        }
        final GameProfile profile = profile1;

        if (!this.skinCache.containsKey(profile.getId())) {
            this.skinCache.put(profile.getId(), Maps.newHashMap());
        }

        skin = this.skinCache.get(profile.getId()).get(type);
        if (skin == null) {
            if (loadIfAbsent) {
                skinCache.get(profile.getId()).put(type, LOADING);
                //NOTE: Reverted from the "executor.submit(() ->" syntax as it caused a java.lang.AbstractMethodError in the actual client 
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        loadTexture(profile, type, new SkinAvailableCallback() {
                            @Override
                            public void skinAvailable(Type type, ResourceLocation location, MinecraftProfileTexture profileTexture) {
                                skinCache.get(profile.getId()).put(type, location);
                            }
                        });
                    }
                });
                //NOTE: Throws java.lang.AbstractMethodError
                //executor.submit(() -> loadTexture(profile, type, (type1, location, profileTexture) -> skinCache.get(profile.getId()).put(type1, location)));
            }
            return Optional.empty();
        }

        return skin == LOADING ? Optional.empty() : Optional.of(skin);

    }

    private void loadTexture(GameProfile profile, final Type type, final SkinAvailableCallback callback) {
    	//first make sure we're not dealing with null data
        if (profile != null && profile.getId() != null) {
        	//call getProfileData (defined below) to get our mapped texture data
            Map<Type, MinecraftProfileTexture> data = getProfileData(profile);
            //get the specific texture type (skin, cape, elytra) that we need
            final MinecraftProfileTexture texture = data.get(type);
            //if it's null then do nothing *womp, womp*
            if (texture == null) {
                return;
            }

            //define skinDir so we know what we are looking for locally
            String skinDir = "hd" + type.toString().toLowerCase() + "s";
            
            //this defines our resource location as the skinDir plus the md5 hash of our texture
            final ResourceLocation skin = new ResourceLocation(skinDir, texture.getHash());
            
            //this defines our file location using a substring of hash + the full hash
            String textureHash = texture.getHash();
            File textureFile = new File(new File("assets/" + skinDir), textureHash.substring(0, 2) + "/" + textureHash);

            final IImageBuffer imagebufferdownload = new ImageBufferDownloadHD();
            ThreadDownloadImageData threaddownloadimagedata = new ThreadDownloadImageData(textureFile, texture.getUrl(),
                    DefaultPlayerSkin.getDefaultSkinLegacy(),
                    new IImageBuffer() {
                        @Override
                        public BufferedImage parseUserSkin(BufferedImage image) {
                            return imagebufferdownload.parseUserSkin(image);
                        }

                        @Override
                        public void skinAvailable() {
                            imagebufferdownload.skinAvailable();
                            if (callback != null) {
                                callback.skinAvailable(type, skin, texture);
                            }
                        }
                    });

            // schedule texture loading on the main thread.
            TextureLoader.loadTexture(skin, threaddownloadimagedata);
        }
    }

    public Map<Type, MinecraftProfileTexture> getProfileData(GameProfile profile) {
    	//if we aren't enabled then piss off and return an empty map
        if (!enabled)
            return ImmutableMap.of();
        
        //profileTextures is a hashmap of profile id (key) -> texture hashmap (value)
        //if we don't already have textures for this profile id then this gets set to null instead
        Map<Type, MinecraftProfileTexture> textures = this.profileTextures.get(profile.getId());

        //if we got "null" then we need to construct our hashmap and slap it in the table
        //this should only happen on game start
        //TODO: Figure out a way to recall this when the cache is cleared or if we push new stuff to the server
        if (textures == null) {
        	LiteLoaderLogger.info("SkinDepot: getProfileData Ran, textures was null");

        	//grab player uuid from the profile id, because our filenames get stored as uuid on the server
            String uuid = UUIDTypeAdapter.fromUUID(profile.getId());

            //construct an empty map to fill with our data
            textures = Maps.newHashMap();
            for (Type type : Type.values()) { //this should create an entry for skin, cape and elytra each
                String url = getCustomTextureURLForId(type, uuid); //gets the url for our texture
                String hash = getTextureHash(type, uuid); //retrieves the md5 hash of our texture from the server

                //put it in the table using the type (skin, cape, elytra) as the keyword
                //HDProfileTexture extends MinecraftProfileTexture, the last entry is supposed to be Map<String, String>
                //no clue what the hell is supposed to go in there normally. It's just referred to as "metadata"
                textures.put(type, new HDProfileTexture(url, hash, null));
            }

            //place our texture data in our profileTextures hashmap
            this.profileTextures.put(profile.getId(), textures);
        }
        return textures;
    }

    private static Map<Type, MinecraftProfileTexture> getTexturesForProfile(GameProfile profile) {
        LiteLoaderLogger.info("SkinDepot: Get textures for " + profile.getId());

        Minecraft minecraft = Minecraft.getMinecraft();
        MinecraftSessionService sessionService = minecraft.getSessionService();
        Map<Type, MinecraftProfileTexture> textures;

        try {
            textures = sessionService.getTextures(profile, true);
        } catch (InsecureTextureException var6) {
            textures = sessionService.getTextures(profile, false);
        }

        if ((textures == null || textures.isEmpty())
                && profile.getId().equals(minecraft.getSession().getProfile().getId())) {
            textures = sessionService.getTextures(sessionService.fillProfileProperties(profile, false), false);
        }
        return textures;
    }

    public void setSkinUrl(String skinUrl) {
        this.skinUrl = skinUrl;
    }

    public void setGatewayURL(String gatewayURL) {
        this.gatewayUrl = gatewayURL;
    }

    public String getGatewayUrl() {
        return String.format("https://%s/", gatewayUrl);
    }

    public String getCustomTextureURLForId(Type type, String uuid, boolean gateway) {
        String server = gateway ? gatewayUrl : skinUrl;
        String path = type.toString().toLowerCase() + "s";
        return String.format("https://%s/%s/%s.png", server, path, uuid);
    }

    public String getCustomTextureURLForId(Type type, String uuid) {
        return getCustomTextureURLForId(type, uuid, false);
    }

    private String getTextureHash(Type type, String uuid) {
    	//this returns the md5 hash of the texture on the server, if it exists
    	//which it SHOULD, if the player has uploaded a texture...
        try {
            URL url = new URL(getCustomTextureURLForId(type, uuid) + ".md5");
            return Resources.asCharSource(url, Charsets.UTF_8).readFirstLine();
        } catch (IOException e) {
            return null;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static PreviewTexture getPreviewTexture(ResourceLocation skinResource, GameProfile profile) {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        ITextureObject skinTexture = textureManager.getTexture(skinResource);
        Map<Type, MinecraftProfileTexture> textures = getTexturesForProfile(profile);
        MinecraftProfileTexture skin = textures.get(Type.SKIN);
        if (skin != null) {
            String url = INSTANCE.getCustomTextureURLForId(Type.SKIN, UUIDTypeAdapter.fromUUID(profile.getId()), true);
            skinTexture = new PreviewTexture(url, DefaultPlayerSkin.getDefaultSkin(profile.getId()), new ImageBufferDownloadHD());
            TextureLoader.loadTexture(skinResource, skinTexture);
        }
        return (PreviewTexture) skinTexture;

    }

    public static void clearSkinCache() {
        LiteLoaderLogger.info("SkinDepot: Clearing local player skin cache");

        try {
        	File hdskins = new File(LiteLoader.getAssetsDirectory(), "hdskins");
        	File hdcapes = new File(LiteLoader.getAssetsDirectory(), "hdcapes");
        	File hdelytras = new File(LiteLoader.getAssetsDirectory(), "hdelytras");
            FileUtils.deleteDirectory(hdskins);
            LiteLoaderLogger.info("SkinDepot: Deleting " + hdskins.toString());
            FileUtils.deleteDirectory(hdcapes);
            LiteLoaderLogger.info("SkinDepot: Deleting " + hdcapes.toString());
            FileUtils.deleteDirectory(hdelytras);
            LiteLoaderLogger.info("SkinDepot: Deleting " + hdelytras.toString());
        } catch (IOException var1) {
            var1.printStackTrace();
        }

    }

    public void addSkinModifier(ISkinModifier modifier) {
        skinModifiers.add(modifier);
    }

    @Nullable
    public ResourceLocation getConvertedSkin(@Nullable ResourceLocation res) {
        return resources.getConvertedResource(res);
    }

    public void convertSkin(BufferedImage image, Graphics dest) {
        for (ISkinModifier skin : skinModifiers) {
            skin.convertSkin(image, dest);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.resources.onResourceManagerReload(resourceManager);
    }
}
