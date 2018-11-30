package net.shadowboxx.skindepot.mod;

import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import com.mumfrey.liteloader.util.ModUtilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.shadowboxx.skindepot.HDSkinManager;
import net.shadowboxx.skindepot.gui.EntityPlayerModel;
import net.shadowboxx.skindepot.gui.GuiSkins;
import net.shadowboxx.skindepot.gui.HDSkinsConfigPanel;
import net.shadowboxx.skindepot.gui.RenderPlayerModel;

import java.io.File;
import java.lang.reflect.Method;

public class LiteModHDSkinsMod implements HDSkinsMod {
    @Override
    public String getName() {
        return "SkinDepot";
    }

    @Override
    public String getVersion() {
        return "1.3.1";
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
