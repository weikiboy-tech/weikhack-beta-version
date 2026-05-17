package com.example.weikhack;

import com.example.weikhack.mixin.EntityAccessor;
import com.example.weikhack.mixin.PlayerInventoryAccessor;
import com.example.weikhack.mixin.SimpleOptionAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
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
    public static final String DISPLAY_VERSION = "Beta Version 0.15";
    private static final String TITLE_CLICK_SOUND_RESOURCE = "/assets/weikhack/sounds/title_click.wav";
    private static final long SERVER_JOIN_SOUND_COOLDOWN_MS = 1500L;

    private static final float MIN_SPEED_MULTIPLIER = 1.0F;
    private static final float MAX_SPEED_MULTIPLIER = 6.0F;
    private static final float MIN_BOAT_FLY_SPEED = 0.5F;
    private static final float MAX_BOAT_FLY_SPEED = 3.0F;
    private static final float MIN_JUMP_BLOCKS = 1.0F;
    private static final float MAX_JUMP_BLOCKS = 5.0F;
    private static final float DEFAULT_WALK_SPEED = 0.1F;
    private static final float DEFAULT_FLY_SPEED = 0.05F;
    private static final double BASE_HORIZONTAL_SPEED = 0.13D;
    private static final double SPRINT_SPEED_MULTIPLIER = 1.3D;
    private static final double NOVELO_SPEED_MULTIPLIER = 1.08D;
    private static final double NOVELO_AIR_CONTROL_MULTIPLIER = 1.12D;
    private static final double FREECAM_SPEED = 0.22D;
    private static final double FREECAM_SPRINT_SPEED = 0.55D;
    private static final double KILL_AURA_RANGE = 4.25D;
    private static final double DEATH_MARKER_CLEAR_DISTANCE = 10.0D;
    private static final int FAKE_LAG_FLUSH_TICKS = 12;
    private static final int FAKE_LAG_MAX_QUEUE = 120;
    private static final float KILL_AURA_MIN_COOLDOWN = 0.98F;
    private static final int AUTO_EQUIP_COOLDOWN_TICKS = 4;
    private static final Set<String> KILL_AURA_HOSTILE_ENTITY_IDS = Set.of(
            "minecraft:blaze",
            "minecraft:bogged",
            "minecraft:breeze",
            "minecraft:camel_husk",
            "minecraft:cave_spider",
            "minecraft:creaking",
            "minecraft:creeper",
            "minecraft:drowned",
            "minecraft:elder_guardian",
            "minecraft:ender_dragon",
            "minecraft:endermite",
            "minecraft:evoker",
            "minecraft:ghast",
            "minecraft:giant",
            "minecraft:guardian",
            "minecraft:hoglin",
            "minecraft:husk",
            "minecraft:illusioner",
            "minecraft:magma_cube",
            "minecraft:phantom",
            "minecraft:piglin_brute",
            "minecraft:pillager",
            "minecraft:ravager",
            "minecraft:shulker",
            "minecraft:silverfish",
            "minecraft:skeleton",
            "minecraft:slime",
            "minecraft:spider",
            "minecraft:stray",
            "minecraft:vex",
            "minecraft:vindicator",
            "minecraft:warden",
            "minecraft:witch",
            "minecraft:wither",
            "minecraft:wither_skeleton",
            "minecraft:zoglin",
            "minecraft:zombie",
            "minecraft:zombie_nautilus",
            "minecraft:zombie_villager",
            "minecraft:zombified_piglin"
    );

    private static boolean menuKeyWasDown;
    private static boolean flightEnabled;
    private static boolean noFallEnabled;
    private static boolean espEnabled;
    private static boolean chestEspEnabled;
    private static boolean fullBrightEnabled;
    private static boolean noWeatherEnabled;
    private static boolean noKnockbackEnabled;
    private static boolean killAuraEnabled;
    private static boolean killAuraMobsEnabled;
    private static boolean killAuraPlayersEnabled;
    private static boolean chestStealerEnabled;
    private static boolean freecamEnabled;
    private static boolean noVeloEnabled;
    private static boolean autoSprintEnabled;
    private static boolean autoSprintAllDirections;
    private static boolean boatFlyEnabled;
    private static boolean elytraBoostEnabled;
    private static boolean airJumpEnabled;
    private static boolean airJumpWasDown;
    private static boolean jumpHeightEnabled;
    private static boolean jumpHeightApplied;
    private static boolean chestEspChests;
    private static boolean chestEspTrappedChests;
    private static boolean chestEspEnderChests;
    private static boolean chestEspBarrels;
    private static boolean chestEspShulkers;
    private static boolean activeListEnabled = true;
    private static boolean xrayEnabled;
    private static final Map<XrayOre, Boolean> xrayOreOptions = new EnumMap<>(XrayOre.class);
    private static boolean healthBarsEnabled;
    private static boolean autoArmorEnabled;
    private static boolean autoTotemEnabled;
    private static boolean autoToolEnabled;
    private static boolean noSlowdownEnabled;
    private static boolean fastPlaceEnabled;
    private static boolean deathMarkerEnabled;
    private static boolean safeWalkEnabled;
    private static boolean fakeLagEnabled;
    private static boolean wasPlayerDead;
    private static boolean deathMarkerAttackWasDown;
    private static boolean releasingFakeLagPackets;
    private static float speedMultiplier = MIN_SPEED_MULTIPLIER;
    private static float boatFlySpeedMultiplier = MIN_SPEED_MULTIPLIER;
    private static float jumpHeightBlocks = MIN_JUMP_BLOCKS;
    private static Vec3d freecamAnchor;
    private static Vec3d previousFreecamPosition;
    private static Vec3d freecamPosition;
    private static float freecamYaw;
    private static float freecamPitch;
    private static float freecamBodyYaw;
    private static float freecamBodyPitch;
    private static Vec3d lastDeathPosition;
    private static String lastDeathDimension;
    private static Entity lastBoatFlyVehicle;
    private static final Set<UUID> espGlowingPlayers = new HashSet<>();
    private static final Map<String, Integer> keyBinds = new LinkedHashMap<>();
    private static final Set<String> pressedBindModules = new HashSet<>();
    private static Double savedGamma;
    private static long lastServerJoinSoundMs;
    private static int autoEquipCooldownTicks;
    private static int fakeLagTickCounter;
    private static final List<QueuedPacket> fakeLagQueue = new ArrayList<>();

    static {
        resetDefaultXrayOres();
        resetDefaultBinds();
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.getWindow() != null) {
            boolean menuKeyDown = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (menuKeyDown && !menuKeyWasDown && isWeikhackScreen(client.currentScreen)) {
                client.setScreen(null);
            } else if (menuKeyDown && !menuKeyWasDown && client.currentScreen == null) {
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
        applyNoWeather(client);

        if (client.player != null) {
            applyAbilities(client);
            applySpeedHack(client);
            applyBoatFly(client);
            applyElytraBoost(client);
            applyMovementHelpers(client);
            applyNoFall(client);
            applyEsp(client);
            applyKillAura(client);
            applyChestStealer(client);
            applyAutoTotem(client);
            applyAutoArmor(client);
            applyAutoTool(client);
            updateDeathMarker(client);
            handleDeathMarkerClearClick(client);
            tickFakeLag(client);
        } else {
            espGlowingPlayers.clear();
            wasPlayerDead = false;
            deathMarkerAttackWasDown = false;
            clearBoatFlyVehicle();
            flushFakeLagPackets();
        }
    }

    public static void playTitleClickSound() {
        playBundledSound();
    }

    private static void playBundledSound() {
        long now = System.currentTimeMillis();
        if (now - lastServerJoinSoundMs < SERVER_JOIN_SOUND_COOLDOWN_MS) {
            return;
        }
        lastServerJoinSoundMs = now;

        Thread soundThread = new Thread(WeikhackMod::playTitleClickClip, "Weikhack title sound");
        soundThread.setDaemon(true);
        soundThread.start();
    }

    public static boolean isFlightEnabled() {
        return flightEnabled;
    }

    private static boolean isWeikhackScreen(Object screen) {
        return screen instanceof WeikhackScreen
                || screen instanceof WeikhackPanelScreen
                || screen instanceof WeikhackClassicScreen;
    }

    public static boolean isNoFallEnabled() {
        return noFallEnabled;
    }

    public static boolean isEspEnabled() {
        return espEnabled;
    }

    public static boolean isChestEspEnabled() {
        return chestEspEnabled;
    }

    public static boolean isFullBrightEnabled() {
        return fullBrightEnabled;
    }

    public static boolean isNoWeatherEnabled() {
        return noWeatherEnabled;
    }

    public static boolean isNoKnockbackEnabled() {
        return noKnockbackEnabled;
    }

    public static boolean isKillAuraEnabled() {
        return killAuraEnabled;
    }

    public static boolean isKillAuraMobsEnabled() {
        return killAuraMobsEnabled;
    }

    public static boolean isKillAuraPlayersEnabled() {
        return killAuraPlayersEnabled;
    }

    public static boolean isChestStealerEnabled() {
        return chestStealerEnabled;
    }

    public static boolean isFreecamEnabled() {
        return freecamEnabled;
    }

    public static boolean shouldCancelFreecamMovementPacket(Packet<?> packet) {
        return freecamEnabled
                && (packet instanceof PlayerMoveC2SPacket
                || packet instanceof PlayerInputC2SPacket
                || packet instanceof ClientCommandC2SPacket);
    }

    public static boolean shouldDelayFakeLagPacket(ClientConnection connection, Packet<?> packet) {
        if (releasingFakeLagPackets
                || !fakeLagEnabled
                || connection == null
                || packet == null
                || freecamEnabled
                || !isFakeLagPacket(packet)) {
            return false;
        }

        fakeLagQueue.add(new QueuedPacket(connection, packet));
        if (fakeLagQueue.size() > FAKE_LAG_MAX_QUEUE) {
            flushFakeLagPackets();
        }
        return true;
    }

    private static boolean isFakeLagPacket(Packet<?> packet) {
        return packet instanceof PlayerMoveC2SPacket
                || packet instanceof PlayerInputC2SPacket
                || packet instanceof ClientCommandC2SPacket;
    }

    public static boolean handleFreecamLook(double lookDeltaX, double lookDeltaY) {
        if (!freecamEnabled || freecamPosition == null) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.currentScreen != null) {
            return false;
        }

        freecamYaw = (float) (freecamYaw + lookDeltaX * 0.15D);
        freecamPitch = (float) Math.max(-90.0D, Math.min(90.0D, freecamPitch + lookDeltaY * 0.15D));
        restoreFreecamBody(client);
        return true;
    }

    public static Vec3d getFreecamRenderPosition(float tickProgress) {
        if (freecamPosition == null) {
            return null;
        }
        if (previousFreecamPosition == null) {
            return freecamPosition;
        }

        double progress = Math.max(0.0D, Math.min(1.0D, tickProgress));
        return previousFreecamPosition.lerp(freecamPosition, progress);
    }

    public static float getFreecamYaw() {
        return freecamYaw;
    }

    public static float getFreecamPitch() {
        return freecamPitch;
    }

    public static boolean isNoVeloEnabled() {
        return noVeloEnabled;
    }

    public static boolean isAutoSprintEnabled() {
        return autoSprintEnabled;
    }

    public static boolean isAutoSprintAllDirections() {
        return autoSprintAllDirections;
    }

    public static boolean isBoatFlyEnabled() {
        return boatFlyEnabled;
    }

    public static boolean isElytraBoostEnabled() {
        return elytraBoostEnabled;
    }

    public static boolean isAirJumpEnabled() {
        return airJumpEnabled;
    }

    public static boolean isJumpHeightEnabled() {
        return jumpHeightEnabled;
    }

    public static boolean isChestEspChestsEnabled() {
        return chestEspChests;
    }

    public static boolean isChestEspTrappedChestsEnabled() {
        return chestEspTrappedChests;
    }

    public static boolean isChestEspEnderChestsEnabled() {
        return chestEspEnderChests;
    }

    public static boolean isChestEspBarrelsEnabled() {
        return chestEspBarrels;
    }

    public static boolean isChestEspShulkersEnabled() {
        return chestEspShulkers;
    }

    public static boolean isActiveListEnabled() {
        return activeListEnabled;
    }

    public static boolean isXrayEnabled() {
        return xrayEnabled;
    }

    public static boolean isXrayOreEnabled(XrayOre ore) {
        return Boolean.TRUE.equals(xrayOreOptions.get(ore));
    }

    public static boolean isHealthBarsEnabled() {
        return healthBarsEnabled;
    }

    public static boolean isAutoArmorEnabled() {
        return autoArmorEnabled;
    }

    public static boolean isAutoTotemEnabled() {
        return autoTotemEnabled;
    }

    public static boolean isAutoToolEnabled() {
        return autoToolEnabled;
    }

    public static boolean isNoSlowdownEnabled() {
        return noSlowdownEnabled;
    }

    public static boolean isFastPlaceEnabled() {
        return fastPlaceEnabled;
    }

    public static boolean isDeathMarkerEnabled() {
        return deathMarkerEnabled;
    }

    public static boolean isSafeWalkEnabled() {
        return safeWalkEnabled;
    }

    public static boolean isFakeLagEnabled() {
        return fakeLagEnabled;
    }

    public static boolean hasDeathMarker() {
        return lastDeathPosition != null && lastDeathDimension != null;
    }

    public static Vec3d getLastDeathPosition() {
        return lastDeathPosition;
    }

    public static String getLastDeathDimension() {
        return lastDeathDimension;
    }

    public static boolean isLastDeathInCurrentDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null
                && lastDeathDimension != null
                && lastDeathDimension.equals(client.world.getRegistryKey().getValue().toString());
    }

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
        if (notify) {
            sendStatus(client, enabled ? "NoFall: an" : "NoFall: aus");
        }
    }

    public static void setEspEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        espEnabled = enabled;
        if (enabled && client != null && client.worldRenderer != null) {
            client.worldRenderer.loadEntityOutlinePostProcessor();
        }
        if (!enabled) {
            clearEsp(client);
        }
        if (notify) {
            sendStatus(client, enabled ? "ESP: an" : "ESP: aus");
        }
    }

    public static void setChestEspEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        chestEspEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "Chest ESP: an" : "Chest ESP: aus");
        }
    }

    public static void setFullBrightEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        fullBrightEnabled = enabled;
        applyFullBright(client);
        if (notify) {
            sendStatus(client, enabled ? "FullBright: an" : "FullBright: aus");
        }
    }

    public static void setNoWeatherEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        noWeatherEnabled = enabled;
        applyNoWeather(client);
        if (notify) {
            sendStatus(client, enabled ? "NoWeather: an" : "NoWeather: aus");
        }
    }

    public static void setNoKnockbackEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        noKnockbackEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "NoKnockback: an" : "NoKnockback: aus");
        }
    }

    public static void setKillAuraEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        killAuraEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "KillAura: an" : "KillAura: aus");
        }
    }

    public static void setKillAuraMobsEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        killAuraMobsEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "KillAura Mobs: an" : "KillAura Mobs: aus");
        }
    }

    public static void setKillAuraPlayersEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        killAuraPlayersEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "KillAura Players: an" : "KillAura Players: aus");
        }
    }

    public static void setChestStealerEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        chestStealerEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "Chest Stealer: an" : "Chest Stealer: aus");
        }
    }

    public static void setFreecamEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        freecamEnabled = enabled;
        applyFreecamState(client);
        if (notify) {
            sendStatus(client, enabled ? "Freecam: an" : "Freecam: aus");
        }
    }

    public static void setNoVeloEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        noVeloEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "NoVelo: an" : "NoVelo: aus");
        }
    }

    public static void setAutoSprintEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        autoSprintEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "AutoSprint: an" : "AutoSprint: aus");
        }
    }

    public static void setAutoSprintAllDirections(boolean enabled, MinecraftClient client, boolean notify) {
        autoSprintAllDirections = enabled;
        if (notify) {
            sendStatus(client, enabled ? "AutoSprint: WASD" : "AutoSprint: W");
        }
    }

    public static void setBoatFlyEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        boatFlyEnabled = enabled;
        if (!enabled) {
            clearBoatFlyVehicle();
        }
        if (notify) {
            sendStatus(client, enabled ? "BoatFly: an" : "BoatFly: aus");
        }
    }

    public static void setElytraBoostEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        elytraBoostEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "ElytraBoost: an" : "ElytraBoost: aus");
        }
    }

    public static void setAirJumpEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        airJumpEnabled = enabled;
        airJumpWasDown = false;
        if (notify) {
            sendStatus(client, enabled ? "AirJump: an" : "AirJump: aus");
        }
    }

    public static void setJumpHeightEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        jumpHeightEnabled = enabled;
        if (!enabled) {
            jumpHeightApplied = false;
        }
        if (notify) {
            sendStatus(client, enabled ? "JumpHeight: an" : "JumpHeight: aus");
        }
    }

    public static void setChestEspChestsEnabled(boolean enabled) {
        chestEspChests = enabled;
    }

    public static void setChestEspTrappedChestsEnabled(boolean enabled) {
        chestEspTrappedChests = enabled;
    }

    public static void setChestEspEnderChestsEnabled(boolean enabled) {
        chestEspEnderChests = enabled;
    }

    public static void setChestEspBarrelsEnabled(boolean enabled) {
        chestEspBarrels = enabled;
    }

    public static void setChestEspShulkersEnabled(boolean enabled) {
        chestEspShulkers = enabled;
    }

    public static void setActiveListEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        activeListEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "Active List: an" : "Active List: aus");
        }
    }

    public static void setXrayEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        xrayEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "XRay: an" : "XRay: aus");
        }
    }

    public static void setXrayOreEnabled(XrayOre ore, boolean enabled, MinecraftClient client, boolean notify) {
        xrayOreOptions.put(ore, enabled);
        if (notify) {
            sendStatus(client, ore.label() + ": " + (enabled ? "an" : "aus"));
        }
    }

    public static void setHealthBarsEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        healthBarsEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "HealthBars: an" : "HealthBars: aus");
        }
    }

    public static void setAutoArmorEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        autoArmorEnabled = enabled;
        autoEquipCooldownTicks = 0;
        if (notify) {
            sendStatus(client, enabled ? "AutoArmor: an" : "AutoArmor: aus");
        }
    }

    public static void setAutoTotemEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        autoTotemEnabled = enabled;
        autoEquipCooldownTicks = 0;
        if (notify) {
            sendStatus(client, enabled ? "AutoTotem: an" : "AutoTotem: aus");
        }
    }

    public static void setAutoToolEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        autoToolEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "AutoTool: an" : "AutoTool: aus");
        }
    }

    public static void setNoSlowdownEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        noSlowdownEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "NoSlowdown: an" : "NoSlowdown: aus");
        }
    }

    public static void setFastPlaceEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        fastPlaceEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "FastPlace: an" : "FastPlace: aus");
        }
    }

    public static void setDeathMarkerEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        deathMarkerEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "Death Marker: an" : "Death Marker: aus");
        }
    }

    public static void setSafeWalkEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        safeWalkEnabled = enabled;
        if (notify) {
            sendStatus(client, enabled ? "SafeWalk: an" : "SafeWalk: aus");
        }
    }

    public static void setFakeLagEnabled(boolean enabled, MinecraftClient client, boolean notify) {
        fakeLagEnabled = enabled;
        if (!enabled) {
            flushFakeLagPackets();
        }
        if (notify) {
            sendStatus(client, enabled ? "FakeLag: an" : "FakeLag: aus");
        }
    }

    public static boolean toggleModule(String moduleName, MinecraftClient client, boolean notify) {
        String module = canonicalModuleName(moduleName);
        if (module == null) {
            return false;
        }

        switch (module) {
            case "flight" -> toggleFlight(client);
            case "speed" -> setSpeedMultiplier(hasSpeedBoost() ? 1.0F : Math.max(2.0F, getSpeedMultiplier()), client, notify);
            case "nofall" -> setNoFallEnabled(!isNoFallEnabled(), client, notify);
            case "esp" -> setEspEnabled(!isEspEnabled(), client, notify);
            case "chestesp" -> setChestEspEnabled(!isChestEspEnabled(), client, notify);
            case "fullbright" -> setFullBrightEnabled(!isFullBrightEnabled(), client, notify);
            case "noweather" -> setNoWeatherEnabled(!isNoWeatherEnabled(), client, notify);
            case "noknockback" -> setNoKnockbackEnabled(!isNoKnockbackEnabled(), client, notify);
            case "killaura" -> setKillAuraEnabled(!isKillAuraEnabled(), client, notify);
            case "cheststealer" -> setChestStealerEnabled(!isChestStealerEnabled(), client, notify);
            case "freecam" -> setFreecamEnabled(!isFreecamEnabled(), client, notify);
            case "novelo" -> setNoVeloEnabled(!isNoVeloEnabled(), client, notify);
            case "autosprint" -> setAutoSprintEnabled(!isAutoSprintEnabled(), client, notify);
            case "boatfly" -> setBoatFlyEnabled(!isBoatFlyEnabled(), client, notify);
            case "elytraboost" -> setElytraBoostEnabled(!isElytraBoostEnabled(), client, notify);
            case "airjump" -> setAirJumpEnabled(!isAirJumpEnabled(), client, notify);
            case "jumpheight" -> setJumpHeightEnabled(!isJumpHeightEnabled(), client, notify);
            case "activelist" -> setActiveListEnabled(!isActiveListEnabled(), client, notify);
            case "xray" -> setXrayEnabled(!isXrayEnabled(), client, notify);
            case "healthbars" -> setHealthBarsEnabled(!isHealthBarsEnabled(), client, notify);
            case "autoarmor" -> setAutoArmorEnabled(!isAutoArmorEnabled(), client, notify);
            case "autototem" -> setAutoTotemEnabled(!isAutoTotemEnabled(), client, notify);
            case "autotool" -> setAutoToolEnabled(!isAutoToolEnabled(), client, notify);
            case "noslowdown" -> setNoSlowdownEnabled(!isNoSlowdownEnabled(), client, notify);
            case "fastplace" -> setFastPlaceEnabled(!isFastPlaceEnabled(), client, notify);
            case "deathmarker" -> setDeathMarkerEnabled(!isDeathMarkerEnabled(), client, notify);
            case "safewalk" -> setSafeWalkEnabled(!isSafeWalkEnabled(), client, notify);
            case "fakelag" -> setFakeLagEnabled(!isFakeLagEnabled(), client, notify);
            default -> {
                return false;
            }
        }
        return true;
    }

    public static boolean bindModule(String moduleName, int keyCode) {
        String module = canonicalModuleName(moduleName);
        if (module == null) {
            return false;
        }

        keyBinds.put(module, keyCode);
        pressedBindModules.remove(module);
        return true;
    }

    public static boolean unbindModule(String moduleName) {
        String module = canonicalModuleName(moduleName);
        if (module == null) {
            return false;
        }

        keyBinds.remove(module);
        pressedBindModules.remove(module);
        return true;
    }

    public static void clearBinds(MinecraftClient client, boolean notify) {
        keyBinds.clear();
        pressedBindModules.clear();
        if (notify) {
            sendStatus(client, "Binds gelöscht");
        }
    }

    public static void saveConfig(MinecraftClient client, boolean notify) {
        Properties properties = new Properties();
        properties.setProperty("config.version", DISPLAY_VERSION);
        properties.setProperty("module.flight", Boolean.toString(flightEnabled));
        properties.setProperty("module.nofall", Boolean.toString(noFallEnabled));
        properties.setProperty("module.esp", Boolean.toString(espEnabled));
        properties.setProperty("module.chestEsp", Boolean.toString(chestEspEnabled));
        properties.setProperty("module.fullBright", Boolean.toString(fullBrightEnabled));
        properties.setProperty("module.noWeather", Boolean.toString(noWeatherEnabled));
        properties.setProperty("module.noKnockback", Boolean.toString(noKnockbackEnabled));
        properties.setProperty("module.killAura", Boolean.toString(killAuraEnabled));
        properties.setProperty("module.chestStealer", Boolean.toString(chestStealerEnabled));
        properties.setProperty("module.freecam", Boolean.toString(freecamEnabled));
        properties.setProperty("module.noVelo", Boolean.toString(noVeloEnabled));
        properties.setProperty("module.autoSprint", Boolean.toString(autoSprintEnabled));
        properties.setProperty("module.boatFly", Boolean.toString(boatFlyEnabled));
        properties.setProperty("module.elytraBoost", Boolean.toString(elytraBoostEnabled));
        properties.setProperty("module.airJump", Boolean.toString(airJumpEnabled));
        properties.setProperty("module.jumpHeight", Boolean.toString(jumpHeightEnabled));
        properties.setProperty("module.xray", Boolean.toString(xrayEnabled));
        properties.setProperty("module.healthBars", Boolean.toString(healthBarsEnabled));
        properties.setProperty("module.autoArmor", Boolean.toString(autoArmorEnabled));
        properties.setProperty("module.autoTotem", Boolean.toString(autoTotemEnabled));
        properties.setProperty("module.autoTool", Boolean.toString(autoToolEnabled));
        properties.setProperty("module.noSlowdown", Boolean.toString(noSlowdownEnabled));
        properties.setProperty("module.fastPlace", Boolean.toString(fastPlaceEnabled));
        properties.setProperty("module.deathMarker", Boolean.toString(deathMarkerEnabled));
        properties.setProperty("module.safeWalk", Boolean.toString(safeWalkEnabled));
        properties.setProperty("module.fakeLag", Boolean.toString(fakeLagEnabled));
        properties.setProperty("option.autoSprintAllDirections", Boolean.toString(autoSprintAllDirections));
        properties.setProperty("option.killAuraMobs", Boolean.toString(killAuraMobsEnabled));
        properties.setProperty("option.killAuraPlayers", Boolean.toString(killAuraPlayersEnabled));
        properties.setProperty("option.chestEspChests", Boolean.toString(chestEspChests));
        properties.setProperty("option.chestEspTrappedChests", Boolean.toString(chestEspTrappedChests));
        properties.setProperty("option.chestEspEnderChests", Boolean.toString(chestEspEnderChests));
        properties.setProperty("option.chestEspBarrels", Boolean.toString(chestEspBarrels));
        properties.setProperty("option.chestEspShulkers", Boolean.toString(chestEspShulkers));
        for (XrayOre ore : XrayOre.values()) {
            properties.setProperty("option.xray." + ore.configKey(), Boolean.toString(isXrayOreEnabled(ore)));
        }
        properties.setProperty("option.activeList", Boolean.toString(activeListEnabled));
        properties.setProperty("speed.multiplier", Float.toString(speedMultiplier));
        properties.setProperty("boatFly.speed", Float.toString(boatFlySpeedMultiplier));
        properties.setProperty("jump.blocks", Float.toString(jumpHeightBlocks));
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
            if (notify) {
                sendStatus(client, "Config gespeichert");
            }
        } catch (IOException exception) {
            if (notify) {
                sendStatus(client, "Config konnte nicht gespeichert werden");
            }
        }
    }

    public static Map<String, Integer> getKeyBinds() {
        return new LinkedHashMap<>(keyBinds);
    }

    public static String canonicalModuleName(String moduleName) {
        if (moduleName == null) {
            return null;
        }

        String normalized = moduleName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        return switch (normalized) {
            case "fly", "flight", "fliegen" -> "flight";
            case "speed", "speedhack" -> "speed";
            case "nofall", "fall", "falldamage" -> "nofall";
            case "esp", "playeresp", "player" -> "esp";
            case "chest", "chests", "chestesp", "storageesp", "kistenesp" -> "chestesp";
            case "fullbright", "bright", "brightness", "gamma", "light" -> "fullbright";
            case "noweather", "weather", "clearweather", "norain", "nosnow", "rain" -> "noweather";
            case "nokb", "kb", "velocity", "antiknockback", "noknockback" -> "noknockback";
            case "killaura", "aura", "ka", "mobaura" -> "killaura";
            case "cheststealer", "stealer", "loot", "autoloot" -> "cheststealer";
            case "freecam", "camera" -> "freecam";
            case "novelo", "nvelo", "constantvelocity", "constantvelo" -> "novelo";
            case "autosprint", "sprint" -> "autosprint";
            case "boatfly", "boat", "boatflight", "bootfly", "bootflug" -> "boatfly";
            case "elytraboost", "elytra", "elytrafly", "elytraflight", "rocketboost" -> "elytraboost";
            case "airjump", "doublejump", "doppelsprung" -> "airjump";
            case "jumpheight", "jump", "highjump", "sprunghoehe", "sprunghöhe" -> "jumpheight";
            case "activelist", "active", "hud", "arraylist" -> "activelist";
            case "xray", "oreesp", "ores" -> "xray";
            case "healthbars", "healthbar", "hpbar", "hp" -> "healthbars";
            case "autoarmor", "armor" -> "autoarmor";
            case "autototem", "totem" -> "autototem";
            case "autotool", "tool", "tools", "besttool", "weapon", "bestweapon" -> "autotool";
            case "noslowdown", "noslow", "nslow" -> "noslowdown";
            case "fastplace", "place" -> "fastplace";
            case "deathmarker", "death", "waypoint", "deathwaypoint", "tod", "todmarker", "todesmarker" -> "deathmarker";
            case "safewalk", "safe", "edgewalk", "edge", "sneakwalk", "blocksafe" -> "safewalk";
            case "fakelag", "lag", "blink", "packetlag" -> "fakelag";
            default -> null;
        };
    }

    public static String displayModuleName(String moduleName) {
        String module = canonicalModuleName(moduleName);
        if (module == null) {
            return moduleName;
        }

        return switch (module) {
            case "flight" -> "Flight";
            case "speed" -> "Speed";
            case "nofall" -> "NoFall";
            case "esp" -> "ESP";
            case "chestesp" -> "Chest ESP";
            case "fullbright" -> "FullBright";
            case "noweather" -> "NoWeather";
            case "noknockback" -> "NoKnockback";
            case "killaura" -> "KillAura";
            case "cheststealer" -> "Chest Stealer";
            case "freecam" -> "Freecam";
            case "novelo" -> "NoVelo";
            case "autosprint" -> "AutoSprint";
            case "boatfly" -> "BoatFly";
            case "elytraboost" -> "ElytraBoost";
            case "airjump" -> "AirJump";
            case "jumpheight" -> "JumpHeight";
            case "activelist" -> "Active List";
            case "xray" -> "XRay";
            case "healthbars" -> "HealthBars";
            case "autoarmor" -> "AutoArmor";
            case "autototem" -> "AutoTotem";
            case "autotool" -> "AutoTool";
            case "noslowdown" -> "NoSlowdown";
            case "fastplace" -> "FastPlace";
            case "deathmarker" -> "Death Marker";
            case "safewalk" -> "SafeWalk";
            case "fakelag" -> "FakeLag";
            default -> moduleName;
        };
    }

    public static String keyName(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + keyCode - GLFW.GLFW_KEY_A));
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + keyCode - GLFW.GLFW_KEY_0));
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }

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
        if (flightEnabled) {
            modules.add("Flight");
        }
        if (hasSpeedBoost()) {
            modules.add("Speed " + String.format("%.1fx", getSpeedMultiplier()));
        }
        if (noFallEnabled) {
            modules.add("NoFall");
        }
        if (safeWalkEnabled) {
            modules.add("SafeWalk");
        }
        if (noVeloEnabled) {
            modules.add("NoVelo");
        }
        if (autoSprintEnabled) {
            modules.add("AutoSprint");
        }
        if (boatFlyEnabled) {
            modules.add("BoatFly " + String.format(Locale.ROOT, "%.1fx", boatFlySpeedMultiplier));
        }
        if (elytraBoostEnabled) {
            modules.add("ElytraBoost");
        }
        if (airJumpEnabled) {
            modules.add("AirJump");
        }
        if (jumpHeightEnabled) {
            modules.add("Jump " + String.format(Locale.ROOT, "%.1f", jumpHeightBlocks) + "b");
        }
        if (espEnabled) {
            modules.add("ESP");
        }
        if (xrayEnabled) {
            modules.add("XRay");
        }
        if (healthBarsEnabled) {
            modules.add("HealthBars");
        }
        if (chestEspEnabled) {
            modules.add("Chest ESP");
        }
        if (fullBrightEnabled) {
            modules.add("FullBright");
        }
        if (noWeatherEnabled) {
            modules.add("NoWeather");
        }
        if (deathMarkerEnabled) {
            modules.add("Death Marker");
        }
        if (noKnockbackEnabled) {
            modules.add("NoKnockback");
        }
        if (killAuraEnabled) {
            modules.add(killAuraTargetLabel());
        }
        if (chestStealerEnabled) {
            modules.add("Chest Stealer");
        }
        if (freecamEnabled) {
            modules.add("Freecam");
        }
        if (autoArmorEnabled) {
            modules.add("AutoArmor");
        }
        if (autoTotemEnabled) {
            modules.add("AutoTotem");
        }
        if (autoToolEnabled) {
            modules.add("AutoTool");
        }
        if (noSlowdownEnabled) {
            modules.add("NoSlowdown");
        }
        if (fastPlaceEnabled) {
            modules.add("FastPlace");
        }
        if (fakeLagEnabled) {
            modules.add("FakeLag");
        }
        return modules;
    }

    public static boolean shouldEspHighlight(Entity entity) {
        var client = MinecraftClient.getInstance();
        return espEnabled
                && client.player != null
                && entity instanceof PlayerEntity
                && entity != client.player
                && !entity.isSpectator();
    }

    public static boolean shouldBlockWeatherSound(SoundInstance sound) {
        if (!noWeatherEnabled || sound == null || sound.getId() == null) {
            return false;
        }

        String id = sound.getId().toString();
        return sound.getCategory() == SoundCategory.WEATHER
                || id.equals("minecraft:weather.rain")
                || id.equals("minecraft:weather.rain.above")
                || id.equals("minecraft:entity.lightning_bolt.thunder")
                || id.equals("minecraft:item.trident.thunder")
                || id.startsWith("minecraft:weather.");
    }

    public static float getSpeedMultiplier() {
        return speedMultiplier;
    }

    public static boolean hasSpeedBoost() {
        return getSpeedMultiplier() > 1.0F;
    }

    public static float getBoatFlySpeedMultiplier() {
        return boatFlySpeedMultiplier;
    }

    public static double getBoatFlySliderValue() {
        return (boatFlySpeedMultiplier - MIN_BOAT_FLY_SPEED) / (MAX_BOAT_FLY_SPEED - MIN_BOAT_FLY_SPEED);
    }

    public static void setBoatFlySliderValue(double value) {
        float multiplier = MIN_BOAT_FLY_SPEED + (float) value * (MAX_BOAT_FLY_SPEED - MIN_BOAT_FLY_SPEED);
        setBoatFlySpeedMultiplier(multiplier, MinecraftClient.getInstance(), false);
    }

    public static void setBoatFlySpeedMultiplier(float multiplier, MinecraftClient client, boolean notify) {
        boatFlySpeedMultiplier = clampBoatFlySpeed(multiplier);
        if (notify) {
            sendStatus(client, "BoatFly Speed: " + String.format(Locale.ROOT, "%.1fx", boatFlySpeedMultiplier));
        }
    }

    public static void cycleSpeed(MinecraftClient client) {
        float next = speedMultiplier + 0.5F;
        if (next > MAX_SPEED_MULTIPLIER) {
            next = MIN_SPEED_MULTIPLIER;
        }

        setSpeedMultiplier(next, client, true);
    }

    public static double getSpeedSliderValue() {
        return (speedMultiplier - MIN_SPEED_MULTIPLIER) / (MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER);
    }

    public static void setSpeedSliderValue(double value) {
        float multiplier = MIN_SPEED_MULTIPLIER + (float) value * (MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER);
        setSpeedMultiplier(multiplier, MinecraftClient.getInstance(), false);
    }

    public static void setSpeedMultiplier(float multiplier, MinecraftClient client, boolean notify) {
        speedMultiplier = clampSpeed(multiplier);
        applyAbilities(client);
        if (notify) {
            sendStatus(client, "Speed: " + String.format("%.1fx", getSpeedMultiplier()));
        }
    }

    public static float getJumpHeightBlocks() {
        return jumpHeightBlocks;
    }

    public static double getJumpSliderValue() {
        return (jumpHeightBlocks - MIN_JUMP_BLOCKS) / (MAX_JUMP_BLOCKS - MIN_JUMP_BLOCKS);
    }

    public static void setJumpSliderValue(double value) {
        float blocks = MIN_JUMP_BLOCKS + (float) value * (MAX_JUMP_BLOCKS - MIN_JUMP_BLOCKS);
        setJumpHeightBlocks(blocks, MinecraftClient.getInstance(), false);
    }

    public static void setJumpHeightBlocks(float blocks, MinecraftClient client, boolean notify) {
        jumpHeightBlocks = clampJumpBlocks(blocks);
        if (notify) {
            sendStatus(client, "Sprunghöhe: " + String.format(Locale.ROOT, "%.1f Blöcke", jumpHeightBlocks));
        }
    }

    public static void applyConfigPreset(String presetName, MinecraftClient client) {
        String preset = presetName == null ? "" : presetName.toLowerCase(Locale.ROOT);
        resetDefaultModules();
        applyFreecamState(client);
        clearEsp(client);

        activeListEnabled = true;
        fullBrightEnabled = true;

        switch (preset) {
            case "pve" -> {
                killAuraEnabled = true;
                killAuraMobsEnabled = true;
                killAuraPlayersEnabled = false;
                autoArmorEnabled = true;
                autoTotemEnabled = true;
                noKnockbackEnabled = true;
                noSlowdownEnabled = true;
                safeWalkEnabled = true;
                healthBarsEnabled = true;
                deathMarkerEnabled = true;
            }
            case "pvp" -> {
                killAuraEnabled = true;
                killAuraMobsEnabled = false;
                killAuraPlayersEnabled = true;
                autoArmorEnabled = true;
                autoTotemEnabled = true;
                noKnockbackEnabled = true;
                noSlowdownEnabled = true;
                healthBarsEnabled = true;
                espEnabled = true;
                noFallEnabled = true;
                safeWalkEnabled = true;
            }
            case "mining" -> {
                xrayEnabled = true;
                chestEspEnabled = true;
                chestEspChests = true;
                chestEspTrappedChests = true;
                chestEspEnderChests = true;
                chestEspBarrels = true;
                chestEspShulkers = true;
                setAllXrayOres(true);
                noFallEnabled = true;
                safeWalkEnabled = true;
                deathMarkerEnabled = true;
            }
            case "travel" -> {
                speedMultiplier = 2.0F;
                boatFlySpeedMultiplier = 1.6F;
                autoSprintEnabled = true;
                autoSprintAllDirections = true;
                boatFlyEnabled = true;
                elytraBoostEnabled = true;
                noFallEnabled = true;
                safeWalkEnabled = true;
                deathMarkerEnabled = true;
            }
            case "rage" -> {
                speedMultiplier = 3.7F;
                killAuraEnabled = true;
                killAuraMobsEnabled = false;
                killAuraPlayersEnabled = true;
                noKnockbackEnabled = true;
                noVeloEnabled = true;
                noSlowdownEnabled = true;
                autoSprintEnabled = true;
                autoSprintAllDirections = true;
                autoArmorEnabled = true;
                autoTotemEnabled = true;
                autoToolEnabled = true;
                fastPlaceEnabled = true;
                espEnabled = true;
                healthBarsEnabled = true;
                noFallEnabled = true;
                safeWalkEnabled = true;
                deathMarkerEnabled = true;
            }
            default -> {
                sendStatus(client, "Unbekannte Config: " + presetName);
                applyAbilities(client);
                applyFullBright(client);
                return;
            }
        }

        if (espEnabled && client != null && client.worldRenderer != null) {
            client.worldRenderer.loadEntityOutlinePostProcessor();
        }
        applyAbilities(client);
        applyFullBright(client);
        sendStatus(client, "Config geladen: " + presetName.toUpperCase(Locale.ROOT));
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
        noWeatherEnabled = false;
        noKnockbackEnabled = false;
        killAuraEnabled = false;
        killAuraMobsEnabled = false;
        killAuraPlayersEnabled = false;
        chestStealerEnabled = false;
        freecamEnabled = false;
        noVeloEnabled = false;
        autoSprintEnabled = false;
        autoSprintAllDirections = false;
        setBoatFlyEnabled(false, null, false);
        elytraBoostEnabled = false;
        airJumpEnabled = false;
        airJumpWasDown = false;
        jumpHeightEnabled = false;
        jumpHeightApplied = false;
        xrayEnabled = false;
        healthBarsEnabled = false;
        autoArmorEnabled = false;
        autoTotemEnabled = false;
        autoToolEnabled = false;
        noSlowdownEnabled = false;
        fastPlaceEnabled = false;
        deathMarkerEnabled = false;
        safeWalkEnabled = false;
        setFakeLagEnabled(false, null, false);
        wasPlayerDead = false;
        chestEspChests = false;
        chestEspTrappedChests = false;
        chestEspEnderChests = false;
        chestEspBarrels = false;
        chestEspShulkers = false;
        resetDefaultXrayOres();
        activeListEnabled = true;
        speedMultiplier = MIN_SPEED_MULTIPLIER;
        boatFlySpeedMultiplier = MIN_SPEED_MULTIPLIER;
        jumpHeightBlocks = MIN_JUMP_BLOCKS;
        autoEquipCooldownTicks = 0;
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
        if (client.player == null) {
            return;
        }

        var abilities = client.player.getAbilities();
        boolean creativeOrSpectator = client.player.isCreative() || client.player.isSpectator();

        abilities.allowFlying = flightEnabled || creativeOrSpectator;
        if (!abilities.allowFlying) {
            abilities.flying = false;
        }

        abilities.setWalkSpeed(DEFAULT_WALK_SPEED);
        abilities.setFlySpeed(flightEnabled ? DEFAULT_FLY_SPEED * getSpeedMultiplier() : DEFAULT_FLY_SPEED);
        client.player.sendAbilitiesUpdate();
    }

    private static void applySpeedHack(MinecraftClient client) {
        if (client.player == null || !hasSpeedBoost() || client.player.hasVehicle() || client.player.isSpectator()) {
            return;
        }

        int forward = pressed(client, client.options.forwardKey) - pressed(client, client.options.backKey);
        int sideways = pressed(client, client.options.leftKey) - pressed(client, client.options.rightKey);
        if (forward == 0 && sideways == 0) {
            return;
        }

        double yawRadians = Math.toRadians(client.player.getYaw());
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        double x = forward * -sin + sideways * cos;
        double z = forward * cos + sideways * sin;
        double length = Math.sqrt(x * x + z * z);
        if (length < 1.0E-4D) {
            return;
        }

        double targetSpeed = BASE_HORIZONTAL_SPEED * speedMultiplier;
        Vec3d direction = new Vec3d(x / length, 0.0D, z / length);
        Vec3d velocity = client.player.getVelocity();
        client.player.setVelocity(direction.x * targetSpeed, velocity.y, direction.z * targetSpeed);
    }

    private static void applyBoatFly(MinecraftClient client) {
        if (client.player == null || client.options == null) {
            clearBoatFlyVehicle();
            return;
        }

        Entity vehicle = client.player.getVehicle();
        if (!boatFlyEnabled || !(vehicle instanceof AbstractBoatEntity)) {
            clearBoatFlyVehicle();
            return;
        }

        lastBoatFlyVehicle = vehicle;
        vehicle.setNoGravity(true);
        vehicle.setOnGround(false);
        vehicle.fallDistance = 0.0D;

        int forward = pressed(client, client.options.forwardKey) - pressed(client, client.options.backKey);
        int sideways = pressed(client, client.options.leftKey) - pressed(client, client.options.rightKey);
        int vertical = pressed(client, client.options.jumpKey) - pressed(client, client.options.sprintKey);
        double horizontalSpeed = 0.45D * boatFlySpeedMultiplier;
        double verticalSpeed = 0.32D * boatFlySpeedMultiplier;

        double x = 0.0D;
        double z = 0.0D;
        if (forward != 0 || sideways != 0) {
            double yawRadians = Math.toRadians(client.player.getYaw());
            double sin = Math.sin(yawRadians);
            double cos = Math.cos(yawRadians);
            x = forward * -sin + sideways * cos;
            z = forward * cos + sideways * sin;
            double length = Math.sqrt(x * x + z * z);
            if (length > 1.0E-4D) {
                x = x / length * horizontalSpeed;
                z = z / length * horizontalSpeed;
            }
        }

        vehicle.setYaw(client.player.getYaw());
        vehicle.setVelocity(x, vertical * verticalSpeed, z);
    }

    private static void clearBoatFlyVehicle() {
        if (lastBoatFlyVehicle != null) {
            lastBoatFlyVehicle.setNoGravity(false);
            lastBoatFlyVehicle = null;
        }
    }

    private static void applyElytraBoost(MinecraftClient client) {
        if (!elytraBoostEnabled
                || client.player == null
                || client.options == null
                || client.player.hasVehicle()
                || !client.player.isGliding()) {
            return;
        }

        boolean boostPressed = client.options.forwardKey.isPressed() || client.options.sprintKey.isPressed();
        if (!boostPressed) {
            return;
        }

        Vec3d look = lookDirection(client.player.getYaw(), client.player.getPitch());
        Vec3d velocity = client.player.getVelocity();
        double acceleration = client.options.sprintKey.isPressed() ? 0.095D : 0.055D;
        double maxSpeed = client.options.sprintKey.isPressed() ? 2.55D : 1.75D;
        Vec3d boosted = velocity.add(look.multiply(acceleration));
        double speed = boosted.length();
        if (speed > maxSpeed) {
            boosted = boosted.multiply(maxSpeed / speed);
        }

        client.player.setVelocity(boosted);
        client.player.fallDistance = 0.0D;
    }

    private static Vec3d lookDirection(float yaw, float pitch) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRadians);
        return new Vec3d(
                -Math.sin(yawRadians) * cosPitch,
                -Math.sin(pitchRadians),
                Math.cos(yawRadians) * cosPitch
        ).normalize();
    }

    private static void applyMovementHelpers(MinecraftClient client) {
        if (client.player == null || client.options == null) {
            return;
        }

        if (autoSprintEnabled && isAutoSprintMovementPressed(client) && !client.player.isSneaking()) {
            client.player.setSprinting(true);
        }

        if (noVeloEnabled && !hasSpeedBoost()) {
            applyConstantHorizontalVelocity(client, noVeloTargetSpeed(client));
        }

        applyJumpHeight(client);
        applyAirJump(client);

        if (freecamEnabled) {
            applyFreecamMovement(client);
        }
    }

    private static void applyAirJump(MinecraftClient client) {
        boolean jumpDown = client.options.jumpKey.isPressed();
        if (!airJumpEnabled
                || client.player.hasVehicle()
                || client.player.isSpectator()
                || client.player.isTouchingWater()
                || client.player.isInLava()) {
            airJumpWasDown = jumpDown;
            return;
        }

        if (client.player.isOnGround() || client.player.getAbilities().flying || client.player.isGliding()) {
            airJumpWasDown = jumpDown;
            return;
        }

        if (jumpDown && !airJumpWasDown) {
            Vec3d velocity = client.player.getVelocity();
            double jumpVelocity = jumpHeightEnabled ? 0.42D * Math.sqrt(jumpHeightBlocks) : 0.42D;
            client.player.setVelocity(velocity.x, Math.max(jumpVelocity, 0.42D), velocity.z);
            client.player.fallDistance = 0.0D;
        }

        airJumpWasDown = jumpDown;
    }

    private static void applyJumpHeight(MinecraftClient client) {
        if (!jumpHeightEnabled || !client.options.jumpKey.isPressed()) {
            jumpHeightApplied = false;
            return;
        }

        Vec3d velocity = client.player.getVelocity();
        double jumpVelocity = 0.42D * Math.sqrt(jumpHeightBlocks);
        if (client.player.isOnGround()) {
            client.player.setVelocity(velocity.x, jumpVelocity, velocity.z);
            jumpHeightApplied = true;
            return;
        }

        if (!jumpHeightApplied && velocity.y > 0.0D && velocity.y < jumpVelocity) {
            client.player.setVelocity(velocity.x, jumpVelocity, velocity.z);
            jumpHeightApplied = true;
        }
    }

    private static double noVeloTargetSpeed(MinecraftClient client) {
        double targetSpeed = BASE_HORIZONTAL_SPEED * NOVELO_SPEED_MULTIPLIER;
        if (shouldUseSprintSpeed(client)) {
            targetSpeed *= SPRINT_SPEED_MULTIPLIER;
        }
        if (!client.player.isOnGround()) {
            targetSpeed *= NOVELO_AIR_CONTROL_MULTIPLIER;
        }
        return targetSpeed;
    }

    private static boolean shouldUseSprintSpeed(MinecraftClient client) {
        return client.player.isSprinting()
                || client.options.sprintKey.isPressed()
                || (autoSprintEnabled && isAutoSprintMovementPressed(client));
    }

    private static void applyConstantHorizontalVelocity(MinecraftClient client, double targetSpeed) {
        int forward = pressed(client, client.options.forwardKey) - pressed(client, client.options.backKey);
        int sideways = pressed(client, client.options.leftKey) - pressed(client, client.options.rightKey);
        Vec3d velocity = client.player.getVelocity();
        if (forward == 0 && sideways == 0) {
            client.player.setVelocity(0.0D, velocity.y, 0.0D);
            return;
        }

        double yawRadians = Math.toRadians(client.player.getYaw());
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        double x = forward * -sin + sideways * cos;
        double z = forward * cos + sideways * sin;
        double length = Math.sqrt(x * x + z * z);
        if (length < 1.0E-4D) {
            return;
        }
        client.player.setVelocity(x / length * targetSpeed, velocity.y, z / length * targetSpeed);
    }

    private static boolean isAutoSprintMovementPressed(MinecraftClient client) {
        if (!autoSprintAllDirections) {
            return client.options.forwardKey.isPressed();
        }
        return client.options.forwardKey.isPressed()
                || client.options.backKey.isPressed()
                || client.options.leftKey.isPressed()
                || client.options.rightKey.isPressed();
    }

    private static void applyFreecamMovement(MinecraftClient client) {
        if (freecamAnchor == null || freecamPosition == null) {
            applyFreecamState(client);
            return;
        }

        int forward = pressed(client, client.options.forwardKey) - pressed(client, client.options.backKey);
        int sideways = pressed(client, client.options.leftKey) - pressed(client, client.options.rightKey);
        int vertical = pressed(client, client.options.jumpKey) - pressed(client, client.options.sneakKey);

        restoreFreecamBody(client);
        if (forward == 0 && sideways == 0 && vertical == 0) {
            previousFreecamPosition = freecamPosition;
            return;
        }

        previousFreecamPosition = freecamPosition;
        double yawRadians = Math.toRadians(freecamYaw);
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        double x = forward * -sin + sideways * cos;
        double z = forward * cos + sideways * sin;
        double length = Math.sqrt(x * x + z * z);
        if (length > 1.0E-4D) {
            x /= length;
            z /= length;
        }
        double speed = client.options.sprintKey.isPressed() ? FREECAM_SPRINT_SPEED : FREECAM_SPEED;
        freecamPosition = freecamPosition.add(x * speed, vertical * speed, z * speed);
    }

    private static void restoreFreecamBody(MinecraftClient client) {
        if (client == null || client.player == null || freecamAnchor == null) {
            return;
        }

        client.player.setVelocity(Vec3d.ZERO);
        client.player.refreshPositionAfterTeleport(freecamAnchor);
        client.player.setYaw(freecamBodyYaw);
        client.player.setPitch(freecamBodyPitch);
        client.player.setHeadYaw(freecamBodyYaw);
        client.player.setBodyYaw(freecamBodyYaw);
    }

    private static void applyFreecamState(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            freecamAnchor = null;
            previousFreecamPosition = null;
            freecamPosition = null;
            freecamYaw = 0.0F;
            freecamPitch = 0.0F;
            freecamBodyYaw = 0.0F;
            freecamBodyPitch = 0.0F;
            return;
        }

        if (!freecamEnabled) {
            if (freecamAnchor != null) {
                restoreFreecamBody(client);
            }
            freecamAnchor = null;
            previousFreecamPosition = null;
            freecamPosition = null;
            freecamYaw = 0.0F;
            freecamPitch = 0.0F;
            freecamBodyYaw = 0.0F;
            freecamBodyPitch = 0.0F;
            return;
        }

        if (freecamPosition == null) {
            freecamAnchor = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            freecamPosition = client.player.getCameraPosVec(1.0F);
            previousFreecamPosition = freecamPosition;
            freecamYaw = client.player.getYaw();
            freecamPitch = client.player.getPitch();
            freecamBodyYaw = client.player.getYaw();
            freecamBodyPitch = client.player.getPitch();
        }
    }

    private static void applyFullBright(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        var gamma = client.options.getGamma();
        if (fullBrightEnabled) {
            if (savedGamma == null) {
                savedGamma = gamma.getValue();
            }
            ((SimpleOptionAccessor) (Object) gamma).weikhack$setValue(16.0D);
            return;
        }

        if (savedGamma != null) {
            gamma.setValue(savedGamma);
            savedGamma = null;
        }
    }

    private static void applyNoWeather(MinecraftClient client) {
        if (!noWeatherEnabled || client == null || client.world == null) {
            return;
        }

        client.world.setRainGradient(0.0F);
        client.world.setThunderGradient(0.0F);
        if (client.getSoundManager() != null) {
            client.getSoundManager().stopSounds(null, SoundCategory.WEATHER);
        }
    }

    private static void applyNoFall(MinecraftClient client) {
        if (!noFallEnabled || client.player == null) {
            return;
        }

        resetFallDistance(client);
        if (client.getNetworkHandler() == null || client.player.isOnGround() || client.player.isCreative() || client.player.isSpectator()) {
            return;
        }

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
            if (player == client.player || player.isSpectator()) {
                continue;
            }

            visibleNow.add(player.getUuid());
            if (!player.isGlowing()) {
                espGlowingPlayers.add(player.getUuid());
            }
            player.setGlowing(true);
        }

        espGlowingPlayers.removeIf(uuid -> {
            if (visibleNow.contains(uuid)) {
                return false;
            }
            return true;
        });
    }

    private static void applyKillAura(MinecraftClient client) {
        if (!killAuraEnabled
                || (!killAuraMobsEnabled && !killAuraPlayersEnabled)
                || client.world == null
                || client.player == null
                || client.interactionManager == null
                || client.currentScreen != null
                || client.player.isUsingItem()
                || client.player.getAttackCooldownProgress(0.0F) < KILL_AURA_MIN_COOLDOWN) {
            return;
        }

        Entity target = findKillAuraTarget(client);
        if (target == null) {
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private static void applyChestStealer(MinecraftClient client) {
        if (!chestStealerEnabled
                || !(client.currentScreen instanceof HandledScreen<?>)
                || client.player == null
                || client.interactionManager == null) {
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null || handler == client.player.playerScreenHandler) {
            return;
        }

        for (Slot slot : handler.slots) {
            if (slot.inventory == client.player.getInventory()) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, client.player);
            return;
        }
    }

    private static void applyAutoTotem(MinecraftClient client) {
        if (!autoTotemEnabled
                || client.currentScreen != null
                || client.player == null
                || client.interactionManager == null
                || autoEquipCooldownTicks > 0
                || client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            if (autoEquipCooldownTicks > 0) {
                autoEquipCooldownTicks--;
            }
            return;
        }

        int inventorySlot = findInventoryItem(Items.TOTEM_OF_UNDYING);
        if (inventorySlot < 0) {
            return;
        }

        int screenSlot = screenSlotForInventoryIndex(inventorySlot);
        if (screenSlot < 0) {
            return;
        }

        int syncId = client.player.playerScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, client.player);
        autoEquipCooldownTicks = AUTO_EQUIP_COOLDOWN_TICKS;
    }

    private static void applyAutoArmor(MinecraftClient client) {
        if (!autoArmorEnabled
                || client.currentScreen != null
                || client.player == null
                || client.interactionManager == null
                || autoEquipCooldownTicks > 0) {
            if (autoEquipCooldownTicks > 0) {
                autoEquipCooldownTicks--;
            }
            return;
        }

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            int currentScore = armorScore(client.player.getEquippedStack(slot), slot);
            int bestInventorySlot = -1;
            int bestScore = currentScore;
            for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
                ItemStack stack = client.player.getInventory().getStack(inventorySlot);
                int score = armorScore(stack, slot);
                if (score > bestScore) {
                    bestScore = score;
                    bestInventorySlot = inventorySlot;
                }
            }

            if (bestInventorySlot >= 0) {
                int screenSlot = screenSlotForInventoryIndex(bestInventorySlot);
                if (screenSlot >= 0) {
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, screenSlot, 0, SlotActionType.QUICK_MOVE, client.player);
                    autoEquipCooldownTicks = AUTO_EQUIP_COOLDOWN_TICKS;
                    return;
                }
            }
        }
    }

    private static void applyAutoTool(MinecraftClient client) {
        if (!autoToolEnabled
                || client.currentScreen != null
                || client.player == null
                || client.world == null
                || client.interactionManager == null
                || freecamEnabled
                || client.crosshairTarget == null) {
            return;
        }

        HitResult target = client.crosshairTarget;
        if (target instanceof EntityHitResult entityTarget && entityTarget.getEntity() instanceof LivingEntity) {
            selectBestInventorySlot(WeikhackMod::weaponScore);
            return;
        }

        if (target instanceof BlockHitResult blockTarget && target.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.world.getBlockState(blockTarget.getBlockPos());
            if (!state.isAir()) {
                selectBestInventorySlot(stack -> blockToolScore(stack, state));
            }
        }
    }

    private static void selectBestInventorySlot(java.util.function.ToIntFunction<ItemStack> scorer) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        PlayerInventoryAccessor inventory = (PlayerInventoryAccessor) client.player.getInventory();
        int selectedSlot = inventory.weikhack$getSelectedSlot();
        int currentScore = scorer.applyAsInt(client.player.getInventory().getStack(selectedSlot));
        int bestSlot = selectedSlot;
        int bestScore = currentScore;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            int score = scorer.applyAsInt(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        if (bestSlot == selectedSlot || bestScore <= 0) {
            return;
        }

        if (bestSlot >= 0 && bestSlot < 9) {
            inventory.weikhack$setSelectedSlot(bestSlot);
            return;
        }

        int screenSlot = screenSlotForInventoryIndex(bestSlot);
        if (screenSlot >= 0) {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, screenSlot, selectedSlot, SlotActionType.SWAP, client.player);
        }
    }

    private static int blockToolScore(ItemStack stack, BlockState state) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        int toolScore = 0;
        if (state.isIn(BlockTags.PICKAXE_MINEABLE) && isPickaxe(item)) {
            toolScore = 5000;
        } else if (state.isIn(BlockTags.AXE_MINEABLE) && isAxe(item)) {
            toolScore = 5000;
        } else if (state.isIn(BlockTags.SHOVEL_MINEABLE) && isShovel(item)) {
            toolScore = 5000;
        } else if (state.isIn(BlockTags.HOE_MINEABLE) && isHoe(item)) {
            toolScore = 5000;
        }

        if (toolScore <= 0) {
            return 0;
        }

        return toolScore + materialScore(item) * 100 + durabilityScore(stack);
    }

    private static int weaponScore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        int base = swordDamage(item);
        if (base <= 0) {
            base = axeDamage(item);
        }
        if (base <= 0) {
            return 0;
        }
        return base * 100 + durabilityScore(stack);
    }

    private static int findInventoryItem(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return -1;
        }

        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static int screenSlotForInventoryIndex(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return 36 + inventorySlot;
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return inventorySlot;
        }
        if (inventorySlot == 40) {
            return 45;
        }
        return -1;
    }

    private static int armorScore(ItemStack stack, EquipmentSlot slot) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        int materialScore = armorMaterialScore(item);
        if (materialScore <= 0 || armorSlot(item) != slot) {
            return 0;
        }

        int durabilityScore = stack.isDamageable() ? Math.max(0, stack.getMaxDamage() - stack.getDamage()) / 25 : 0;
        return materialScore * 1000 + durabilityScore;
    }

    private static EquipmentSlot armorSlot(Item item) {
        if (item == Items.NETHERITE_HELMET || item == Items.DIAMOND_HELMET || item == Items.IRON_HELMET || item == Items.COPPER_HELMET || item == Items.GOLDEN_HELMET || item == Items.CHAINMAIL_HELMET || item == Items.LEATHER_HELMET || item == Items.TURTLE_HELMET) {
            return EquipmentSlot.HEAD;
        }
        if (item == Items.NETHERITE_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.IRON_CHESTPLATE || item == Items.COPPER_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE || item == Items.LEATHER_CHESTPLATE || item == Items.ELYTRA) {
            return EquipmentSlot.CHEST;
        }
        if (item == Items.NETHERITE_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.IRON_LEGGINGS || item == Items.COPPER_LEGGINGS || item == Items.GOLDEN_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS || item == Items.LEATHER_LEGGINGS) {
            return EquipmentSlot.LEGS;
        }
        if (item == Items.NETHERITE_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.IRON_BOOTS || item == Items.COPPER_BOOTS || item == Items.GOLDEN_BOOTS || item == Items.CHAINMAIL_BOOTS || item == Items.LEATHER_BOOTS) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    private static int armorMaterialScore(Item item) {
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) {
            return 6;
        }
        if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) {
            return 5;
        }
        if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) {
            return 4;
        }
        if (item == Items.COPPER_HELMET || item == Items.COPPER_CHESTPLATE || item == Items.COPPER_LEGGINGS || item == Items.COPPER_BOOTS) {
            return 3;
        }
        if (item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS) {
            return 3;
        }
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) {
            return 2;
        }
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS || item == Items.TURTLE_HELMET || item == Items.ELYTRA) {
            return 1;
        }
        return 0;
    }

    private static boolean isPickaxe(Item item) {
        return item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.IRON_PICKAXE || item == Items.STONE_PICKAXE || item == Items.GOLDEN_PICKAXE || item == Items.WOODEN_PICKAXE;
    }

    private static boolean isAxe(Item item) {
        return item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE || item == Items.IRON_AXE || item == Items.STONE_AXE || item == Items.GOLDEN_AXE || item == Items.WOODEN_AXE;
    }

    private static boolean isShovel(Item item) {
        return item == Items.NETHERITE_SHOVEL || item == Items.DIAMOND_SHOVEL || item == Items.IRON_SHOVEL || item == Items.STONE_SHOVEL || item == Items.GOLDEN_SHOVEL || item == Items.WOODEN_SHOVEL;
    }

    private static boolean isHoe(Item item) {
        return item == Items.NETHERITE_HOE || item == Items.DIAMOND_HOE || item == Items.IRON_HOE || item == Items.STONE_HOE || item == Items.GOLDEN_HOE || item == Items.WOODEN_HOE;
    }

    private static int materialScore(Item item) {
        if (item == Items.NETHERITE_PICKAXE || item == Items.NETHERITE_AXE || item == Items.NETHERITE_SHOVEL || item == Items.NETHERITE_HOE || item == Items.NETHERITE_SWORD) {
            return 6;
        }
        if (item == Items.DIAMOND_PICKAXE || item == Items.DIAMOND_AXE || item == Items.DIAMOND_SHOVEL || item == Items.DIAMOND_HOE || item == Items.DIAMOND_SWORD) {
            return 5;
        }
        if (item == Items.IRON_PICKAXE || item == Items.IRON_AXE || item == Items.IRON_SHOVEL || item == Items.IRON_HOE || item == Items.IRON_SWORD) {
            return 4;
        }
        if (item == Items.STONE_PICKAXE || item == Items.STONE_AXE || item == Items.STONE_SHOVEL || item == Items.STONE_HOE || item == Items.STONE_SWORD) {
            return 3;
        }
        if (item == Items.GOLDEN_PICKAXE || item == Items.GOLDEN_AXE || item == Items.GOLDEN_SHOVEL || item == Items.GOLDEN_HOE || item == Items.GOLDEN_SWORD) {
            return 2;
        }
        if (item == Items.WOODEN_PICKAXE || item == Items.WOODEN_AXE || item == Items.WOODEN_SHOVEL || item == Items.WOODEN_HOE || item == Items.WOODEN_SWORD) {
            return 1;
        }
        return 0;
    }

    private static int swordDamage(Item item) {
        if (item == Items.NETHERITE_SWORD) {
            return 8;
        }
        if (item == Items.DIAMOND_SWORD) {
            return 7;
        }
        if (item == Items.IRON_SWORD) {
            return 6;
        }
        if (item == Items.STONE_SWORD) {
            return 5;
        }
        if (item == Items.GOLDEN_SWORD || item == Items.WOODEN_SWORD) {
            return 4;
        }
        return 0;
    }

    private static int axeDamage(Item item) {
        if (item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE || item == Items.IRON_AXE) {
            return 9;
        }
        if (item == Items.STONE_AXE) {
            return 9;
        }
        if (item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE) {
            return 7;
        }
        return 0;
    }

    private static int durabilityScore(ItemStack stack) {
        return stack.isDamageable() ? Math.max(0, stack.getMaxDamage() - stack.getDamage()) / 25 : 0;
    }

    private static Entity findKillAuraTarget(MinecraftClient client) {
        Entity nearestTarget = null;
        double nearestDistance = KILL_AURA_RANGE * KILL_AURA_RANGE;

        if (killAuraMobsEnabled) {
            List<LivingEntity> targets = client.world.getEntitiesByClass(
                    LivingEntity.class,
                    client.player.getBoundingBox().expand(KILL_AURA_RANGE),
                    entity -> isValidKillAuraMob(client, entity)
            );
            for (LivingEntity target : targets) {
                double distance = target.squaredDistanceTo(client.player);
                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = target;
                }
            }
        }

        if (killAuraPlayersEnabled) {
            for (PlayerEntity player : client.world.getPlayers()) {
                if (!isValidKillAuraPlayer(client, player)) {
                    continue;
                }

                double distance = player.squaredDistanceTo(client.player);
                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = player;
                }
            }
        }

        return nearestTarget;
    }

    private static boolean isValidKillAuraMob(MinecraftClient client, LivingEntity entity) {
        if (entity == null
                || entity == client.player
                || entity instanceof PlayerEntity
                || !entity.isAlive()
                || entity.isRemoved()
                || entity.isSpectator()
                || entity.getHealth() <= 0.0F) {
            return false;
        }

        if (entity instanceof HostileEntity || entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) {
            return true;
        }

        String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        if (KILL_AURA_HOSTILE_ENTITY_IDS.contains(entityId)) {
            return true;
        }

        return entity instanceof MobEntity mob && mob.getTarget() == client.player;
    }

    private static boolean isValidKillAuraPlayer(MinecraftClient client, PlayerEntity player) {
        return player != null
                && player != client.player
                && player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && player.getHealth() > 0.0F;
    }

    private static String killAuraTargetLabel() {
        if (killAuraMobsEnabled && killAuraPlayersEnabled) {
            return "KillAura All";
        }
        if (killAuraPlayersEnabled) {
            return "KillAura Players";
        }
        if (killAuraMobsEnabled) {
            return "KillAura Mobs";
        }
        return "KillAura";
    }

    private static void clearEsp(MinecraftClient client) {
        if (client != null && client.world != null) {
            for (var player : client.world.getPlayers()) {
                if (espGlowingPlayers.contains(player.getUuid())) {
                    player.setGlowing(false);
                }
            }
        }
        espGlowingPlayers.clear();
    }

    private static void resetFallDistance(MinecraftClient client) {
        if (client.player != null) {
            ((EntityAccessor) client.player).weikhack$setFallDistance(0.0D);
        }
    }

    private static void resetDefaultBinds() {
        keyBinds.clear();
        pressedBindModules.clear();
    }

    private static void loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            return;
        }

        if (!properties.containsKey("config.version")) {
            loadBinds(properties);
            removeLegacyDefaultBinds();
            return;
        }

        flightEnabled = booleanProperty(properties, "module.flight", flightEnabled);
        noFallEnabled = booleanProperty(properties, "module.nofall", noFallEnabled);
        espEnabled = booleanProperty(properties, "module.esp", espEnabled);
        chestEspEnabled = booleanProperty(properties, "module.chestEsp", chestEspEnabled);
        fullBrightEnabled = booleanProperty(properties, "module.fullBright", fullBrightEnabled);
        noWeatherEnabled = booleanProperty(properties, "module.noWeather", noWeatherEnabled);
        noKnockbackEnabled = booleanProperty(properties, "module.noKnockback", noKnockbackEnabled);
        killAuraEnabled = booleanProperty(properties, "module.killAura", killAuraEnabled);
        chestStealerEnabled = booleanProperty(properties, "module.chestStealer", chestStealerEnabled);
        freecamEnabled = booleanProperty(properties, "module.freecam", freecamEnabled);
        noVeloEnabled = booleanProperty(properties, "module.noVelo", noVeloEnabled);
        autoSprintEnabled = booleanProperty(properties, "module.autoSprint", autoSprintEnabled);
        boatFlyEnabled = booleanProperty(properties, "module.boatFly", boatFlyEnabled);
        elytraBoostEnabled = booleanProperty(properties, "module.elytraBoost", elytraBoostEnabled);
        airJumpEnabled = booleanProperty(properties, "module.airJump", airJumpEnabled);
        jumpHeightEnabled = booleanProperty(properties, "module.jumpHeight", jumpHeightEnabled);
        xrayEnabled = booleanProperty(properties, "module.xray", xrayEnabled);
        healthBarsEnabled = booleanProperty(properties, "module.healthBars", healthBarsEnabled);
        autoArmorEnabled = booleanProperty(properties, "module.autoArmor", autoArmorEnabled);
        autoTotemEnabled = booleanProperty(properties, "module.autoTotem", autoTotemEnabled);
        autoToolEnabled = booleanProperty(properties, "module.autoTool", autoToolEnabled);
        noSlowdownEnabled = booleanProperty(properties, "module.noSlowdown", noSlowdownEnabled);
        fastPlaceEnabled = booleanProperty(properties, "module.fastPlace", fastPlaceEnabled);
        deathMarkerEnabled = booleanProperty(properties, "module.deathMarker", deathMarkerEnabled);
        safeWalkEnabled = booleanProperty(properties, "module.safeWalk", safeWalkEnabled);
        fakeLagEnabled = booleanProperty(properties, "module.fakeLag", fakeLagEnabled);
        killAuraMobsEnabled = booleanProperty(properties, "option.killAuraMobs", killAuraMobsEnabled);
        killAuraPlayersEnabled = booleanProperty(properties, "option.killAuraPlayers", killAuraPlayersEnabled);
        autoSprintAllDirections = booleanProperty(properties, "option.autoSprintAllDirections", autoSprintAllDirections);
        chestEspChests = booleanProperty(properties, "option.chestEspChests", chestEspChests);
        chestEspTrappedChests = booleanProperty(properties, "option.chestEspTrappedChests", chestEspTrappedChests);
        chestEspEnderChests = booleanProperty(properties, "option.chestEspEnderChests", chestEspEnderChests);
        chestEspBarrels = booleanProperty(properties, "option.chestEspBarrels", chestEspBarrels);
        chestEspShulkers = booleanProperty(properties, "option.chestEspShulkers", chestEspShulkers);
        for (XrayOre ore : XrayOre.values()) {
            xrayOreOptions.put(ore, booleanProperty(properties, "option.xray." + ore.configKey(), isXrayOreEnabled(ore)));
        }
        activeListEnabled = booleanProperty(properties, "option.activeList", activeListEnabled);
        speedMultiplier = floatProperty(properties, "speed.multiplier", speedMultiplier);
        boatFlySpeedMultiplier = boatFlySpeedProperty(properties, "boatFly.speed", boatFlySpeedMultiplier);
        jumpHeightBlocks = jumpProperty(properties, "jump.blocks", jumpHeightBlocks);
        loadBinds(properties);
        removeLegacyDefaultBinds();
    }

    private static void updateDeathMarker(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            wasPlayerDead = false;
            return;
        }

        boolean deadNow = client.player.isDead() || !client.player.isAlive() || client.player.getHealth() <= 0.0F;
        if (deadNow && !wasPlayerDead) {
            lastDeathPosition = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            lastDeathDimension = client.world.getRegistryKey().getValue().toString();
            if (deathMarkerEnabled) {
                sendStatus(client, "Death Marker gespeichert");
            }
        }
        wasPlayerDead = deadNow;
    }

    private static void handleDeathMarkerClearClick(MinecraftClient client) {
        boolean attackDown = client.options.attackKey.isPressed();
        boolean clicked = attackDown && !deathMarkerAttackWasDown;
        deathMarkerAttackWasDown = attackDown;

        if (!clicked
                || client.currentScreen != null
                || !deathMarkerEnabled
                || !hasDeathMarker()
                || !isLastDeathInCurrentDimension()) {
            return;
        }

        if (client.player.squaredDistanceTo(lastDeathPosition) > DEATH_MARKER_CLEAR_DISTANCE * DEATH_MARKER_CLEAR_DISTANCE) {
            return;
        }

        lastDeathPosition = null;
        lastDeathDimension = null;
        sendStatus(client, "Death Marker geloescht");
    }

    private static void tickFakeLag(MinecraftClient client) {
        if (!fakeLagEnabled || client.player == null || client.getNetworkHandler() == null) {
            flushFakeLagPackets();
            fakeLagTickCounter = 0;
            return;
        }

        fakeLagTickCounter++;
        if (fakeLagTickCounter >= FAKE_LAG_FLUSH_TICKS) {
            flushFakeLagPackets();
            fakeLagTickCounter = 0;
        }
    }

    private static void flushFakeLagPackets() {
        if (fakeLagQueue.isEmpty()) {
            return;
        }

        releasingFakeLagPackets = true;
        try {
            List<QueuedPacket> packets = new ArrayList<>(fakeLagQueue);
            fakeLagQueue.clear();
            for (QueuedPacket queued : packets) {
                queued.connection().send(queued.packet());
            }
        } catch (RuntimeException ignored) {
            fakeLagQueue.clear();
        } finally {
            releasingFakeLagPackets = false;
        }
    }

    private static void loadBinds(Properties properties) {
        if (!booleanProperty(properties, "binds.saved", false)) {
            return;
        }

        keyBinds.clear();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("bind.")) {
                continue;
            }

            String module = canonicalModuleName(key.substring("bind.".length()));
            if (module == null) {
                continue;
            }

            try {
                keyBinds.put(module, Integer.parseInt(properties.getProperty(key)));
            } catch (NumberFormatException ignored) {
            }
        }
        pressedBindModules.clear();
    }

    private static void removeLegacyDefaultBinds() {
        removeBindIfMatches("flight", GLFW.GLFW_KEY_F);
        removeBindIfMatches("nofall", GLFW.GLFW_KEY_N);
        removeBindIfMatches("killaura", GLFW.GLFW_KEY_R);
        removeBindIfMatches("killaura", GLFW.GLFW_KEY_V);
    }

    private static void removeBindIfMatches(String module, int keyCode) {
        Integer savedKey = keyBinds.get(module);
        if (savedKey != null && savedKey == keyCode) {
            keyBinds.remove(module);
            pressedBindModules.remove(module);
        }
    }

    private static boolean booleanProperty(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static void resetDefaultXrayOres() {
        setAllXrayOres(true);
    }

    private static void setAllXrayOres(boolean enabled) {
        for (XrayOre ore : XrayOre.values()) {
            xrayOreOptions.put(ore, enabled);
        }
    }

    private static float floatProperty(Properties properties, String key, float fallback) {
        try {
            return clampSpeed(Float.parseFloat(properties.getProperty(key, Float.toString(fallback))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float jumpProperty(Properties properties, String key, float fallback) {
        try {
            return clampJumpBlocks(Float.parseFloat(properties.getProperty(key, Float.toString(fallback))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float boatFlySpeedProperty(Properties properties, String key, float fallback) {
        try {
            return clampBoatFlySpeed(Float.parseFloat(properties.getProperty(key, Float.toString(fallback))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float clampSpeed(float multiplier) {
        return Math.max(MIN_SPEED_MULTIPLIER, Math.min(MAX_SPEED_MULTIPLIER, Math.round(multiplier * 10.0F) / 10.0F));
    }

    private static float clampBoatFlySpeed(float multiplier) {
        return Math.max(MIN_BOAT_FLY_SPEED, Math.min(MAX_BOAT_FLY_SPEED, Math.round(multiplier * 10.0F) / 10.0F));
    }

    private static float clampJumpBlocks(float blocks) {
        return Math.max(MIN_JUMP_BLOCKS, Math.min(MAX_JUMP_BLOCKS, Math.round(blocks * 10.0F) / 10.0F));
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");
    }

    private static void playTitleClickClip() {
        try (InputStream rawInput = WeikhackMod.class.getResourceAsStream(TITLE_CLICK_SOUND_RESOURCE)) {
            if (rawInput == null) {
                return;
            }
            try (AudioInputStream audioInput = AudioSystem.getAudioInputStream(new BufferedInputStream(rawInput))) {
                Clip clip = AudioSystem.getClip();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP || event.getType() == LineEvent.Type.CLOSE) {
                        clip.close();
                    }
                });
                clip.open(audioInput);
                clip.start();
            }
        } catch (Exception ignored) {
        }
    }

    private static int pressed(MinecraftClient client, net.minecraft.client.option.KeyBinding key) {
        return key.isPressed() ? 1 : 0;
    }

    public static void sendCommandMessage(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[Weikhack] " + message), false);
        }
    }

    private static void sendStatus(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    public enum XrayOre {
        DIAMOND("Diamond", "diamond"),
        IRON("Iron", "iron"),
        GOLD("Gold", "gold"),
        LAPIS("Lapis", "lapis"),
        EMERALD("Emerald", "emerald"),
        REDSTONE("Redstone", "redstone"),
        COAL("Coal", "coal"),
        COPPER("Copper", "copper"),
        ANCIENT_DEBRIS("Ancient Debris", "ancientDebris");

        private final String label;
        private final String configKey;

        XrayOre(String label, String configKey) {
            this.label = label;
            this.configKey = configKey;
        }

        public String label() {
            return label;
        }

        private String configKey() {
            return configKey;
        }
    }

    private record QueuedPacket(ClientConnection connection, Packet<?> packet) {
    }
}
