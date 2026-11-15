package com.extraction.menu.client.bg;

import com.extraction.menu.ExtractionMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Presentacion de fondos con fundido cruzado.
 *
 * Fondos que usa (todos mezclados, cualquier nombre, .gif y .png):
 *  1) los que lleva el jar en assets/extractionmenu/backgrounds/
 *  2) los de la carpeta  <carpeta del juego>/extractionmenu/backgrounds/   (aqui se pueden meter los GIF
 *     pesados SIN recompilar el mod ni subirlos a GitHub)
 *
 * Empieza por uno al azar y cada HOLD_MS pasa a otro distinto con un fundido.
 */
public final class BackgroundManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RNG = new Random();

    /** Segundos que se ve cada fondo antes de pasar al siguiente. */
    private static final long HOLD_MS = 15_000L;
    /** Duracion del fundido. */
    private static final long FADE_MS = 1_500L;
    /** El siguiente archivo se empieza a leer (en otro hilo) este tiempo antes de usarlo. */
    private static final long PRELOAD_MS = 5_000L;

    private record Entry(String name, boolean gif, Callable<byte[]> reader) {}

    private static List<Entry> entries;
    private static final Set<String> broken = new HashSet<>();

    private static MenuBackground current;
    private static String currentName;
    private static MenuBackground next;
    private static String nextName;
    private static CompletableFuture<byte[]> pending;
    private static Entry pendingEntry;
    private static long shownSince;
    private static long fadeStart;

    private BackgroundManager() {}

    /** Dibuja el fondo (y gestiona los cambios). Devuelve false si no hay ningun fondo utilizable. */
    public static boolean render(GuiGraphics g, int x, int y, int w, int h) {
        long now = Util.getMillis();
        if (entries == null) {
            entries = scan();
        }
        if (current == null) {
            loadFirst(now);
            if (current == null) {
                return false;
            }
        }

        updateNext(now);

        current.tick();
        current.render(g, x, y, w, h);

        if (next != null) {
            next.tick();
            float a = Mth.clamp((now - fadeStart) / (float) FADE_MS, 0.0F, 1.0F);
            RenderSystem.enableBlend();
            g.setColor(1.0F, 1.0F, 1.0F, a);
            next.render(g, x, y, w, h);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (a >= 1.0F) {
                current.close();
                current = next;
                currentName = nextName;
                next = null;
                nextName = null;
                shownSince = now;
            }
        }
        return true;
    }

    /** Libera todo (y para los hilos de los GIF). Se llama al entrar en un mundo. */
    public static void release() {
        if (current != null) {
            current.close();
            current = null;
        }
        if (next != null) {
            next.close();
            next = null;
        }
        if (pending != null) {
            pending.cancel(true);
            pending = null;
        }
        pendingEntry = null;
        currentName = null;
        nextName = null;
        entries = null; // se vuelve a escanear la proxima vez (por si se han anadido archivos)
    }

    // ------------------------------------------------------------------------------------------

    private static void loadFirst(long now) {
        while (current == null) {
            Entry e = pick(null);
            if (e == null) {
                return;
            }
            try {
                current = create(e, e.reader().call());
                currentName = e.name();
                shownSince = now;
                LOGGER.info("[extractionmenu] Background: {}", e.name());
            } catch (Exception ex) {
                LOGGER.warn("[extractionmenu] Could not load background {}", e.name(), ex);
                broken.add(e.name());
            }
        }
    }

    private static void updateNext(long now) {
        if (next != null || usable() < 2) {
            return;
        }
        long shown = now - shownSince;

        if (pending == null && shown >= HOLD_MS - PRELOAD_MS) {
            final Entry e = pick(currentName);
            if (e != null) {
                pendingEntry = e;
                pending = CompletableFuture.supplyAsync(() -> {
                    try {
                        return e.reader().call();
                    } catch (Exception ex) {
                        throw new CompletionException(ex);
                    }
                });
            }
        }

        if (pending != null && pending.isDone() && shown >= HOLD_MS) {
            Entry e = pendingEntry;
            CompletableFuture<byte[]> f = pending;
            pending = null;
            pendingEntry = null;
            try {
                next = create(e, f.join());
                nextName = e.name();
                fadeStart = now;
                LOGGER.info("[extractionmenu] Background -> {}", e.name());
            } catch (Exception ex) {
                LOGGER.warn("[extractionmenu] Could not load background {}", e.name(), ex);
                broken.add(e.name());
            }
        }
    }

    private static MenuBackground create(Entry e, byte[] data) throws IOException {
        if (e.gif()) {
            return new GifBackground(data);
        }
        return new StaticBackground(new ByteArrayInputStream(data));
    }

    private static int usable() {
        int n = 0;
        for (Entry e : entries) {
            if (!broken.contains(e.name())) {
                n++;
            }
        }
        return n;
    }

    /** Un fondo al azar que no este roto y que no sea {@code except}. */
    private static Entry pick(String except) {
        List<Entry> pool = new ArrayList<>();
        for (Entry e : entries) {
            if (!broken.contains(e.name()) && !e.name().equals(except)) {
                pool.add(e);
            }
        }
        return pool.isEmpty() ? null : pool.get(RNG.nextInt(pool.size()));
    }

    // ------------------------------------------------------------------------------------------

    private static boolean isGif(String file) {
        return file.toLowerCase(Locale.ROOT).endsWith(".gif");
    }

    private static boolean isImage(String file) {
        String f = file.toLowerCase(Locale.ROOT);
        return f.endsWith(".gif") || f.endsWith(".png");
    }

    private static List<Entry> scan() {
        List<Entry> list = new ArrayList<>();

        // 1) dentro del jar: cualquier .gif / .png de assets/extractionmenu/backgrounds/
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> found = rm.listResources("backgrounds",
                rl -> rl.getNamespace().equals(ExtractionMenu.MODID) && isImage(rl.getPath()));
        for (Map.Entry<ResourceLocation, Resource> me : found.entrySet()) {
            String path = me.getKey().getPath();
            Resource res = me.getValue();
            list.add(new Entry("jar:" + path, isGif(path), () -> {
                try (InputStream in = res.open()) {
                    return in.readAllBytes();
                }
            }));
        }

        // 2) carpeta externa del juego: <gamedir>/extractionmenu/backgrounds/
        Path dir = FMLPaths.GAMEDIR.get().resolve("extractionmenu").resolve("backgrounds");
        try {
            Files.createDirectories(dir);
            Path readme = dir.resolve("LEEME.txt");
            if (!Files.exists(readme)) {
                Files.write(readme, ("Pon aqui tus fondos (.gif o .png, cualquier nombre).\n"
                        + "Se mezclan con los que lleva el mod y cambian solos con un fundido.\n"
                        + "Reinicia el juego (o entra y sal de un mundo) para que detecte archivos nuevos.\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path p : ds) {
                    String file = p.getFileName().toString();
                    if (Files.isRegularFile(p) && isImage(file)) {
                        list.add(new Entry("file:" + file, isGif(file), () -> Files.readAllBytes(p)));
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("[extractionmenu] Could not read external backgrounds folder {}", dir, ex);
        }

        LOGGER.info("[extractionmenu] {} background(s) found", list.size());
        return list;
    }
}
