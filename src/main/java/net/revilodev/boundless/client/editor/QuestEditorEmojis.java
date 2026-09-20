package net.revilodev.boundless.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

// shared emoji names and textures used by editor text and icon pickers
public final class QuestEditorEmojis {
    public static final String ICON_PREFIX = "boundless:emoji/";
    private static final String RESOURCE_PATH = "textures/icon/emoji";
    private static List<String> cachedNames;
    private static Set<String> cachedNameSet;

    private QuestEditorEmojis() {}

    public static List<String> names() {
        if (cachedNames != null) return cachedNames;
        cachedNames = Minecraft.getInstance().getResourceManager().listResources(RESOURCE_PATH, path -> path.getPath().endsWith(".png"))
                .keySet().stream()
                .filter(id -> !id.getPath().endsWith("/emojis.png"))
                .map(id -> id.getPath().substring(RESOURCE_PATH.length() + 1, id.getPath().length() - 4))
                .sorted(Comparator.naturalOrder())
                .toList();
        cachedNameSet = Set.copyOf(cachedNames);
        return cachedNames;
    }

    public static int tokenEnd(String text, int start) {
        if (text == null || start < 0 || start >= text.length() || text.charAt(start) != ':') return -1;
        int end = text.indexOf(':', start + 1);
        if (end <= start + 1 || end - start > 80) return -1;
        names();
        return cachedNameSet.contains(text.substring(start + 1, end)) ? end : -1;
    }

    public static ResourceLocation texture(String name) { return ResourceLocation.fromNamespaceAndPath("boundless", RESOURCE_PATH + "/" + name + ".png"); }
    public static String text(String name) { return ":" + name + ":"; }
    public static String iconId(String name) { return ICON_PREFIX + name; }
    public static String nameFromIconId(String id) { return id != null && id.startsWith(ICON_PREFIX) ? id.substring(ICON_PREFIX.length()) : null; }
}
