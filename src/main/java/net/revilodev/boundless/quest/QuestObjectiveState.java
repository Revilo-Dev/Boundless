package net.revilodev.boundless.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestObjectiveState extends SavedData {
    private final Map<String, Map<String, Integer>> itemProgressByPlayer = new HashMap<>();
    private final Map<String, Map<String, Boolean>> effectProgressByPlayer = new HashMap<>();
    private final Map<String, Map<String, String>> inputProgressByPlayer = new HashMap<>();

    private QuestObjectiveState() {}

    public static QuestObjectiveState get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                QuestObjectiveState::load, QuestObjectiveState::new, "boundless_quest_objectives"
        );
    }

    public static QuestObjectiveState load(CompoundTag tag) {
        QuestObjectiveState s = new QuestObjectiveState();

        if (tag.contains("items", Tag.TAG_COMPOUND)) {
            CompoundTag itemsRoot = tag.getCompound("items");
            for (String playerKey : itemsRoot.getAllKeys()) {
                CompoundTag inner = itemsRoot.getCompound(playerKey);
                Map<String, Integer> m = new HashMap<>();
                for (String k : inner.getAllKeys()) {
                    if (inner.contains(k, Tag.TAG_INT)) m.put(k, inner.getInt(k));
                }
                if (!m.isEmpty()) s.itemProgressByPlayer.put(playerKey, m);
            }
        }

        if (tag.contains("effects", Tag.TAG_COMPOUND)) {
            CompoundTag effectsRoot = tag.getCompound("effects");
            for (String playerKey : effectsRoot.getAllKeys()) {
                CompoundTag inner = effectsRoot.getCompound(playerKey);
                Map<String, Boolean> m = new HashMap<>();
                for (String k : inner.getAllKeys()) {
                    if (inner.contains(k, Tag.TAG_BYTE)) m.put(k, inner.getBoolean(k));
                }
                if (!m.isEmpty()) s.effectProgressByPlayer.put(playerKey, m);
            }
        }

        if (tag.contains("inputs", Tag.TAG_COMPOUND)) {
            CompoundTag inputsRoot = tag.getCompound("inputs");
            for (String playerKey : inputsRoot.getAllKeys()) {
                CompoundTag inner = inputsRoot.getCompound(playerKey);
                Map<String, String> m = new HashMap<>();
                for (String k : inner.getAllKeys()) {
                    if (inner.contains(k, Tag.TAG_STRING)) {
                        String value = inner.getString(k);
                        if (!value.isBlank()) m.put(k, value);
                    }
                }
                if (!m.isEmpty()) s.inputProgressByPlayer.put(playerKey, m);
            }
        }

        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag itemsRoot = new CompoundTag();
        for (Map.Entry<String, Map<String, Integer>> e : itemProgressByPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, Integer> q : e.getValue().entrySet()) {
                inner.putInt(q.getKey(), Math.max(0, q.getValue()));
            }
            itemsRoot.put(e.getKey(), inner);
        }
        tag.put("items", itemsRoot);

        CompoundTag effectsRoot = new CompoundTag();
        for (Map.Entry<String, Map<String, Boolean>> e : effectProgressByPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, Boolean> q : e.getValue().entrySet()) {
                inner.putBoolean(q.getKey(), Boolean.TRUE.equals(q.getValue()));
            }
            effectsRoot.put(e.getKey(), inner);
        }
        tag.put("effects", effectsRoot);

        CompoundTag inputsRoot = new CompoundTag();
        for (Map.Entry<String, Map<String, String>> e : inputProgressByPlayer.entrySet()) {
            CompoundTag inner = new CompoundTag();
            for (Map.Entry<String, String> q : e.getValue().entrySet()) {
                String value = q.getValue() == null ? "" : q.getValue().trim();
                if (!value.isBlank()) inner.putString(q.getKey(), value);
            }
            if (!inner.isEmpty()) inputsRoot.put(e.getKey(), inner);
        }
        tag.put("inputs", inputsRoot);

        return tag;
    }

    public int getItemProgress(UUID player, String key) {
        Map<String, Integer> m = itemProgressByPlayer.get(player.toString());
        if (m == null) return 0;
        return Math.max(0, m.getOrDefault(key, 0));
    }

    public int updateItemProgress(UUID player, String key, int current, int required) {
        String p = player.toString();
        Map<String, Integer> m = itemProgressByPlayer.get(p);
        int prev = m == null ? 0 : Math.max(0, m.getOrDefault(key, 0));
        int now = Math.max(prev, Math.min(Math.max(0, current), Math.max(0, required)));
        if (now <= 0) {
            if (m != null && m.remove(key) != null) {
                if (m.isEmpty()) itemProgressByPlayer.remove(p);
                setDirty();
            }
            return 0;
        }
        if (now != prev) {
            if (m == null) {
                m = new HashMap<>();
                itemProgressByPlayer.put(p, m);
            }
            m.put(key, now);
            setDirty();
        }
        return now;
    }

    public boolean getEffectDone(UUID player, String key) {
        return getFlagDone(player, key);
    }

    public boolean getFlagDone(UUID player, String key) {
        Map<String, Boolean> m = effectProgressByPlayer.get(player.toString());
        if (m == null) return false;
        return Boolean.TRUE.equals(m.get(key));
    }

    public boolean updateEffectDone(UUID player, String key, boolean hasNow) {
        return updateFlagDone(player, key, hasNow);
    }

    public boolean updateFlagDone(UUID player, String key, boolean hasNow) {
        String p = player.toString();
        Map<String, Boolean> m = effectProgressByPlayer.get(p);
        boolean prev = m != null && Boolean.TRUE.equals(m.get(key));
        boolean now = prev || hasNow;
        if (now) {
            if (!prev) {
                if (m == null) {
                    m = new HashMap<>();
                    effectProgressByPlayer.put(p, m);
                }
                m.put(key, true);
                setDirty();
            }
        } else {
            if (m != null && m.remove(key) != null) {
                if (m.isEmpty()) effectProgressByPlayer.remove(p);
                setDirty();
            }
        }
        return now;
    }

    public String getInputProgress(UUID player, String key) {
        if (key == null || key.isBlank()) return "";
        Map<String, String> m = inputProgressByPlayer.get(player.toString());
        if (m == null) return "";
        return m.getOrDefault(key, "");
    }

    public void setInputProgress(UUID player, String key, String value) {
        if (key == null || key.isBlank()) return;
        String p = player.toString();
        String normalized = value == null ? "" : value.trim();
        Map<String, String> m = inputProgressByPlayer.get(p);
        if (normalized.isBlank()) {
            if (m != null && m.remove(key) != null) {
                if (m.isEmpty()) inputProgressByPlayer.remove(p);
                setDirty();
            }
            return;
        }
        String prev = m == null ? null : m.get(key);
        if (normalized.equals(prev)) return;
        if (m == null) {
            m = new HashMap<>();
            inputProgressByPlayer.put(p, m);
        }
        m.put(key, normalized);
        setDirty();
    }

    public void clearPlayer(UUID player) {
        String p = player.toString();
        boolean changed = itemProgressByPlayer.remove(p) != null;
        changed |= effectProgressByPlayer.remove(p) != null;
        changed |= inputProgressByPlayer.remove(p) != null;
        if (changed) setDirty();
    }

    public void clearQuest(UUID player, String questId) {
        String p = player.toString();
        if (questId == null || questId.isBlank()) return;
        boolean changed = false;

        Map<String, Integer> items = itemProgressByPlayer.get(p);
        if (items != null) {
            boolean removed = items.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(questId + ":"));
            if (removed) changed = true;
            if (items.isEmpty()) itemProgressByPlayer.remove(p);
        }

        Map<String, Boolean> effects = effectProgressByPlayer.get(p);
        if (effects != null) {
            boolean removed = effects.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(questId + ":"));
            if (removed) changed = true;
            if (effects.isEmpty()) effectProgressByPlayer.remove(p);
        }

        Map<String, String> inputs = inputProgressByPlayer.get(p);
        if (inputs != null) {
            boolean removed = inputs.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(questId + ":"));
            if (removed) changed = true;
            if (inputs.isEmpty()) inputProgressByPlayer.remove(p);
        }

        if (changed) setDirty();
    }

    public Map<String, Integer> itemSnapshotFor(UUID player) {
        Map<String, Integer> snapshot = itemProgressByPlayer.get(player.toString());
        return snapshot == null || snapshot.isEmpty() ? Map.of() : Map.copyOf(snapshot);
    }

    public Map<String, Boolean> flagSnapshotFor(UUID player) {
        Map<String, Boolean> snapshot = effectProgressByPlayer.get(player.toString());
        return snapshot == null || snapshot.isEmpty() ? Map.of() : Map.copyOf(snapshot);
    }

    public Map<String, String> inputSnapshotFor(UUID player) {
        Map<String, String> snapshot = inputProgressByPlayer.get(player.toString());
        return snapshot == null || snapshot.isEmpty() ? Map.of() : Map.copyOf(snapshot);
    }
}
