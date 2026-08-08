package net.revilodev.boundless.client.editor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.quest.QuestItemSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// editor item and mob icons
public final class QuestEditorItemIcons {
    private QuestEditorItemIcons() {
    }

    // icon stack from id
    public static ItemStack iconStackFromId(String raw) {
        if (raw == null || raw.isBlank()) return ItemStack.EMPTY;
        try {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(raw)));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    // strip tag prefix
    public static String stripTagPrefix(String tagId) {
        String value = safe(tagId).trim();
        return value.startsWith("#") ? value.substring(1).trim() : value;
    }

    // item tag icon stack
    public static ItemStack itemTagIconStack(String tagId, Map<String, List<Item>> itemTagIconItemCache) {
        String normalized = QuestEditorEntryCodec.normalizeNamespacedId(tagId, true);
        String clean = stripTagPrefix(normalized);
        ResourceLocation tagRl = ResourceLocation.tryParse(clean);
        if (tagRl == null) return ItemStack.EMPTY;
        try {
            List<Item> matching = itemTagIconItemCache == null ? null : itemTagIconItemCache.get(tagId);
            if (matching == null) {
                TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagRl);
                matching = new ArrayList<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    ItemStack stack = new ItemStack(item);
                    if (stack.is(tag)) matching.add(item);
                }
                if (itemTagIconItemCache != null) itemTagIconItemCache.put(tagId, matching);
            }
            if (!matching.isEmpty()) {
                int index = (int) ((System.currentTimeMillis() / 900L) % matching.size());
                ItemStack stack = new ItemStack(matching.get(index));
                stack.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.literal(tagId));
                return stack;
            }
        } catch (Exception ignored) {
        }
        ItemStack fallback = new ItemStack(net.minecraft.world.item.Items.PAPER);
        fallback.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.literal(tagId));
        return fallback;
    }

    // render effect picker icon
    public static void renderEffectPickerIcon(GuiGraphics gg, ItemStack stack, int x, int y) {
        String effectId = effectIdFromStack(stack, List.of());
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl != null) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/effects/" + rl.getPath() + ".png");
            gg.blit(tex, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            gg.renderItem(stack, x, y);
        }
    }

    // effect icon stack
    public static ItemStack effectIconStack(String effectId) {
        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl != null) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("boundless",
                    "textures/gui/effects/" + rl.getPath() + ".png");
            ItemStack stack = new ItemStack(net.minecraft.world.item.Items.POTION);
            if (tex != null) {
                stack.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.literal(effectId));
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    // mob egg icon stack
    public static ItemStack mobEggIconStack(String entityId) {
        ResourceLocation entityRl = ResourceLocation.tryParse(entityId);
        if (entityRl == null) return ItemStack.EMPTY;
        ResourceLocation eggRl = ResourceLocation.fromNamespaceAndPath(entityRl.getNamespace(), entityRl.getPath() + "_spawn_egg");
        if (BuiltInRegistries.ITEM.containsKey(eggRl)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(eggRl));
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof net.minecraft.world.item.SpawnEggItem)) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && key.getPath().contains(entityRl.getPath())) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    // mob display name for mob id
    public static Component mobDisplayNameForMobId(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription();
        }
        return Component.literal(id == null ? "" : id);
    }

    // effect display name for picker item
    public static Component effectDisplayNameForPickerItem(ItemStack stack, List<String> effectIds) {
        String id = effectIdFromStack(stack, effectIds);
        return Component.literal(effectDisplayName(id));
    }

    // effect display name
    public static String effectDisplayName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null && BuiltInRegistries.MOB_EFFECT.containsKey(rl)) {
            return BuiltInRegistries.MOB_EFFECT.get(rl).getDisplayName().getString();
        }
        return id == null ? "" : id;
    }

    // effect id from stack
    public static String effectIdFromStack(ItemStack stack, List<String> effectIds) {
        if (stack == null || stack.isEmpty()) return "";
        String name = stack.getHoverName().getString();
        for (String effectId : effectIds) {
            if (effectId.equals(name)) return effectId;
        }
        return name;
    }

    // mob id from stack
    public static String mobIdFromStack(ItemStack stack, List<String> entityIds) {
        if (stack == null || stack.isEmpty()) return "";
        Item item = stack.getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) return "";
        String path = key.getPath();
        if (!path.endsWith("_spawn_egg")) return "";
        String entityPath = path.substring(0, path.length() - "_spawn_egg".length());
        String id = key.getNamespace() + ":" + entityPath;
        return entityIds.contains(id) ? id : "";
    }

    // display name for item
    public static String displayNameForItem(String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return itemId;
        return new ItemStack(BuiltInRegistries.ITEM.get(rl)).getHoverName().getString();
    }

    // selected item stack
    public static ItemStack selectedItemStack(String idPart, String type, Map<String, List<Item>> itemTagIconItemCache) {
        String strippedId = QuestItemSpec.stripComponents(idPart);
        if (strippedId.startsWith("#")) return itemTagIconStack(strippedId, itemTagIconItemCache);
        if ("effect".equals(type)) return effectIconStack(strippedId);
        if ("kill".equals(type) || "entity".equals(type)) return mobEggIconStack(strippedId);
        ResourceLocation rl = ResourceLocation.tryParse(QuestEditorEntryCodec.normalizeNamespacedId(strippedId, false));
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(rl));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
