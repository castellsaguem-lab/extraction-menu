package com.extraction.menu.client;

import com.extraction.menu.ExtractionMenu;
import com.extraction.menu.client.bg.BackgroundManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.ModListScreen;

public class ExtractionTitleScreen extends Screen {
    private static final ResourceLocation LOGO =
            new ResourceLocation(ExtractionMenu.MODID, "textures/gui/logo.png");
    private static final int LOGO_W = 1024;
    private static final int LOGO_H = 295;

    private static final int ROW_H = 22;
    private static final int GROUP_GAP = 12;
    private static final int BUTTON_W = 160;

    private float offX;
    private float offY;

    public ExtractionTitleScreen() {
        super(Component.literal("Extraction"));
    }

    @Override
    protected void init() {
        int left = Math.max(24, this.width / 28);
        int logoW = Mth.clamp((int) (this.width * 0.34F), 150, 340);
        int logoH = logoW * LOGO_H / LOGO_W;
        int logoY = Math.max(16, this.height / 16);

        int rows = 8;
        int total = rows * ROW_H + 2 * GROUP_GAP;
        int y = this.height - 36 - total;
        y = Math.max(y, logoY + logoH + 34);

        y = add(left, y, "Singleplayer", () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
        y = add(left, y, "Multiplayer", () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this)));
        y = add(left, y, "Realms", () -> this.minecraft.setScreen(new RealmsMainScreen(this)));
        y = add(left, y, "Mods", () -> this.minecraft.setScreen(new ModListScreen(this)));
        y += GROUP_GAP;
        y = add(left, y, "Options...", () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)));
        y = add(left, y, "Language...", () -> this.minecraft.setScreen(
                new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())));
        y = add(left, y, "Accessibility", () -> this.minecraft.setScreen(
                new AccessibilityOptionsScreen(this, this.minecraft.options)));
        y += GROUP_GAP;
        add(left, y, "Quit Game", () -> this.minecraft.stop());
    }

    private int add(int x, int y, String label, Runnable action) {
        this.addRenderableWidget(new MenuButton(x - 8, y, BUTTON_W, ROW_H, Component.literal(label), b -> action.run()));
        return y + ROW_H;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // --- parallax: background drifts slightly against the cursor ---
        float tx = Mth.clamp((mouseX / (float) this.width - 0.5F) * 2.0F, -1.0F, 1.0F);
        float ty = Mth.clamp((mouseY / (float) this.height - 0.5F) * 2.0F, -1.0F, 1.0F);
        offX += (tx - offX) * 0.07F;
        offY += (ty - offY) * 0.07F;

        int m = Math.max(10, this.width / 50);
        if (!BackgroundManager.render(g, -m - Math.round(offX * m), -m - Math.round(offY * m),
                this.width + 2 * m, this.height + 2 * m)) {
            g.fill(0, 0, this.width, this.height, 0xFF0B0B0B);
        }

        // --- dark gradient on the left so the menu text is readable on any background ---
        int panelW = (int) (this.width * 0.5F);
        for (int i = 0; i < panelW; i += 3) {
            float t = 1.0F - (float) i / panelW;
            int alpha = (int) (200 * t * t);
            g.fill(i, 0, i + 3, this.height, alpha << 24);
        }
        g.fillGradient(0, this.height - 70, this.width, this.height, 0x00000000, 0xAA000000);

        // --- logo + version ---
        int left = Math.max(24, this.width / 28);
        int logoW = Mth.clamp((int) (this.width * 0.34F), 150, 340);
        int logoH = logoW * LOGO_H / LOGO_W;
        int logoY = Math.max(16, this.height / 16);
        RenderSystem.enableBlend();
        g.blit(LOGO, left, logoY, logoW, logoH, 0.0F, 0.0F, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
        g.drawString(this.font, "Version " + ExtractionMenu.PACK_VERSION, left + 2, logoY + logoH + 8, 0xFF9A968A, true);
        g.drawString(this.font, "Minecraft 1.20.1", left + 2, this.height - 14, 0xFF6F6B60, false);

        super.render(g, mouseX, mouseY, partialTick); // widgets
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
