package com.extraction.menu.client.bg;

import com.extraction.menu.ExtractionMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Picks one random background (gif or png) that actually exists in the mod resources. */
public final class BackgroundManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] NAMES = {
            "fondo1.gif", "fondo2.gif", "fondo3.gif", "fondo4.gif", "fondo5.png", "fondo6.png"
    };

    private static MenuBackground current;

    private BackgroundManager() {}

    public static MenuBackground current() {
        return current;
    }

    public static MenuBackground get() {
        if (current == null) {
            current = create();
        }
        return current;
    }

    public static void release() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    private static ResourceLocation loc(String name) {
        return new ResourceLocation(ExtractionMenu.MODID, "backgrounds/" + name);
    }

    private static MenuBackground create() {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        List<String> available = new ArrayList<>();
        for (String name : NAMES) {
            if (rm.getResource(loc(name)).isPresent()) {
                available.add(name);
            }
        }
        Collections.shuffle(available);

        for (String name : available) {
            try {
                Resource resource = rm.getResource(loc(name)).orElseThrow();
                try (InputStream in = resource.open()) {
                    if (name.endsWith(".gif")) {
                        return new GifBackground(in.readAllBytes());
                    }
                    return new StaticBackground(in);
                }
            } catch (Exception e) {
                LOGGER.warn("[extractionmenu] Could not load background {}", name, e);
            }
        }
        return null;
    }
}
