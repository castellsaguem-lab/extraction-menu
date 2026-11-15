package com.extraction.menu.client;

import com.extraction.menu.ExtractionMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.SelectMusicEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plays song1/song2/song3 (random order, no immediate repeat) while there is no world loaded,
 * replacing the vanilla menu music on every menu screen.
 */
@Mod.EventBusSubscriber(modid = ExtractionMenu.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MenuMusic {
    private static final String[] SONGS = {"song1", "song2", "song3"};
    private static final Random RNG = new Random();

    private static Music music;
    private static String currentName;
    private static boolean wasPlaying;

    private MenuMusic() {}

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null || mc.player != null) {
            return; // in a world: leave vanilla music alone
        }
        Music m = currentMusic(mc);
        if (m != null) {
            event.setMusic(m);
        }
    }

    private static Music currentMusic(Minecraft mc) {
        List<String> available = new ArrayList<>();
        for (String name : SONGS) {
            ResourceLocation file = new ResourceLocation(ExtractionMenu.MODID, "sounds/" + name + ".ogg");
            if (mc.getResourceManager().getResource(file).isPresent()) {
                available.add(name);
            }
        }
        if (available.isEmpty()) {
            return null; // no songs added yet -> vanilla behaviour
        }

        if (music != null) {
            boolean playing = mc.getMusicManager().isPlayingMusic(music);
            if (playing) {
                wasPlaying = true;
            } else if (wasPlaying) {
                // the song finished: choose another one next time
                wasPlaying = false;
                music = null;
            }
        }

        if (music == null || currentName == null || !available.contains(currentName)) {
            List<String> pool = new ArrayList<>(available);
            if (pool.size() > 1 && currentName != null) {
                pool.remove(currentName);
            }
            currentName = pool.get(RNG.nextInt(pool.size()));
            SoundEvent sound = SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ExtractionMenu.MODID, "menu_" + currentName));
            // 40 ticks of silence between songs; replaceCurrentMusic = true swaps vanilla menu music out.
            music = new Music(Holder.direct(sound), 40, 40, true);
            wasPlaying = false;
        }
        return music;
    }
}
