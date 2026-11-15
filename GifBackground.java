package com.extraction.menu.client.bg;

import com.extraction.menu.ExtractionMenu;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** Animated background: a worker thread decodes frames, the render thread uploads them on time. */
public final class GifBackground implements MenuBackground {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final GifDecoder decoder;
    private final DynamicTexture texture;
    private final ResourceLocation location;
    private final int texW;
    private final int texH;
    private final BlockingQueue<GifDecoder.Frame> queue = new ArrayBlockingQueue<>(3);
    private final Thread worker;
    private volatile boolean running = true;
    private long nextFrameAt;

    public GifBackground(byte[] data) throws IOException {
        this.decoder = new GifDecoder(data);
        GifDecoder.Frame first = decoder.next();
        this.texW = first.w();
        this.texH = first.h();

        NativeImage image = new NativeImage(texW, texH, false);
        copyInto(image, first);
        this.texture = new DynamicTexture(image);
        this.texture.setFilter(true, false);
        this.location = Minecraft.getInstance().getTextureManager().register(ExtractionMenu.MODID + "_gif", texture);
        this.nextFrameAt = Util.getMillis() + first.delayMs();

        this.worker = new Thread(this::decodeLoop, "extractionmenu-gif-decoder");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    private void decodeLoop() {
        try {
            while (running) {
                GifDecoder.Frame frame = decoder.next();
                while (running && !queue.offer(frame, 100, TimeUnit.MILLISECONDS)) {
                    // queue full: wait for the render thread to consume
                }
            }
        } catch (InterruptedException ignored) {
            // closing
        } catch (Exception e) {
            if (running) {
                LOGGER.warn("[extractionmenu] GIF decoding stopped", e);
            }
        }
    }

    private static void copyInto(NativeImage image, GifDecoder.Frame f) {
        int[] px = f.abgr();
        for (int y = 0; y < f.h(); y++) {
            int row = y * f.w();
            for (int x = 0; x < f.w(); x++) {
                image.setPixelRGBA(x, y, px[row + x]);
            }
        }
    }

    @Override
    public void tick() {
        long now = Util.getMillis();
        if (now < nextFrameAt) {
            return;
        }
        GifDecoder.Frame frame = queue.poll();
        if (frame == null) {
            return;
        }
        NativeImage pixels = texture.getPixels();
        if (pixels == null || frame.w() != pixels.getWidth() || frame.h() != pixels.getHeight()) {
            return;
        }
        copyInto(pixels, frame);
        texture.upload();
        nextFrameAt = now + frame.delayMs();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h) {
        MenuBackground.drawCover(g, location, texW, texH, x, y, w, h);
    }

    @Override
    public void close() {
        running = false;
        worker.interrupt();
        decoder.close();
        Minecraft.getInstance().getTextureManager().release(location);
    }
}
