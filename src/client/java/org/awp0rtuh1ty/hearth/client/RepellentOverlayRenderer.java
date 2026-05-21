package org.awp0rtuh1ty.hearth.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.awp0rtuh1ty.hearth.block.RepellentBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class RepellentOverlayRenderer {

    private RepellentOverlayRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(RepellentOverlayRenderer::onHudRender);
    }

    private static void onHudRender(GuiGraphics drawContext, DeltaTracker tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.level == null) return;
        if (client.screen != null) return;

        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = client.level.getBlockEntity(pos);
        if (be == null) return;

        List<Component> lines = new ArrayList<>();

        if (be instanceof RepellentBlockEntity repellent) {
            buildRepellentLines(repellent, lines);
        }

        if (lines.isEmpty()) return;

        renderTooltip(drawContext, client, lines);
    }


    private static void buildRepellentLines(RepellentBlockEntity repellent, List<Component> lines) {
        int ticks = repellent.getRemainingTicks();
        int potionCount = repellent.getPotionCount();

        if (ticks > 0) {
            int seconds = ticks / 20;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            lines.add(Component.translatable("hud.hearth.repellent.remaining", String.format("%d:%02d", minutes, seconds)));
        }
        if (potionCount > 0) {
            lines.add(Component.translatable("hud.hearth.repellent.potions", potionCount));
        }
    }

    private static void renderTooltip(GuiGraphics drawContext, Minecraft client, List<Component> lines) {
        Font font = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int crosshairY = screenHeight / 2;

        // Measure text dimensions
        int maxWidth = 0;
        for (Component line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int lineHeight = font.lineHeight + 2;
        int padding = 4;
        int bgWidth = maxWidth + padding * 2;
        int bgHeight = lines.size() * lineHeight + padding * 2;
        int bgX = centerX - bgWidth / 2;
        int bgY = crosshairY + 14; // below crosshair

        // Draw semi-transparent background
        drawContext.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xC0101010);
        drawContext.fill(bgX - 1, bgY - 1, bgX + bgWidth + 1, bgY, 0xFF404040);
        drawContext.fill(bgX - 1, bgY + bgHeight, bgX + bgWidth + 1, bgY + bgHeight + 1, 0xFF404040);
        drawContext.fill(bgX - 1, bgY, bgX, bgY + bgHeight, 0xFF404040);
        drawContext.fill(bgX + bgWidth, bgY, bgX + bgWidth + 1, bgY + bgHeight, 0xFF404040);

        // Draw text lines
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int textWidth = font.width(line);
            drawContext.drawString(font, line,
                    centerX - textWidth / 2,
                    bgY + padding + i * lineHeight,
                    0xFFFFFF, true);
        }
    }
}
