package net.revilodev.boundless.client.editor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.Config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads PNG quest icons from config/boundless/assets into the client texture manager. */
public final class QuestEditorCustomIcons {
    public static final String PREFIX = "boundless:custom_assets/";
    private static final Map<String, ResourceLocation> TEXTURES = new LinkedHashMap<>();
    private static boolean loaded;

    private QuestEditorCustomIcons() {}

    public static Path directory() {
        return Config.boundlessConfigRoot().resolve("assets");
    }

    public static List<String> ids() {
        ensureLoaded();
        return List.copyOf(TEXTURES.keySet());
    }

    public static ResourceLocation texture(String iconId) {
        if (iconId == null || !iconId.startsWith(PREFIX)) return null;
        ensureLoaded();
        return TEXTURES.get(iconId);
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : TEXTURES.values()) minecraft.getTextureManager().release(texture);
        TEXTURES.clear();
        loaded = false;
    }

    private static void ensureLoaded() {
        if (!loaded) reload();
    }

    public static void reload() {
        loaded = true;
        try {
            Files.createDirectories(directory());
            Map<String, ResourceLocation> found = new LinkedHashMap<>();
            try (var paths = Files.walk(directory())) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> load(path, found));
            }
            TEXTURES.keySet().removeIf(id -> !found.containsKey(id));
            TEXTURES.putAll(found);
        } catch (IOException ignored) {
        }
    }

    private static void load(Path path, Map<String, ResourceLocation> found) {
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            String relative = directory().relativize(path).toString().replace('\\', '/');
            String id = PREFIX + relative.substring(0, relative.length() - 4).toLowerCase(Locale.ROOT);
            ResourceLocation texture = TEXTURES.get(id);
            if (texture == null) {
                String key = Integer.toUnsignedString(path.toAbsolutePath().normalize().toString().hashCode(), 16);
                texture = Minecraft.getInstance().getTextureManager().register("boundless_custom/" + key, new DynamicTexture(image));
            } else {
                image.close();
            }
            found.put(id, texture);
        } catch (Exception ignored) {
        }
    }
}
