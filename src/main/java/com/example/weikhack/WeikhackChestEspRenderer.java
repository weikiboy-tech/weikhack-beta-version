package com.example.weikhack;

import com.example.weikhack.mixin.RenderLayerAccessor;
import com.example.weikhack.mixin.RenderPipelinesAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
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
    private static final ChestStyle NORMAL_CHEST = new ChestStyle(0xFF34F85A, "Chest");
    private static final ChestStyle TRAPPED_CHEST = new ChestStyle(0xFFFF5555, "Trapped Chest");
    private static final ChestStyle ENDER_CHEST = new ChestStyle(0xFFB66CFF, "Ender Chest");
    private static final ChestStyle BARREL = new ChestStyle(0xFFFFC857, "Barrel");
    private static final ChestStyle SHULKER = new ChestStyle(0xFF55D7FF, "Shulker");
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
    private static BlockPos lastBarrelScanCenter;
    private static long nextBarrelScanTick;

    private WeikhackChestEspRenderer() {
    }

    public static void render(VertexConsumerProvider.Immediate vertexConsumers, MatrixStack matrices, WorldRenderState renderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!WeikhackMod.isChestEspEnabled() || client.world == null || client.player == null || renderState.cameraRenderState == null) {
            return;
        }

        Vec3d cameraPos = renderState.cameraRenderState.pos;
        if (cameraPos == null) {
            cameraPos = client.gameRenderer.getCamera().getCameraPos();
        }

        VertexConsumer chestLines = vertexConsumers.getBuffer(CHEST_XRAY_LINES);
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
        if (!WeikhackMod.isChestEspEnabled() || client.world == null || client.player == null || client.textRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        double verticalFov = Math.toRadians(client.options.getFov().getValue());
        double focalLength = height / (2.0D * Math.tan(verticalFov / 2.0D));

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

    private record ScreenPoint(double x, double y) {
    }

    private record ScreenBox(int left, int top, int right, int bottom) {
    }
}
