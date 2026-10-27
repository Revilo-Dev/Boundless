package net.revilodev.boundless.client.editor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.Config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// shared emoji names and textures used by editor text and icon pickers
public final class QuestEditorEmojis {
    public static final String ICON_PREFIX = "boundless:emoji/";
    private static final String RESOURCE_PATH = "textures/icon/emoji";
    private static final int GLYPH_SIZE = 9;
    private static List<String> cachedNames;
    private static Set<String> cachedNameSet;
    private static Map<String, ResourceLocation> cachedTextures;
    private static final Set<ResourceLocation> CUSTOM_TEXTURES = new HashSet<>();

    private QuestEditorEmojis() {}

    public static List<String> names() {
        if (cachedNames != null) return cachedNames;
        Map<String, ResourceLocation> discovered = new LinkedHashMap<>();
        Minecraft.getInstance().getResourceManager().listResources("textures", path -> isEmojiTexture(path.getPath()))
                .keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(texture -> {
                    String name = emojiName(texture);
                    if (name.isBlank()) return;
                    String qualified = texture.getNamespace() + ":" + name;
                    discovered.putIfAbsent(qualified, texture);
                    if ("boundless".equals(texture.getNamespace())) discovered.putIfAbsent(name, texture);
                });
        discoverCustomEmojis(discovered);
        cachedTextures = Map.copyOf(discovered);
        cachedNames = discovered.keySet().stream()
                .filter(name -> !name.startsWith("boundless:"))
                .sorted(Comparator.naturalOrder())
                .toList();
        cachedNameSet = Set.copyOf(cachedNames);
        return cachedNames;
    }

    public static int tokenEnd(String text, int start) {
        if (text == null || start < 0 || start >= text.length() || text.charAt(start) != ':') return -1;
        names();
        int end = start;
        while ((end = text.indexOf(':', end + 1)) >= 0 && end - start <= 120) {
            if (end > start + 1 && cachedNameSet.contains(text.substring(start + 1, end))) return end;
        }
        return -1;
    }

    public static ResourceLocation texture(String name) {
        names();
        ResourceLocation texture = cachedTextures.get(name);
        return texture == null ? ResourceLocation.fromNamespaceAndPath("boundless", RESOURCE_PATH + "/" + name + ".png") : texture;
    }
    public static int glyphSize() { return GLYPH_SIZE; }
    public static String text(String name) { return ":" + name + ":"; }
    public static String iconId(String name) { return ICON_PREFIX + name; }
    public static String nameFromIconId(String id) { return id != null && id.startsWith(ICON_PREFIX) ? id.substring(ICON_PREFIX.length()) : null; }

    public static void clearCache() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : CUSTOM_TEXTURES) minecraft.getTextureManager().release(texture);
        CUSTOM_TEXTURES.clear();
        cachedNames = null;
        cachedNameSet = null;
        cachedTextures = null;
    }

    public static Path directory() {
        return Config.emojiRoot();
    }

    private static void discoverCustomEmojis(Map<String, ResourceLocation> discovered) {
        try {
            Files.createDirectories(directory());
            try (var paths = Files.walk(directory())) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> loadCustomEmoji(path, discovered));
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadCustomEmoji(Path path, Map<String, ResourceLocation> discovered) {
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() != image.getHeight()) {
                image.close();
                return;
            }
            String relative = directory().relativize(path).toString().replace('\\', '/');
            String name = relative.substring(0, relative.length() - 4).toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_./-]", "_");
            ResourceLocation texture = Minecraft.getInstance().getTextureManager().register(
                    "boundless_custom_emoji/" + Integer.toHexString(path.toAbsolutePath().toString().hashCode()),
                    new DynamicTexture(image));
            CUSTOM_TEXTURES.add(texture);
            discovered.put(name, texture);
        } catch (Exception ignored) {
        }
    }

    private static boolean isEmojiTexture(String path) {
        if (path == null || !path.endsWith(".png") || path.endsWith("/emojis.png")) return false;
        return path.startsWith(RESOURCE_PATH + "/") || path.startsWith("textures/emoji/")
                || path.contains("/emoji/") || path.contains("/emojis/");
    }

    private static String emojiName(ResourceLocation texture) {
        String path = texture.getPath();
        int marker = path.lastIndexOf("/emoji/");
        int pluralMarker = path.lastIndexOf("/emojis/");
        int start = Math.max(marker < 0 ? -1 : marker + 7, pluralMarker < 0 ? -1 : pluralMarker + 8);
        if (start < 0 && path.startsWith("textures/emoji/")) start = "textures/emoji/".length();
        if (start < 0 || !path.endsWith(".png")) return "";
        return path.substring(start, path.length() - 4);
    }
}
