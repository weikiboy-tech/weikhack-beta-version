package com.example.weikhack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeikhackScreen extends Screen {
    private static final String TITLE_TEXT = "Weikhack";
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 7;
    private static final int PANEL_WIDTH = 110;
    private static final int HEADER_HEIGHT = 14;
    private static final int MODULE_HEIGHT = 18;
    private static final int OPTION_HEIGHT = 16;
    private static final int PANEL_GAP = 10;
    private static final int PANEL_FILL = 0xD0060A0E;
    private static final int PANEL_BORDER = 0xAA1B2B36;
    private static final int ACCENT = 0xFF00C6A2;
    private static final int ACCENT_DARK = 0xDD073B33;
    private static final int TEXT = 0xFFE6F7F4;
    private static final int MUTED = 0xFF8FA4AE;

    private final List<Panel> panels = new ArrayList<>();
    private Panel draggingPanel;
    private double dragOffsetX;
    private double dragOffsetY;
    private String draggingSlider;
    private boolean configPanelOpen;

    protected WeikhackScreen() {
        super(Text.literal("Weikhack"));
    }

    @Override
    protected void init() {
        if (!panels.isEmpty()) {
            return;
        }

        int startY = Math.max(28, this.height / 10);
        int[] x = defaultPanelColumns();
        int stackY = startY + HEADER_HEIGHT + MODULE_HEIGHT * 2 + 12;

        panels.add(new Panel("Player", x[0], startY)
                .module("Chest Stealer", "Auto loot", Module.CHEST_STEALER, false)
                .module("Freecam", "Camera", Module.FREECAM, false)
                .module("AutoArmor", "Equip", Module.AUTO_ARMOR, false)
                .module("AutoTotem", "Offhand", Module.AUTO_TOTEM, false)
                .module("FastPlace", "Blocks", Module.FAST_PLACE, false));
        panels.add(new Panel("Movement", x[1], startY)
                .module("Flight", "Survival", Module.FLIGHT, false)
                .module("Speed", "Multiplier", Module.SPEED, true)
                .module("NoFall", "Damage", Module.NO_FALL, false)
                .module("SafeWalk", "Edges", Module.SAFE_WALK, false)
                .module("NoVelo", "Air control", Module.NO_VELO, false)
                .module("AutoSprint", "Run", Module.AUTO_SPRINT, true)
                .module("JumpHeight", "Blocks", Module.JUMP_HEIGHT, true)
                .module("NoSlow", "Use move", Module.NO_SLOWDOWN, false));
        panels.add(new Panel("Render", x[2], startY)
                .module("Player ESP", "Outline", Module.PLAYER_ESP, false)
                .module("Chest ESP", "Storage", Module.CHEST_ESP, true)
                .module("XRay", "Ores", Module.XRAY, false)
                .module("HealthBars", "HP", Module.HEALTHBARS, false)
                .module("FullBright", "Gamma", Module.FULL_BRIGHT, false)
                .module("Death Marker", "Waypoint", Module.DEATH_MARKER, false));
        panels.add(new Panel("Combat", x[3], startY)
                .module("KillAura", "Auto combat", Module.KILL_AURA, true)
                .module("NoKnockback", "Velocity", Module.NO_KNOCKBACK, false));
        panels.add(new Panel("Misc", x[3], Math.min(this.height - HEADER_HEIGHT, stackY))
                .module("Active List", "HUD", Module.ACTIVE_LIST, false)
                .module("FakeLag", "Packets", Module.FAKE_LAG, false)
                .module("Config", "Save/reset", Module.CONFIG, true));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x33000000);
        boolean titleHovered = isHover(mouseX, mouseY, TITLE_X, TITLE_Y - 2, this.textRenderer.getWidth(TITLE_TEXT), 12);
        context.drawTextWithShadow(this.textRenderer, Text.literal(TITLE_TEXT), TITLE_X, TITLE_Y, titleHovered ? ACCENT : TEXT);
        context.drawTextWithShadow(this.textRenderer, Text.literal(WeikhackMod.DISPLAY_VERSION), TITLE_X + this.textRenderer.getWidth(TITLE_TEXT) + 8, TITLE_Y, ACCENT);

        for (Panel panel : panels) {
            drawPanel(context, mouseX, mouseY, panel);
        }

        drawConfigButton(context, mouseX, mouseY);
        if (configPanelOpen) {
            drawConfigPanel(context, mouseX, mouseY);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0 && isHover(mouseX, mouseY, TITLE_X, TITLE_Y - 2, this.textRenderer.getWidth(TITLE_TEXT), 12)) {
            WeikhackMod.playTitleClickSound();
            return true;
        }

        if (button == 0 && isHover(mouseX, mouseY, 8, this.height - 24, 82, 16)) {
            configPanelOpen = !configPanelOpen;
            return true;
        }

        if (configPanelOpen && handleConfigPanelClick(mouseX, mouseY, button)) {
            return true;
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (button == 0 && isHover(mouseX, mouseY, panel.x, panel.y, PANEL_WIDTH, HEADER_HEIGHT)) {
                draggingPanel = panel;
                dragOffsetX = mouseX - panel.x;
                dragOffsetY = mouseY - panel.y;
                panels.remove(panel);
                panels.add(panel);
                return true;
            }

            int rowY = panel.y + HEADER_HEIGHT;
            for (ModuleEntry entry : panel.modules) {
                if (isHover(mouseX, mouseY, panel.x, rowY, PANEL_WIDTH, MODULE_HEIGHT)) {
                    if (entry.hasOptions && (button == 1 || mouseX <= panel.x + 16 || mouseX >= panel.x + PANEL_WIDTH - 14)) {
                        entry.open = !entry.open;
                    } else if (button == 0) {
                        toggle(entry.module);
                    }
                    return true;
                }
                rowY += MODULE_HEIGHT;

                if (entry.open) {
                    int optionHeight = optionHeight(entry.module);
                    if (isHover(mouseX, mouseY, panel.x, rowY, PANEL_WIDTH, optionHeight)) {
                        clickOption(entry.module, mouseX, mouseY, panel.x, rowY, button);
                        return true;
                    }
                    rowY += optionHeight;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingPanel != null) {
            draggingPanel.x = (int) Math.round(clamp(click.x() - dragOffsetX, 0, this.width - PANEL_WIDTH));
            draggingPanel.y = (int) Math.round(clamp(click.y() - dragOffsetY, 0, this.height - HEADER_HEIGHT));
            return true;
        }
        if (draggingSlider != null) {
            updateSlider(draggingSlider, click.x());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        draggingSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawPanel(DrawContext context, int mouseX, int mouseY, Panel panel) {
        int height = HEADER_HEIGHT;
        for (ModuleEntry entry : panel.modules) {
            height += MODULE_HEIGHT;
            if (entry.open) {
                height += optionHeight(entry.module);
            }
        }

        fillBox(context, panel.x, panel.y, panel.x + PANEL_WIDTH, panel.y + height, PANEL_FILL, PANEL_BORDER);
        context.fill(panel.x + 1, panel.y + 1, panel.x + PANEL_WIDTH - 1, panel.y + HEADER_HEIGHT, ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(panel.title), panel.x + PANEL_WIDTH / 2, panel.y + 4, 0xFFFFFFFF);

        int rowY = panel.y + HEADER_HEIGHT;
        for (ModuleEntry entry : panel.modules) {
            drawModule(context, mouseX, mouseY, panel.x, rowY, entry);
            rowY += MODULE_HEIGHT;
            if (entry.open) {
                drawOptions(context, mouseX, mouseY, panel.x, rowY, entry.module);
                rowY += optionHeight(entry.module);
            }
        }
    }

    private void drawModule(DrawContext context, int mouseX, int mouseY, int x, int y, ModuleEntry entry) {
        boolean active = isActive(entry.module);
        boolean hovered = isHover(mouseX, mouseY, x, y, PANEL_WIDTH, MODULE_HEIGHT);
        int fill = active ? ACCENT_DARK : hovered ? 0xDD111B22 : 0xCC080D12;
        context.fill(x + 1, y, x + PANEL_WIDTH - 1, y + MODULE_HEIGHT, fill);
        if (entry.hasOptions) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(entry.open ? "-" : "+"), x + 5, y + 5, active ? 0xFFFFFFFF : ACCENT);
        }
        context.drawTextWithShadow(this.textRenderer, Text.literal(entry.name), x + 18, y + 5, active ? 0xFFFFFFFF : TEXT);
        context.drawTextWithShadow(this.textRenderer, Text.literal(active ? "on" : "off"), x + PANEL_WIDTH - 24, y + 5, active ? 0xFF6EE7B7 : 0xFF5F717B);
    }

    private void drawOptions(DrawContext context, int mouseX, int mouseY, int x, int y, Module module) {
        context.fill(x + 1, y, x + PANEL_WIDTH - 1, y + optionHeight(module), 0xDD05090D);
        switch (module) {
            case KILL_AURA -> {
                drawOptionToggle(context, x, y, "Mobs", WeikhackMod.isKillAuraMobsEnabled(), 0);
                drawOptionToggle(context, x, y, "Players", WeikhackMod.isKillAuraPlayersEnabled(), 1);
            }
            case CHEST_ESP -> {
                drawOptionToggle(context, x, y, "Chests", WeikhackMod.isChestEspChestsEnabled(), 0);
                drawOptionToggle(context, x, y, "Trapped", WeikhackMod.isChestEspTrappedChestsEnabled(), 1);
                drawOptionToggle(context, x, y, "Ender", WeikhackMod.isChestEspEnderChestsEnabled(), 2);
                drawOptionToggle(context, x, y, "Barrels", WeikhackMod.isChestEspBarrelsEnabled(), 3);
                drawOptionToggle(context, x, y, "Shulkers", WeikhackMod.isChestEspShulkersEnabled(), 4);
            }
            case AUTO_SPRINT -> drawOptionToggle(context, x, y, "WASD", WeikhackMod.isAutoSprintAllDirections(), 0);
            case SPEED -> drawSlider(context, x + 8, y + 10, x + PANEL_WIDTH - 8, WeikhackMod.getSpeedSliderValue(), WeikhackMod.hasSpeedBoost(), String.format(Locale.ROOT, "%.1fx", WeikhackMod.getSpeedMultiplier()));
            case JUMP_HEIGHT -> drawSlider(context, x + 8, y + 10, x + PANEL_WIDTH - 8, WeikhackMod.getJumpSliderValue(), WeikhackMod.isJumpHeightEnabled(), String.format(Locale.ROOT, "%.1fb", WeikhackMod.getJumpHeightBlocks()));
            case CONFIG -> {
                drawAction(context, x + 6, y + 5, 46, "SAVE", false);
                drawAction(context, x + 58, y + 5, 46, "RESET", true);
            }
            default -> context.drawTextWithShadow(this.textRenderer, Text.literal("No options"), x + 8, y + 5, MUTED);
        }
    }

    private void drawOptionToggle(DrawContext context, int x, int y, String label, boolean active, int index) {
        int top = y + index * OPTION_HEIGHT;
        context.drawTextWithShadow(this.textRenderer, Text.literal(active ? "-" : "+"), x + 8, top + 5, active ? ACCENT : MUTED);
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), x + 20, top + 5, active ? 0xFF6EE7B7 : MUTED);
        context.fill(x + PANEL_WIDTH - 12, top + 5, x + PANEL_WIDTH - 6, top + 11, active ? ACCENT : 0xFF202B36);
    }

    private void drawSlider(DrawContext context, int left, int top, int right, double value, boolean active, String label) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), left, top - 8, active ? 0xFF6EE7B7 : MUTED);
        int fillRight = left + (int) Math.round((right - left) * clamp(value, 0.0D, 1.0D));
        context.fill(left, top + 5, right, top + 8, 0xFF26323E);
        context.fill(left, top + 5, fillRight, top + 8, active ? ACCENT : 0xFF64748B);
        context.fill(fillRight - 2, top + 1, fillRight + 2, top + 12, 0xFFE6F7F4);
    }

    private void drawAction(DrawContext context, int x, int y, int width, String label, boolean destructive) {
        fillBox(context, x, y, x + width, y + 13, destructive ? 0xFF28161A : 0xFF111E24, destructive ? 0xFFF87171 : ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), x + width / 2, y + 3, destructive ? 0xFFFFB4B4 : 0xFFCFFAE8);
    }

    private void drawConfigButton(DrawContext context, int mouseX, int mouseY) {
        boolean hovered = isHover(mouseX, mouseY, 8, this.height - 24, 82, 16);
        fillBox(context, 8, this.height - 24, 90, this.height - 8, hovered || configPanelOpen ? 0xFF17312B : 0xCC111E24, ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Configs"), 49, this.height - 19, 0xFFCFFAE8);
    }

    private void drawConfigPanel(DrawContext context, int mouseX, int mouseY) {
        int left = 8;
        int bottom = this.height - 32;
        int top = Math.max(28, bottom - 94);
        int right = left + 238;
        fillBox(context, left, top, right, bottom, PANEL_FILL, PANEL_BORDER);
        context.fill(left + 1, top + 1, right - 1, top + HEADER_HEIGHT, ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Config Presets"), (left + right) / 2, top + 4, 0xFFFFFFFF);

        drawPresetAction(context, mouseX, mouseY, left + 8, top + 22, 68, "PVE", false);
        drawPresetAction(context, mouseX, mouseY, left + 85, top + 22, 68, "PVP", false);
        drawPresetAction(context, mouseX, mouseY, left + 162, top + 22, 68, "Rage", false);
        drawPresetAction(context, mouseX, mouseY, left + 8, top + 44, 106, "Mining", false);
        drawPresetAction(context, mouseX, mouseY, left + 124, top + 44, 106, "Travel", false);

        drawPresetAction(context, mouseX, mouseY, left + 8, bottom - 20, 58, "SAVE", false);
        drawPresetAction(context, mouseX, mouseY, left + 74, bottom - 20, 74, "CLEAR", true);
        drawPresetAction(context, mouseX, mouseY, left + 156, bottom - 20, 74, "RESET", true);
    }

    private void drawPresetAction(DrawContext context, int mouseX, int mouseY, int x, int y, int width, String label, boolean destructive) {
        boolean hovered = isHover(mouseX, mouseY, x, y, width, 14);
        fillBox(context, x, y, x + width, y + 14, hovered ? 0xFF17312B : destructive ? 0xFF28161A : 0xFF111E24, destructive ? 0xFFF87171 : ACCENT);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), x + width / 2, y + 4, destructive ? 0xFFFFB4B4 : 0xFFCFFAE8);
    }

    private boolean handleConfigPanelClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        int left = 8;
        int bottom = this.height - 32;
        int top = Math.max(28, bottom - 94);
        MinecraftClient client = MinecraftClient.getInstance();
        if (isHover(mouseX, mouseY, left + 8, top + 22, 68, 14)) {
            WeikhackMod.applyConfigPreset("pve", client);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 85, top + 22, 68, 14)) {
            WeikhackMod.applyConfigPreset("pvp", client);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 162, top + 22, 68, 14)) {
            WeikhackMod.applyConfigPreset("rage", client);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 8, top + 44, 106, 14)) {
            WeikhackMod.applyConfigPreset("mining", client);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 124, top + 44, 106, 14)) {
            WeikhackMod.applyConfigPreset("travel", client);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 8, bottom - 20, 58, 14)) {
            WeikhackMod.saveConfig(client, true);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 74, bottom - 20, 74, 14)) {
            WeikhackMod.clearBinds(client, true);
            return true;
        }
        if (isHover(mouseX, mouseY, left + 156, bottom - 20, 74, 14)) {
            WeikhackMod.reset(client);
            return true;
        }
        return isHover(mouseX, mouseY, left, top, 238, bottom - top);
    }

    private void clickOption(Module module, double mouseX, double mouseY, int x, int y, int button) {
        if (button != 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int index = (int) ((mouseY - y) / OPTION_HEIGHT);
        switch (module) {
            case KILL_AURA -> {
                if (index == 0) {
                    WeikhackMod.setKillAuraMobsEnabled(!WeikhackMod.isKillAuraMobsEnabled(), client, true);
                } else if (index == 1) {
                    WeikhackMod.setKillAuraPlayersEnabled(!WeikhackMod.isKillAuraPlayersEnabled(), client, true);
                }
            }
            case CHEST_ESP -> toggleChestType(index);
            case AUTO_SPRINT -> WeikhackMod.setAutoSprintAllDirections(!WeikhackMod.isAutoSprintAllDirections(), client, true);
            case SPEED -> {
                draggingSlider = "speed";
                updateSlider(draggingSlider, mouseX);
            }
            case JUMP_HEIGHT -> {
                draggingSlider = "jump";
                updateSlider(draggingSlider, mouseX);
            }
            case CONFIG -> {
                if (mouseX < x + PANEL_WIDTH / 2.0D) {
                    WeikhackMod.saveConfig(client, true);
                } else {
                    WeikhackMod.reset(client);
                }
            }
            default -> {
            }
        }
    }

    private static void toggleChestType(int index) {
        switch (index) {
            case 0 -> WeikhackMod.setChestEspChestsEnabled(!WeikhackMod.isChestEspChestsEnabled());
            case 1 -> WeikhackMod.setChestEspTrappedChestsEnabled(!WeikhackMod.isChestEspTrappedChestsEnabled());
            case 2 -> WeikhackMod.setChestEspEnderChestsEnabled(!WeikhackMod.isChestEspEnderChestsEnabled());
            case 3 -> WeikhackMod.setChestEspBarrelsEnabled(!WeikhackMod.isChestEspBarrelsEnabled());
            case 4 -> WeikhackMod.setChestEspShulkersEnabled(!WeikhackMod.isChestEspShulkersEnabled());
            default -> {
            }
        }
    }

    private void updateSlider(String slider, double mouseX) {
        int sliderLeft = 0;
        int sliderRight = 0;
        for (Panel panel : panels) {
            int rowY = panel.y + HEADER_HEIGHT;
            for (ModuleEntry entry : panel.modules) {
                rowY += MODULE_HEIGHT;
                if (entry.open && ((slider.equals("speed") && entry.module == Module.SPEED) || (slider.equals("jump") && entry.module == Module.JUMP_HEIGHT))) {
                    sliderLeft = panel.x + 8;
                    sliderRight = panel.x + PANEL_WIDTH - 8;
                }
                if (entry.open) {
                    rowY += optionHeight(entry.module);
                }
            }
        }
        if (sliderRight <= sliderLeft) {
            return;
        }
        double value = clamp((mouseX - sliderLeft) / (double) (sliderRight - sliderLeft), 0.0D, 1.0D);
        if (slider.equals("speed")) {
            WeikhackMod.setSpeedSliderValue(value);
        } else {
            WeikhackMod.setJumpSliderValue(value);
        }
    }

    private static int optionHeight(Module module) {
        return switch (module) {
            case KILL_AURA -> OPTION_HEIGHT * 2;
            case CHEST_ESP -> OPTION_HEIGHT * 5;
            case AUTO_SPRINT -> OPTION_HEIGHT;
            case SPEED, JUMP_HEIGHT, CONFIG -> 28;
            default -> OPTION_HEIGHT;
        };
    }

    private static boolean isActive(Module module) {
        return switch (module) {
            case FLIGHT -> WeikhackMod.isFlightEnabled();
            case SPEED -> WeikhackMod.hasSpeedBoost();
            case NO_FALL -> WeikhackMod.isNoFallEnabled();
            case SAFE_WALK -> WeikhackMod.isSafeWalkEnabled();
            case NO_VELO -> WeikhackMod.isNoVeloEnabled();
            case AUTO_SPRINT -> WeikhackMod.isAutoSprintEnabled();
            case JUMP_HEIGHT -> WeikhackMod.isJumpHeightEnabled();
            case KILL_AURA -> WeikhackMod.isKillAuraEnabled();
            case NO_KNOCKBACK -> WeikhackMod.isNoKnockbackEnabled();
            case PLAYER_ESP -> WeikhackMod.isEspEnabled();
            case CHEST_ESP -> WeikhackMod.isChestEspEnabled();
            case FULL_BRIGHT -> WeikhackMod.isFullBrightEnabled();
            case XRAY -> WeikhackMod.isXrayEnabled();
            case HEALTHBARS -> WeikhackMod.isHealthBarsEnabled();
            case DEATH_MARKER -> WeikhackMod.isDeathMarkerEnabled();
            case CHEST_STEALER -> WeikhackMod.isChestStealerEnabled();
            case FREECAM -> WeikhackMod.isFreecamEnabled();
            case AUTO_ARMOR -> WeikhackMod.isAutoArmorEnabled();
            case AUTO_TOTEM -> WeikhackMod.isAutoTotemEnabled();
            case NO_SLOWDOWN -> WeikhackMod.isNoSlowdownEnabled();
            case FAST_PLACE -> WeikhackMod.isFastPlaceEnabled();
            case ACTIVE_LIST -> WeikhackMod.isActiveListEnabled();
            case FAKE_LAG -> WeikhackMod.isFakeLagEnabled();
            case CONFIG -> false;
        };
    }

    private static void toggle(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();
        switch (module) {
            case FLIGHT -> WeikhackMod.toggleFlight(client);
            case SPEED -> WeikhackMod.setSpeedMultiplier(WeikhackMod.hasSpeedBoost() ? 1.0F : Math.max(2.0F, WeikhackMod.getSpeedMultiplier()), client, true);
            case NO_FALL -> WeikhackMod.setNoFallEnabled(!WeikhackMod.isNoFallEnabled(), client, true);
            case SAFE_WALK -> WeikhackMod.setSafeWalkEnabled(!WeikhackMod.isSafeWalkEnabled(), client, true);
            case NO_VELO -> WeikhackMod.setNoVeloEnabled(!WeikhackMod.isNoVeloEnabled(), client, true);
            case AUTO_SPRINT -> WeikhackMod.setAutoSprintEnabled(!WeikhackMod.isAutoSprintEnabled(), client, true);
            case JUMP_HEIGHT -> WeikhackMod.setJumpHeightEnabled(!WeikhackMod.isJumpHeightEnabled(), client, true);
            case KILL_AURA -> WeikhackMod.setKillAuraEnabled(!WeikhackMod.isKillAuraEnabled(), client, true);
            case NO_KNOCKBACK -> WeikhackMod.setNoKnockbackEnabled(!WeikhackMod.isNoKnockbackEnabled(), client, true);
            case PLAYER_ESP -> WeikhackMod.setEspEnabled(!WeikhackMod.isEspEnabled(), client, true);
            case CHEST_ESP -> WeikhackMod.setChestEspEnabled(!WeikhackMod.isChestEspEnabled(), client, true);
            case FULL_BRIGHT -> WeikhackMod.setFullBrightEnabled(!WeikhackMod.isFullBrightEnabled(), client, true);
            case XRAY -> WeikhackMod.setXrayEnabled(!WeikhackMod.isXrayEnabled(), client, true);
            case HEALTHBARS -> WeikhackMod.setHealthBarsEnabled(!WeikhackMod.isHealthBarsEnabled(), client, true);
            case DEATH_MARKER -> WeikhackMod.setDeathMarkerEnabled(!WeikhackMod.isDeathMarkerEnabled(), client, true);
            case CHEST_STEALER -> WeikhackMod.setChestStealerEnabled(!WeikhackMod.isChestStealerEnabled(), client, true);
            case FREECAM -> WeikhackMod.setFreecamEnabled(!WeikhackMod.isFreecamEnabled(), client, true);
            case AUTO_ARMOR -> WeikhackMod.setAutoArmorEnabled(!WeikhackMod.isAutoArmorEnabled(), client, true);
            case AUTO_TOTEM -> WeikhackMod.setAutoTotemEnabled(!WeikhackMod.isAutoTotemEnabled(), client, true);
            case NO_SLOWDOWN -> WeikhackMod.setNoSlowdownEnabled(!WeikhackMod.isNoSlowdownEnabled(), client, true);
            case FAST_PLACE -> WeikhackMod.setFastPlaceEnabled(!WeikhackMod.isFastPlaceEnabled(), client, true);
            case ACTIVE_LIST -> WeikhackMod.setActiveListEnabled(!WeikhackMod.isActiveListEnabled(), client, true);
            case FAKE_LAG -> WeikhackMod.setFakeLagEnabled(!WeikhackMod.isFakeLagEnabled(), client, true);
            case CONFIG -> {
            }
        }
    }

    private static boolean isHover(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height;
    }

    private static void fillBox(DrawContext context, int left, int top, int right, int bottom, int fill, int border) {
        context.fill(left, top, right, bottom, border);
        context.fill(left + 1, top + 1, right - 1, bottom - 1, fill);
    }

    private int[] defaultPanelColumns() {
        int columns = this.width >= PANEL_WIDTH * 4 + 8 * 3 + 16 ? 4 : Math.max(1, Math.min(3, (this.width - 16 + PANEL_GAP) / (PANEL_WIDTH + PANEL_GAP)));
        int gap = columns == 4 ? Math.max(8, (this.width - 16 - PANEL_WIDTH * 4) / 3) : PANEL_GAP;
        int rowWidth = columns * PANEL_WIDTH + (columns - 1) * gap;
        int startX = Math.max(8, (this.width - rowWidth) / 2);
        int[] x = new int[4];
        for (int i = 0; i < x.length; i++) {
            int column = Math.min(i, columns - 1);
            int row = columns == 1 ? i : i / columns;
            x[i] = Math.min(this.width - PANEL_WIDTH, startX + column * (PANEL_WIDTH + gap) + row * 8);
        }
        return x;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Panel {
        private final String title;
        private final List<ModuleEntry> modules = new ArrayList<>();
        private int x;
        private int y;

        private Panel(String title, int x, int y) {
            this.title = title;
            this.x = x;
            this.y = y;
        }

        private Panel module(String name, String tag, Module module, boolean hasOptions) {
            modules.add(new ModuleEntry(name, tag, module, hasOptions));
            return this;
        }
    }

    private static class ModuleEntry {
        private final String name;
        private final String tag;
        private final Module module;
        private final boolean hasOptions;
        private boolean open;

        private ModuleEntry(String name, String tag, Module module, boolean hasOptions) {
            this.name = name;
            this.tag = tag;
            this.module = module;
            this.hasOptions = hasOptions;
        }
    }

    private enum Module {
        FLIGHT,
        SPEED,
        NO_FALL,
        SAFE_WALK,
        NO_VELO,
        AUTO_SPRINT,
        JUMP_HEIGHT,
        KILL_AURA,
        NO_KNOCKBACK,
        PLAYER_ESP,
        CHEST_ESP,
        FULL_BRIGHT,
        XRAY,
        HEALTHBARS,
        DEATH_MARKER,
        CHEST_STEALER,
        FREECAM,
        AUTO_ARMOR,
        AUTO_TOTEM,
        NO_SLOWDOWN,
        FAST_PLACE,
        ACTIVE_LIST,
        FAKE_LAG,
        CONFIG
    }
}
