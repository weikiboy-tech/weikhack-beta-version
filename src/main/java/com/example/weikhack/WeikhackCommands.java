package com.example.weikhack;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Map;

public final class WeikhackCommands {
    private WeikhackCommands() {
    }

    public static boolean handleChatMessage(String message) {
        if (message == null || !message.startsWith(".")) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String trimmed = message.trim();
        if (trimmed.length() <= 1) {
            send(client, "Tippe .help fuer Befehle.");
            return true;
        }

        String[] parts = trimmed.substring(1).split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "help", "h", "?" -> showHelp(client);
            case "toggle", "t" -> handleToggle(client, parts);
            case "bind", "b" -> handleBind(client, parts);
            case "unbind" -> handleUnbind(client, parts);
            case "clearbinds", "clearbind", "bindclear" -> handleClearBinds(client);
            case "list", "modules" -> showModules(client);
            case "speed" -> handleSpeed(client, parts);
            case "reset" -> WeikhackMod.reset(client);
            default -> send(client, "Unbekannter Befehl. Nutze .help");
        }
        return true;
    }

    private static void showHelp(MinecraftClient client) {
        send(client, ".help - zeigt diese Hilfe");
        send(client, ".toggle <modul> - schaltet ein Modul um");
        send(client, ".bind <modul> <taste> - bindet ein Modul, z.B. .bind chestesp x");
        send(client, ".unbind <modul> - entfernt einen Bind");
        send(client, ".clearbinds - loescht alle Binds");
        send(client, ".speed <1.0-6.0> - setzt den Speed-Regler");
        send(client, "Standard-Binds: F=flight, N=nofall, R=killaura");
        send(client, "Module: flight, speed, nofall, esp, chestesp, noknockback, killaura, activelist");
    }

    private static void handleToggle(MinecraftClient client, String[] parts) {
        if (parts.length < 2) {
            send(client, "Nutzung: .toggle <modul>");
            return;
        }

        if (!WeikhackMod.toggleModule(parts[1], client, true)) {
            send(client, "Unbekanntes Modul: " + parts[1]);
        }
    }

    private static void handleBind(MinecraftClient client, String[] parts) {
        if (parts.length == 1 || parts.length == 2 && parts[1].equalsIgnoreCase("list")) {
            showBinds(client);
            return;
        }
        if (parts.length == 2 && parts[1].equalsIgnoreCase("clear")) {
            handleClearBinds(client);
            return;
        }
        if (parts.length < 3) {
            send(client, "Nutzung: .bind <modul> <taste>");
            return;
        }

        String module = WeikhackMod.canonicalModuleName(parts[1]);
        if (module == null) {
            send(client, "Unbekanntes Modul: " + parts[1]);
            return;
        }

        Integer keyCode = parseKey(parts[2]);
        if (keyCode == null) {
            send(client, "Unbekannte Taste: " + parts[2]);
            return;
        }

        WeikhackMod.bindModule(module, keyCode);
        send(client, "Bind gesetzt: " + WeikhackMod.displayModuleName(module) + " -> " + WeikhackMod.keyName(keyCode));
    }

    private static void handleUnbind(MinecraftClient client, String[] parts) {
        if (parts.length < 2) {
            send(client, "Nutzung: .unbind <modul>");
            return;
        }

        if (WeikhackMod.unbindModule(parts[1])) {
            send(client, "Bind entfernt: " + WeikhackMod.displayModuleName(parts[1]));
        } else {
            send(client, "Unbekanntes Modul: " + parts[1]);
        }
    }

    private static void handleClearBinds(MinecraftClient client) {
        WeikhackMod.clearBinds(client, true);
    }

    private static void handleSpeed(MinecraftClient client, String[] parts) {
        if (parts.length < 2) {
            send(client, "Aktueller Speed: " + String.format(Locale.ROOT, "%.1fx", WeikhackMod.getSpeedMultiplier()));
            return;
        }

        try {
            float speed = Float.parseFloat(parts[1].replace(',', '.'));
            WeikhackMod.setSpeedMultiplier(speed, client, true);
        } catch (NumberFormatException ignored) {
            send(client, "Nutzung: .speed <1.0-6.0>");
        }
    }

    private static void showModules(MinecraftClient client) {
        send(client, "Module: flight, speed, nofall, esp, chestesp, noknockback, killaura, activelist");
        showBinds(client);
    }

    private static void showBinds(MinecraftClient client) {
        Map<String, Integer> binds = WeikhackMod.getKeyBinds();
        if (binds.isEmpty()) {
            send(client, "Keine Binds gesetzt. Beispiel: .bind flight f");
            return;
        }

        for (Map.Entry<String, Integer> bind : binds.entrySet()) {
            send(client, WeikhackMod.displayModuleName(bind.getKey()) + " -> " + WeikhackMod.keyName(bind.getValue()));
        }
    }

    private static Integer parseKey(String rawKey) {
        String key = rawKey.toUpperCase(Locale.ROOT).replace("-", "_");
        if (key.length() == 1) {
            char character = key.charAt(0);
            if (character >= 'A' && character <= 'Z') {
                return GLFW.GLFW_KEY_A + character - 'A';
            }
            if (character >= '0' && character <= '9') {
                return GLFW.GLFW_KEY_0 + character - '0';
            }
        }
        if (key.matches("F\\d{1,2}")) {
            int functionKey = Integer.parseInt(key.substring(1));
            if (functionKey >= 1 && functionKey <= 12) {
                return GLFW.GLFW_KEY_F1 + functionKey - 1;
            }
        }

        return switch (key) {
            case "SPACE", "LEERTASTE" -> GLFW.GLFW_KEY_SPACE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
            case "LSHIFT", "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT", "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL", "LEFT_CTRL", "LEFT_CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL", "RIGHT_CTRL", "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT", "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT", "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            default -> null;
        };
    }

    private static void send(MinecraftClient client, String message) {
        WeikhackMod.sendCommandMessage(client, message);
    }
}
