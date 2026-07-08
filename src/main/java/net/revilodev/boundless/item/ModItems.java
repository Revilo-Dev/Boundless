package net.revilodev.boundless.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.revilodev.boundless.BoundlessMod;

public final class ModItems {
    public static final Item QUEST_BOOK = new QuestBookItem(new Item.Properties().stacksTo(1));
    public static final Item QUEST_COMPLETION_SCROLL = new QuestCompletionScrollItem(new Item.Properties().stacksTo(1));

    public static ItemStack createQuestScroll(String questId) {
        return QuestCompletionScrollItem.withQuestId(new ItemStack(QUEST_COMPLETION_SCROLL), questId);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BoundlessMod.MOD_ID, "quest_book"), QUEST_BOOK);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BoundlessMod.MOD_ID, "quest_completion_scroll"), QUEST_COMPLETION_SCROLL);
    }
}
