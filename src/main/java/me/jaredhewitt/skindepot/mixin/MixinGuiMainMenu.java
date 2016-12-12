package me.jaredhewitt.skindepot.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.jaredhewitt.skindepot.gui.GuiButtonSkins;
import me.jaredhewitt.skindepot.gui.GuiSkins;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu extends GuiScreen {

    private static final int SKINS = 5000;

    @Inject(method = "initGui()V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.buttonList.add(new GuiButtonSkins(SKINS, width - 50, height - 50));
    }

    @Inject(method = "actionPerformed(Lnet/minecraft/client/gui/GuiButton;)V", at = @At("RETURN"))
    private void onActionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == SKINS) {
            this.mc.displayGuiScreen(new GuiSkins());
        }
    }

}
