package net.revilodev.boundless.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.revilodev.boundless.Config;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class QuestBookKeybinds {
    private static final String CATEGORY = "key.categories.boundless";
    private static final String KEY_OPEN = "key.boundless.open_quest_book";
    private static KeyMapping openQuestBook;
    private static boolean registered = false;

    private QuestBookKeybinds() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (registered) return;
        registered = true;
        if (openQuestBook == null) {
            openQuestBook = new KeyMapping(KEY_OPEN, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);
        }
        event.register(openQuestBook);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (openQuestBook == null) return;
        if (Config.disableQuestBook()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        while (openQuestBook.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new net.revilodev.boundless.client.screen.StandaloneQuestBookScreen());
            }
        }
    }
}
