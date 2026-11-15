package com.extraction.menu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Text-only menu entry (no button box) with a red marker on hover. */
public class MenuButton extends Button {
    private static final float SCALE = 1.5F;
    private static final int COLOR_NORMAL = 0xFFC9C5B6;
    private static final int COLOR_HOVER = 0xFFFFFFFF;
    private static final int RED = 0xFFB3201A;

    private float hover;

    public MenuButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean hovered = this.isHoveredOrFocused();
        hover += ((hovered ? 1.0F : 0.0F) - hover) * 0.3F;

        int color = hovered ? COLOR_HOVER : COLOR_NORMAL;
        float textY = this.getY() + (this.height - 8 * SCALE) / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(this.getX() + 8 + hover * 5.0F, textY, 0.0F);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.drawString(font, this.getMessage(), 0, 0, color, true);
        graphics.pose().popPose();

        if (hover > 0.05F) {
            int h = (int) ((this.height - 4) * hover);
            int top = this.getY() + (this.height - h) / 2;
            graphics.fill(this.getX(), top, this.getX() + 2, top + h, RED);
        }
    }
}
