package net.revilodev.boundless.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.BoundlessMod;

public record QuestToastPayload(String questId) {
    public static final ResourceLocation ID =
            new ResourceLocation(BoundlessMod.MOD_ID, "quest_toast");

    public static void encode(QuestToastPayload msg, FriendlyByteBuf buf) { buf.writeUtf(msg.questId); }
    public static QuestToastPayload decode(FriendlyByteBuf buf) { return new QuestToastPayload(buf.readUtf()); }
}
