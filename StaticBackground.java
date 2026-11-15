package com.extraction.menu.client.bg;

import com.extraction.menu.ExtractionMenu;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public final class StaticBackground implements MenuBackground {
    private final ResourceLocation location;
    private final int texW;
    private final int texH;

    public StaticBackground(InputStream in) throws IOException {
        NativeImage image = NativeImage.read(in);
        this.texW = image.getWidth();
        this.texH = image.getHeight();
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(true, false); // smooth scaling for the parallax margin
        this.location = Minecraft.getInstance().getTextureManager().register(ExtractionMenu.MODID + "_bg", texture);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h) {
        MenuBackground.drawCover(g, location, texW, texH, x, y, w, h);
    }

    @Override
    public void close() {
        Minecraft.getInstance().getTextureManager().release(location);
    }
}
