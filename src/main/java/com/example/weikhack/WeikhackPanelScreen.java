package com.example.weikhack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Locale;

public class WeikhackPanelScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 560;
    private static final int MAX_PANEL_HEIGHT = 344;
    private static final int MIN_PANEL_WIDTH = 360;
    private static final int WINDOW_MARGIN = 24;
    private static final int HEADER_HEIGHT = 32;
    private static final int SIDEBAR_WIDTH = 108;
    private static final int OUTER_PAD = 10;
    private static final int CONTENT_PAD = 8;
    private static final int CATEGORY_HEIGHT = 20;
    private static final int CATEGORY_GAP = 3;
    private static final int ROW_HEIGHT = 40;
    private static final int SPEED_ROW_HEIGHT = 58;
    private static final int ROW_GAP = 7;
    private static final int CONTENT_HEADER_HEIGHT = 18;
    private static final int COMBAT_OPTION_HEIGHT = 22;
    private static final int COMBAT_OPTION_GAP = 3;
    private static final int COMBAT_OPTION_TOP_GAP = 5;
    private static final int KILL_AURA_TYPE_COUNT = 2;
    private static final int TYPE_BUTTON_WIDTH = 34;
    private static final int CHEST_OPTION_HEIGHT = 22;
    private static final int CHEST_OPTION_GAP = 3;
    private static final int CHEST_OPTION_TOP_GAP = 5;
    private static final int CHEST_TYPE_COUNT = 5;
    private static final int DUAL_BUTTON_WIDTH = 54;
    private static final int DUAL_BUTTON_GAP = 8;
    private static final int NORMAL_CHEST_COLOR = 0xFF34F85A;
    private static final int TRAPPED_CHEST_COLOR = 0xFFFF5555;
    private static final int ENDER_CHEST_COLOR = 0xFFB66CFF;
    private static final int BARREL_COLOR = 0xFFFFC857;
    private static final int SHULKER_COLOR = 0xFF55D7FF;

    private Category selectedCategory = Category.MOVEMENT;
    private double moduleScroll;
    private boolean draggingSpeedSlider;
    private boolean draggingJumpSlider;
    private boolean killAuraTypesOpen;
    private boolean chestTypesOpen;

    protected WeikhackPanelScreen() {
        super(Text.literal("Weikhack Alte UI"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = panelLeft(panelWidth);
        int top = panelTop(panelHeight);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int sidebarLeft = left + OUTER_PAD;
        int sidebarTop = top + HEADER_HEIGHT + OUTER_PAD;
        int sidebarRight = left + SIDEBAR_WIDTH - OUTER_PAD;
        int sidebarBottom = bottom - OUTER_PAD;
        int contentLeft = left + SIDEBAR_WIDTH + CONTENT_PAD;
        int contentRight = right - CONTENT_PAD;
        int contentTop = top + HEADER_HEIGHT + CONTENT_PAD;
        int moduleTop = contentTop + CONTENT_HEADER_HEIGHT;
        int moduleBottom = bottom - CONTENT_PAD;

        moduleScroll = clamp(moduleScroll, 0.0D, maxModuleScroll(moduleTop, moduleBottom));

        context.fill(0, 0, this.width, this.height, 0xA004070B);
        fillBox(context, left, top, right, bottom, 0xF0090E15, 0xFF2B3642);
        context.fill(left + 1, top + 1, right - 1, top + HEADER_HEIGHT, 0xF5131A23);
        context.fill(left + 1, top + HEADER_HEIGHT, left + SIDEBAR_WIDTH, bottom - 1, 0xF00C121A);
        context.fill(left + SIDEBAR_WIDTH, top + HEADER_HEIGHT, left + SIDEBAR_WIDTH + 1, bottom - 1, 0xFF26323E);
        context.fill(left + 1, top + HEADER_HEIGHT, right - 1, top + HEADER_HEIGHT + 1, 0xFF26323E);
        context.fill(left + 1, top + HEADER_HEIGHT - 1, right - 1, top + HEADER_HEIGHT, 0xFF6EE7B7);

        context.drawTextWithShadow(this.textRenderer, this.title, left + 16, top + 12, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(WeikhackMod.DISPLAY_VERSION), left + 16 + this.textRenderer.getWidth(this.title) + 8, top + 12, 0xFF6EE7B7);

        context.enableScissor(sidebarLeft, sidebarTop, sidebarRight, sidebarBottom);
        drawCategories(context, mouseX, mouseY, sidebarLeft, sidebarTop, sidebarRight - sidebarLeft);
        context.disableScissor();

        drawContentHeader(context, contentLeft, contentTop, contentRight);
        context.enableScissor(contentLeft, moduleTop, contentRight, moduleBottom);
        drawModules(context, mouseX, mouseY, contentLeft, moduleTop - (int) moduleScroll, contentRight);
        context.disableScissor();
        drawScrollbar(context, contentRight + 5, moduleTop, moduleBottom, moduleContentHeight(), moduleBottom - moduleTop, moduleScroll);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = panelLeft(panelWidth);
        int top = panelTop(panelHeight);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int sidebarLeft = left + OUTER_PAD;
        int sidebarTop = top + HEADER_HEIGHT + OUTER_PAD;
        int sidebarRight = left + SIDEBAR_WIDTH - OUTER_PAD;
        int sidebarBottom = bottom - OUTER_PAD;
        int contentLeft = left + SIDEBAR_WIDTH + CONTENT_PAD;
        int contentRight = right - CONTENT_PAD;
        int contentTop = top + HEADER_HEIGHT + CONTENT_PAD;
        int moduleTop = contentTop + CONTENT_HEADER_HEIGHT;
        int moduleBottom = bottom - CONTENT_PAD;

        if (isHover(mouseX, mouseY, sidebarLeft, sidebarTop, sidebarRight - sidebarLeft, sidebarBottom - sidebarTop)) {
            Category clickedCategory = categoryAt(mouseX, mouseY, sidebarLeft, sidebarTop, sidebarRight - sidebarLeft);
            if (clickedCategory != null) {
                selectedCategory = clickedCategory;
                moduleScroll = 0.0D;
                return true;
            }
        }

        if (!isHover(mouseX, mouseY, contentLeft, moduleTop, contentRight - contentLeft, moduleBottom - moduleTop)) {
            return super.mouseClicked(click, doubled);
        }

        double scrolledMouseY = mouseY + moduleScroll;
        if (selectedCategory == Category.MOVEMENT) {
            return clickMovement(mouseX, scrolledMouseY, contentLeft, moduleTop, contentRight);
        }

        if (selectedCategory == Category.RENDER) {
            return clickRender(mouseX, scrolledMouseY, contentLeft, moduleTop, contentRight);
        }

        if (selectedCategory == Category.COMBAT) {
            return clickCombat(mouseX, scrolledMouseY, contentLeft, moduleTop, contentRight);
        }

        if (selectedCategory == Category.PLAYER) {
            return clickPlayer(mouseX, scrolledMouseY, contentLeft, moduleTop, contentRight);
        }

        if (selectedCategory == Category.MISC) {
            return clickMisc(mouseX, scrolledMouseY, contentLeft, moduleTop, contentRight);
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingSpeedSlider) {
            updateSpeedFromMouse(click.x());
            return true;
        }
        if (draggingJumpSlider) {
            updateJumpFromMouse(click.x());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSpeedSlider) {
            draggingSpeedSlider = false;
            return true;
        }
        if (draggingJumpSlider) {
            draggingJumpSlider = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = panelLeft(panelWidth);
        int top = panelTop(panelHeight);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int moduleTop = top + HEADER_HEIGHT + CONTENT_PAD + CONTENT_HEADER_HEIGHT;
        int moduleBottom = bottom - CONTENT_PAD;

        if (isHover(mouseX, mouseY, left + SIDEBAR_WIDTH + CONTENT_PAD, moduleTop, right - left - SIDEBAR_WIDTH - CONTENT_PAD * 2, moduleBottom - moduleTop)) {
            moduleScroll = clamp(moduleScroll - verticalAmount * 14.0D, 0.0D, maxModuleScroll(moduleTop, moduleBottom));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean clickMovement(double mouseX, double mouseY, int left, int top, int right) {
        int rowTop = top;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.toggleFlight(MinecraftClient.getInstance());
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, SPEED_ROW_HEIGHT)) {
            if (isHover(mouseX, mouseY, left + 14, rowTop + 39, right - left - 28, 16)) {
                draggingSpeedSlider = true;
                updateSpeedFromMouse(mouseX);
            } else {
                toggleSpeed();
            }
            return true;
        }

        rowTop += SPEED_ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setNoFallEnabled(!WeikhackMod.isNoFallEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setSafeWalkEnabled(!WeikhackMod.isSafeWalkEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setNoVeloEnabled(!WeikhackMod.isNoVeloEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            if (isHover(mouseX, mouseY, autoSprintModeButtonLeft(right), rowTop + 10, DUAL_BUTTON_WIDTH, 20)) {
                WeikhackMod.setAutoSprintAllDirections(!WeikhackMod.isAutoSprintAllDirections(), MinecraftClient.getInstance(), true);
            } else {
                WeikhackMod.setAutoSprintEnabled(!WeikhackMod.isAutoSprintEnabled(), MinecraftClient.getInstance(), true);
            }
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, SPEED_ROW_HEIGHT)) {
            if (isHover(mouseX, mouseY, left + 14, rowTop + 39, right - left - 28, 16)) {
                draggingJumpSlider = true;
                updateJumpFromMouse(mouseX);
            } else {
                WeikhackMod.setJumpHeightEnabled(!WeikhackMod.isJumpHeightEnabled(), MinecraftClient.getInstance(), true);
            }
            return true;
        }

        rowTop += SPEED_ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setNoSlowdownEnabled(!WeikhackMod.isNoSlowdownEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        return false;
    }

    private boolean clickPlayer(double mouseX, double mouseY, int left, int top, int right) {
        int rowTop = top;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setChestStealerEnabled(!WeikhackMod.isChestStealerEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setFreecamEnabled(!WeikhackMod.isFreecamEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setAutoArmorEnabled(!WeikhackMod.isAutoArmorEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setAutoTotemEnabled(!WeikhackMod.isAutoTotemEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setFastPlaceEnabled(!WeikhackMod.isFastPlaceEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        return false;
    }

    private boolean clickCombat(double mouseX, double mouseY, int left, int top, int right) {
        int rowTop = top;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            if (isHover(mouseX, mouseY, typeButtonLeft(right), rowTop + 10, TYPE_BUTTON_WIDTH, 20)) {
                killAuraTypesOpen = !killAuraTypesOpen;
                return true;
            }
            WeikhackMod.setKillAuraEnabled(!WeikhackMod.isKillAuraEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT;
        if (killAuraTypesOpen) {
            rowTop += COMBAT_OPTION_TOP_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, COMBAT_OPTION_HEIGHT)) {
                WeikhackMod.setKillAuraMobsEnabled(!WeikhackMod.isKillAuraMobsEnabled(), MinecraftClient.getInstance(), true);
                return true;
            }

            rowTop += COMBAT_OPTION_HEIGHT + COMBAT_OPTION_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, COMBAT_OPTION_HEIGHT)) {
                WeikhackMod.setKillAuraPlayersEnabled(!WeikhackMod.isKillAuraPlayersEnabled(), MinecraftClient.getInstance(), true);
                return true;
            }

            rowTop += COMBAT_OPTION_HEIGHT + ROW_GAP;
        } else {
            rowTop += ROW_GAP;
        }

        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setNoKnockbackEnabled(!WeikhackMod.isNoKnockbackEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        return false;
    }

    private boolean clickMisc(double mouseX, double mouseY, int left, int top, int right) {
        int rowTop = top;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setActiveListEnabled(!WeikhackMod.isActiveListEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setFakeLagEnabled(!WeikhackMod.isFakeLagEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            if (isHover(mouseX, mouseY, safeButtonLeft(right), rowTop + 10, DUAL_BUTTON_WIDTH, 20)) {
                WeikhackMod.saveConfig(MinecraftClient.getInstance(), true);
            } else if (isHover(mouseX, mouseY, resetButtonLeft(right), rowTop + 10, DUAL_BUTTON_WIDTH, 20)) {
                WeikhackMod.reset(MinecraftClient.getInstance());
            }
            return true;
        }

        return false;
    }

    private boolean clickRender(double mouseX, double mouseY, int left, int top, int right) {
        int rowTop = top;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setEspEnabled(!WeikhackMod.isEspEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            if (isHover(mouseX, mouseY, typeButtonLeft(right), rowTop + 10, TYPE_BUTTON_WIDTH, 20)) {
                chestTypesOpen = !chestTypesOpen;
                return true;
            }
            WeikhackMod.setChestEspEnabled(!WeikhackMod.isChestEspEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT;
        if (chestTypesOpen) {
            rowTop += CHEST_OPTION_TOP_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, CHEST_OPTION_HEIGHT)) {
                WeikhackMod.setChestEspChestsEnabled(!WeikhackMod.isChestEspChestsEnabled());
                return true;
            }

            rowTop += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, CHEST_OPTION_HEIGHT)) {
                WeikhackMod.setChestEspTrappedChestsEnabled(!WeikhackMod.isChestEspTrappedChestsEnabled());
                return true;
            }

            rowTop += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, CHEST_OPTION_HEIGHT)) {
                WeikhackMod.setChestEspEnderChestsEnabled(!WeikhackMod.isChestEspEnderChestsEnabled());
                return true;
            }

            rowTop += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, CHEST_OPTION_HEIGHT)) {
                WeikhackMod.setChestEspBarrelsEnabled(!WeikhackMod.isChestEspBarrelsEnabled());
                return true;
            }

            rowTop += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            if (isHover(mouseX, mouseY, left + 8, rowTop, right - left - 8, CHEST_OPTION_HEIGHT)) {
                WeikhackMod.setChestEspShulkersEnabled(!WeikhackMod.isChestEspShulkersEnabled());
                return true;
            }

            rowTop += CHEST_OPTION_HEIGHT + ROW_GAP;
        } else {
            rowTop += ROW_GAP;
        }

        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setXrayEnabled(!WeikhackMod.isXrayEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setHealthBarsEnabled(!WeikhackMod.isHealthBarsEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setFullBrightEnabled(!WeikhackMod.isFullBrightEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        rowTop += ROW_HEIGHT + ROW_GAP;
        if (isHover(mouseX, mouseY, left, rowTop, right - left, ROW_HEIGHT)) {
            WeikhackMod.setDeathMarkerEnabled(!WeikhackMod.isDeathMarkerEnabled(), MinecraftClient.getInstance(), true);
            return true;
        }

        return false;
    }

    private void drawCategories(DrawContext context, int mouseX, int mouseY, int left, int top, int width) {
        for (Category category : Category.values()) {
            boolean active = category == selectedCategory;
            boolean hovered = isHover(mouseX, mouseY, left, top, width, CATEGORY_HEIGHT);
            int fill = active ? 0xFF18342E : hovered ? 0xFF131E29 : 0x00000000;
            if (fill != 0) {
                context.fill(left, top, left + width, top + CATEGORY_HEIGHT, fill);
            }
            if (active) {
                context.fill(left, top + 5, left + 3, top + CATEGORY_HEIGHT - 5, 0xFF6EE7B7);
            }
            context.drawTextWithShadow(this.textRenderer, Text.literal(category.label), left + 9, top + 6, active ? 0xFFF5F7FA : 0xFF9EAAB7);
            top += CATEGORY_HEIGHT + CATEGORY_GAP;
        }
    }

    private void drawContentHeader(DrawContext context, int left, int top, int right) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(selectedCategory.label), left, top, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(selectedCategory.countLabel()), right - this.textRenderer.getWidth(selectedCategory.countLabel()), top, 0xFF7DD3C7);
        context.fill(left, top + 13, right, top + 14, 0xFF22303B);
    }

    private void drawModules(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        if (selectedCategory == Category.MOVEMENT) {
            drawMovement(context, mouseX, mouseY, left, top, right);
            return;
        }

        if (selectedCategory == Category.RENDER) {
            drawRender(context, mouseX, mouseY, left, top, right);
            return;
        }

        if (selectedCategory == Category.COMBAT) {
            drawCombat(context, mouseX, mouseY, left, top, right);
            return;
        }

        if (selectedCategory == Category.PLAYER) {
            drawPlayer(context, mouseX, mouseY, left, top, right);
            return;
        }

        if (selectedCategory == Category.MISC) {
            drawMisc(context, mouseX, mouseY, left, top, right);
            return;
        }

        drawEmptyState(context, left, top, right);
    }

    private void drawMovement(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "Flight", "Survival", WeikhackMod.isFlightEnabled(), "Toggle");
        top += ROW_HEIGHT + ROW_GAP;
        drawSpeedRow(context, mouseX, mouseY, left, top, right);
        top += SPEED_ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "NoFall", "Damage", WeikhackMod.isNoFallEnabled(), "Check");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "SafeWalk", "Edge hold", WeikhackMod.isSafeWalkEnabled(), "Safe");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "NoVelo", "Constant speed", WeikhackMod.isNoVeloEnabled(), "Velo");
        top += ROW_HEIGHT + ROW_GAP;
        drawAutoSprintRow(context, mouseX, mouseY, left, top, right);
        top += ROW_HEIGHT + ROW_GAP;
        drawJumpRow(context, mouseX, mouseY, left, top, right);
        top += SPEED_ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "NoSlow", "Use move", WeikhackMod.isNoSlowdownEnabled(), "Toggle");
    }

    private void drawCombat(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        drawKillAuraRow(context, mouseX, mouseY, left, top, right);
        top += ROW_HEIGHT;
        if (killAuraTypesOpen) {
            top += COMBAT_OPTION_TOP_GAP;
            drawCombatOption(context, mouseX, mouseY, left + 8, top, right, "Hostile Mobs", "auto target", 0xFF6EE7B7, WeikhackMod.isKillAuraMobsEnabled());
            top += COMBAT_OPTION_HEIGHT + COMBAT_OPTION_GAP;
            drawCombatOption(context, mouseX, mouseY, left + 8, top, right, "Players", "player target", 0xFF60A5FA, WeikhackMod.isKillAuraPlayersEnabled());
            top += COMBAT_OPTION_HEIGHT + ROW_GAP;
        } else {
            top += ROW_GAP;
        }
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "NoKnockback", "Velocity", WeikhackMod.isNoKnockbackEnabled(), "Block");
    }

    private void drawPlayer(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "Chest Stealer", "Auto loot", WeikhackMod.isChestStealerEnabled(), "Loot");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "Freecam", "Camera move", WeikhackMod.isFreecamEnabled(), "Move");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "AutoArmor", "Equip best", WeikhackMod.isAutoArmorEnabled(), "Equip");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "AutoTotem", "Offhand", WeikhackMod.isAutoTotemEnabled(), "Totem");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "FastPlace", "Blocks", WeikhackMod.isFastPlaceEnabled(), "Place");
    }

    private void drawMisc(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "Active List", "HUD", WeikhackMod.isActiveListEnabled(), "Show");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "FakeLag", "Packet delay", WeikhackMod.isFakeLagEnabled(), "Lag");
        top += ROW_HEIGHT + ROW_GAP;
        drawConfigRow(context, mouseX, mouseY, left, top, right);
    }

    private void drawRender(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "ESP", "Players", WeikhackMod.isEspEnabled(), "Glow");
        top += ROW_HEIGHT + ROW_GAP;
        drawChestEspRow(context, mouseX, mouseY, left, top, right);
        top += ROW_HEIGHT;
        if (chestTypesOpen) {
            top += CHEST_OPTION_TOP_GAP;
            drawChestOption(context, mouseX, mouseY, left + 8, top, right, "Normal Chests", NORMAL_CHEST_COLOR, WeikhackMod.isChestEspChestsEnabled());
            top += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            drawChestOption(context, mouseX, mouseY, left + 8, top, right, "Redstone Chests", TRAPPED_CHEST_COLOR, WeikhackMod.isChestEspTrappedChestsEnabled());
            top += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            drawChestOption(context, mouseX, mouseY, left + 8, top, right, "Ender Chests", ENDER_CHEST_COLOR, WeikhackMod.isChestEspEnderChestsEnabled());
            top += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            drawChestOption(context, mouseX, mouseY, left + 8, top, right, "Barrels", BARREL_COLOR, WeikhackMod.isChestEspBarrelsEnabled());
            top += CHEST_OPTION_HEIGHT + CHEST_OPTION_GAP;
            drawChestOption(context, mouseX, mouseY, left + 8, top, right, "Shulkers", SHULKER_COLOR, WeikhackMod.isChestEspShulkersEnabled());
            top += CHEST_OPTION_HEIGHT + ROW_GAP;
        } else {
            top += ROW_GAP;
        }
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "XRay", "Ore boxes", WeikhackMod.isXrayEnabled(), "Ore");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "HealthBars", "Player HP", WeikhackMod.isHealthBarsEnabled(), "HP");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "FullBright", "Gamma", WeikhackMod.isFullBrightEnabled(), "Toggle");
        top += ROW_HEIGHT + ROW_GAP;
        drawModuleRow(context, mouseX, mouseY, left, top, right, ROW_HEIGHT, "Death Marker", "Last death", WeikhackMod.isDeathMarkerEnabled(), "Mark");
    }

    private void drawChestEspRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        boolean enabled = WeikhackMod.isChestEspEnabled();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, ROW_HEIGHT);
        boolean artHovered = isHover(mouseX, mouseY, typeButtonLeft(right), top + 10, TYPE_BUTTON_WIDTH, 20);
        int border = enabled ? 0xFF6EE7B7 : 0xFF27323D;
        int fill = enabled ? 0xFF102720 : hovered ? 0xFF151F2A : 0xFF0F151D;
        fillBox(context, left, top, right, top + ROW_HEIGHT, fill, border);
        context.fill(left + 1, top + 1, left + 4, top + ROW_HEIGHT - 1, enabled ? 0xFF6EE7B7 : 0xFF33404B);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Chest ESP"), left + 12, top + 8, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(chestTypesOpen ? "Storage types" : "Storage boxes"), left + 12, top + 24, 0xFF8E9BA8);
        drawDropdownButton(context, typeButtonLeft(right), top + 10, TYPE_BUTTON_WIDTH, 20, "ART", artHovered || chestTypesOpen);
        drawCheckbox(context, right - 24, top + 13, enabled);
    }

    private void drawKillAuraRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        boolean enabled = WeikhackMod.isKillAuraEnabled();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, ROW_HEIGHT);
        boolean artHovered = isHover(mouseX, mouseY, typeButtonLeft(right), top + 10, TYPE_BUTTON_WIDTH, 20);
        int border = enabled ? 0xFF6EE7B7 : 0xFF27323D;
        int fill = enabled ? 0xFF102720 : hovered ? 0xFF151F2A : 0xFF0F151D;
        fillBox(context, left, top, right, top + ROW_HEIGHT, fill, border);
        context.fill(left + 1, top + 1, left + 4, top + ROW_HEIGHT - 1, enabled ? 0xFF6EE7B7 : 0xFF33404B);
        context.drawTextWithShadow(this.textRenderer, Text.literal("KillAura"), left + 12, top + 8, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(killAuraTargetText()), left + 12, top + 24, 0xFF8E9BA8);
        drawDropdownButton(context, typeButtonLeft(right), top + 10, TYPE_BUTTON_WIDTH, 20, "ART", artHovered || killAuraTypesOpen);
        drawCheckbox(context, right - 24, top + 13, enabled);
    }

    private void drawChestOption(DrawContext context, int mouseX, int mouseY, int left, int top, int right, String label, int color, boolean enabled) {
        boolean masterEnabled = WeikhackMod.isChestEspEnabled();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, CHEST_OPTION_HEIGHT);
        int border = enabled ? color : 0xFF27323D;
        int fill = enabled && masterEnabled ? 0xFF0D271F : hovered ? 0xFF151F2A : 0xFF0F151D;
        fillBox(context, left, top, right, top + CHEST_OPTION_HEIGHT, fill, border);
        context.fill(left + 1, top + 1, left + 4, top + CHEST_OPTION_HEIGHT - 1, enabled ? color : 0xFF33404B);
        context.fill(left + 11, top + 8, left + 17, top + 14, color);
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), left + 24, top + 7, enabled ? 0xFFF5F7FA : 0xFF8E9BA8);
        drawCheckbox(context, right - 24, top + 5, enabled);
    }

    private void drawCombatOption(DrawContext context, int mouseX, int mouseY, int left, int top, int right, String label, String tag, int color, boolean enabled) {
        boolean masterEnabled = WeikhackMod.isKillAuraEnabled();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, COMBAT_OPTION_HEIGHT);
        int border = enabled ? color : 0xFF27323D;
        int fill = enabled && masterEnabled ? 0xFF0D271F : hovered ? 0xFF151F2A : 0xFF0F151D;
        fillBox(context, left, top, right, top + COMBAT_OPTION_HEIGHT, fill, border);
        context.fill(left + 1, top + 1, left + 4, top + COMBAT_OPTION_HEIGHT - 1, enabled ? color : 0xFF33404B);
        context.fill(left + 11, top + 8, left + 17, top + 14, color);
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), left + 24, top + 4, enabled ? 0xFFF5F7FA : 0xFF8E9BA8);
        context.drawTextWithShadow(this.textRenderer, Text.literal(tag), left + 112, top + 4, 0xFF8E9BA8);
        drawCheckbox(context, right - 24, top + 5, enabled);
    }

    private void drawModuleRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right, int height, String name, String tag, boolean enabled, String action) {
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, height);
        int border = enabled ? 0xFF6EE7B7 : 0xFF27323D;
        int fill = enabled ? 0xFF102720 : hovered ? 0xFF151F2A : 0xFF0F151D;
        fillBox(context, left, top, right, top + height, fill, border);
        context.fill(left + 1, top + 1, left + 4, top + height - 1, enabled ? 0xFF6EE7B7 : 0xFF33404B);
        context.drawTextWithShadow(this.textRenderer, Text.literal(name), left + 12, top + 8, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(tag), left + 12, top + 24, 0xFF8E9BA8);
        drawBadge(context, right - 74, top + 11, action, enabled);
        drawCheckbox(context, right - 24, top + 13, enabled);
    }

    private void drawSpeedRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        boolean active = WeikhackMod.hasSpeedBoost();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, SPEED_ROW_HEIGHT);
        fillBox(context, left, top, right, top + SPEED_ROW_HEIGHT, active ? 0xFF102720 : hovered ? 0xFF151F2A : 0xFF0F151D, active ? 0xFF6EE7B7 : 0xFF27323D);
        context.fill(left + 1, top + 1, left + 4, top + SPEED_ROW_HEIGHT - 1, active ? 0xFF6EE7B7 : 0xFF33404B);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Speed"), left + 12, top + 8, 0xFFF5F7FA);
        String speed = String.format(Locale.ROOT, "%.1fx", WeikhackMod.getSpeedMultiplier());
        context.drawTextWithShadow(this.textRenderer, Text.literal(speed), right - 50, top + 8, active ? 0xFF6EE7B7 : 0xFF8E9BA8);
        drawCheckbox(context, right - 24, top + 8, active);
        drawSlider(context, left + 14, top + 43, right - 14, WeikhackMod.getSpeedSliderValue(), active);
    }

    private void drawJumpRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        boolean active = WeikhackMod.isJumpHeightEnabled();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, SPEED_ROW_HEIGHT);
        fillBox(context, left, top, right, top + SPEED_ROW_HEIGHT, active ? 0xFF102720 : hovered ? 0xFF151F2A : 0xFF0F151D, active ? 0xFF6EE7B7 : 0xFF27323D);
        context.fill(left + 1, top + 1, left + 4, top + SPEED_ROW_HEIGHT - 1, active ? 0xFF6EE7B7 : 0xFF33404B);
        context.drawTextWithShadow(this.textRenderer, Text.literal("JumpHeight"), left + 12, top + 8, 0xFFF5F7FA);
        String height = String.format(Locale.ROOT, "%.1fb", WeikhackMod.getJumpHeightBlocks());
        context.drawTextWithShadow(this.textRenderer, Text.literal(height), right - 50, top + 8, active ? 0xFF6EE7B7 : 0xFF8E9BA8);
        drawCheckbox(context, right - 24, top + 8, active);
        drawSlider(context, left + 14, top + 43, right - 14, WeikhackMod.getJumpSliderValue(), active);
    }

    private void drawAutoSprintRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        boolean active = WeikhackMod.isAutoSprintEnabled();
        boolean wasd = WeikhackMod.isAutoSprintAllDirections();
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, ROW_HEIGHT);
        boolean buttonHovered = isHover(mouseX, mouseY, autoSprintModeButtonLeft(right), top + 10, DUAL_BUTTON_WIDTH, 20);
        int border = active ? 0xFF6EE7B7 : 0xFF27323D;
        int fill = active ? 0xFF102720 : hovered ? 0xFF151F2A : 0xFF0F151D;
        fillBox(context, left, top, right, top + ROW_HEIGHT, fill, border);
        context.fill(left + 1, top + 1, left + 4, top + ROW_HEIGHT - 1, active ? 0xFF6EE7B7 : 0xFF33404B);
        context.drawTextWithShadow(this.textRenderer, Text.literal("AutoSprint"), left + 12, top + 8, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(wasd ? "All directions" : "Forward only"), left + 12, top + 24, 0xFF8E9BA8);
        drawDropdownButton(context, autoSprintModeButtonLeft(right), top + 10, DUAL_BUTTON_WIDTH, 20, wasd ? "WASD" : "W", wasd || buttonHovered);
        drawCheckbox(context, right - 24, top + 13, active);
    }

    private void drawCommandRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right, String name, String tag, String action, boolean destructive) {
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, ROW_HEIGHT);
        fillBox(context, left, top, right, top + ROW_HEIGHT, hovered ? 0xFF151F2A : 0xFF0F151D, 0xFF27323D);
        context.fill(left + 1, top + 1, left + 4, top + ROW_HEIGHT - 1, destructive ? 0xFFF87171 : 0xFF6EE7B7);
        context.drawTextWithShadow(this.textRenderer, Text.literal(name), left + 12, top + 8, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal(tag), left + 12, top + 24, 0xFF8E9BA8);
        drawActionButton(context, right - 72, top + 10, 54, 20, action, destructive, hovered);
    }

    private void drawConfigRow(DrawContext context, int mouseX, int mouseY, int left, int top, int right) {
        boolean hovered = isHover(mouseX, mouseY, left, top, right - left, ROW_HEIGHT);
        boolean safeHovered = isHover(mouseX, mouseY, safeButtonLeft(right), top + 10, DUAL_BUTTON_WIDTH, 20);
        boolean resetHovered = isHover(mouseX, mouseY, resetButtonLeft(right), top + 10, DUAL_BUTTON_WIDTH, 20);
        fillBox(context, left, top, right, top + ROW_HEIGHT, hovered ? 0xFF151F2A : 0xFF0F151D, 0xFF27323D);
        context.fill(left + 1, top + 1, left + 4, top + ROW_HEIGHT - 1, 0xFF6EE7B7);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Config"), left + 12, top + 8, 0xFFF5F7FA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Save + reset"), left + 12, top + 24, 0xFF8E9BA8);
        drawActionButton(context, safeButtonLeft(right), top + 10, DUAL_BUTTON_WIDTH, 20, "SAFE", false, safeHovered);
        drawActionButton(context, resetButtonLeft(right), top + 10, DUAL_BUTTON_WIDTH, 20, "RESET", true, resetHovered);
    }

    private void drawEmptyState(DrawContext context, int left, int top, int right) {
        int height = Math.min(62, Math.max(44, moduleContentHeight()));
        fillBox(context, left, top, right, top + height, 0xFF0F151D, 0xFF27323D);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Noch leer"), (left + right) / 2, top + 16, 0xFFF5F7FA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Module folgen"), (left + right) / 2, top + 34, 0xFF8E9BA8);
    }

    private void drawActionButton(DrawContext context, int left, int top, int width, int height, String label, boolean destructive, boolean hovered) {
        int fill = destructive ? (hovered ? 0xFF3A1D22 : 0xFF28161A) : (hovered ? 0xFF17312B : 0xFF111E24);
        int border = destructive ? 0xFFF87171 : 0xFF6EE7B7;
        int textColor = destructive ? 0xFFFFB4B4 : 0xFFCFFAE8;
        fillBox(context, left, top, left + width, top + height, fill, border);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), left + width / 2, top + 5, textColor);
    }

    private void drawDropdownButton(DrawContext context, int left, int top, int width, int height, String label, boolean active) {
        int fill = active ? 0xFF4A2F10 : 0xFF271B10;
        int border = active ? 0xFFFFD166 : 0xFFFFA726;
        fillBox(context, left, top, left + width, top + height, fill, border);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), left + width / 2, top + 5, 0xFFFFE7A3);
    }

    private void drawBadge(DrawContext context, int left, int top, String label, boolean active) {
        int width = Math.max(36, this.textRenderer.getWidth(label) + 12);
        fillBox(context, left, top, left + width, top + 16, active ? 0xFF12382F : 0xFF151F2A, active ? 0xFF6EE7B7 : 0xFF2A3946);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(label), left + width / 2, top + 4, active ? 0xFF6EE7B7 : 0xFFABB7C3);
    }

    private void drawCheckbox(DrawContext context, int left, int top, boolean checked) {
        context.fill(left, top, left + 13, top + 13, checked ? 0xFF6EE7B7 : 0xFF202B36);
        context.fill(left + 1, top + 1, left + 12, top + 12, checked ? 0xFF10241F : 0xFF0B1017);
        if (checked) {
            context.fill(left + 4, top + 7, left + 6, top + 9, 0xFF6EE7B7);
            context.fill(left + 6, top + 9, left + 8, top + 11, 0xFF6EE7B7);
            context.fill(left + 8, top + 5, left + 10, top + 10, 0xFF6EE7B7);
            context.fill(left + 10, top + 4, left + 12, top + 7, 0xFF6EE7B7);
        }
    }

    private void drawSlider(DrawContext context, int left, int top, int right, double value, boolean active) {
        int width = right - left;
        int fillRight = left + (int) Math.round(width * clamp(value, 0.0D, 1.0D));
        context.fill(left, top, right, top + 4, 0xFF27323D);
        context.fill(left, top, fillRight, top + 4, active ? 0xFF6EE7B7 : 0xFF64748B);
        context.fill(fillRight - 3, top - 4, fillRight + 3, top + 8, active ? 0xFFB8F7DD : 0xFF93A4B5);
    }

    private void drawScrollbar(DrawContext context, int left, int top, int bottom, int contentHeight, int viewportHeight, double scroll) {
        if (contentHeight <= viewportHeight || viewportHeight <= 0) {
            return;
        }

        int trackHeight = bottom - top;
        int thumbHeight = Math.max(18, trackHeight * viewportHeight / contentHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int thumbTop = top + (int) Math.round((trackHeight - thumbHeight) * (scroll / maxScroll));
        context.fill(left, top, left + 2, bottom, 0x662A3946);
        context.fill(left, thumbTop, left + 2, thumbTop + thumbHeight, 0xCC6EE7B7);
    }

    private void drawTrollFace(DrawContext context, int x, int y, int scale) {
        int black = 0xFF050505;
        int white = 0xFFF2F2E8;

        context.fill(x + 8 * scale, y, x + 34 * scale, y + 2 * scale, black);
        context.fill(x + 4 * scale, y + 2 * scale, x + 40 * scale, y + 4 * scale, black);
        context.fill(x + 2 * scale, y + 4 * scale, x + 42 * scale, y + 18 * scale, black);
        context.fill(x + 6 * scale, y + 18 * scale, x + 38 * scale, y + 22 * scale, black);
        context.fill(x + 8 * scale, y + 2 * scale, x + 34 * scale, y + 4 * scale, white);
        context.fill(x + 4 * scale, y + 4 * scale, x + 40 * scale, y + 18 * scale, white);
        context.fill(x + 8 * scale, y + 18 * scale, x + 36 * scale, y + 20 * scale, white);
        context.fill(x + 9 * scale, y + 7 * scale, x + 18 * scale, y + 9 * scale, black);
        context.fill(x + 27 * scale, y + 7 * scale, x + 36 * scale, y + 9 * scale, black);
        context.fill(x + 11 * scale, y + 9 * scale, x + 17 * scale, y + 12 * scale, black);
        context.fill(x + 29 * scale, y + 9 * scale, x + 35 * scale, y + 12 * scale, black);
        context.fill(x + 13 * scale, y + 10 * scale, x + 16 * scale, y + 11 * scale, white);
        context.fill(x + 31 * scale, y + 10 * scale, x + 34 * scale, y + 11 * scale, white);
        context.fill(x + 21 * scale, y + 10 * scale, x + 23 * scale, y + 14 * scale, black);
        context.fill(x + 19 * scale, y + 14 * scale, x + 25 * scale, y + 15 * scale, black);
        context.fill(x + 10 * scale, y + 15 * scale, x + 36 * scale, y + 18 * scale, black);
        context.fill(x + 12 * scale, y + 16 * scale, x + 34 * scale, y + 17 * scale, white);
        context.fill(x + 13 * scale, y + 17 * scale, x + 33 * scale, y + 18 * scale, white);
        context.fill(x + 16 * scale, y + 15 * scale, x + 17 * scale, y + 18 * scale, black);
        context.fill(x + 22 * scale, y + 15 * scale, x + 23 * scale, y + 18 * scale, black);
        context.fill(x + 28 * scale, y + 15 * scale, x + 29 * scale, y + 18 * scale, black);
        context.fill(x + 7 * scale, y + 12 * scale, x + 12 * scale, y + 13 * scale, black);
        context.fill(x + 33 * scale, y + 12 * scale, x + 38 * scale, y + 13 * scale, black);
    }

    private void toggleSpeed() {
        float nextSpeed = WeikhackMod.hasSpeedBoost() ? 1.0F : Math.max(2.0F, WeikhackMod.getSpeedMultiplier());
        WeikhackMod.setSpeedMultiplier(nextSpeed, MinecraftClient.getInstance(), true);
    }

    private void updateSpeedFromMouse(double mouseX) {
        int panelWidth = panelWidth();
        int left = panelLeft(panelWidth) + SIDEBAR_WIDTH + CONTENT_PAD;
        int right = panelLeft(panelWidth) + panelWidth - CONTENT_PAD;
        int sliderLeft = left + 14;
        int sliderRight = right - 14;
        double value = (mouseX - sliderLeft) / (double) (sliderRight - sliderLeft);
        WeikhackMod.setSpeedSliderValue(clamp(value, 0.0D, 1.0D));
    }

    private void updateJumpFromMouse(double mouseX) {
        int panelWidth = panelWidth();
        int left = panelLeft(panelWidth) + SIDEBAR_WIDTH + CONTENT_PAD;
        int right = panelLeft(panelWidth) + panelWidth - CONTENT_PAD;
        int sliderLeft = left + 14;
        int sliderRight = right - 14;
        double value = (mouseX - sliderLeft) / (double) (sliderRight - sliderLeft);
        WeikhackMod.setJumpSliderValue(clamp(value, 0.0D, 1.0D));
    }

    private Category categoryAt(double mouseX, double mouseY, int left, int top, int width) {
        for (Category category : Category.values()) {
            if (isHover(mouseX, mouseY, left, top, width, CATEGORY_HEIGHT)) {
                return category;
            }
            top += CATEGORY_HEIGHT + CATEGORY_GAP;
        }
        return null;
    }

    private int panelWidth() {
        return Math.max(MIN_PANEL_WIDTH, Math.min(MAX_PANEL_WIDTH, this.width - 32));
    }

    private int panelHeight() {
        int available = Math.max(176, this.height - WINDOW_MARGIN * 2);
        return Math.min(MAX_PANEL_HEIGHT, available);
    }

    private int panelLeft(int panelWidth) {
        return (this.width - panelWidth) / 2;
    }

    private int panelTop(int panelHeight) {
        return Math.max(WINDOW_MARGIN, (this.height - panelHeight) / 2);
    }

    private int categoryContentHeight() {
        return Category.values().length * CATEGORY_HEIGHT + (Category.values().length - 1) * CATEGORY_GAP;
    }

    private int moduleContentHeight() {
        if (selectedCategory == Category.MOVEMENT) {
            return ROW_HEIGHT + ROW_GAP + SPEED_ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + SPEED_ROW_HEIGHT + ROW_GAP + ROW_HEIGHT;
        }
        if (selectedCategory == Category.COMBAT) {
            return combatContentHeight();
        }
        if (selectedCategory == Category.MISC) {
            return ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT;
        }
        if (selectedCategory == Category.RENDER) {
            return renderContentHeight();
        }
        if (selectedCategory == Category.PLAYER) {
            return ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT;
        }
        return 62;
    }

    private int renderContentHeight() {
        int height = ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT + ROW_GAP + ROW_HEIGHT;
        if (chestTypesOpen) {
            height += CHEST_OPTION_TOP_GAP + CHEST_TYPE_COUNT * CHEST_OPTION_HEIGHT + (CHEST_TYPE_COUNT - 1) * CHEST_OPTION_GAP;
        }
        return height;
    }

    private int combatContentHeight() {
        int height = ROW_HEIGHT + ROW_GAP + ROW_HEIGHT;
        if (killAuraTypesOpen) {
            height += COMBAT_OPTION_TOP_GAP + KILL_AURA_TYPE_COUNT * COMBAT_OPTION_HEIGHT + (KILL_AURA_TYPE_COUNT - 1) * COMBAT_OPTION_GAP;
        }
        return height;
    }

    private double maxModuleScroll(int top, int bottom) {
        return Math.max(0, moduleContentHeight() - Math.max(0, bottom - top));
    }

    private static boolean isHover(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height;
    }

    private static void fillBox(DrawContext context, int left, int top, int right, int bottom, int fill, int border) {
        context.fill(left, top, right, bottom, border);
        context.fill(left + 1, top + 1, right - 1, bottom - 1, fill);
    }

    private String killAuraTargetText() {
        if (WeikhackMod.isKillAuraMobsEnabled() && WeikhackMod.isKillAuraPlayersEnabled()) {
            return "Mobs + Players";
        }
        if (WeikhackMod.isKillAuraPlayersEnabled()) {
            return "Players";
        }
        if (WeikhackMod.isKillAuraMobsEnabled()) {
            return "Hostile Mobs";
        }
        return "No targets";
    }

    private static int typeButtonLeft(int right) {
        return right - 68;
    }

    private static int autoSprintModeButtonLeft(int right) {
        return right - 86;
    }

    private static int resetButtonLeft(int right) {
        return right - DUAL_BUTTON_WIDTH - 18;
    }

    private static int safeButtonLeft(int right) {
        return resetButtonLeft(right) - DUAL_BUTTON_GAP - DUAL_BUTTON_WIDTH;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Category {
        MOVEMENT("Movement", "8 modules"),
        COMBAT("Combat", "2 modules"),
        RENDER("Render", "6 modules"),
        PLAYER("Player", "5 modules"),
        MISC("Misc", "3 modules");

        private final String label;
        private final String count;

        Category(String label, String count) {
            this.label = label;
            this.count = count;
        }

        private String countLabel() {
            return count;
        }
    }
}
