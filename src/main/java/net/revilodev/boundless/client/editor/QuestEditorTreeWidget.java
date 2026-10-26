package net.revilodev.boundless.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.client.QuestCategoryBackgroundRenderer;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// Editable quest tree canvas used by the quest-pack editor.
@OnlyIn(Dist.CLIENT)
public final class QuestEditorTreeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 32;
    private static final int GRID_SIZE = 16;
    private static final int CONNECTOR_OUTER_WIDTH = 5;
    private static final int CONNECTOR_INNER_WIDTH = 3;
    private static final int CONNECTOR_HIT_RADIUS = 4;
    private static final int ALIGN_SNAP_DISTANCE = 6;
    private static final int DIAGONAL_SNAP_DISTANCE = 16;
    private static final long SHIFT_DOUBLE_CLICK_MS = 350L;
    private static final ResourceLocation ZOOM_IN = texture("button-zoom-in");
    private static final ResourceLocation ZOOM_OUT = texture("button-zoom-out");
    private static final ResourceLocation ZOOM_CENTRE = texture("button-zoom-centre");

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Consumer<QuestEntryData> onSelect;
    private final BiConsumer<Integer, Integer> onPlace;
    private final NodeMoved onMoved;
    private final NodeDeleteRequested onDeleteRequested;
    private final DependencyChanged onDependencyChanged;
    private final BiConsumer<QuestEntryData, String> onIncompatibilityToggle;
    private List<QuestEntryData> quests = List.of();
    private Map<String, QuestEntryData> questsById = Map.of();
    private Map<QuestEntryData, Integer> questIndexes = Map.of();
    private Map<QuestEntryData, ItemStack> questIcons = Map.of();
    private List<RenderedConnector> renderedConnectors = List.of();
    private String background = "";
    private float zoom = 1f;
    private float panX;
    private float panY;
    private QuestEntryData draggedQuest;
    private QuestEntryData connectorSource;
    private boolean draggedQuestMoved;
    private boolean draggingCanvas;
    private Tool tool = Tool.NONE;
    private String lastShiftEdgeKey = "";
    private long lastShiftEdgeClickMs;
    private boolean tooltipSuppressed;

    public QuestEditorTreeWidget(int x, int y, int width, int height, Consumer<QuestEntryData> onSelect,
                                 BiConsumer<Integer, Integer> onPlace, NodeMoved onMoved,
                                 NodeDeleteRequested onDeleteRequested, DependencyChanged onDependencyChanged,
                                 BiConsumer<QuestEntryData, String> onIncompatibilityToggle) {
        super(x, y, width, height, Component.empty());
        this.onSelect = onSelect;
        this.onPlace = onPlace;
        this.onMoved = onMoved;
        this.onDeleteRequested = onDeleteRequested;
        this.onDependencyChanged = onDependencyChanged;
        this.onIncompatibilityToggle = onIncompatibilityToggle;
    }

    public void setQuests(List<QuestEntryData> quests) {
        this.quests = quests == null ? List.of() : List.copyOf(quests);
        rebuildCaches();
        if (connectorSource != null && questById(connectorSource.id) == null) connectorSource = null;
    }

    public void setBackground(String background) {
        this.background = background == null ? "" : background;
    }

    public void setTooltipSuppressed(boolean tooltipSuppressed) {
        this.tooltipSuppressed = tooltipSuppressed;
    }

    public void setTool(Tool tool) {
        this.tool = tool == null ? Tool.NONE : tool;
        connectorSource = null;
        draggedQuest = null;
        draggedQuestMoved = false;
        draggingCanvas = false;
    }

    public Tool tool() {
        return tool;
    }

    public void setPlacing(boolean placing) {
        setTool(placing ? Tool.CREATE : Tool.NONE);
    }

    public boolean isPlacing() {
        return tool == Tool.CREATE;
    }

    public boolean hasNodeAt(double mouseX, double mouseY) {
        return questAt(mouseX, mouseY) != null;
    }

    public void centreView() {
        zoom = 1f;
        QuestEntryData origin = quests.stream().filter(quest -> parseBoolean(quest.treeOrigin)).findFirst().orElse(null);
        panX = origin == null ? 0f : -parse(origin.treeX, 0);
        panY = origin == null ? 0f : -parse(origin.treeY, 0);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        QuestEntryData hovered = tooltipSuppressed || isOverZoomControls(mouseX, mouseY) ? null : questAt(mouseX, mouseY);
        QuestCategoryBackgroundRenderer.render(gg, background, getX(), getY(), width, height);
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        gg.pose().pushPose();
        applyCanvasTransform(gg);
        renderConnectors(gg);
        renderNodes(gg);
        gg.pose().popPose();
        gg.disableScissor();
        renderZoomButtons(gg);
        if (hovered != null) renderTreeTooltip(gg, Component.literal(displayName(hovered)), mouseX, mouseY);
        else if (tool == Tool.CONNECT && connectorSource != null && isMouseOver(mouseX, mouseY)) {
            renderTreeTooltip(gg, Component.literal("Select the quest that should depend on "
                    + displayName(connectorSource)), mouseX, mouseY - 14);
        }
    }

    private void renderConnectors(GuiGraphics gg) {
        for (RenderedConnector connector : renderedConnectors) drawConnector(gg, connector);
    }

    private void renderNodes(GuiGraphics gg) {
        for (QuestEntryData quest : quests) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            gg.blit(texture(normalizeBackground(quest.treeBackground) + "-incomplete"),
                    x, y, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
            ItemStack icon = questIcons.getOrDefault(quest, ItemStack.EMPTY);
            if (!icon.isEmpty()) gg.renderItem(icon, x + 8, y + 8);
            if (quest == connectorSource) drawNodeSelection(gg, x, y, 0xFFFFFFFF);
            if (parseBoolean(quest.treeOrigin)) drawNodeSelection(gg, x, y, 0xFFFFC040);
        }
    }

    private void applyCanvasTransform(GuiGraphics gg) {
        gg.pose().translate(getX() + width / 2f, getY() + height / 2f, 0);
        gg.pose().scale(zoom, zoom, 1f);
        gg.pose().translate(panX, panY, 0);
    }

    private void renderTreeTooltip(GuiGraphics gg, Component text, int mouseX, int mouseY) {
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 900);
        gg.renderTooltip(minecraft.font, text, mouseX, mouseY);
        gg.pose().popPose();
    }

    private void drawNodeSelection(GuiGraphics gg, int x, int y, int color) {
        gg.fill(x - 2, y - 2, x + NODE_SIZE + 2, y, 0xFF000000);
        gg.fill(x - 2, y + NODE_SIZE, x + NODE_SIZE + 2, y + NODE_SIZE + 2, 0xFF000000);
        gg.fill(x - 2, y, x, y + NODE_SIZE, 0xFF000000);
        gg.fill(x + NODE_SIZE, y, x + NODE_SIZE + 2, y + NODE_SIZE, 0xFF000000);
        gg.fill(x - 1, y - 1, x + NODE_SIZE + 1, y, color);
        gg.fill(x - 1, y + NODE_SIZE, x + NODE_SIZE + 1, y + NODE_SIZE + 1, color);
        gg.fill(x - 1, y, x, y + NODE_SIZE, color);
        gg.fill(x + NODE_SIZE, y, x + NODE_SIZE + 1, y + NODE_SIZE, color);
    }

    private void renderZoomButtons(GuiGraphics gg) {
        int x = getX() + width - 31;
        int y = getY() + 3;
        gg.blit(ZOOM_OUT, x, y, 0, 0, 7, 7, 7, 7);
        gg.blit(ZOOM_CENTRE, x + 10, y - 1, 0, 0, 9, 9, 9, 9);
        gg.blit(ZOOM_IN, x + 22, y, 0, 0, 7, 7, 7, 7);
    }

    private boolean isOverZoomControls(double mouseX, double mouseY) {
        int x = getX() + width - 31;
        int y = getY() + 2;
        return mouseX >= x && mouseX <= x + 29 && mouseY >= y && mouseY <= y + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        if (handleZoomClick(mouseX, mouseY)) return true;

        QuestEntryData quest = questAt(mouseX, mouseY);
        DependencyEdge clickedEdge = quest == null ? edgeAt(mouseX, mouseY) : null;
        if (tool != Tool.DELETE && Screen.hasShiftDown() && clickedEdge != null) {
            long now = Util.getMillis();
            String edgeKey = clickedEdge.dependency.id + "->" + clickedEdge.quest.id;
            if (edgeKey.equals(lastShiftEdgeKey) && now - lastShiftEdgeClickMs <= SHIFT_DOUBLE_CLICK_MS) {
                lastShiftEdgeKey = "";
                lastShiftEdgeClickMs = 0L;
                if (onIncompatibilityToggle != null) {
                    onIncompatibilityToggle.accept(clickedEdge.quest, clickedEdge.dependency.id);
                }
            } else {
                lastShiftEdgeKey = edgeKey;
                lastShiftEdgeClickMs = now;
            }
            return true;
        }
        if (tool == Tool.DELETE) {
            if (quest != null) {
                if (onDeleteRequested != null) onDeleteRequested.accept(quest, Screen.hasShiftDown());
                return true;
            }
            DependencyEdge edge = clickedEdge == null ? edgeAt(mouseX, mouseY) : clickedEdge;
            if (edge != null && onDependencyChanged != null) {
                onDependencyChanged.accept(edge.quest, edge.dependency.id, false, Screen.hasShiftDown());
                return true;
            }
            return isMouseOver(mouseX, mouseY);
        }

        if (tool == Tool.CONNECT) {
            if (quest == null) return isMouseOver(mouseX, mouseY);
            if (connectorSource == null) {
                connectorSource = quest;
            } else if (connectorSource == quest) {
                connectorSource = null;
            } else {
                if (onDependencyChanged != null) {
                    onDependencyChanged.accept(quest, connectorSource.id, true, false);
                }
                connectorSource = null;
            }
            return true;
        }

        if (tool == Tool.MOVE) {
            setFocused(true);
            if (quest != null) {
                draggedQuest = quest;
                draggedQuestMoved = false;
            } else {
                draggingCanvas = true;
            }
            return true;
        }

        if (quest != null) {
            setFocused(true);
            if (tool == Tool.CREATE) setTool(Tool.NONE);
            if (onSelect != null) onSelect.accept(quest);
            return true;
        }
        if (tool == Tool.CREATE) {
            int[] position = snapPosition(canvasNodeX(mouseX), canvasNodeY(mouseY), null);
            if (onPlace != null) onPlace.accept(position[0], position[1]);
            return true;
        }
        draggingCanvas = true;
        setFocused(true);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!active || !visible || button != 0) return false;
        if (draggingCanvas) {
            panX += (float) dragX / zoom;
            panY += (float) dragY / zoom;
            return true;
        }
        if (tool != Tool.MOVE) return false;
        if (draggedQuest != null) {
            int[] position = snapPosition(canvasNodeX(mouseX), canvasNodeY(mouseY), draggedQuest);
            int nextX = position[0];
            int nextY = position[1];
            if (nextX != parse(draggedQuest.treeX, 0) || nextY != parse(draggedQuest.treeY, 0)) {
                draggedQuest.treeX = Integer.toString(nextX);
                draggedQuest.treeY = Integer.toString(nextY);
                draggedQuestMoved = true;
                rebuildConnectorCache();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (draggedQuest != null && draggedQuestMoved && onMoved != null) {
            onMoved.accept(draggedQuest, parse(draggedQuest.treeX, 0), parse(draggedQuest.treeY, 0));
        }
        draggedQuest = null;
        draggedQuestMoved = false;
        draggingCanvas = false;
        return true;
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
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && tool != Tool.NONE) {
            setTool(Tool.NONE);
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
        double canvasX = canvasX(mouseX);
        double canvasY = canvasY(mouseY);
        for (int index = quests.size() - 1; index >= 0; index--) {
            QuestEntryData quest = quests.get(index);
            int x = nodeX(quest);
            int y = nodeY(quest);
            if (canvasX >= x && canvasX <= x + NODE_SIZE && canvasY >= y && canvasY <= y + NODE_SIZE) return quest;
        }
        return null;
    }

    private DependencyEdge edgeAt(double mouseX, double mouseY) {
        double canvasX = canvasX(mouseX);
        double canvasY = canvasY(mouseY);
        for (RenderedConnector rendered : renderedConnectors) {
            if (connectorHit(rendered.connector, canvasX, canvasY)) {
                return new DependencyEdge(rendered.dependency, rendered.quest);
            }
        }
        return null;
    }

    private boolean connectorHit(Connector connector, double mouseX, double mouseY) {
        List<Point> points = connector.points;
        for (int index = 1; index < points.size(); index++) {
            Point start = points.get(index - 1);
            Point end = points.get(index);
            if (distanceToSegment(mouseX, mouseY, start.x, start.y, end.x, end.y) <= CONNECTOR_HIT_RADIUS) return true;
        }
        return false;
    }

    private static double distanceToSegment(double px, double py, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0D, Math.min(1D, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    private QuestEntryData questById(String id) {
        return questsById.get(id);
    }

    private int nodeX(QuestEntryData quest) {
        int index = questIndexes.getOrDefault(quest, 0);
        int base = parse(quest.treeX, (index % 4 - 2) * 48);
        return base - NODE_SIZE / 2;
    }

    private int nodeY(QuestEntryData quest) {
        int index = questIndexes.getOrDefault(quest, 0);
        int base = parse(quest.treeY, (index / 4) * 48);
        return base - NODE_SIZE / 2;
    }

    private void zoomAt(double mouseX, double mouseY, float targetZoom) {
        float nextZoom = Mth.clamp(targetZoom, .15f, 2f);
        if (nextZoom == zoom) return;
        float centerX = getX() + width / 2f;
        float centerY = getY() + height / 2f;
        float canvasX = (float) ((mouseX - centerX) / zoom - panX);
        float canvasY = (float) ((mouseY - centerY) / zoom - panY);
        zoom = nextZoom;
        panX = (float) ((mouseX - centerX) / zoom - canvasX);
        panY = (float) ((mouseY - centerY) / zoom - canvasY);
    }

    private Connector connector(QuestEntryData dependency, QuestEntryData quest) {
        int x1 = nodeX(dependency) + NODE_SIZE / 2;
        int y1 = nodeY(dependency) + NODE_SIZE / 2;
        int x2 = nodeX(quest) + NODE_SIZE / 2;
        int y2 = nodeY(quest) + NODE_SIZE / 2;
        int dx = x2 - x1;
        int dy = y2 - y1;
        List<Point> points = new ArrayList<>();
        points.add(new Point(x1, y1));
        if (dx != 0 && dy != 0 && !isDiagonal(dx, dy)) {
            points.add(new Point(x2, y1));
        }
        points.add(new Point(x2, y2));
        return new Connector(List.copyOf(points));
    }

    private void drawConnector(GuiGraphics gg, RenderedConnector connector) {
        for (PixelSpan span : connector.outerSpans) {
            gg.fill(span.x, span.y, span.x + span.width, span.y + 1, 0xFF000000);
        }
        int color = connector.incompatible ? 0xFFFF3030 : 0xFFFFFFFF;
        for (PixelSpan span : connector.innerSpans) {
            gg.fill(span.x, span.y, span.x + span.width, span.y + 1, color);
        }
    }

    private RenderedConnector renderedConnector(QuestEntryData dependency, QuestEntryData quest, boolean incompatible) {
        Connector connector = connector(dependency, quest);
        Set<Long> outer = new HashSet<>();
        Set<Long> inner = new HashSet<>();
        for (int index = 1; index < connector.points.size(); index++) {
            Point start = connector.points.get(index - 1);
            Point end = connector.points.get(index);
            rasterLine(start.x, start.y, end.x, end.y, outer, inner);
        }
        outer.removeAll(inner);
        return new RenderedConnector(dependency, quest, connector, incompatible,
                horizontalSpans(outer), horizontalSpans(inner));
    }

    private static List<PixelSpan> horizontalSpans(Set<Long> pixels) {
        List<Point> points = new ArrayList<>(pixels.size());
        for (long packed : pixels) points.add(new Point((int) (packed >> 32), (int) packed));
        points.sort(Comparator.comparingInt(Point::y).thenComparingInt(Point::x));
        List<PixelSpan> spans = new ArrayList<>();
        int index = 0;
        while (index < points.size()) {
            Point first = points.get(index++);
            int endX = first.x;
            while (index < points.size()) {
                Point next = points.get(index);
                if (next.y != first.y || next.x != endX + 1) break;
                endX = next.x;
                index++;
            }
            spans.add(new PixelSpan(first.x, first.y, endX - first.x + 1));
        }
        return List.copyOf(spans);
    }

    private void rebuildCaches() {
        Map<String, QuestEntryData> byId = new HashMap<>();
        Map<QuestEntryData, Integer> indexes = new IdentityHashMap<>();
        Map<QuestEntryData, ItemStack> icons = new IdentityHashMap<>();
        for (int index = 0; index < quests.size(); index++) {
            QuestEntryData quest = quests.get(index);
            if (quest.id != null && !quest.id.isBlank()) byId.put(quest.id, quest);
            indexes.put(quest, index);
            icons.put(quest, QuestEditorItemIcons.iconStackFromId(quest.icon));
        }
        questsById = byId;
        questIndexes = indexes;
        questIcons = icons;
        rebuildConnectorCache();
    }

    private void rebuildConnectorCache() {
        List<RenderedConnector> connectors = new ArrayList<>();
        for (QuestEntryData quest : quests) {
            Set<String> incompatible = new HashSet<>(incompatibleDependencies(quest));
            for (String dependencyId : dependencies(quest)) {
                QuestEntryData dependency = questById(dependencyId);
                if (dependency != null) {
                    connectors.add(renderedConnector(dependency, quest, incompatible.contains(dependencyId)));
                }
            }
        }
        renderedConnectors = List.copyOf(connectors);
    }

    private static void rasterLine(int x1, int y1, int x2, int y2, Set<Long> outer, Set<Long> inner) {
        int dx = Math.abs(x2 - x1);
        int sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1);
        int sy = y1 < y2 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            addBrush(outer, x1, y1, CONNECTOR_OUTER_WIDTH / 2);
            addBrush(inner, x1, y1, CONNECTOR_INNER_WIDTH / 2);
            if (x1 == x2 && y1 == y2) return;
            int twice = error * 2;
            if (twice >= dy) { error += dy; x1 += sx; }
            if (twice <= dx) { error += dx; y1 += sy; }
        }
    }

    private static void addBrush(Set<Long> pixels, int centerX, int centerY, int radius) {
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                pixels.add(((long) x << 32) | (y & 0xFFFFFFFFL));
            }
        }
    }

    private int canvasNodeX(double mouseX) {
        return Mth.floor((float) canvasX(mouseX));
    }

    private int canvasNodeY(double mouseY) {
        return Mth.floor((float) canvasY(mouseY));
    }

    private double canvasX(double screenX) {
        return (screenX - (getX() + width / 2D)) / zoom - panX;
    }

    private double canvasY(double screenY) {
        return (screenY - (getY() + height / 2D)) / zoom - panY;
    }

    private int[] snapPosition(int rawX, int rawY, QuestEntryData moving) {
        int snappedX = Math.round(rawX / (float) GRID_SIZE) * GRID_SIZE;
        int snappedY = Math.round(rawY / (float) GRID_SIZE) * GRID_SIZE;
        for (QuestEntryData quest : quests) {
            if (quest == moving) continue;
            int otherX = parse(quest.treeX, 0);
            int otherY = parse(quest.treeY, 0);
            if (Math.abs(snappedX - otherX) <= ALIGN_SNAP_DISTANCE) snappedX = otherX;
            if (Math.abs(snappedY - otherY) <= ALIGN_SNAP_DISTANCE) snappedY = otherY;
            int difference = otherX - otherY;
            if (Math.abs((snappedX - snappedY) - difference) <= DIAGONAL_SNAP_DISTANCE) snappedY = snappedX - difference;
            int sum = otherX + otherY;
            if (Math.abs((snappedX + snappedY) - sum) <= DIAGONAL_SNAP_DISTANCE) snappedY = sum - snappedX;
        }
        return new int[]{snappedX, snappedY};
    }

    private static List<String> dependencies(QuestEntryData quest) {
        return QuestEditorEntryCodec.extractEntryLines(quest == null ? "" : quest.dependencies);
    }

    private static List<String> incompatibleDependencies(QuestEntryData quest) {
        if (quest == null) return List.of();
        List<String> incompatible = QuestEditorEntryCodec.extractEntryLines(quest.incompatibleDependencies);
        return incompatible.isEmpty() && parseBoolean(quest.lockAfterDependency) ? dependencies(quest) : incompatible;
    }

    private static int parse(String raw, int fallback) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String raw) {
        return "true".equalsIgnoreCase(raw == null ? "" : raw.trim());
    }

    private static boolean isDiagonal(int dx, int dy) {
        int longest = Math.max(Math.abs(dx), Math.abs(dy));
        return Math.abs(Math.abs(dx) - Math.abs(dy)) <= Math.max(12, longest / 5);
    }

    private static String displayName(QuestEntryData quest) {
        return quest == null || quest.name == null || quest.name.isBlank() ? (quest == null ? "quest" : quest.id) : quest.name;
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

    public enum Tool {
        NONE,
        MOVE,
        CREATE,
        DELETE,
        CONNECT
    }

    private record Point(int x, int y) {
    }

    private record Connector(List<Point> points) {
    }

    private record DependencyEdge(QuestEntryData dependency, QuestEntryData quest) {
    }

    private record RenderedConnector(QuestEntryData dependency, QuestEntryData quest, Connector connector,
                                     boolean incompatible, List<PixelSpan> outerSpans, List<PixelSpan> innerSpans) {
    }

    private record PixelSpan(int x, int y, int width) {
    }

    @FunctionalInterface
    public interface NodeMoved {
        void accept(QuestEntryData quest, int x, int y);
    }

    @FunctionalInterface
    public interface NodeDeleteRequested {
        void accept(QuestEntryData quest, boolean bypassConfirmation);
    }

    @FunctionalInterface
    public interface DependencyChanged {
        void accept(QuestEntryData quest, String dependencyId, boolean add, boolean bypassConfirmation);
    }
}
