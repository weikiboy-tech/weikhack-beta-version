package com.example.weikhack;

import com.example.weikhack.mixin.RenderLayerAccessor;
import com.example.weikhack.mixin.RenderPipelinesAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WeikhackChestEspRenderer {
    private static final double BOX_PADDING = 0.004D;
    private static final double MAX_HUD_DISTANCE = 128.0D;
    private static final int BARREL_SCAN_RADIUS = 48;
    private static final int BARREL_SCAN_VERTICAL_RADIUS = 24;
    private static final long BARREL_SCAN_INTERVAL_TICKS = 10L;
    private static final int XRAY_SCAN_RADIUS = 48;
    private static final int XRAY_SCAN_VERTICAL_RADIUS = 32;
    private static final long XRAY_SCAN_INTERVAL_TICKS = 20L;
    private static final ChestStyle NORMAL_CHEST = new ChestStyle(0xFF34F85A, "Chest");
    private static final ChestStyle TRAPPED_CHEST = new ChestStyle(0xFFFF5555, "Trapped Chest");
    private static final ChestStyle ENDER_CHEST = new ChestStyle(0xFFB66CFF, "Ender Chest");
    private static final ChestStyle BARREL = new ChestStyle(0xFFFFC857, "Barrel");
    private static final ChestStyle SHULKER = new ChestStyle(0xFF55D7FF, "Shulker");
    private static final ChestStyle DIAMOND_ORE = new ChestStyle(0xFF55D7FF, "Diamond");
    private static final ChestStyle GOLD_ORE = new ChestStyle(0xFFFFD166, "Gold");
    private static final ChestStyle IRON_ORE = new ChestStyle(0xFFD8DEE9, "Iron");
    private static final ChestStyle LAPIS_ORE = new ChestStyle(0xFF60A5FA, "Lapis");
    private static final int DEATH_MARKER_COLOR = 0xFFFF4D6D;
    private static final RenderLayer CHEST_XRAY_LINES = createXrayLineLayer();
    private static final VoxelShape CHEST_BOX = VoxelShapes.cuboid(new Box(
            -BOX_PADDING,
            -BOX_PADDING,
            -BOX_PADDING,
            1.0D + BOX_PADDING,
            1.0D + BOX_PADDING,
            1.0D + BOX_PADDING
    ));
    private static final List<BlockPos> cachedBarrelPositions = new ArrayList<>();
    private static final List<OreTarget> cachedOrePositions = new ArrayList<>();
    private static BlockPos lastBarrelScanCenter;
    private static BlockPos lastXrayScanCenter;
    private static long nextBarrelScanTick;
    private static long nextXrayScanTick;

    private WeikhackChestEspRenderer() {
    }

    public static void render(VertexConsumerProvider.Immediate vertexConsumers, MatrixStack matrices, WorldRenderState renderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((!WeikhackMod.isChestEspEnabled() && !WeikhackMod.isXrayEnabled() && !WeikhackMod.isHealthBarsEnabled()) || client.world == null || client.player == null || renderState.cameraRenderState == null) {
            return;
        }

        Vec3d cameraPos = renderState.cameraRenderState.pos;
        if (cameraPos == null) {
            cameraPos = client.gameRenderer.getCamera().getCameraPos();
        }

        VertexConsumer chestLines = vertexConsumers.getBuffer(CHEST_XRAY_LINES);
        if (WeikhackMod.isChestEspEnabled()) {
            Set<BlockPos> renderedBarrels = new HashSet<>();
            for (BlockEntity blockEntity : client.world.getBlockEntities()) {
                ChestStyle style = styleFor(blockEntity);
                if (style == null) {
                    continue;
                }

                if (style == BARREL) {
                    renderedBarrels.add(blockEntity.getPos());
                }
                double x = blockEntity.getPos().getX() - cameraPos.x;
                double y = blockEntity.getPos().getY() - cameraPos.y;
                double z = blockEntity.getPos().getZ() - cameraPos.z;
                VertexRendering.drawOutline(matrices, chestLines, CHEST_BOX, x, y, z, style.color(), 5.0F);
            }

            renderCachedBarrels(client, matrices, chestLines, cameraPos, renderedBarrels);
        }

        renderXrayOres(client, matrices, chestLines, cameraPos);
        renderPlayerHealthBars(client, matrices, vertexConsumers, chestLines, cameraPos);
        vertexConsumers.draw(CHEST_XRAY_LINES);
    }

    public static void submitChestBlockEntityOutline(ChestBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue) {
        if (!WeikhackMod.isChestEspEnabled()) {
            return;
        }

        ChestStyle style = styleFor(state);
        if (style == null) {
            return;
        }

        queue.submitCustom(matrices, CHEST_XRAY_LINES, (entry, consumer) -> drawLocalChestOutline(entry, consumer, style.color()));
    }

    public static void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || client.textRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        double verticalFov = Math.toRadians(client.options.getFov().getValue());
        double focalLength = height / (2.0D * Math.tan(verticalFov / 2.0D));

        if (WeikhackMod.isChestEspEnabled()) {
            for (BlockEntity blockEntity : client.world.getBlockEntities()) {
                ChestStyle style = styleFor(blockEntity);
                if (style == null) {
                    continue;
                }

                if (style == BARREL) {
                    continue;
                }
                drawHudBoxForStorage(context, client, camera, cameraPos, focalLength, width, height, blockEntity.getPos(), style);
            }
        }

        renderDeathMarkerHud(context, client, camera, cameraPos, focalLength, width, height);
    }

    private static ChestStyle styleFor(BlockEntity blockEntity) {
        BlockEntityType<?> type = blockEntity.getType();
        if (type == BlockEntityType.CHEST && WeikhackMod.isChestEspChestsEnabled()) {
            return NORMAL_CHEST;
        }
        if (type == BlockEntityType.TRAPPED_CHEST && WeikhackMod.isChestEspTrappedChestsEnabled()) {
            return TRAPPED_CHEST;
        }
        if (type == BlockEntityType.ENDER_CHEST && WeikhackMod.isChestEspEnderChestsEnabled()) {
            return ENDER_CHEST;
        }
        if (type == BlockEntityType.BARREL && WeikhackMod.isChestEspBarrelsEnabled()) {
            return BARREL;
        }
        if (type == BlockEntityType.SHULKER_BOX && WeikhackMod.isChestEspShulkersEnabled()) {
            return SHULKER;
        }
        return null;
    }

    private static ChestStyle styleFor(ChestBlockEntityRenderState state) {
        return switch (state.variant) {
            case REGULAR, CHRISTMAS, COPPER_UNAFFECTED, COPPER_EXPOSED, COPPER_WEATHERED, COPPER_OXIDIZED ->
                    WeikhackMod.isChestEspChestsEnabled() ? NORMAL_CHEST : null;
            case TRAPPED -> WeikhackMod.isChestEspTrappedChestsEnabled() ? TRAPPED_CHEST : null;
            case ENDER_CHEST -> WeikhackMod.isChestEspEnderChestsEnabled() ? ENDER_CHEST : null;
        };
    }

    private static RenderLayer createXrayLineLayer() {
        RenderPipeline pipeline = RenderPipelinesAccessor.weikhack$register(RenderPipeline.builder(RenderPipelinesAccessor.weikhack$getLinesSnippet())
                .withLocation("pipeline/weikhack_chest_esp_xray_lines")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .build());
        return RenderLayerAccessor.weikhack$of("weikhack_chest_esp_xray_lines", RenderSetup.builder(pipeline)
                .translucent()
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .build());
    }

    private static void renderCachedBarrels(MinecraftClient client, MatrixStack matrices, VertexConsumer chestLines, Vec3d cameraPos, Set<BlockPos> renderedBarrels) {
        if (!WeikhackMod.isChestEspBarrelsEnabled()) {
            cachedBarrelPositions.clear();
            return;
        }

        updateBarrelCache(client);
        for (BlockPos pos : cachedBarrelPositions) {
            if (renderedBarrels.contains(pos) || !isBarrel(client, pos)) {
                continue;
            }

            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;
            VertexRendering.drawOutline(matrices, chestLines, CHEST_BOX, x, y, z, BARREL.color(), 5.0F);
        }
    }

    private static void renderXrayOres(MinecraftClient client, MatrixStack matrices, VertexConsumer lines, Vec3d cameraPos) {
        if (!WeikhackMod.isXrayEnabled()) {
            cachedOrePositions.clear();
            return;
        }

        updateXrayCache(client);
        for (OreTarget target : cachedOrePositions) {
            if (oreStyle(client.world.getBlockState(target.pos())) == null) {
                continue;
            }

            double x = target.pos().getX() - cameraPos.x;
            double y = target.pos().getY() - cameraPos.y;
            double z = target.pos().getZ() - cameraPos.z;
            VertexRendering.drawOutline(matrices, lines, CHEST_BOX, x, y, z, target.style().color(), 4.0F);
        }
    }

    private static void renderDeathMarker(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, VertexConsumer lines, Vec3d cameraPos) {
        if (!WeikhackMod.isDeathMarkerEnabled() || !WeikhackMod.hasDeathMarker() || !WeikhackMod.isLastDeathInCurrentDimension()) {
            return;
        }

        Vec3d death = anchoredDeathPosition();
        if (death == null) {
            return;
        }

        double x = death.x - cameraPos.x;
        double y = death.y - cameraPos.y;
        double z = death.z - cameraPos.z;
        drawDeathMarkerBox(matrices.peek(), lines, x, y, z);
        drawDeathTracer(matrices, lines, death, cameraPos);

        double distance = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()).distanceTo(death);
        drawWorldDeathLabel(client, matrices, vertexConsumers, cameraPos, death, "Death " + Math.round(distance) + "m");
    }

    private static void renderDeathMarkerHud(DrawContext context, MinecraftClient client, Camera camera, Vec3d cameraPos, double focalLength, int width, int height) {
        if (!WeikhackMod.isDeathMarkerEnabled() || !WeikhackMod.hasDeathMarker()) {
            return;
        }

        if (!WeikhackMod.isLastDeathInCurrentDimension()) {
            String dimension = WeikhackMod.getLastDeathDimension();
            String label = "Death: " + (dimension == null ? "other dimension" : dimension);
            drawCenteredHudLabel(context, client, width / 2, 24, label, DEATH_MARKER_COLOR);
            return;
        }

        Vec3d death = anchoredDeathPosition();
        if (death == null) {
            return;
        }

        double distance = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()).distanceTo(death);
        String label = "Death " + Math.round(distance) + "m";
        Vec3d marker = death.add(0.0D, 0.95D, 0.0D);
        ScreenPoint projected = projectDeathMarker(marker, camera, cameraPos, focalLength, width, height);
        ScreenPoint pinned = projected == null
                ? fallbackDeathDirection(death, camera, cameraPos, width, height)
                : clampToScreenEdge(projected, width, height, 24);
        int x = (int) Math.round(pinned.x());
        int y = (int) Math.round(pinned.y());

        drawDeathCross(context, x, y);
        drawCenteredHudLabel(context, client, x, y + 10, label, DEATH_MARKER_COLOR);
    }

    private static void updateXrayCache(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            cachedOrePositions.clear();
            return;
        }

        BlockPos center = client.player.getBlockPos();
        long now = client.world.getTime();
        if (lastXrayScanCenter != null
                && now < nextXrayScanTick
                && squaredDistance(center, lastXrayScanCenter) < 100) {
            return;
        }

        cachedOrePositions.clear();
        for (BlockPos pos : BlockPos.iterateOutwards(center, XRAY_SCAN_RADIUS, XRAY_SCAN_VERTICAL_RADIUS, XRAY_SCAN_RADIUS)) {
            ChestStyle style = oreStyle(client.world.getBlockState(pos));
            if (style != null) {
                cachedOrePositions.add(new OreTarget(pos.toImmutable(), style));
            }
        }
        lastXrayScanCenter = center.toImmutable();
        nextXrayScanTick = now + XRAY_SCAN_INTERVAL_TICKS;
    }

    private static ChestStyle oreStyle(BlockState state) {
        if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            return DIAMOND_ORE;
        }
        if (state.isOf(Blocks.GOLD_ORE) || state.isOf(Blocks.DEEPSLATE_GOLD_ORE) || state.isOf(Blocks.NETHER_GOLD_ORE)) {
            return GOLD_ORE;
        }
        if (state.isOf(Blocks.IRON_ORE) || state.isOf(Blocks.DEEPSLATE_IRON_ORE)) {
            return IRON_ORE;
        }
        if (state.isOf(Blocks.LAPIS_ORE) || state.isOf(Blocks.DEEPSLATE_LAPIS_ORE)) {
            return LAPIS_ORE;
        }
        return null;
    }

    private static void updateBarrelCache(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            cachedBarrelPositions.clear();
            return;
        }

        BlockPos center = client.player.getBlockPos();
        long now = client.world.getTime();
        if (lastBarrelScanCenter != null
                && now < nextBarrelScanTick
                && squaredDistance(center, lastBarrelScanCenter) < 64) {
            return;
        }

        cachedBarrelPositions.clear();
        for (BlockPos pos : BlockPos.iterateOutwards(center, BARREL_SCAN_RADIUS, BARREL_SCAN_VERTICAL_RADIUS, BARREL_SCAN_RADIUS)) {
            if (isBarrel(client, pos)) {
                cachedBarrelPositions.add(pos.toImmutable());
            }
        }
        lastBarrelScanCenter = center.toImmutable();
        nextBarrelScanTick = now + BARREL_SCAN_INTERVAL_TICKS;
    }

    private static boolean isBarrel(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        return state.isOf(Blocks.BARREL);
    }

    private static void renderPlayerHealthBars(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, VertexConsumer lines, Vec3d cameraPos) {
        if (!WeikhackMod.isHealthBarsEnabled() || client.world == null || client.player == null || client.textRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        for (var player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || !player.isAlive()) {
                continue;
            }

            Vec3d labelPos = new Vec3d(player.getX(), player.getY() + player.getHeight() + 0.65D, player.getZ());
            if (labelPos.squaredDistanceTo(cameraPos) > MAX_HUD_DISTANCE * MAX_HUD_DISTANCE) {
                continue;
            }

            float healthPercent = Math.max(0.0F, Math.min(1.0F, player.getHealth() / Math.max(1.0F, player.getMaxHealth())));
            int barColor = healthPercent > 0.55F ? 0xFF34F85A : healthPercent > 0.25F ? 0xFFFFC857 : 0xFFFF5555;
            float barWidth = 42.0F;
            float fillWidth = barWidth * healthPercent;

            matrices.push();
            matrices.translate(labelPos.x - cameraPos.x, labelPos.y - cameraPos.y, labelPos.z - cameraPos.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);
            MatrixStack.Entry entry = matrices.peek();

            line(entry, lines, -barWidth / 2.0F, -2.0F, 0.0F, barWidth / 2.0F, -2.0F, 0.0F, 0xFF17212B);
            line(entry, lines, -barWidth / 2.0F, -2.0F, -0.01F, -barWidth / 2.0F + fillWidth, -2.0F, -0.01F, barColor);
            matrices.pop();
        }

        vertexConsumers.draw(CHEST_XRAY_LINES);

        for (var player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || !player.isAlive()) {
                continue;
            }

            Vec3d labelPos = new Vec3d(player.getX(), player.getY() + player.getHeight() + 0.65D, player.getZ());
            if (labelPos.squaredDistanceTo(cameraPos) > MAX_HUD_DISTANCE * MAX_HUD_DISTANCE) {
                continue;
            }

            String label = String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth());
            int textWidth = client.textRenderer.getWidth(label);

            matrices.push();
            matrices.translate(labelPos.x - cameraPos.x, labelPos.y - cameraPos.y, labelPos.z - cameraPos.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);
            MatrixStack.Entry entry = matrices.peek();
            client.textRenderer.draw(label, -textWidth / 2.0F, -16.0F, 0xFFE6F7F4, false, entry.getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0x99000000, 0xF000F0);
            matrices.pop();
        }
    }

    private static void renderPlayerHud(DrawContext context, MinecraftClient client, Camera camera, Vec3d cameraPos, double focalLength, int width, int height) {
        if (!WeikhackMod.isHealthBarsEnabled()) {
            return;
        }

        for (var player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || !player.isAlive()) {
                continue;
            }

            Vec3d labelPos = new Vec3d(player.getX(), player.getY() + player.getHeight() + 0.45D, player.getZ());
            if (labelPos.squaredDistanceTo(cameraPos) > MAX_HUD_DISTANCE * MAX_HUD_DISTANCE) {
                continue;
            }

            ScreenPoint point = projectPoint(labelPos.x, labelPos.y, labelPos.z, camera, focalLength, width, height);
            if (point == null || point.x() < -80 || point.x() > width + 80 || point.y() < -40 || point.y() > height + 40) {
                continue;
            }

            int centerX = (int) Math.round(point.x());
            int top = (int) Math.round(point.y());
            String health = String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth());
            String label = health;
            int textWidth = client.textRenderer.getWidth(label);
            int left = centerX - textWidth / 2 - 4;
            int right = centerX + textWidth / 2 + 4;
            context.fill(left, top - 2, right, top + 19, 0xAA05090D);
            context.drawTextWithShadow(client.textRenderer, Text.literal(label), centerX - textWidth / 2, top, 0xFFE6F7F4);

            if (WeikhackMod.isHealthBarsEnabled()) {
                float healthPercent = Math.max(0.0F, Math.min(1.0F, player.getHealth() / Math.max(1.0F, player.getMaxHealth())));
                int barLeft = left + 3;
                int barRight = right - 3;
                int fillRight = barLeft + Math.round((barRight - barLeft) * healthPercent);
                int barColor = healthPercent > 0.55F ? 0xFF34F85A : healthPercent > 0.25F ? 0xFFFFC857 : 0xFFFF5555;
                context.fill(barLeft, top + 12, barRight, top + 15, 0xFF1C2732);
                context.fill(barLeft, top + 12, fillRight, top + 15, barColor);
            }
        }
    }

    private static Vec3d anchoredDeathPosition() {
        Vec3d death = WeikhackMod.getLastDeathPosition();
        if (death == null) {
            return null;
        }

        return new Vec3d(death.x, death.y, death.z);
    }

    private static void drawWorldDeathLabel(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, Vec3d cameraPos, Vec3d death, String label) {
        if (client.textRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        int textWidth = client.textRenderer.getWidth(label);
        Vec3d labelPos = death.add(0.0D, 2.12D, 0.0D);

        matrices.push();
        matrices.translate(labelPos.x - cameraPos.x, labelPos.y - cameraPos.y, labelPos.z - cameraPos.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.035F, -0.035F, 0.035F);
        MatrixStack.Entry entry = matrices.peek();

        client.textRenderer.draw(label, -textWidth / 2.0F, 0.0F, DEATH_MARKER_COLOR, false, entry.getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0xAA05090D, 0xF000F0);
        client.textRenderer.draw("+", -client.textRenderer.getWidth("+") / 2.0F, -14.0F, DEATH_MARKER_COLOR, false, entry.getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0x00000000, 0xF000F0);
        matrices.pop();
    }

    private static void drawDeathTracer(MatrixStack matrices, VertexConsumer lines, Vec3d death, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();
        float targetX = (float) (death.x - cameraPos.x);
        float targetY = (float) (death.y + 0.95D - cameraPos.y);
        float targetZ = (float) (death.z - cameraPos.z);
        line(entry, lines, 0.0F, 0.0F, 0.0F, targetX, targetY, targetZ, DEATH_MARKER_COLOR);
    }

    private static void drawDeathMarkerBox(MatrixStack.Entry entry, VertexConsumer lines, double x, double y, double z) {
        float minX = (float) (x - 0.38D);
        float minY = (float) y;
        float minZ = (float) (z - 0.38D);
        float maxX = (float) (x + 0.38D);
        float maxY = (float) (y + 1.9D);
        float maxZ = (float) (z + 0.38D);

        line(entry, lines, minX, minY, minZ, maxX, minY, minZ, DEATH_MARKER_COLOR);
        line(entry, lines, maxX, minY, minZ, maxX, minY, maxZ, DEATH_MARKER_COLOR);
        line(entry, lines, maxX, minY, maxZ, minX, minY, maxZ, DEATH_MARKER_COLOR);
        line(entry, lines, minX, minY, maxZ, minX, minY, minZ, DEATH_MARKER_COLOR);

        line(entry, lines, minX, maxY, minZ, maxX, maxY, minZ, DEATH_MARKER_COLOR);
        line(entry, lines, maxX, maxY, minZ, maxX, maxY, maxZ, DEATH_MARKER_COLOR);
        line(entry, lines, maxX, maxY, maxZ, minX, maxY, maxZ, DEATH_MARKER_COLOR);
        line(entry, lines, minX, maxY, maxZ, minX, maxY, minZ, DEATH_MARKER_COLOR);

        line(entry, lines, minX, minY, minZ, minX, maxY, minZ, DEATH_MARKER_COLOR);
        line(entry, lines, maxX, minY, minZ, maxX, maxY, minZ, DEATH_MARKER_COLOR);
        line(entry, lines, maxX, minY, maxZ, maxX, maxY, maxZ, DEATH_MARKER_COLOR);
        line(entry, lines, minX, minY, maxZ, minX, maxY, maxZ, DEATH_MARKER_COLOR);
    }

    private static boolean isRoughlyInFront(Camera camera, Vec3d cameraPos, Vec3d death) {
        double targetYaw = Math.toDegrees(Math.atan2(-(death.x - cameraPos.x), death.z - cameraPos.z));
        double relativeYaw = Math.abs(wrapDegrees(targetYaw - camera.getYaw()));
        return relativeYaw < 58.0D;
    }

    private static ScreenPoint projectDeathMarker(Vec3d marker, Camera camera, Vec3d cameraPos, double focalLength, int width, int height) {
        double dx = marker.x - cameraPos.x;
        double dy = marker.y - cameraPos.y;
        double dz = marker.z - cameraPos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) {
            return new ScreenPoint(width * 0.5D, height * 0.5D);
        }

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double targetPitch = -Math.toDegrees(Math.atan2(dy, horizontal));
        double relativeYaw = wrapDegrees(targetYaw - camera.getYaw());
        double relativePitch = wrapDegrees(targetPitch - camera.getPitch());

        double verticalFov = 2.0D * Math.atan(height / (2.0D * focalLength));
        double horizontalFov = 2.0D * Math.atan(Math.tan(verticalFov * 0.5D) * width / (double) height);
        double horizontalLimit = Math.toDegrees(horizontalFov) * 0.5D + 4.0D;
        double verticalLimit = Math.toDegrees(verticalFov) * 0.5D + 8.0D;
        if (Math.abs(relativeYaw) > horizontalLimit || Math.abs(relativePitch) > verticalLimit) {
            return null;
        }

        double yawRadians = Math.toRadians(relativeYaw);
        double pitchRadians = Math.toRadians(relativePitch);
        double x = projectedDeathMarkerX(marker, camera, cameraPos, focalLength, width, yawRadians);
        double y = height * 0.5D + Math.tan(pitchRadians) / Math.tan(verticalFov * 0.5D) * height * 0.5D;
        return new ScreenPoint(x, y);
    }

    private static double projectedDeathMarkerX(Vec3d marker, Camera camera, Vec3d cameraPos, double focalLength, int width, double yawRadians) {
        double dx = marker.x - cameraPos.x;
        double dy = marker.y - cameraPos.y;
        double dz = marker.z - cameraPos.z;
        Vector3fc right = camera.getDiagonalPlane();
        Vector3fc forward = camera.getHorizontalPlane();
        double cameraX = dx * right.x() + dy * right.y() + dz * right.z();
        double cameraZ = dx * forward.x() + dy * forward.y() + dz * forward.z();
        if (cameraZ > 0.05D) {
            return width * 0.5D - cameraX * focalLength / cameraZ;
        }
        return width * 0.5D - Math.tan(yawRadians) * focalLength;
    }

    private static ScreenPoint clampToScreenEdge(ScreenPoint point, int width, int height, int margin) {
        double centerX = width * 0.5D;
        double centerY = height * 0.5D;
        double dx = point.x() - centerX;
        double dy = point.y() - centerY;
        double minX = margin;
        double minY = margin;
        double maxX = width - margin;
        double maxY = height - margin;

        if (point.x() >= minX && point.x() <= maxX && point.y() >= minY && point.y() <= maxY) {
            return point;
        }

        double scaleX = dx == 0.0D ? Double.POSITIVE_INFINITY : (dx > 0.0D ? (maxX - centerX) / dx : (minX - centerX) / dx);
        double scaleY = dy == 0.0D ? Double.POSITIVE_INFINITY : (dy > 0.0D ? (maxY - centerY) / dy : (minY - centerY) / dy);
        double scale = Math.max(0.0D, Math.min(scaleX, scaleY));
        return new ScreenPoint(centerX + dx * scale, centerY + dy * scale);
    }

    private static ScreenPoint fallbackDeathDirection(Vec3d death, Camera camera, Vec3d cameraPos, int width, int height) {
        double targetYaw = Math.toDegrees(Math.atan2(-(death.x - cameraPos.x), death.z - cameraPos.z));
        double relativeYaw = wrapDegrees(targetYaw - camera.getYaw());
        double angle = Math.toRadians(relativeYaw);
        double centerX = width * 0.5D;
        double centerY = height * 0.5D;
        return clampToScreenEdge(
                new ScreenPoint(centerX - Math.sin(angle) * width, centerY - Math.cos(angle) * height),
                width,
                height,
                24
        );
    }

    private static void drawDeathCross(DrawContext context, int x, int y) {
        context.fill(x - 5, y - 1, x + 6, y + 2, DEATH_MARKER_COLOR);
        context.fill(x - 1, y - 5, x + 2, y + 6, DEATH_MARKER_COLOR);
        context.fill(x - 3, y - 3, x + 4, y + 4, 0x55000000);
    }

    private static void drawCenteredHudLabel(DrawContext context, MinecraftClient client, int centerX, int y, String label, int color) {
        int textWidth = client.textRenderer.getWidth(label);
        int left = centerX - textWidth / 2 - 4;
        context.fill(left, y - 2, left + textWidth + 8, y + 11, 0xAA05090D);
        context.drawTextWithShadow(client.textRenderer, Text.literal(label), centerX - textWidth / 2, y, color);
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    private static int squaredDistance(BlockPos first, BlockPos second) {
        int x = first.getX() - second.getX();
        int y = first.getY() - second.getY();
        int z = first.getZ() - second.getZ();
        return x * x + y * y + z * z;
    }

    private static void drawHudBoxForStorage(DrawContext context, MinecraftClient client, Camera camera, Vec3d cameraPos, double focalLength, int width, int height, BlockPos pos, ChestStyle style) {
        Vec3d center = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (center.squaredDistanceTo(cameraPos) > MAX_HUD_DISTANCE * MAX_HUD_DISTANCE) {
            return;
        }

        ScreenBox box = projectChest(pos, camera, focalLength, width, height);
        if (box == null) {
            return;
        }

        drawHudBox(context, box, style.color());
        context.drawTextWithShadow(client.textRenderer, Text.literal(style.label()), box.left(), Math.max(2, box.top() - 10), style.color());
    }

    private static ScreenBox projectChest(BlockPos pos, Camera camera, double focalLength, int screenWidth, int screenHeight) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        boolean visible = false;

        for (int cornerX = 0; cornerX <= 1; cornerX++) {
            for (int cornerY = 0; cornerY <= 1; cornerY++) {
                for (int cornerZ = 0; cornerZ <= 1; cornerZ++) {
                    ScreenPoint point = projectPoint(
                            pos.getX() + cornerX,
                            pos.getY() + cornerY,
                            pos.getZ() + cornerZ,
                            camera,
                            focalLength,
                            screenWidth,
                            screenHeight
                    );
                    if (point == null) {
                        continue;
                    }

                    visible = true;
                    minX = Math.min(minX, point.x());
                    minY = Math.min(minY, point.y());
                    maxX = Math.max(maxX, point.x());
                    maxY = Math.max(maxY, point.y());
                }
            }
        }

        if (!visible || maxX < 0.0D || maxY < 0.0D || minX > screenWidth || minY > screenHeight) {
            return null;
        }

        int left = Math.max(0, (int) Math.floor(minX) - 2);
        int top = Math.max(0, (int) Math.floor(minY) - 2);
        int right = Math.min(screenWidth, (int) Math.ceil(maxX) + 2);
        int bottom = Math.min(screenHeight, (int) Math.ceil(maxY) + 2);
        if (right - left < 6 || bottom - top < 6) {
            return null;
        }

        return new ScreenBox(left, top, right, bottom);
    }

    private static ScreenPoint projectPoint(double worldX, double worldY, double worldZ, Camera camera, double focalLength, int screenWidth, int screenHeight) {
        Vec3d cameraPos = camera.getCameraPos();
        double x = worldX - cameraPos.x;
        double y = worldY - cameraPos.y;
        double z = worldZ - cameraPos.z;

        Vector3fc right = camera.getDiagonalPlane();
        Vector3fc up = camera.getVerticalPlane();
        Vector3fc forward = camera.getHorizontalPlane();

        double cameraX = x * right.x() + y * right.y() + z * right.z();
        double cameraY = x * up.x() + y * up.y() + z * up.z();
        double cameraZ = x * forward.x() + y * forward.y() + z * forward.z();
        if (cameraZ <= 0.05D) {
            return null;
        }

        double screenX = screenWidth * 0.5D + cameraX * focalLength / cameraZ;
        double screenY = screenHeight * 0.5D - cameraY * focalLength / cameraZ;
        return new ScreenPoint(screenX, screenY);
    }

    private static void drawHudBox(DrawContext context, ScreenBox box, int color) {
        context.fill(box.left(), box.top(), box.right(), box.bottom(), withAlpha(color, 0x22));
        context.fill(box.left(), box.top(), box.right(), box.top() + 2, color);
        context.fill(box.left(), box.bottom() - 2, box.right(), box.bottom(), color);
        context.fill(box.left(), box.top(), box.left() + 2, box.bottom(), color);
        context.fill(box.right() - 2, box.top(), box.right(), box.bottom(), color);
    }

    private static void drawLocalChestOutline(MatrixStack.Entry entry, VertexConsumer consumer, int color) {
        float min = -0.006F;
        float max = 1.006F;

        line(entry, consumer, min, min, min, max, min, min, color);
        line(entry, consumer, max, min, min, max, min, max, color);
        line(entry, consumer, max, min, max, min, min, max, color);
        line(entry, consumer, min, min, max, min, min, min, color);

        line(entry, consumer, min, max, min, max, max, min, color);
        line(entry, consumer, max, max, min, max, max, max, color);
        line(entry, consumer, max, max, max, min, max, max, color);
        line(entry, consumer, min, max, max, min, max, min, color);

        line(entry, consumer, min, min, min, min, max, min, color);
        line(entry, consumer, max, min, min, max, max, min, color);
        line(entry, consumer, max, min, max, max, max, max, color);
        line(entry, consumer, min, min, max, min, max, max, color);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 1.0E-4F) {
            return;
        }

        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;
        vertex(entry, consumer, x1, y1, z1, nx, ny, nz, color);
        vertex(entry, consumer, x2, y2, z2, nx, ny, nz, color);
    }

    private static void vertex(MatrixStack.Entry entry, VertexConsumer consumer, float x, float y, float z, float normalX, float normalY, float normalZ, int color) {
        consumer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(color)
                .normal(entry, normalX, normalY, normalZ)
                .lineWidth(5.0F);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private record ChestStyle(int color, String label) {
    }

    private record OreTarget(BlockPos pos, ChestStyle style) {
    }

    private record ScreenPoint(double x, double y) {
    }

    private record ScreenBox(int left, int top, int right, int bottom) {
    }
}
