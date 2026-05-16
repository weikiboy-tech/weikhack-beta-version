# Weikhack Beta Version 0.0.1

Weikhack Beta Version 0.0.1 ist ein externer Fabric Client-Mod für Minecraft 1.21.11. Der Mod wird einfach als normale `.jar` in den Minecraft `mods`-Ordner gelegt und kann dadurch zusammen mit anderen Fabric-Mods genutzt werden, zum Beispiel Cinematica oder weiteren Client-Erweiterungen.

## Features

- Modernes Ingame-UI über `Right Shift`
- Movement-Module wie Flight, Speed und NoFall
- Render-Module wie Player ESP und Storage/Chest ESP
- Combat-Module wie NoKnockback und KillAura für Mobs und optional Spieler
- Chat-Befehle wie `.help`, `.bind`, `.unbind`, `.clearbinds` und `.speed`
- Standard-Binds: `F` für Flight, `N` für NoFall, `R` für KillAura

## Installation

1. Fabric Loader für Minecraft 1.21.11 installieren.
2. Die aktuelle `weikhack-beta-version-0.0.1.jar` aus dem Ordner `releases/` herunterladen.
3. Die Jar in den Minecraft `mods`-Ordner legen.
4. Minecraft mit dem Fabric-Profil starten.

Fabric API wird aktuell nicht als Pflicht-Abhängigkeit benötigt.

## Kompatibilität

Weikhack ist als externer Fabric-Mod gedacht. Dadurch kann er parallel zu Cinematica und anderen Fabric-Mods genutzt werden, solange diese ebenfalls zur Minecraft-Version 1.21.11 passen und keine direkten Konflikte mit denselben Minecraft-Klassen verursachen.

## Download

Die jeweils hochgeladene Jar liegt im Ordner `releases/` dieses Repositories.

## Build

```powershell
.\.gradle-local\gradle-9.2.1\bin\gradle.bat clean build
```

Oder auf GitHub über den Actions-Workflow.

Die fertige Jar liegt danach in:

```text
build/libs/
```

## Creator

Erstellt von Weik.

Verbesserungsvorschläge, Ideen und Bug-Reports sind ausdrücklich willkommen. Bitte schreib dazu möglichst genau, was du impelemtiert haben willst.
