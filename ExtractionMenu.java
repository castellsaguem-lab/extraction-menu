package com.extraction.menu;

import net.minecraftforge.fml.common.Mod;

/** Client-side custom main menu for the Extraction modpack. */
@Mod(ExtractionMenu.MODID)
public class ExtractionMenu {
    public static final String MODID = "extractionmenu";
    public static final String PACK_VERSION = "1.2.0";

    public ExtractionMenu() {
        // Everything is handled by client-side event subscribers.
    }
}
