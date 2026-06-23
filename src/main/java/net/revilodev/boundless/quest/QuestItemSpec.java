package net.revilodev.boundless.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Locale;

public final class QuestItemSpec {
    public final String raw;
    public final String id;
    public final String components;
    public final boolean tag;

    private QuestItemSpec(String raw, String id, String components, boolean tag) {
        this.raw = raw == null ? "" : raw;
        this.id = id == null ? "" : id;
        this.components = components == null ? "" : components;
        this.tag = tag;
    }

    public static QuestItemSpec parse(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) return new QuestItemSpec("", "", "", false);

        int componentStart = componentStart(value);
        String idPart = componentStart >= 0 ? value.substring(0, componentStart).trim() : value;
        String components = componentStart >= 0 ? value.substring(componentStart).trim() : "";
        boolean tag = idPart.startsWith("#");
        String id = tag ? idPart.substring(1).trim() : idPart;
        if (!id.isBlank() && !id.contains(":")) id = "minecraft:" + id;
        id = id.toLowerCase(Locale.ROOT);
        return new QuestItemSpec(value, id, components, tag);
    }

    public String serialized() {
        return (tag ? "#" : "") + id + components;
    }

    public String commandSyntax() {
        if (components.length() >= 2 && components.charAt(0) == '{' && components.charAt(components.length() - 1) == '}') {
            return (tag ? "#" : "") + id + "[" + components.substring(1, components.length() - 1) + "]";
        }
        return serialized();
    }

    public ResourceLocation idLocation() {
        return ResourceLocation.tryParse(id);
    }

    public Item item() {
        ResourceLocation rl = idLocation();
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    public boolean matches(ItemStack stack) {
        return matches(stack, null);
    }

    public boolean matches(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation rl = idLocation();
        if (rl == null) return false;
        if (tag) {
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, rl);
            if (!stack.is(itemTag)) return false;
        } else {
            Item item = item();
            if (item == null || !stack.is(item)) return false;
        }
        if (components.isBlank()) return true;
        return componentsMatch(stack, registries);
    }

    public boolean componentsMatch(ItemStack stack, HolderLookup.Provider registries) {
        if (components.isBlank()) return true;
        return componentMatches(components, describeStackComponents(stack, registries));
    }

    public static String stripComponents(String raw) {
        QuestItemSpec spec = parse(raw);
        return (spec.tag ? "#" : "") + spec.id;
    }

    public static String describeStackComponents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        try {
            Method m = stack.getClass().getMethod("getComponentsPatch");
            Object patch = m.invoke(stack);
            return patch == null ? "" : patch.toString();
        } catch (Exception ignored) {
        }
        try {
            Method m = stack.getClass().getMethod("getComponents");
            Object comps = m.invoke(stack);
            return comps == null ? "" : comps.toString();
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String describeStackComponents(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return "";
        if (stack.hasTag() && stack.getTag() != null) {
            return stack.getTag().toString();
        }
        return describeStackComponents(stack);
    }

    private static boolean componentMatches(String expected, String actual) {
        String e = normalizeComponents(expected);
        String a = normalizeComponents(actual);
        if (e.isBlank()) return true;
        if (a.isBlank()) return false;
        return a.equals(e) || a.contains(e);
    }

    private static String normalizeComponents(String value) {
        String s = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s.replace(" ", "");
    }

    private static int componentStart(String value) {
        int brace = value.indexOf('{');
        int bracket = value.indexOf('[');
        if (brace < 0) return bracket;
        if (bracket < 0) return brace;
        return Math.min(brace, bracket);
    }
}
