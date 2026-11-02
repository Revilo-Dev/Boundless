package net.revilodev.boundless.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.revilodev.boundless.BoundlessMod;

// tells the client which quest completion toast to show
public record QuestToastPayload(String questId) {
    // Small client payload sent when a quest becomes available.
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BoundlessMod.MOD_ID, "quest_toast");

    public static final StreamCodec<FriendlyByteBuf, QuestToastPayload> CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeUtf(msg.questId),
                    buf -> new QuestToastPayload(buf.readUtf())
            );
}
