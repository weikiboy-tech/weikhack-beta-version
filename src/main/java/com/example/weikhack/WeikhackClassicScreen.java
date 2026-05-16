package com.example.weikhack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class WeikhackClassicScreen extends Screen {
    private static final int PANEL_WIDTH = 250;
    private static final int PANEL_HEIGHT = 252;
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 20;

    protected WeikhackClassicScreen() {
        super(Text.literal("Weikhack Classic"));
    }

    @Override
    protected void init() {
        int left = (this.width - BUTTON_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2 + 94;

        addDrawableChild(ButtonWidget.builder(flightText(), button -> {
            WeikhackMod.toggleFlight(MinecraftClient.getInstance());
            button.setMessage(flightText());
        }).dimensions(left, top, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addDrawableChild(new SpeedSliderWidget(left, top + 28, BUTTON_WIDTH, BUTTON_HEIGHT));

        addDrawableChild(CheckboxWidget.builder(noFallText(), this.textRenderer)
                .pos(left, top + 60)
                .maxWidth(BUTTON_WIDTH)
                .checked(WeikhackMod.isNoFallEnabled())
                .callback((checkbox, checked) -> WeikhackMod.setNoFallEnabled(checked, MinecraftClient.getInstance(), true))
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> {
            WeikhackMod.reset(MinecraftClient.getInstance());
            refreshButtons();
        }).dimensions(left, top + 92, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Modern UI"), button -> {
            MinecraftClient.getInstance().setScreen(new WeikhackScreen());
        }).dimensions(left, top + 120, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelLeft = (this.width - PANEL_WIDTH) / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;
        int panelRight = panelLeft + PANEL_WIDTH;
        int panelBottom = panelTop + PANEL_HEIGHT;

        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC111820);
        drawBorder(context, panelLeft, panelTop, panelRight, panelBottom, 0xFF68D391);
        drawTrollFace(context, this.width / 2 - 42, panelTop + 30, 2);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelTop + 12, 0xFF68D391);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(WeikhackMod.DISPLAY_VERSION), this.width / 2, panelTop + 24, 0xFFB7F7DA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Right Shift oeffnet dieses Menu"), this.width / 2, panelBottom - 18, 0xFFB7C4C9);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void refreshButtons() {
        clearAndInit();
    }

    private static Text flightText() {
        return Text.literal("Fliegen: " + (WeikhackMod.isFlightEnabled() ? "an" : "aus"));
    }

    private static Text noFallText() {
        return Text.literal("NoFall");
    }

    private static Text speedText() {
        return Text.literal("Speed: " + String.format("%.1fx", WeikhackMod.getSpeedMultiplier()));
    }

    private static class SpeedSliderWidget extends SliderWidget {
        SpeedSliderWidget(int x, int y, int width, int height) {
            super(x, y, width, height, speedText(), WeikhackMod.getSpeedSliderValue());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(speedText());
        }

        @Override
        protected void applyValue() {
            WeikhackMod.setSpeedSliderValue(this.value);
            updateMessage();
        }
    }

    private static void drawBorder(DrawContext context, int left, int top, int right, int bottom, int color) {
        context.fill(left, top, right, top + 1, color);
        context.fill(left, bottom - 1, right, bottom, color);
        context.fill(left, top, left + 1, bottom, color);
        context.fill(right - 1, top, right, bottom, color);
    }

    private static void drawTrollFace(DrawContext context, int x, int y, int scale) {
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
}
