package net.minecraft.network.protocol.common.custom;

import net.minecraft.resources.ResourceLocation;

public interface CustomPacketPayload {
    Type<?> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
