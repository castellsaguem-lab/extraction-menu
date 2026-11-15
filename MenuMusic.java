package com.extraction.menu.client;

import com.extraction.menu.ExtractionMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.SelectMusicEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reproduce song1/song2/song3 (orden aleatorio, sin repetir la misma dos veces seguidas) mientras no
 * hay mundo cargado.
 *
 * Antes se sustituia la musica vanilla via SelectMusicEvent (Music + MusicManager), y cualquier otro mod
 * de musica/sonido del modpack podia pisarla. Ahora la musica vanilla del menu se silencia y las
 * canciones las reproducimos directamente con el SoundManager, que es mucho mas fiable.
 * Se respeta el volumen de "Musica" de las opciones.
 */
@Mod.EventBusSubscriber(modid = ExtractionMenu.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MenuMusic {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] SONGS = {"song1", "song2", "song3"};
    private static final Random RNG = new Random();

    private static SimpleSoundInstance current;
    private static String currentName;
    private static List<String> cachedSongs;
    private static int cooldown = 40;   // ticks hasta empezar la siguiente cancion
    private static int grace;           // margen tras empezar una cancion antes de comprobar si sigue sonando
    private static boolean warnedEmpty;

    private MenuMusic() {}

    private static boolean inMenu(Minecraft mc) {
        return mc.level == null && mc.player == null;
    }

    /** Silencia la musica vanilla del menu (la nuestra va por su cuenta). LOWEST para ganar a otros mods. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (inMenu(mc) && !songs(mc).isEmpty()) {
            event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SoundManager sm = mc.getSoundManager();

        if (!inMenu(mc)) {
            // dentro de un mundo: parar la nuestra y dejar la musica vanilla
            if (current != null) {
                sm.stop(current);
                current = null;
            }
            cachedSongs = null;
            cooldown = 40;
            return;
        }

        if (current != null) {
            if (grace > 0) {
                grace--;
                return;
            }
            if (sm.isActive(current)) {
                return;
            }
            // la cancion ha terminado: pausa y otra distinta
            current = null;
            cooldown = 60 + RNG.nextInt(140);
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        start(mc, sm);
    }

    private static void start(Minecraft mc, SoundManager sm) {
        List<String> available = songs(mc);
        if (available.isEmpty()) {
            cooldown = 200;
            return;
        }
        List<String> pool = new ArrayList<>(available);
        if (pool.size() > 1 && currentName != null) {
            pool.remove(currentName);
        }
        currentName = pool.get(RNG.nextInt(pool.size()));

        ResourceLocation id = new ResourceLocation(ExtractionMenu.MODID, "menu_" + currentName);
        SimpleSoundInstance sound = SimpleSoundInstance.forMusic(SoundEvent.createVariableRangeEvent(id));
        sm.play(sound);
        current = sound;
        grace = 10;

        if (sound.getSound() == SoundManager.EMPTY_SOUND) {
            if (!warnedEmpty) {
                warnedEmpty = true;
                LOGGER.warn("[extractionmenu] Sound event {} has no sound: check assets/extractionmenu/sounds.json", id);
            }
        } else {
            LOGGER.info("[extractionmenu] Playing menu song {}", currentName);
        }
    }

    /** Canciones que de verdad existen en el mod. */
    private static List<String> songs(Minecraft mc) {
        if (cachedSongs == null) {
            List<String> list = new ArrayList<>();
            for (String name : SONGS) {
                ResourceLocation file = new ResourceLocation(ExtractionMenu.MODID, "sounds/" + name + ".ogg");
                if (mc.getResourceManager().getResource(file).isPresent()) {
                    list.add(name);
                }
            }
            if (list.isEmpty()) {
                LOGGER.warn("[extractionmenu] No song1/2/3.ogg found in assets/extractionmenu/sounds/");
            }
            cachedSongs = list;
        }
        return cachedSongs;
    }
}
