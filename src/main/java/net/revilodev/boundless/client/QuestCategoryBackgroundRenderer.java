package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.editor.QuestEditorCustomIcons;

public final class QuestCategoryBackgroundRenderer {
    private static final int BASE_TILE_SIZE = 32;
    private static final int SOURCE_TILE_SIZE = 16;

    private QuestCategoryBackgroundRenderer() {
    }

    public static void render(GuiGraphics graphics, String background, int x, int y, int width, int height) {
        render(graphics, background, false, 0f, 0.5f, x, y, width, height);
    }

    public static void render(GuiGraphics graphics, String background, boolean darkening, float darkeningOpacity,
                              int x, int y, int width, int height) {
        render(graphics, background, darkening, darkeningOpacity, 0.5f, x, y, width, height);
    }

    public static void render(GuiGraphics graphics, String background, boolean darkening, float darkeningOpacity,
                              float tileScale, int x, int y, int width, int height) {
        if (!Config.customBackgrounds() || background == null || background.isBlank() || width <= 0 || height <= 0) return;
        int tileSize = Math.max(3, Math.round(BASE_TILE_SIZE * Math.max(0.1f, Math.min(2f, tileScale))));
        String value = background.trim();
        if (value.startsWith("custom_tile:")) {
            renderTexture(graphics, value.substring("custom_tile:".length()), x, y, width, height, true, tileSize);
        } else if (value.startsWith("full:")) {
            renderTexture(graphics, value.substring("full:".length()), x, y, width, height, false, tileSize);
        } else {
            if (value.startsWith("tile:")) value = value.substring("tile:".length());
            Block block = block(value);
            if (block != null && block != Blocks.AIR) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
                        .getBlockModelShaper().getParticleIcon(block.defaultBlockState());
                graphics.enableScissor(x, y, x + width, y + height);
                for (int tileY = y; tileY < y + height; tileY += tileSize) {
                    for (int tileX = x; tileX < x + width; tileX += tileSize) {
                        graphics.blit(tileX, tileY, 0, tileSize, tileSize, sprite);
                    }
                }
                graphics.disableScissor();
            }
        }
        if (darkening) {
            int alpha = Math.max(0, Math.min(255, Math.round(darkeningOpacity * 255f)));
            if (alpha > 0) graphics.fill(x, y, x + width, y + height, alpha << 24);
        }
    }

    private static void renderTexture(GuiGraphics graphics, String assetId, int x, int y, int width, int height,
                                      boolean tiled, int tileSize) {
        ResourceLocation texture = QuestEditorCustomIcons.texture(assetId.trim());
        if (texture == null) return;
        graphics.enableScissor(x, y, x + width, y + height);
        if (!tiled) {
            graphics.blit(texture, x, y, 0, 0, width, height, width, height);
        } else {
            float scale = tileSize / (float) SOURCE_TILE_SIZE;
            for (int tileY = y; tileY < y + height; tileY += tileSize) {
                for (int tileX = x; tileX < x + width; tileX += tileSize) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(tileX, tileY, 0);
                    graphics.pose().scale(scale, scale, 1f);
                    graphics.blit(texture, 0, 0, 0, 0, SOURCE_TILE_SIZE, SOURCE_TILE_SIZE,
                            SOURCE_TILE_SIZE, SOURCE_TILE_SIZE);
                    graphics.pose().popPose();
                }
            }
        }
        graphics.disableScissor();
    }

    private static Block block(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        try {
            ResourceLocation id = ResourceLocation.parse(rawId.trim());
            return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
