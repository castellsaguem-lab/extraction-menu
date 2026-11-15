package com.extraction.menu.client.bg;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public interface MenuBackground {
    /** Called once per rendered frame before render(). */
    default void tick() {}

    void render(GuiGraphics graphics, int x, int y, int width, int height);

    /** Must be called on the render thread. */
    void close();

    /** Draws a texture so it covers the whole rectangle (center crop, no stretching). */
    static void drawCover(GuiGraphics g, ResourceLocation tex, int texW, int texH, int x, int y, int w, int h) {
        float texAspect = (float) texW / texH;
        float dstAspect = (float) w / h;
        int u = 0;
        int v = 0;
        int uw = texW;
        int vh = texH;
        if (texAspect > dstAspect) {
            uw = Math.round(texH * dstAspect);
            u = (texW - uw) / 2;
        } else {
            vh = Math.round(texW / dstAspect);
            v = (texH - vh) / 2;
        }
        g.blit(tex, x, y, w, h, (float) u, (float) v, uw, vh, texW, texH);
    }
}
