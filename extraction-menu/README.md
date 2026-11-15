# Extraction Menu (Forge 1.20.1, client-side)

Custom main menu: random background (fondo1-4.gif / fondo5-6.png) with mouse parallax,
logo + "Version 1.2.0", Ready-or-Not-style text menu in English, random menu music (song1-3).

## Where to put your files (BEFORE uploading to GitHub)

| File | Folder |
|---|---|
| fondo1.gif ... fondo4.gif | src/main/resources/assets/extractionmenu/backgrounds/ |
| song1.ogg, song2.ogg, song3.ogg | src/main/resources/assets/extractionmenu/sounds/ |

fondo5.png and fondo6.png are already in place. Names must be lowercase and exact.
Songs MUST be OGG Vorbis. Files that are missing are skipped (the mod still works).

## Build with GitHub (no install needed)
1. Create a repository and upload the CONTENTS of this folder (including the hidden .github folder).
2. Open the "Actions" tab -> the "Build mod" run starts by itself.
3. When it turns green, open it and download the artifact "extraction-menu" (a zip containing the .jar).
4. Put the .jar in the mods folder of the modpack.

If the build fails, open the run, copy the red error lines and send them.
