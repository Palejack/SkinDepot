package me.jaredhewitt.skindepot.mod;

import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import com.mumfrey.liteloader.util.ModUtilities;

import me.jaredhewitt.skindepot.HDSkinManager;
import me.jaredhewitt.skindepot.gui.EntityPlayerModel;
import me.jaredhewitt.skindepot.gui.GuiSkins;
import me.jaredhewitt.skindepot.gui.HDSkinsConfigPanel;
import me.jaredhewitt.skindepot.gui.RenderPlayerModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;

import java.io.File;
import java.lang.reflect.Method;

public class LiteModHDSkinsMod implements HDSkinsMod {
    @Override
    public String getName() {
        return "SkinDepot";
    }

    @Override
    public String getVersion() {
        return "1.11-1.2";
    }

    @Override
    public void init(File configPath) {
        try {
            Class<?> ex = Class.forName("com.thevoxelbox.voxelmenu.GuiMainMenuVoxelBox");
            Method mRegisterCustomScreen = ex.getDeclaredMethod("registerCustomScreen", Class.class, String.class);
            mRegisterCustomScreen.invoke(null, GuiSkins.class, "SkinDepot Manager");
        } catch (ClassNotFoundException var4) {
            // voxelmenu's not here, man
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        IReloadableResourceManager irrm = (IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
        irrm.registerReloadListener(HDSkinManager.INSTANCE);
    }

    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {
        HDSkinManager.clearSkinCache();
    }

    @Override
    public Class<? extends ConfigPanel> getConfigPanelClass() {
        return HDSkinsConfigPanel.class;
    }

    @Override
    public void onInitCompleted(Minecraft minecraft, LiteLoader loader) {
        ModUtilities.addRenderer(EntityPlayerModel.class, new RenderPlayerModel<>(minecraft.getRenderManager()));
    }
}
