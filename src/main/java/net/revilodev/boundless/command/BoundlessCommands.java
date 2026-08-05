package net.revilodev.boundless.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BoundlessCommands {
    // local quest pack directory used by questpack commands
    private static final Path INSTANCE_QUEST_PACKS_ROOT =
            net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("config").resolve("boundless").resolve("questpacks");

    // register the root boundless command and its subcommands
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("boundless")
                .requires(s -> s.hasPermission(2))
                // boundless reload reloads quest data and resyncs players
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            QuestData.loadServer(server, true);
                            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                BoundlessNetwork.syncPlayer(p);
                            }
                            ctx.getSource().sendSuccess(() -> cmd("reload.success"), true);
                            return 1;
                        }))
                // boundless reset clears saved quest progress
                .then(Commands.literal("reset")
                        .then(Commands.literal("all")
                                .executes(ctx -> resetAll(ctx.getSource(), selfOrEmpty(ctx.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetAll(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> resetQuest(
                                        ctx.getSource(),
                                        selfOrEmpty(ctx.getSource()),
                                        StringArgumentType.getString(ctx, "id")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> resetQuest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "id"))))))
                // boundless complete force completes quests without rewards
                .then(Commands.literal("complete")
                        .then(Commands.literal("all")
                                .executes(ctx -> completeAll(ctx.getSource(), selfOrEmpty(ctx.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> completeAll(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> completeQuest(
                                        ctx.getSource(),
                                        selfOrEmpty(ctx.getSource()),
                                        StringArgumentType.getString(ctx, "id")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> completeQuest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "id"))))))
                // boundless redeem force redeems quests and grants rewards
                .then(Commands.literal("redeem")
                        .then(Commands.literal("all")
                                .executes(ctx -> redeemAll(ctx.getSource(), selfOrEmpty(ctx.getSource())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> redeemAll(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    for (QuestData.Quest q : QuestData.all()) builder.suggest(q.id);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> redeemQuest(
                                        ctx.getSource(),
                                        selfOrEmpty(ctx.getSource()),
                                        StringArgumentType.getString(ctx, "id")))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> redeemQuest(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "id"))))))
                // boundless questpack manages server enabled quest packs
                .then(Commands.literal("questpack")
                        .then(Commands.literal("enable")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            listInstanceQuestPacks().keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setQuestPackEnabled(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            listInstanceQuestPacks().keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> setQuestPackEnabled(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false))))
                        .then(Commands.literal("list")
                                .executes(ctx -> listQuestPacks(ctx.getSource())))));
    }

    // default target is the command player
    private static List<ServerPlayer> selfOrEmpty(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? List.of() : List.of(player);
    }

    // reset all quest progress for the target players
    private static int resetAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.isEmpty()) {
            source.sendFailure(cmd("error.no_targets"));
            return 0;
        }
        for (ServerPlayer player : targets) {
            QuestTracker.reset(player);
        }
        source.sendSuccess(() -> cmd("reset.all.success", targets.size()), false);
        return targets.size();
    }

    // reset one quest for the target players
    private static int resetQuest(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        if (targets.isEmpty()) {
            source.sendFailure(cmd("error.no_targets"));
            return 0;
        }
        MinecraftServer server = source.getServer();
        var opt = QuestData.byIdServer(server, id);
        if (opt.isEmpty()) {
            source.sendFailure(cmd("error.unknown_quest", id));
            return 0;
        }
        for (ServerPlayer player : targets) {
            QuestTracker.setServerStatus(player, id, null);
            BoundlessNetwork.sendStatus(player, id, QuestTracker.Status.INCOMPLETE.name());
            BoundlessNetwork.sendProgressMeta(player, id);
        }
        source.sendSuccess(() -> cmd("reset.quest.success", id, targets.size()), false);
        return targets.size();
    }

    // complete every quest for the target players
    private static int completeAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.isEmpty()) {
            source.sendFailure(cmd("error.no_targets"));
            return 0;
        }
        for (ServerPlayer player : targets) {
            for (QuestData.Quest q : QuestData.allServer(source.getServer())) {
                QuestTracker.forceCompleteWithoutRewards(q, player);
                BoundlessNetwork.sendProgressMeta(player, q.id);
            }
        }
        source.sendSuccess(() -> cmd("complete.all.success", targets.size()), false);
        return targets.size();
    }

    // complete one quest for the target players
    private static int completeQuest(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        if (targets.isEmpty()) {
            source.sendFailure(cmd("error.no_targets"));
            return 0;
        }
        var opt = QuestData.byIdServer(source.getServer(), id);
        if (opt.isEmpty()) {
            source.sendFailure(cmd("error.invalid_quest", id));
            return 0;
        }
        QuestData.Quest q = opt.get();
        for (ServerPlayer player : targets) {
            QuestTracker.forceCompleteWithoutRewards(q, player);
            BoundlessNetwork.sendProgressMeta(player, q.id);
        }
        source.sendSuccess(() -> cmd("complete.quest.success", q.id, targets.size()), false);
        return targets.size();
    }

    // redeem every quest for the target players
    private static int redeemAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.isEmpty()) {
            source.sendFailure(cmd("error.no_targets"));
            return 0;
        }
        int redeemedCount = 0;
        for (ServerPlayer player : targets) {
            for (QuestData.Quest q : QuestData.allServer(source.getServer())) {
                QuestTracker.forceCompleteWithoutRewards(q, player);
                if (QuestTracker.serverRedeem(q, player)) {
                    BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.REDEEMED.name());
                    BoundlessNetwork.sendProgressMeta(player, q.id);
                    redeemedCount++;
                }
            }
        }
        int finalRedeemedCount = redeemedCount;
        source.sendSuccess(() -> cmd("redeem.all.success", finalRedeemedCount, targets.size()), false);
        return redeemedCount;
    }

    // redeem one quest for the target players
    private static int redeemQuest(CommandSourceStack source, Collection<ServerPlayer> targets, String id) {
        if (targets.isEmpty()) {
            source.sendFailure(cmd("error.no_targets"));
            return 0;
        }
        var opt = QuestData.byIdServer(source.getServer(), id);
        if (opt.isEmpty()) {
            source.sendFailure(cmd("error.invalid_quest", id));
            return 0;
        }
        QuestData.Quest q = opt.get();
        int redeemedPlayers = 0;
        for (ServerPlayer player : targets) {
            QuestTracker.forceCompleteWithoutRewards(q, player);
            if (QuestTracker.serverRedeem(q, player)) {
                BoundlessNetwork.sendStatus(player, q.id, QuestTracker.Status.REDEEMED.name());
                BoundlessNetwork.sendProgressMeta(player, q.id);
                redeemedPlayers++;
            }
        }
        int finalRedeemedPlayers = redeemedPlayers;
        source.sendSuccess(() -> cmd("redeem.quest.success", q.id, finalRedeemedPlayers), false);
        return redeemedPlayers;
    }

    // enable or disable one quest pack on the server
    private static int setQuestPackEnabled(CommandSourceStack source, String id, boolean enabled) {
        String key = id == null ? "" : id.trim();
        if (key.isBlank()) {
            source.sendFailure(cmd("error.questpack_id_required"));
            return 0;
        }

        Path packRoot = INSTANCE_QUEST_PACKS_ROOT.resolve(key);
        if (!Files.isDirectory(packRoot)) {
            source.sendFailure(cmd("error.unknown_questpack", key));
            return 0;
        }
        Config.setQuestPackApplied(key, enabled);

        MinecraftServer server = source.getServer();
        QuestData.loadServer(server, true);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            BoundlessNetwork.syncPlayer(p);
        }
        source.sendSuccess(() -> cmd(enabled ? "questpack.enable.success" : "questpack.disable.success", key), true);
        return 1;
    }

    // list all quest packs visible to the server
    private static int listQuestPacks(CommandSourceStack source) {
        Map<String, Boolean> packs = listInstanceQuestPacks();
        if (packs.isEmpty()) {
            source.sendSuccess(() -> cmd("questpack.list.empty"), false);
            return 0;
        }
        source.sendSuccess(() -> cmd("questpack.list.header"), false);
        packs.forEach((id, enabled) ->
                source.sendSuccess(() -> cmd("questpack.list.entry", id, cmd(enabled ? "state.enabled" : "state.disabled").getString()), false));
        return packs.size();
    }

    // scan the questpack directory for available packs
    private static Map<String, Boolean> listInstanceQuestPacks() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        if (!Files.isDirectory(INSTANCE_QUEST_PACKS_ROOT)) return out;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(INSTANCE_QUEST_PACKS_ROOT)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) continue;
                String id = path.getFileName() == null ? "" : path.getFileName().toString();
                if (id.isBlank()) continue;
                out.put(id, readQuestPackEnabled(path));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    // read the effective enabled state for one quest pack
    private static boolean readQuestPackEnabled(Path packRoot) {
        String id = packRoot == null || packRoot.getFileName() == null ? "" : packRoot.getFileName().toString();
        Boolean enabledFromPackJson = readEnabledFlag(packRoot.resolve("boundless").resolve("pack.json"));
        boolean defaultEnabled;
        if (enabledFromPackJson != null) {
            defaultEnabled = enabledFromPackJson;
        } else {
            Boolean enabledFromMcmeta = readEnabledFlag(packRoot.resolve("pack.mcmeta"));
            defaultEnabled = enabledFromMcmeta == null || enabledFromMcmeta;
        }
        return Config.isQuestPackApplied(id, defaultEnabled);
    }

    // read the enabled flag from pack metadata
    private static Boolean readEnabledFlag(Path metaPath) {
        if (metaPath == null || !Files.exists(metaPath)) return null;
        try (BufferedReader reader = Files.newBufferedReader(metaPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!(parsed instanceof JsonObject root)) return null;
            if (!root.has("boundless") || !root.get("boundless").isJsonObject()) return null;
            JsonObject boundless = root.getAsJsonObject("boundless");
            if (!boundless.has("enabled")) return null;
            JsonElement enabled = boundless.get("enabled");
            if (enabled == null || !enabled.isJsonPrimitive()) return null;
            if (enabled.getAsJsonPrimitive().isBoolean()) return enabled.getAsBoolean();
            return Boolean.parseBoolean(enabled.getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Component cmd(String key, Object... args) {
        return Component.translatable("commands.boundless." + key, args);
    }
}
