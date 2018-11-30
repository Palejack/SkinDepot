package net.shadowboxx.skindepot.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonSkins extends GuiButton {

    public GuiButtonSkins(int buttonId, int x, int y) {
        super(buttonId, x, y, 60, 20, "SkinDepot");
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
    }
}
