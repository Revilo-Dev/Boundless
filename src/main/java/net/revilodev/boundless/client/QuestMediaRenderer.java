package net.revilodev.boundless.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.Config;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestMediaRenderer {
    private static final Map<Path, MediaAsset> CACHE = new HashMap<>();
    private static final Map<Path, Long> RESTART_TIMES = new HashMap<>();

    private QuestMediaRenderer() {
    }

    public static Path directory() {
        return Config.questMediaRoot();
    }

    public static int preferredHeight(String reference, int maxWidth) {
        MediaAsset asset = asset(reference);
        if (asset == null || maxWidth <= 0) return 0;
        return Math.max(1, Math.min(112, Math.round(maxWidth * (asset.height / (float) asset.width))));
    }

    public static RenderResult render(GuiGraphics graphics, String reference, int x, int y, int maxWidth) {
        MediaAsset asset = asset(reference);
        if (asset == null || asset.frames.isEmpty() || maxWidth <= 0) return RenderResult.EMPTY;
        int height = preferredHeight(reference, maxWidth);
        int width = Math.max(1, Math.min(maxWidth, Math.round(height * (asset.width / (float) asset.height))));
        int drawX = x + (maxWidth - width) / 2;
        MediaFrame frame = asset.frameAt(Util.getMillis() - RESTART_TIMES.getOrDefault(asset.path, 0L));
        graphics.blit(frame.texture, drawX, y, width, height, 0, 0, asset.width, asset.height, asset.width, asset.height);
        boolean animated = asset.frames.size() > 1;
        if (animated) {
            int buttonX = drawX + width - 13;
            int buttonY = y + height - 13;
            graphics.fill(buttonX, buttonY, buttonX + 11, buttonY + 11, 0xB0000000);
            graphics.drawString(Minecraft.getInstance().font, "↻", buttonX + 2, buttonY + 1, 0xFFFFFFFF, false);
            return new RenderResult(height, buttonX, buttonY, 11, 11, asset.path);
        }
        return new RenderResult(height, 0, 0, 0, 0, asset.path);
    }

    public static void restart(Path path) {
        if (path != null) RESTART_TIMES.put(path, Util.getMillis());
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (MediaAsset asset : CACHE.values()) {
            if (asset == null) continue;
            for (MediaFrame frame : asset.frames) minecraft.getTextureManager().release(frame.texture);
        }
        CACHE.clear();
        RESTART_TIMES.clear();
    }

    private static MediaAsset asset(String reference) {
        Path path = resolve(reference);
        if (path == null) return null;
        return CACHE.computeIfAbsent(path, QuestMediaRenderer::load);
    }

    private static Path resolve(String reference) {
        if (reference == null || reference.isBlank()) return null;
        try {
            Path root = directory().toAbsolutePath().normalize();
            Path path = root.resolve(reference.replace('\\', '/')).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) return null;
            return path;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static MediaAsset load(Path path) {
        try {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".gif")) return loadGif(path);
            try (InputStream input = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(input);
                ResourceLocation texture = register(path, 0, image);
                return new MediaAsset(path, image.getWidth(), image.getHeight(), List.of(new MediaFrame(texture, Long.MAX_VALUE)));
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static MediaAsset loadGif(Path path) throws IOException {
        List<MediaFrame> frames = new ArrayList<>();
        int width = 0;
        int height = 0;
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, false);
                int count = reader.getNumImages(true);
                for (int index = 0; index < count; index++) {
                    BufferedImage buffered = reader.read(index);
                    width = buffered.getWidth();
                    height = buffered.getHeight();
                    ByteArrayOutputStream png = new ByteArrayOutputStream();
                    ImageIO.write(buffered, "png", png);
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
                    frames.add(new MediaFrame(register(path, index, image), gifDelay(reader.getImageMetadata(index))));
                }
            } finally {
                reader.dispose();
            }
        }
        return frames.isEmpty() ? null : new MediaAsset(path, width, height, List.copyOf(frames));
    }

    private static ResourceLocation register(Path path, int frame, NativeImage image) {
        String key = Integer.toHexString(path.toString().hashCode()) + "/" + frame;
        return Minecraft.getInstance().getTextureManager().register("boundless_media/" + key, new DynamicTexture(image));
    }

    private static long gifDelay(IIOMetadata metadata) {
        try {
            Node root = metadata.getAsTree("javax_imageio_gif_image_1.0");
            Node control = findNode(root, "GraphicControlExtension");
            NamedNodeMap attributes = control == null ? null : control.getAttributes();
            Node delay = attributes == null ? null : attributes.getNamedItem("delayTime");
            return Math.max(20L, Long.parseLong(delay.getNodeValue()) * 10L);
        } catch (Exception ignored) {
            return 100L;
        }
    }

    private static Node findNode(Node node, String name) {
        if (node == null) return null;
        if (name.equals(node.getNodeName())) return node;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node found = findNode(child, name);
            if (found != null) return found;
        }
        return null;
    }

    public record RenderResult(int height, int restartX, int restartY, int restartWidth, int restartHeight, Path path) {
        private static final RenderResult EMPTY = new RenderResult(0, 0, 0, 0, 0, null);

        public boolean restartContains(double mouseX, double mouseY) {
            return restartWidth > 0 && mouseX >= restartX && mouseX <= restartX + restartWidth
                    && mouseY >= restartY && mouseY <= restartY + restartHeight;
        }
    }

    private record MediaFrame(ResourceLocation texture, long duration) {
    }

    private record MediaAsset(Path path, int width, int height, List<MediaFrame> frames) {
        MediaFrame frameAt(long elapsed) {
            if (frames.size() == 1) return frames.getFirst();
            long duration = 0L;
            for (MediaFrame frame : frames) duration += frame.duration;
            long time = Math.floorMod(elapsed, Math.max(1L, duration));
            for (MediaFrame frame : frames) {
                if (time < frame.duration) return frame;
                time -= frame.duration;
            }
            return frames.getLast();
        }
    }
}
