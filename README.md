# Extraction Menu (Forge 1.20.1, client-side)

Menu principal personalizado: fondos que cambian solos con fundido (parallax con el raton),
logo + "Version 1.2.0" y menu de texto estilo Ready or Not.

## Fondos
- Los que van DENTRO del jar: `src/main/resources/assets/extractionmenu/backgrounds/`
  (cualquier nombre, `.gif` o `.png`). Ya estan fondo4.png, fondo5.png y fondo6.png.
- Los GIF PESADOS NO hace falta subirlos a GitHub: ponlos en la carpeta del juego/modpack
  `extractionmenu/backgrounds/` (se crea sola la primera vez que abres el juego).
  Se mezclan con los del jar. Pueden llamarse fondo1.GIF, fondo2.GIF, etc.
- Cada 15 s pasa a otro fondo distinto con un fundido (HOLD_MS / FADE_MS en BackgroundManager.java).

## Musica
Este mod NO añade ni toca la musica del menu: eso lo hacen los mods de musica del pack (Project Pneuma, el mod de OST...).

## Compilar con GitHub (sin instalar nada)
1. Sube el CONTENIDO de esta carpeta (incluida `.github`).
2. Pestana "Actions" -> "Build mod" se lanza sola.
3. Con el check verde, baja el artefacto `extraction-menu` (zip con el .jar) y ponlo en `mods`.
