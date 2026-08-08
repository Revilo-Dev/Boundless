package net.revilodev.boundless.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.revilodev.boundless.client.editor.QuestEditorModels.ItemPickerTab;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerMode;
import net.revilodev.boundless.client.editor.QuestEditorModels.PickerPageData;
import net.revilodev.boundless.client.editor.QuestEditorModels.TagPageData;
import net.revilodev.boundless.client.editor.QuestEditorModels.TagPageEntry;
import net.revilodev.boundless.quest.QuestItemSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// item picker page data
public final class QuestEditorItemPickerData {
    private String cachedTagFilterQuery = "";
    private final List<String> cachedFilteredTagIds = new ArrayList<>();
    private final LinkedHashMap<String, PickerPageData> itemPickerPageCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PickerPageData> eldest) {
            return size() > 16;
        }
    };
    private final LinkedHashMap<String, TagPageData> itemTagPageCache = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TagPageData> eldest) {
            return size() > 8;
        }
    };
    private final LinkedHashMap<String, List<Item>> itemTagIconItemCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Item>> eldest) {
            return size() > 128;
        }
    };

    public Map<String, List<Item>> itemTagIconItemCache() {
        return itemTagIconItemCache;
    }

    public void invalidate() {
        cachedTagFilterQuery = "";
        cachedFilteredTagIds.clear();
        itemPickerPageCache.clear();
        itemTagPageCache.clear();
        itemTagIconItemCache.clear();
    }

    public String pickerSelectionId(ItemStack stack, PickerMode pickerMode, ItemPickerTab itemPickerTab, Minecraft minecraft) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return "";
        if (pickerMode == PickerMode.ITEMS && itemPickerTab == ItemPickerTab.INVENTORY) {
            String components = safe(QuestItemSpec.describeStackComponents(
                    stack,
                    minecraft == null || minecraft.level == null ? null : minecraft.level.registryAccess()
            )).trim();
            return components.isBlank() || "{}".equals(components) ? key.toString() : key + components;
        }
        return key.toString();
    }

    public PickerPageResult currentPickerPageData(PickerPageRequest request) {
        if (request == null) return new PickerPageResult(0, new PickerPageData(0, List.of(), List.of()));
        if (request.pickerMode == PickerMode.ITEMS && request.itemPickerTab == ItemPickerTab.TAGS) {
            TagPageResult tagPage = currentTagPageData(request.itemPickerPage, request.itemPickerSearchQuery, request.pageSize, request.suggestionCaches);
            List<ItemStack> items = new ArrayList<>(tagPage.data.entries().size());
            List<String> ids = new ArrayList<>(tagPage.data.entries().size());
            for (TagPageEntry entry : tagPage.data.entries()) {
                items.add(entry.icon());
                ids.add(entry.tagId());
            }
            return new PickerPageResult(tagPage.page, new PickerPageData(tagPage.data.totalCount(), items, ids));
        }

        String query = safe(request.itemPickerSearchQuery).trim().toLowerCase(Locale.ROOT);
        String cacheKey = request.pickerMode + "|" + request.itemPickerTab + "|" + request.itemPickerPage + "|" + query;
        if (request.pickerMode != PickerMode.ITEMS || request.itemPickerTab != ItemPickerTab.INVENTORY) {
            PickerPageData cached = itemPickerPageCache.get(cacheKey);
            if (cached != null) return new PickerPageResult(request.itemPickerPage, cached);
        }

        int start = request.itemPickerPage * request.pageSize;
        List<ItemStack> items = new ArrayList<>(request.pageSize);
        List<String> ids = new ArrayList<>(request.pageSize);
        int matched = 0;

        switch (request.pickerMode) {
            case ITEMS -> {
                if (request.itemPickerTab == ItemPickerTab.CREATIVE) {
                    for (String id : request.suggestionCaches.itemSuggestions()) {
                        if ("minecraft:air".equals(id)) continue;
                        if (!query.isBlank() && !id.contains(query)) continue;
                        ResourceLocation rl = ResourceLocation.tryParse(id);
                        if (rl == null) continue;
                        Item item = BuiltInRegistries.ITEM.get(rl);
                        if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
                        if (matched >= start && items.size() < request.pageSize) {
                            items.add(new ItemStack(item));
                            ids.add(id);
                        }
                        matched++;
                    }
                } else if (request.minecraft != null && request.minecraft.player != null) {
                    for (ItemStack stack : request.minecraft.player.getInventory().items) {
                        if (stack == null || stack.isEmpty()) continue;
                        if (stack.getItem() == net.minecraft.world.item.Items.AIR) continue;
                        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        if (!query.isBlank() && !id.contains(query)) continue;
                        if (matched >= start && items.size() < request.pageSize) {
                            items.add(stack.copy());
                            ids.add(id);
                        }
                        matched++;
                    }
                }
            }
            case EFFECTS -> {
                for (String effectId : request.suggestionCaches.effectSuggestions()) {
                    if (!query.isBlank() && !effectId.toLowerCase(Locale.ROOT).contains(query)) continue;
                    if (matched >= start && items.size() < request.pageSize) {
                        items.add(QuestEditorItemIcons.effectIconStack(effectId));
                        ids.add(effectId);
                    }
                    matched++;
                }
            }
            case MOBS -> {
                for (String mobId : request.suggestionCaches.entitySuggestions()) {
                    if (!request.suggestionCaches.isSelectableMobEntityId(mobId)) continue;
                    if (!query.isBlank() && !mobId.toLowerCase(Locale.ROOT).contains(query)) continue;
                    if (matched >= start && items.size() < request.pageSize) {
                        ItemStack egg = QuestEditorItemIcons.mobEggIconStack(mobId);
                        items.add(egg.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.EGG) : egg);
                        ids.add(mobId);
                    }
                    matched++;
                }
            }
            default -> {
            }
        }

        PickerPageData created = new PickerPageData(matched, items, ids);
        if (request.pickerMode != PickerMode.ITEMS || request.itemPickerTab != ItemPickerTab.INVENTORY) {
            itemPickerPageCache.put(cacheKey, created);
        }
        return new PickerPageResult(request.itemPickerPage, created);
    }

    private TagPageResult currentTagPageData(int itemPickerPage, String itemPickerSearchQuery, int pageSize, QuestEditorSuggestionCaches suggestionCaches) {
        List<String> filtered = itemTagPickerIds(itemPickerSearchQuery, suggestionCaches);
        int maxPage = Math.max(0, (filtered.size() - 1) / pageSize);
        int page = Math.max(0, Math.min(itemPickerPage, maxPage));
        String cacheKey = page + "|" + safe(itemPickerSearchQuery).trim().toLowerCase(Locale.ROOT);
        TagPageData cached = itemTagPageCache.get(cacheKey);
        if (cached != null) return new TagPageResult(page, cached);
        int start = page * pageSize;
        int end = Math.min(filtered.size(), start + pageSize);
        List<TagPageEntry> entries = new ArrayList<>();
        for (int i = start; i < end; i++) {
            String tagId = filtered.get(i);
            entries.add(new TagPageEntry(tagId, QuestEditorItemIcons.itemTagIconStack(tagId, itemTagIconItemCache), Component.literal(tagId)));
        }
        TagPageData created = new TagPageData(filtered.size(), entries);
        itemTagPageCache.put(cacheKey, created);
        return new TagPageResult(page, created);
    }

    private List<String> itemTagPickerIds(String itemPickerSearchQuery, QuestEditorSuggestionCaches suggestionCaches) {
        String query = safe(itemPickerSearchQuery).trim().toLowerCase(Locale.ROOT);
        if (query.equals(cachedTagFilterQuery) && !cachedFilteredTagIds.isEmpty()) {
            return cachedFilteredTagIds;
        }
        cachedTagFilterQuery = query;
        cachedFilteredTagIds.clear();
        for (String tagId : suggestionCaches.itemTagSuggestions()) {
            String lower = tagId.toLowerCase(Locale.ROOT);
            if (!query.isBlank() && !lower.contains(query)) continue;
            cachedFilteredTagIds.add(tagId);
        }
        return cachedFilteredTagIds;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // picker page request
    public static final class PickerPageRequest {
        public final PickerMode pickerMode;
        public final ItemPickerTab itemPickerTab;
        public final int itemPickerPage;
        public final String itemPickerSearchQuery;
        public final int pageSize;
        public final QuestEditorSuggestionCaches suggestionCaches;
        public final Minecraft minecraft;

        public PickerPageRequest(PickerMode pickerMode, ItemPickerTab itemPickerTab, int itemPickerPage, String itemPickerSearchQuery,
                                 int pageSize, QuestEditorSuggestionCaches suggestionCaches, Minecraft minecraft) {
            this.pickerMode = pickerMode;
            this.itemPickerTab = itemPickerTab;
            this.itemPickerPage = itemPickerPage;
            this.itemPickerSearchQuery = itemPickerSearchQuery == null ? "" : itemPickerSearchQuery;
            this.pageSize = pageSize;
            this.suggestionCaches = suggestionCaches;
            this.minecraft = minecraft;
        }
    }

    // picker page result
    public record PickerPageResult(int page, PickerPageData data) {}

    private record TagPageResult(int page, TagPageData data) {}
}
