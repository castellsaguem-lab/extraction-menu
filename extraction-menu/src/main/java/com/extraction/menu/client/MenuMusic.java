package com.extraction.menu.client;

import com.extraction.menu.ExtractionMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plays song1/song2/song3 (random order, never the same one twice in a row) while no world is loaded,
 * and cancels any other music (vanilla menu music, other mods) in that situation.
 */
@Mod.EventBusSubscriber(modid = ExtractionMenu.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MenuMusic {
    private static final String[] SONGS = {"song1", "song2", "song3"};
    private static final int GAP_TICKS = 40;
    private static final Random RNG = new Random();

    private static SoundInstance current;
    private static String currentName;
    private static int gap = 100; // short delay before the first song

    private MenuMusic() {}

    private static boolean inMenu(Minecraft mc) {
        return mc.level == null && mc.player == null;
    }

    private static List<String> availableSongs(Minecraft mc) {
        List<String> list = new ArrayList<>();
        for (String name : SONGS) {
            ResourceLocation file = new ResourceLocation(ExtractionMenu.MODID, "sounds/" + name + ".ogg");
            if (mc.getResourceManager().getResource(file).isPresent()) {
                list.add(name);
            }
        }
        return list;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        if (!inMenu(mc)) {
            if (current != null) {
                mc.getSoundManager().stop(current);
                current = null;
            }
            gap = 40;
            return;
        }

        if (current != null) {
            if (mc.getSoundManager().isActive(current)) {
                return; // still playing
            }
            current = null;
            gap = GAP_TICKS;
        }
        if (gap > 0) {
            gap--;
            return;
        }

        List<String> available = availableSongs(mc);
        if (available.isEmpty()) {
            return;
        }
        if (available.size() > 1 && currentName != null) {
            available.remove(currentName);
        }
        currentName = available.get(RNG.nextInt(available.size()));
        SoundEvent sound = SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ExtractionMenu.MODID, "menu_" + currentName));
        current = SimpleSoundInstance.forMusic(sound);
        mc.getSoundManager().play(current);
    }

    /** While in the menu, block every other music track (vanilla menu music, other mods' menu music). */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || sound.getSource() != SoundSource.MUSIC) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!inMenu(mc) || ExtractionMenu.MODID.equals(sound.getLocation().getNamespace())) {
            return;
        }
        if (!availableSongs(mc).isEmpty()) { // only silence others if our songs are present
            event.setSound(null);
        }
    }
}
