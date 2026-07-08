package net.revilodev.boundless.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.revilodev.boundless.Config;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class QuestBookKeybinds {
    private static final String CATEGORY = "key.categories.boundless";
    private static final String KEY_OPEN = "key.boundless.open_quest_book";
    private static KeyMapping openQuestBook;
    private static boolean registered = false;

    private QuestBookKeybinds() {}

    public static KeyMapping openQuestBook() {
        if (openQuestBook == null) {
            openQuestBook = new KeyMapping(KEY_OPEN, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);
        }
        return openQuestBook;
    }

    public static void onClientTick(Minecraft mc) {
        if (openQuestBook == null) return;
        if (Config.disableQuestBook()) return;

        if (mc.player == null || mc.level == null) return;

        while (openQuestBook.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new net.revilodev.boundless.client.screen.StandaloneQuestBookScreen());
            }
        }
    }
}


