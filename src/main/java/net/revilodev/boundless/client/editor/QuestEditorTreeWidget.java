package net.revilodev.boundless.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// Editable quest tree canvas used by the quest-pack editor.
@OnlyIn(Dist.CLIENT)
public final class QuestEditorTreeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 32;
    private static final int CONNECTOR_W = 6;
    private static final ResourceLocation ZOOM_IN = texture("button-zoom-in");
    private static final ResourceLocation ZOOM_OUT = texture("button-zoom-out");
    private static final ResourceLocation ZOOM_CENTRE = texture("button-zoom-centre");
    private static final ResourceLocation CONNECTOR_COMPLETE = texture("connector-complete");
    private static final ResourceLocation CONNECTOR_INCOMPLETE = texture("connector-incomplete");
    private static final ResourceLocation ATTENTION = texture("icon-attention");

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Consumer<QuestEntryData> onSelect;
    private final BiConsumer<Integer, Integer> onPlace;
    private final NodeMoved onMoved;
    private final Set<String> dependencyIds = new LinkedHashSet<>();
    private List<QuestEntryData> quests = List.of();
    private float zoom = 1f;
    private float panX;
    private float panY;
    private QuestEntryData draggedQuest;
    private boolean placing;

    public QuestEditorTreeWidget(int x, int y, int width, int height, Consumer<QuestEntryData> onSelect,
                                 BiConsumer<Integer, Integer> onPlace, NodeMoved onMoved) {
        super(x, y, width, height, Component.empty());
        this.onSelect = onSelect;
        this.onPlace = onPlace;
        this.onMoved = onMoved;
    }

    public void setQuests(List<QuestEntryData> quests) {
        this.quests = quests == null ? List.of() : List.copyOf(quests);
        dependencyIds.retainAll(this.quests.stream().map(quest -> quest.id).collect(java.util.stream.Collectors.toSet()));
    }

    public void setPlacing(boolean placing) {
        this.placing = placing;
        if (!placing) dependencyIds.clear();
    }

    public Set<String> placementDependencies() {
        return Set.copyOf(dependencyIds);
    }

    public boolean isPlacing() {
        return placing;
    }

    public boolean hasNodeAt(double mouseX, double mouseY) {
        return questAt(mouseX, mouseY) != null;
    }

    public void centreView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        renderConnectors(gg);
        renderNodes(gg, mouseX, mouseY);
        gg.disableScissor();
        renderZoomButtons(gg);
        if (placing && isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(minecraft.font, minecraft.font.split(Component.literal("L-Click to place new quest, R-click existing quest(s) to set as dependency, L-Click to place with dependencies"), 118), mouseX, mouseY);
        }
    }

    private void renderConnectors(GuiGraphics gg) {
        for (QuestEntryData quest : quests) {
            for (String dependencyId : dependencies(quest)) {
                QuestEntryData dependency = questById(dependencyId);
                if (dependency == null) continue;
                int x1 = nodeX(dependency) + nodeSize() / 2;
                int y1 = nodeY(dependency) + nodeSize();
                int x2 = nodeX(quest) + nodeSize() / 2;
                int y2 = nodeY(quest);
                drawStraightConnector(gg, x1, y1, x2, y2, 0xFF808080);
            }
        }
    }

    private void renderNodes(GuiGraphics gg, int mouseX, int mouseY) {
        for (QuestEntryData quest : quests) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            boolean dependency = dependencyIds.contains(quest.id);
            int nodeSize = nodeSize();
            gg.blit(texture(normalizeBackground(quest.treeBackground) + "-incomplete"), x, y, 0, 0, nodeSize, nodeSize, NODE_SIZE, NODE_SIZE);
            var icon = QuestEditorItemIcons.iconStackFromId(quest.icon);
            if (!icon.isEmpty()) renderNodeItem(gg, icon, x, y);
            if (dependency) gg.fill(x, y, x + nodeSize, y + nodeSize, 0x40FFD700);
            if (mouseX >= x && mouseX <= x + nodeSize && mouseY >= y && mouseY <= y + nodeSize) {
                gg.renderTooltip(minecraft.font, Component.literal(quest.name.isBlank() ? quest.id : quest.name), mouseX, mouseY);
            }
        }
    }

    private void renderZoomButtons(GuiGraphics gg) {
        int x = getX() + width - 31;
        int y = getY() + 3;
        gg.blit(ZOOM_OUT, x, y, 0, 0, 7, 7, 7, 7);
        gg.blit(ZOOM_CENTRE, x + 10, y - 1, 0, 0, 9, 9, 9, 9);
        gg.blit(ZOOM_IN, x + 22, y, 0, 0, 7, 7, 7, 7);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (button == 0 && handleZoomClick(mouseX, mouseY)) return true;
        QuestEntryData quest = questAt(mouseX, mouseY);
        if (button == 1 && placing && quest != null) {
            if (!dependencyIds.add(quest.id)) dependencyIds.remove(quest.id);
            return true;
        }
        if (button != 0) return false;
        if (quest != null) {
            setFocused(true);
            if (onSelect != null) onSelect.accept(quest);
            draggedQuest = placing ? null : quest;
            return true;
        }
        if (placing) {
            if (onPlace != null) onPlace.accept(canvasX(mouseX), canvasY(mouseY));
            return true;
        }
        draggedQuest = null;
        setFocused(true);
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!active || !visible || button != 0) return false;
        if (draggedQuest != null) {
            int nextX = Math.max(0, canvasX(mouseX) - NODE_SIZE / 2);
            int nextY = Math.max(0, canvasY(mouseY) - NODE_SIZE / 2);
            draggedQuest.treeX = Integer.toString(nextX);
            draggedQuest.treeY = Integer.toString(nextY);
            if (onMoved != null) onMoved.accept(draggedQuest, nextX, nextY);
            return true;
        }
        panX += (float) dragX / zoom;
        panY += (float) dragY / zoom;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) draggedQuest = null;
        return button == 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        zoomAt(mouseX, mouseY, zoom + (float) deltaY * .1f);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !active) return false;
        double centerX = getX() + width / 2D;
        double centerY = getY() + height / 2D;
        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            zoomAt(centerX, centerY, zoom + .1f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoomAt(centerX, centerY, zoom - .1f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            centreView();
            return true;
        }
        return false;
    }

    private boolean handleZoomClick(double mouseX, double mouseY) {
        int x = getX() + width - 31;
        int y = getY() + 3;
        if (mouseY < y - 1 || mouseY > y + 9) return false;
        if (mouseX >= x && mouseX <= x + 7) zoomAt(mouseX, mouseY, zoom - .1f);
        else if (mouseX >= x + 10 && mouseX <= x + 19) centreView();
        else if (mouseX >= x + 22 && mouseX <= x + 29) zoomAt(mouseX, mouseY, zoom + .1f);
        else return false;
        return true;
    }

    private QuestEntryData questAt(double mouseX, double mouseY) {
        for (QuestEntryData quest : quests) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            if (mouseX >= x && mouseX <= x + nodeSize() && mouseY >= y && mouseY <= y + nodeSize()) return quest;
        }
        return null;
    }

    private QuestEntryData questById(String id) {
        for (QuestEntryData quest : quests) if (quest.id.equals(id)) return quest;
        return null;
    }

    private int nodeX(QuestEntryData quest) {
        int index = quests.indexOf(quest);
        int base = parse(quest.treeX, (index % 4) * 46);
        return getX() + Mth.floor((base + panX) * zoom);
    }

    private int nodeY(QuestEntryData quest) {
        int index = quests.indexOf(quest);
        int base = parse(quest.treeY, (index / 4) * 54);
        return getY() + Mth.floor((base + panY) * zoom);
    }

    private int nodeSize() {
        return Math.max(1, Mth.floor(NODE_SIZE * zoom));
    }

    private void renderNodeItem(GuiGraphics gg, net.minecraft.world.item.ItemStack item, int x, int y) {
        gg.pose().pushPose();
        gg.pose().translate(x + 8 * zoom, y + 8 * zoom, 0);
        gg.pose().scale(zoom, zoom, 1f);
        gg.renderItem(item, 0, 0);
        gg.pose().popPose();
    }

    private void zoomAt(double mouseX, double mouseY, float targetZoom) {
        float nextZoom = Mth.clamp(targetZoom, .15f, 2f);
        if (nextZoom == zoom) return;
        float canvasX = (float) ((mouseX - getX()) / zoom - panX);
        float canvasY = (float) ((mouseY - getY()) / zoom - panY);
        zoom = nextZoom;
        panX = (float) ((mouseX - getX()) / zoom - canvasX);
        panY = (float) ((mouseY - getY()) / zoom - canvasY);
    }

    private void drawStraightConnector(GuiGraphics gg, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            gg.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, 0xFF000000);
            gg.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
            int twiceError = error * 2;
            if (twiceError >= dy) { error += dy; x0 += sx; }
            if (twiceError <= dx) { error += dx; y0 += sy; }
        }
    }

    private int canvasX(double mouseX) { return Mth.floor((float) ((mouseX - getX()) / zoom - panX)); }
    private int canvasY(double mouseY) { return Mth.floor((float) ((mouseY - getY()) / zoom - panY)); }

    private static List<String> dependencies(QuestEntryData quest) {
        return QuestEditorEntryCodec.extractEntryLines(quest == null ? "" : quest.dependencies);
    }

    private static int parse(String raw, int fallback) {
        try { return Integer.parseInt(raw == null ? "" : raw.trim()); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static String normalizeBackground(String raw) {
        return switch (raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "circle", "diamond", "square" -> raw.trim().toLowerCase(java.util.Locale.ROOT);
            case "rounded_square", "rounded-square" -> "rounded-square";
            default -> "square";
        };
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/tree/" + name + ".png");
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    @FunctionalInterface
    public interface NodeMoved {
        void accept(QuestEntryData quest, int x, int y);
    }
}
