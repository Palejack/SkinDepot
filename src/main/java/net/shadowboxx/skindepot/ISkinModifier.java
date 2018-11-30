package net.shadowboxx.skindepot;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public interface ISkinModifier {

    void convertSkin(BufferedImage skin, Graphics dest);
}
