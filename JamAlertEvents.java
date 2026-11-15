package com.extraction.jamalert;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JamAlert.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class JamAlertEvents {
    private static final int REMIND_TICKS = 100; // reminder every 5 s while the held weapon stays jammed

    private static boolean wasJammed;
    private static int lastSlot = -1;
    private static Item lastItem;
    private static int sinceAlert;

    private JamAlertEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            wasJammed = false;
            lastSlot = -1;
            lastItem = null;
            return;
        }

        ItemStack held = player.getMainHandItem();
        int slot = player.getInventory().selected;
        boolean jammed = isJammed(held);

        if (slot != lastSlot || held.getItem() != lastItem) {
            // changed weapon/slot: do not alert, just remember the state
            lastSlot = slot;
            lastItem = held.getItem();
            wasJammed = jammed;
            sinceAlert = 0;
            return;
        }

        if (jammed) {
            if (!wasJammed) {
                alert(mc, player);
                sinceAlert = 0;
            } else if (++sinceAlert >= REMIND_TICKS) {
                alert(mc, player);
                sinceAlert = 0;
            }
        } else {
            sinceAlert = 0;
        }
        wasJammed = jammed;
    }

    /** Gun Durability stores a "Jammed" flag in the weapon NBT (read at top level and under "gundb"). */
    private static boolean isJammed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        if (tag.getBoolean("Jammed")) {
            return true;
        }
        return tag.contains("gundb", Tag.TAG_COMPOUND) && tag.getCompound("gundb").getBoolean("Jammed");
    }

    private static void alert(Minecraft mc, LocalPlayer player) {
        MutableComponent msg = Component.literal("Your weapon got jammed! Inspect weapon to unjam")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        String key = inspectKeyName(mc);
        if (key != null) {
            msg.append(Component.literal("  [" + key + "]").withStyle(ChatFormatting.GRAY));
        }
        player.displayClientMessage(msg, true); // action bar
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0F));
    }

    /** Finds TACZ's inspect keybind to show the actual key, or null if not found. */
    private static String inspectKeyName(Minecraft mc) {
        try {
            for (KeyMapping km : mc.options.keyMappings) {
                String name = km.getName().toLowerCase();
                if (name.contains("tacz") && name.contains("inspect")) {
                    return km.getTranslatedKeyMessage().getString();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }
}
