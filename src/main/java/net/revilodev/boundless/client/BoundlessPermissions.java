package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BoundlessPermissions {
    private BoundlessPermissions() {
    }

    public static boolean canManageQuests() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        if (minecraft.getSingleplayerServer() != null) {
            ServerPlayer serverPlayer = minecraft.getSingleplayerServer().getPlayerList()
                    .getPlayer(minecraft.player.getUUID());
            return serverPlayer != null && serverPlayer.createCommandSourceStack().hasPermission(2);
        }
        return minecraft.player.hasPermissions(2);
    }
}
