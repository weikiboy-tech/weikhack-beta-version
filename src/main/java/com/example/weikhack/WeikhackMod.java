package com.example.weikhack;

import com.example.weikhack.mixin.EntityAccessor;
import com.example.weikhack.mixin.SimpleOptionAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class WeikhackMod implements ClientModInitializer {
    public static final String MOD_ID = "weikhack";
    public static final String DISPLAY_VERSION = "Beta Version 0.0.4";

    private static final float MIN_SPEED_MULTIPLIER = 1.0F;
    private static final float MAX_SPEED_MULTIPLIER = 6.0F;
    private static final float DEFAULT_WALK_SPEED = 0.1F;
    private static final float DEFAULT_FLY_SPEED = 0.05F;
    private static final double BASE_HORIZONTAL_SPEED = 0.13D;
    private static final double KILL_AURA_RANGE = 4.25D;
    private static final float KILL_AURA_MIN_COOLDOWN = 0.98F;

    private static boolean menuKeyWasDown;
    private static boolean flightEnabled;
    private static boolean noFallEnabled;
    private static boolean espEnabled;
    private static boolean chestEspEnabled;
    private static boolean fullBrightEnabled;
    private static boolean noKnockbackEnabled;
    private static boolean killAuraEnabled;
    private static boolean killAuraMobsEnabled = true;
    private static boolean killAuraPlayersEnabled;
    private static boolean chestEspChests = true;
    private static boolean chestEspTrappedChests = true;
    private static boolean chestEspEnderChests = true;
    private static boolean chestEspBarrels = true;
    private static boolean chestEspShulkers = true;
    private static boolean activeListEnabled = true;
    private static float speedMultiplier = MIN_SPEED_MULTIPLIER;
    private static Double savedGamma;
    private static final Set<UUID> espGlowingPlayers = new HashSet<>();
    private static final Map<String, Integer> keyBinds = new LinkedHashMap<>();
    private static final Set<String> pressedBindModules = new HashSet<>();

    static {
        resetDefaultBinds();
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.getWindow() != null) {
            boolean menuKeyDown = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (menuKeyDown && !menuKeyWasDown && client.currentScreen == null) {
                client.setScreen(new WeikhackScreen());
            }
            menuKeyWasDown = menuKeyDown;
        }

        if (client.currentScreen == null) {
            handleKeyBinds(client);
        } else {
            pressedBindModules.clear();
        }

        applyFullBright(client);

        if (client.player != null) {
            applyAbilities(client);
            applySpeedHack(client);
            applyNoFall(client);
            applyEsp(client);
            applyKillAura(client);
        } else {
            espGlowingPlayers.clear();
        }
    }

    public static boolean isFlightEnabled() { return flightEnabled; }
    public static boolean isNoFallEnabled() { return noFallEnabled; }
    public static boolean isEspEnabled() { return espEnabled; }
    public static boolean isChestEspEnabled() { return chestEspEnabled; }
    public static boolean isFullBrightEnabled() { return fullBrightEnabled; }
    public static boolean isNoKnockbackEnabled() { return noKnockbackEnabled; }
    public static boolean isKillAuraEnabled() { return killAuraEnabled; }
    public static boolean isKillAuraMobsEnabled() { return killAuraMobsEnabled; }
    public static boolean isKillAuraPlayersEnabled() { return killAuraPlayersEnabled; }
    public static boolean isChestEspChestsEnabled() { return chestEspChests; }
    public static boolean isChestEspTrappedChestsEnabled() { return chestEspTrappedChests; }
    public static boolean isChestEspEnderChestsEnabled() { return chestEspEnderChests; }
    public static boolean isChestEspBarrelsEnabled() { return chestEspBarrels; }
    public static boolean isChestEspShulkersEnabled() { return chestEspShulkers; }
    public static boolean isActiveListEnabled() { return activeListEnabled; }
    public static float getSpeedMultiplier() { return speedMultiplier; }
    public static boolean hasSpeedBoost() { return speedMultiplier > 1.0F; }
    public static Map<String, Integer> getKeyBinds() { return new LinkedHashMap<>(keyBinds); }

    public static void toggleFlight(MinecraftClient client) {
        flightEnabled = !flightEnabled;
        applyAbilities(client);
        sendStatus(client, flightEnabled ? "Fliegen: an" : "Fliegen: aus");
    }

    public static void setNoFallEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        noFallEnabled = enabled;
        if (enabled && client != null && client.player != null) {
            resetFallDistance(client);
        }
        if (notify) sendStatus(client, enabled ? "NoFall: an" : "NoFall: aus");
    }

    public static void setEspEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        espEnabled = enabled;
        if (enabled && client != null && client.worldRenderer != null) {
            client.worldRenderer.loadEntityOutlinePostProcessor();
        }
        if (!enabled) clearEsp(client);
        if (notify) sendStatus(client, enabled ? "ESP: an" : "ESP: aus");
    }

    public static void setChestEspEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        chestEspEnabled = enabled;
        if (notify) sendStatus(client, enabled ? "Chest ESP: an" : "Chest ESP: aus");
    }

    public static void setFullBrightEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        fullBrightEnabled = enabled;
        applyFullBright(client);
        if (notify) sendStatus(client, enabled ? "FullBright: an" : "FullBright: aus");
    }

    public static void setNoKnockbackEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        noKnockbackEnabled = enabled;
        if (notify) sendStatus(client, enabled ? "NoKnockback: an" : "NoKnockback: aus");
    }

    public static void setKillAuraEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        killAuraEnabled = enabled;
        if (notify) sendStatus(client, enabled ? "KillAura: an" : "KillAura: aus");
    }

    public static void setKillAuraMobsEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        killAuraMobsEnabled = enabled;
        if (notify) sendStatus(client, enabled ? "KillAura Mobs: an" : "KillAura Mobs: aus");
    }

    public static void setKillAuraPlayersEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        killAuraPlayersEnabled = enabled;
        if (notify) sendStatus(client, enabled ? "KillAura Players: an" : "KillAura Players: aus");
    }

    public static void setChestEspChestsEnabled(boolean enabled) { chestEspChests = enabled; }
    public static void setChestEspTrappedChestsEnabled(boolean enabled) { chestEspTrappedChests = enabled; }
    public static void setChestEspEnderChestsEnabled(boolean enabled) { chestEspEnderChests = enabled; }
    public static void setChestEspBarrelsEnabled(boolean enabled) { chestEspBarrels = enabled; }
    public static void setChestEspShulkersEnabled(boolean enabled) { chestEspShulkers = enabled; }

    public static void setActiveListEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        activeListEnabled = enabled;
        if (notify) sendStatus(client, enabled ? "Active List: an" : "Active List: aus");
    }

    public static boolean toggleModule(String moduleName, MinecraftClient client, boolean notify) {
        String module = canonicalModuleName(moduleName);
        if (module == null) return false;
        switch (module) {
            case "flight" -> toggleFlight(client);
            case "speed" -> setSpeedMultiplier(hasSpeedBoost() ? 1.0F : Math.max(2.0F, speedMultiplier), client, notify);
            case "nofall" -> setNoFallEnabled(!noFallEnabled, client, notify);
            case "esp" -> setEspEnabled(!espEnabled, client, notify);
            case "chestesp" -> setChestEspEnabled(!chestEspEnabled, client, notify);
            case "fullbright" -> setFullBrightEnabled(!fullBrightEnabled, client, notify);
            case "noknockback" -> setNoKnockbackEnabled(!noKnockbackEnabled, client, notify);
            case "killaura" -> setKillAuraEnabled(!killAuraEnabled, client, notify);
            case "activelist" -> setActiveListEnabled(!activeListEnabled, client, notify);
            default -> { return false; }
        }
        return true;
    }

    public static boolean bindModule(String moduleName, int keyCode) {
        String module = canonicalModuleName(moduleName);
        if (module == null) return false;
        keyBinds.put(module, keyCode);
        pressedBindModules.remove(module);
        return true;
    }

    public static boolean unbindModule(String moduleName) {
        String module = canonicalModuleName(moduleName);
        if (module == null) return false;
        keyBinds.remove(module);
        pressedBindModules.remove(module);
        return true;
    }

    public static void clearBinds(MinecraftClient client, boolean notify) {
        keyBinds.clear();
        pressedBindModules.clear();
        if (notify) sendStatus(client, "Binds gelöscht");
    }

    public static void saveConfig(MinecraftClient client, boolean notify) {
        Properties properties = new Properties();
        properties.setProperty("module.flight", Boolean.toString(flightEnabled));
        properties.setProperty("module.nofall", Boolean.toString(noFallEnabled));
        properties.setProperty("module.esp", Boolean.toString(espEnabled));
        properties.setProperty("module.chestEsp", Boolean.toString(chestEspEnabled));
        properties.setProperty("module.fullBright", Boolean.toString(fullBrightEnabled));
        properties.setProperty("module.noKnockback", Boolean.toString(noKnockbackEnabled));
        properties.setProperty("module.killAura", Boolean.toString(killAuraEnabled));
        properties.setProperty("option.killAuraMobs", Boolean.toString(killAuraMobsEnabled));
        properties.setProperty("option.killAuraPlayers", Boolean.toString(killAuraPlayersEnabled));
        properties.setProperty("option.chestEspChests", Boolean.toString(chestEspChests));
        properties.setProperty("option.chestEspTrappedChests", Boolean.toString(chestEspTrappedChests));
        properties.setProperty("option.chestEspEnderChests", Boolean.toString(chestEspEnderChests));
        properties.setProperty("option.chestEspBarrels", Boolean.toString(chestEspBarrels));
        properties.setProperty("option.chestEspShulkers", Boolean.toString(chestEspShulkers));
        properties.setProperty("option.activeList", Boolean.toString(activeListEnabled));
        properties.setProperty("speed.multiplier", Float.toString(speedMultiplier));
        properties.setProperty("binds.saved", "true");
        for (Map.Entry<String, Integer> bind : keyBinds.entrySet()) {
            properties.setProperty("bind." + bind.getKey(), Integer.toString(bind.getValue()));
        }
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "Weikhack config");
            }
            if (notify) sendStatus(client, "Config gespeichert");
        } catch (IOException exception) {
            if (notify) sendStatus(client, "Config konnte nicht gespeichert werden");
        }
    }

    public static String canonicalModuleName(String moduleName) {
        if (moduleName == null) return null;
        String normalized = moduleName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        return switch (normalized) {
            case "fly", "flight", "fliegen" -> "flight";
            case "speed", "speedhack" -> "speed";
            case "nofall", "fall", "falldamage" -> "nofall";
            case "esp", "playeresp", "player" -> "esp";
            case "chest", "chests", "chestesp", "storageesp", "kistenesp" -> "chestesp";
            case "fullbright", "bright", "brightness", "gamma", "light" -> "fullbright";
            case "nokb", "kb", "velocity", "antiknockback", "noknockback" -> "noknockback";
            case "killaura", "aura", "ka", "mobaura" -> "killaura";
            case "activelist", "active", "hud", "arraylist" -> "activelist";
            default -> null;
        };
    }

    public static String displayModuleName(String moduleName) {
        String module = canonicalModuleName(moduleName);
        if (module == null) return moduleName;
        return switch (module) {
            case "flight" -> "Flight";
            case "speed" -> "Speed";
            case "nofall" -> "NoFall";
            case "esp" -> "ESP";
            case "chestesp" -> "Chest ESP";
            case "fullbright" -> "FullBright";
            case "noknockback" -> "NoKnockback";
            case "killaura" -> "KillAura";
            case "activelist" -> "Active List";
            default -> moduleName;
        };
    }

    public static String keyName(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) return String.valueOf((char) ('A' + keyCode - GLFW.GLFW_KEY_A));
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) return String.valueOf((char) ('0' + keyCode - GLFW.GLFW_KEY_0));
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> "KEY" + keyCode;
        };
    }

    public static List<String> getActiveModuleNames() {
        List<String> modules = new ArrayList<>();
        if (flightEnabled) modules.add("Flight");
        if (hasSpeedBoost()) modules.add("Speed " + String.format(Locale.ROOT, "%.1fx", speedMultiplier));
        if (noFallEnabled) modules.add("NoFall");
        if (espEnabled) modules.add("ESP");
        if (chestEspEnabled) modules.add("Chest ESP");
        if (fullBrightEnabled) modules.add("FullBright");
        if (noKnockbackEnabled) modules.add("NoKnockback");
        if (killAuraEnabled) modules.add(killAuraTargetLabel());
        return modules;
    }

    public static boolean shouldEspHighlight(Entity entity) {
        var client = MinecraftClient.getInstance();
        return espEnabled && client.player != null && entity instanceof PlayerEntity && entity != client.player && !entity.isSpectator();
    }

    public static double getSpeedSliderValue() {
        return (speedMultiplier - MIN_SPEED_MULTIPLIER) / (MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER);
    }

    public static void setSpeedSliderValue(double value) {
        setSpeedMultiplier(MIN_SPEED_MULTIPLIER + (float) value * (MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER), MinecraftClient.getInstance(), false);
    }

    public static void setSpeedMultiplier(float multiplier, MinecraftClient client, boolean notify) {
        speedMultiplier = clampSpeed(multiplier);
        applyAbilities(client);
        if (notify) sendStatus(client, "Speed: " + String.format(Locale.ROOT, "%.1fx", speedMultiplier));
    }

    public static void reset(MinecraftClient client) {
        resetDefaultModules();
        resetDefaultBinds();
        clearEsp(client);
        applyAbilities(client);
        applyFullBright(client);
        sendStatus(client, "Weikhack " + DISPLAY_VERSION + " zurückgesetzt: Module und Binds");
    }

    private static void resetDefaultModules() {
        flightEnabled = false;
        noFallEnabled = false;
        espEnabled = false;
        chestEspEnabled = false;
        fullBrightEnabled = false;
        noKnockbackEnabled = false;
        killAuraEnabled = false;
        killAuraMobsEnabled = true;
        killAuraPlayersEnabled = false;
        chestEspChests = true;
        chestEspTrappedChests = true;
        chestEspEnderChests = true;
        chestEspBarrels = true;
        chestEspShulkers = true;
        activeListEnabled = true;
        speedMultiplier = MIN_SPEED_MULTIPLIER;
    }

    private static void resetDefaultBinds() {
        keyBinds.clear();
        pressedBindModules.clear();
    }

    private static void handleKeyBinds(MinecraftClient client) {
        if (client.getWindow() == null || client.player == null) {
            pressedBindModules.clear();
            return;
        }
        for (Map.Entry<String, Integer> bind : keyBinds.entrySet()) {
            String module = bind.getKey();
            boolean down = InputUtil.isKeyPressed(client.getWindow(), bind.getValue());
            if (down && !pressedBindModules.contains(module)) {
                toggleModule(module, client, true);
                pressedBindModules.add(module);
            } else if (!down) {
                pressedBindModules.remove(module);
            }
        }
    }

    private static void applyAbilities(MinecraftClient client) {
        if (client == null || client.player == null) return;
        var abilities = client.player.getAbilities();
        boolean creativeOrSpectator = client.player.isCreative() || client.player.isSpectator();
        abilities.allowFlying = flightEnabled || creativeOrSpectator;
        if (!abilities.allowFlying) abilities.flying = false;
        abilities.setWalkSpeed(DEFAULT_WALK_SPEED);
        abilities.setFlySpeed(flightEnabled ? DEFAULT_FLY_SPEED * speedMultiplier : DEFAULT_FLY_SPEED);
        client.player.sendAbilitiesUpdate();
    }

    private static void applySpeedHack(MinecraftClient client) {
        if (client.player == null || !hasSpeedBoost() || client.player.hasVehicle() || client.player.isSpectator()) return;
        int forward = pressed(client, client.options.forwardKey) - pressed(client, client.options.backKey);
        int sideways = pressed(client, client.options.leftKey) - pressed(client, client.options.rightKey);
        if (forward == 0 && sideways == 0) return;
        double yawRadians = Math.toRadians(client.player.getYaw());
        double x = forward * -Math.sin(yawRadians) + sideways * Math.cos(yawRadians);
        double z = forward * Math.cos(yawRadians) + sideways * Math.sin(yawRadians);
        double length = Math.sqrt(x * x + z * z);
        if (length < 1.0E-4D) return;
        Vec3d velocity = client.player.getVelocity();
        double targetSpeed = BASE_HORIZONTAL_SPEED * speedMultiplier;
        client.player.setVelocity(x / length * targetSpeed, velocity.y, z / length * targetSpeed);
    }

    private static void applyFullBright(MinecraftClient client) {
        if (client == null || client.options == null) return;
        var gamma = client.options.getGamma();
        if (fullBrightEnabled) {
            if (savedGamma == null) savedGamma = gamma.getValue();
            ((SimpleOptionAccessor) (Object) gamma).weikhack$setValue(16.0D);
            return;
        }
        if (savedGamma != null) {
            gamma.setValue(savedGamma);
            savedGamma = null;
        }
    }

    private static void applyNoFall(MinecraftClient client) {
        if (!noFallEnabled || client.player == null) return;
        resetFallDistance(client);
        if (client.getNetworkHandler() == null || client.player.isOnGround() || client.player.isCreative() || client.player.isSpectator()) return;
        if (client.player.getVelocity().y < -0.05D) {
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
        }
    }

    private static void applyEsp(MinecraftClient client) {
        if (!espEnabled || client.world == null) {
            clearEsp(client);
            return;
        }
        client.worldRenderer.loadEntityOutlinePostProcessor();
        Set<UUID> visibleNow = new HashSet<>();
        for (var player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator()) continue;
            visibleNow.add(player.getUuid());
            if (!player.isGlowing()) espGlowingPlayers.add(player.getUuid());
            player.setGlowing(true);
        }
        espGlowingPlayers.removeIf(uuid -> !visibleNow.contains(uuid));
    }

    private static void applyKillAura(MinecraftClient client) {
        if (!killAuraEnabled || (!killAuraMobsEnabled && !killAuraPlayersEnabled) || client.world == null || client.player == null || client.interactionManager == null || client.currentScreen != null || client.player.isUsingItem() || client.player.getAttackCooldownProgress(0.0F) < KILL_AURA_MIN_COOLDOWN) return;
        Entity target = findKillAuraTarget(client);
        if (target == null) return;
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private static Entity findKillAuraTarget(MinecraftClient client) {
        Entity nearestTarget = null;
        double nearestDistance = KILL_AURA_RANGE * KILL_AURA_RANGE;
        if (killAuraMobsEnabled) {
            for (HostileEntity target : client.world.getEntitiesByClass(HostileEntity.class, client.player.getBoundingBox().expand(KILL_AURA_RANGE), WeikhackMod::isValidKillAuraMob)) {
                double distance = target.squaredDistanceTo(client.player);
                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = target;
                }
            }
        }
        if (killAuraPlayersEnabled) {
            for (PlayerEntity player : client.world.getPlayers()) {
                if (!isValidKillAuraPlayer(client, player)) continue;
                double distance = player.squaredDistanceTo(client.player);
                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = player;
                }
            }
        }
        return nearestTarget;
    }

    private static boolean isValidKillAuraMob(HostileEntity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved() && !entity.isSpectator() && entity.getHealth() > 0.0F;
    }

    private static boolean isValidKillAuraPlayer(MinecraftClient client, PlayerEntity player) {
        return player != null && player != client.player && player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.getHealth() > 0.0F;
    }

    private static String killAuraTargetLabel() {
        if (killAuraMobsEnabled && killAuraPlayersEnabled) return "KillAura All";
        if (killAuraPlayersEnabled) return "KillAura Players";
        if (killAuraMobsEnabled) return "KillAura Mobs";
        return "KillAura";
    }

    private static void clearEsp(MinecraftClient client) {
        if (client != null && client.world != null) {
            for (var player : client.world.getPlayers()) {
                if (espGlowingPlayers.contains(player.getUuid())) player.setGlowing(false);
            }
        }
        espGlowingPlayers.clear();
    }

    private static void resetFallDistance(MinecraftClient client) {
        if (client.player != null) ((EntityAccessor) client.player).weikhack$setFallDistance(0.0D);
    }

    private static void loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            return;
        }
        flightEnabled = booleanProperty(properties, "module.flight", flightEnabled);
        noFallEnabled = booleanProperty(properties, "module.nofall", noFallEnabled);
        espEnabled = booleanProperty(properties, "module.esp", espEnabled);
        chestEspEnabled = booleanProperty(properties, "module.chestEsp", chestEspEnabled);
        fullBrightEnabled = booleanProperty(properties, "module.fullBright", fullBrightEnabled);
        noKnockbackEnabled = booleanProperty(properties, "module.noKnockback", noKnockbackEnabled);
        killAuraEnabled = booleanProperty(properties, "module.killAura", killAuraEnabled);
        killAuraMobsEnabled = booleanProperty(properties, "option.killAuraMobs", killAuraMobsEnabled);
        killAuraPlayersEnabled = booleanProperty(properties, "option.killAuraPlayers", killAuraPlayersEnabled);
        chestEspChests = booleanProperty(properties, "option.chestEspChests", chestEspChests);
        chestEspTrappedChests = booleanProperty(properties, "option.chestEspTrappedChests", chestEspTrappedChests);
        chestEspEnderChests = booleanProperty(properties, "option.chestEspEnderChests", chestEspEnderChests);
        chestEspBarrels = booleanProperty(properties, "option.chestEspBarrels", chestEspBarrels);
        chestEspShulkers = booleanProperty(properties, "option.chestEspShulkers", chestEspShulkers);
        activeListEnabled = booleanProperty(properties, "option.activeList", activeListEnabled);
        speedMultiplier = floatProperty(properties, "speed.multiplier", speedMultiplier);
        loadBinds(properties);
    }

    private static void loadBinds(Properties properties) {
        if (!booleanProperty(properties, "binds.saved", false)) return;
        keyBinds.clear();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("bind.")) continue;
            String module = canonicalModuleName(key.substring("bind.".length()));
            if (module == null) continue;
            try {
                keyBinds.put(module, Integer.parseInt(properties.getProperty(key)));
            } catch (NumberFormatException ignored) {
            }
        }
        pressedBindModules.clear();
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static float floatProperty(Properties properties, String key, float fallback) {
        try {
            return clampSpeed(Float.parseFloat(properties.getProperty(key, Float.toString(fallback))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float clampSpeed(float multiplier) {
        return Math.max(MIN_SPEED_MULTIPLIER, Math.min(MAX_SPEED_MULTIPLIER, Math.round(multiplier * 10.0F) / 10.0F));
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");
    }

    private static int pressed(MinecraftClient client, net.minecraft.client.option.KeyBinding key) {
        return key.isPressed() ? 1 : 0;
    }

    public static void sendCommandMessage(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[Weikhack " + DISPLAY_VERSION + "] " + message), false);
        }
    }

    private static void sendStatus(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }
}
