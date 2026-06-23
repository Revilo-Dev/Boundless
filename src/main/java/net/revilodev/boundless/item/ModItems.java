package net.revilodev.boundless.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.revilodev.boundless.BoundlessMod;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BoundlessMod.MOD_ID);

    public static final RegistryObject<Item> QUEST_BOOK =
            ITEMS.register("quest_book", () -> new QuestBookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> QUEST_COMPLETION_SCROLL =
            ITEMS.register("quest_completion_scroll", () -> new QuestCompletionScrollItem(new Item.Properties().stacksTo(1)));

    public static ItemStack createQuestScroll(String questId) {
        return QuestCompletionScrollItem.withQuestId(new ItemStack(QUEST_COMPLETION_SCROLL.get()), questId);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
