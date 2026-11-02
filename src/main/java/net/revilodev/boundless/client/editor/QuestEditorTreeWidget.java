package net.revilodev.boundless.client.editor;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.client.QuestCategoryBackgroundRenderer;
import net.revilodev.boundless.client.editor.QuestEditorModels.QuestEntryData;
import net.revilodev.boundless.quest.QuestData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
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
// edits quest positions, links, and stickers in the tree workspace
public final class QuestEditorTreeWidget extends AbstractWidget {
    private static final int MAX_CONNECTOR_RASTER_STEPS = 4096;
    private static final int DETAILED_ROUTING_MAX_QUESTS = 128;
    private static final int DETAILED_ROUTING_MAX_CONNECTORS = 256;
    private static final int SPATIAL_CELL_SIZE = 64;
    private static final int NODE_SIZE = 32;
    private static final int GRID_SIZE = 16;
    private static final int CONNECTOR_OUTER_WIDTH = 5;
    private static final int CONNECTOR_INNER_WIDTH = 3;
    private static final int CONNECTOR_HIT_RADIUS = 4;
    private static final int ALIGN_SNAP_DISTANCE = 6;
    private static final int DIAGONAL_SNAP_DISTANCE = 16;
    private static final long SHIFT_DOUBLE_CLICK_MS = 350L;
    private static final long STICKER_DOUBLE_CLICK_MS = 350L;
    private static final ResourceLocation ZOOM_IN = texture("button-zoom-in");
    private static final ResourceLocation ZOOM_OUT = texture("button-zoom-out");
    private static final ResourceLocation ZOOM_CENTRE = texture("button-zoom-centre");
    private static final ResourceLocation ORIGIN_STAR =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/icon/emoji/star.png");
    private static final ResourceLocation IMPORT_WARNING =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/icon/exclamation.png");
    private static final Map<String, ResourceLocation> NODE_TEXTURES = Map.of(
            "circle", texture("circle-incomplete"),
            "circle-selected", texture("circle-incomplete-selected"),
            "diamond", texture("diamond-incomplete"),
            "diamond-selected", texture("diamond-incomplete-selected"),
            "rounded-square", texture("rounded-square-incomplete"),
            "rounded-square-selected", texture("rounded-square-incomplete-selected"),
            "square", texture("square-incomplete"),
            "square-selected", texture("square-incomplete-selected")
    );

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Consumer<QuestEntryData> onSelect;
    private final BiConsumer<Integer, Integer> onPlace;
    private final NodeMoved onMoved;
    private final NodeDeleteRequested onDeleteRequested;
    private final DependencyChanged onDependencyChanged;
    private final BiConsumer<QuestEntryData, String> onIncompatibilityToggle;
    private final Consumer<List<QuestData.Sticker>> onStickersChanged;
    private final BiConsumer<Integer, Integer> onStickerIconRequested;
    private final BiConsumer<Integer, QuestData.Sticker> onStickerTextEditRequested;
    private List<QuestEntryData> quests = List.of();
    private Map<String, QuestEntryData> questsById = Map.of();
    private Map<QuestEntryData, Point> questPositions = new IdentityHashMap<>();
    private Map<QuestEntryData, ItemStack> questIcons = new IdentityHashMap<>();
    private Map<Long, List<QuestEntryData>> questSpatialIndex = Map.of();
    private Set<QuestEntryData> originQuests = Set.of();
    private List<RenderedConnector> renderedConnectors = List.of();
    private String background = "";
    private float backgroundTileScale = 0.5f;
    private boolean backgroundDarkening;
    private float backgroundDarkeningOpacity = 0.45f;
    private float zoom = 1f;
    private float panX;
    private float panY;
    private QuestEntryData draggedQuest;
    private QuestEntryData connectorSource;
    private QuestEntryData selectedQuest;
    private boolean draggedQuestMoved;
    private boolean draggingCanvas;
    private Tool tool = Tool.NONE;
    private String lastShiftEdgeKey = "";
    private long lastShiftEdgeClickMs;
    private boolean tooltipSuppressed;
    private List<QuestData.Sticker> stickers = List.of();
    private boolean stickerMenuOpen;
    private int stickerMenuX;
    private int stickerMenuY;
    private int pendingStickerX;
    private int pendingStickerY;
    private boolean editingStickerText;
    private String stickerText = "";
    private int selectedStickerIndex = -1;
    private int draggedStickerIndex = -1;
    private int stickerDragOffsetX;
    private int stickerDragOffsetY;
    private boolean draggedStickerMoved;
    private int resizingStickerIndex = -1;
    private float resizeStartScale;
    private double resizeStartDistance;
    private double resizeCenterX;
    private double resizeCenterY;
    private int lastStickerClickIndex = -1;
    private long lastStickerClickMs;

    public QuestEditorTreeWidget(int x, int y, int width, int height, Consumer<QuestEntryData> onSelect,
                                 BiConsumer<Integer, Integer> onPlace, NodeMoved onMoved,
                                 NodeDeleteRequested onDeleteRequested, DependencyChanged onDependencyChanged,
                                 BiConsumer<QuestEntryData, String> onIncompatibilityToggle,
                                 Consumer<List<QuestData.Sticker>> onStickersChanged,
                                 BiConsumer<Integer, Integer> onStickerIconRequested,
                                 BiConsumer<Integer, QuestData.Sticker> onStickerTextEditRequested) {
        super(x, y, width, height, Component.empty());
        this.onSelect = onSelect;
        this.onPlace = onPlace;
        this.onMoved = onMoved;
        this.onDeleteRequested = onDeleteRequested;
        this.onDependencyChanged = onDependencyChanged;
        this.onIncompatibilityToggle = onIncompatibilityToggle;
        this.onStickersChanged = onStickersChanged;
        this.onStickerIconRequested = onStickerIconRequested;
        this.onStickerTextEditRequested = onStickerTextEditRequested;
    }

    public void setQuests(List<QuestEntryData> quests) {
        // replace the data once then rebuild all render caches together
        this.quests = quests == null ? List.of() : List.copyOf(quests);
        rebuildCaches();
        if (connectorSource != null && questById(connectorSource.id) == null) connectorSource = null;
    }

    public void setBackground(String background) {
        setBackground(background, false, 0.45f);
    }

    public void setBackground(String background, boolean darkening, float darkeningOpacity) {
        setBackground(background, darkening, darkeningOpacity, 0.5f);
    }

    public void setBackground(String background, boolean darkening, float darkeningOpacity, float tileScale) {
        this.background = background == null ? "" : background;
        this.backgroundTileScale = Math.max(0.1f, Math.min(2f, tileScale));
        this.backgroundDarkening = darkening;
        this.backgroundDarkeningOpacity = Math.max(0f, Math.min(1f, darkeningOpacity));
    }

    public void setStickers(List<QuestData.Sticker> stickers) {
        this.stickers = stickers == null ? List.of() : List.copyOf(stickers);
        if (selectedStickerIndex >= this.stickers.size()) selectedStickerIndex = -1;
    }

    public void addIconSticker(int x, int y, String icon) {
        if (icon == null || icon.isBlank()) return;
        addSticker(new QuestData.Sticker("icon", icon, x, y));
    }

    public void setTooltipSuppressed(boolean tooltipSuppressed) {
        this.tooltipSuppressed = tooltipSuppressed;
    }

    public void setTool(Tool tool) {
        this.tool = tool == null ? Tool.NONE : tool;
        connectorSource = null;
        draggedQuest = null;
        draggedQuestMoved = false;
        draggedStickerIndex = -1;
        resizingStickerIndex = -1;
        draggedStickerMoved = false;
        if (this.tool != Tool.MOVE) selectedStickerIndex = -1;
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
        QuestEntryData origin = quests.stream().filter(originQuests::contains).findFirst().orElse(null);
        panX = origin == null ? 0f : -(nodeX(origin) + NODE_SIZE / 2f);
        panY = origin == null ? 0f : -(nodeY(origin) + NODE_SIZE / 2f);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // draw background, stickers, links, then nodes in depth order
        QuestEntryData hovered = tooltipSuppressed || isOverZoomControls(mouseX, mouseY) ? null : questAt(mouseX, mouseY);
        QuestEntryData hoveredWarning = hovered != null && isOverImportWarning(hovered, mouseX, mouseY) ? hovered : null;
        QuestCategoryBackgroundRenderer.render(gg, background, backgroundDarkening, backgroundDarkeningOpacity,
                backgroundTileScale,
                getX(), getY(), width, height);
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        gg.pose().pushPose();
        applyCanvasTransform(gg);
        CanvasBounds visibleBounds = visibleCanvasBounds();
        renderStickers(gg, visibleBounds);
        renderConnectors(gg, visibleBounds);
        renderNodes(gg, hovered, visibleBounds);
        gg.pose().popPose();
        gg.flush();
        gg.disableScissor();
        renderZoomButtons(gg);
        renderStickerMenu(gg, mouseX, mouseY);
        if (hoveredWarning != null) renderImportWarningTooltip(gg, hoveredWarning, mouseX, mouseY);
        else if (hovered != null) renderTreeTooltip(gg, Component.literal(displayName(hovered)), mouseX, mouseY);
        else if (tool == Tool.CONNECT && connectorSource != null && isMouseOver(mouseX, mouseY)) {
            renderTreeTooltip(gg, Component.literal("Select the quest that should depend on "
                    + displayName(connectorSource)), mouseX, mouseY - 14);
        }
    }

    private void renderConnectors(GuiGraphics gg, CanvasBounds visibleBounds) {
        for (RenderedConnector connector : renderedConnectors) {
            if (connectorVisible(connector.connector, visibleBounds)) drawConnector(gg, connector);
        }
    }

    private void renderStickers(GuiGraphics gg, CanvasBounds visibleBounds) {
        for (int index = 0; index < stickers.size(); index++) {
            QuestData.Sticker sticker = stickers.get(index);
            float[] bounds = stickerBounds(sticker);
            if (!visibleBounds.intersects(bounds[0], bounds[1], bounds[2], bounds[3])) continue;
            gg.pose().pushPose();
            if ("icon".equals(sticker.type())) {
                gg.pose().translate(sticker.x(), sticker.y(), 0);
                gg.pose().scale(sticker.scale(), sticker.scale(), 1f);
                QuestEditorItemIcons.renderIcon(gg, sticker.value(), -8, -8);
            } else {
                gg.pose().translate(sticker.x(), sticker.y(), 0);
                gg.pose().scale(sticker.scale(), sticker.scale(), 1f);
                QuestStickerTextRenderer.render(gg, minecraft.font, sticker.value(), 0, 0);
            }
            gg.pose().popPose();
            if (tool == Tool.MOVE && index == selectedStickerIndex) drawStickerSelection(gg, sticker);
        }
    }

    private void drawStickerSelection(GuiGraphics gg, QuestData.Sticker sticker) {
        int left;
        int top;
        int right;
        int bottom;
        float[] bounds = stickerBounds(sticker);
        left = Mth.floor(bounds[0]) - 2;
        top = Mth.floor(bounds[1]) - 2;
        right = Mth.ceil(bounds[2]) + 2;
        bottom = Mth.ceil(bounds[3]) + 2;
        gg.fill(left, top, right, top + 1, 0xFFFFFFFF);
        gg.fill(left, bottom - 1, right, bottom, 0xFFFFFFFF);
        gg.fill(left, top, left + 1, bottom, 0xFFFFFFFF);
        gg.fill(right - 1, top, right, bottom, 0xFFFFFFFF);
        drawResizeHandle(gg, left, top);
        drawResizeHandle(gg, right - 3, top);
        drawResizeHandle(gg, left, bottom - 3);
        drawResizeHandle(gg, right - 3, bottom - 3);
    }

    private void drawResizeHandle(GuiGraphics gg, int x, int y) {
        gg.fill(x, y, x + 3, y + 3, 0xFF000000);
        gg.fill(x + 1, y + 1, x + 2, y + 2, 0xFFFFFFFF);
    }

    private void renderStickerMenu(GuiGraphics gg, int mouseX, int mouseY) {
        if (!stickerMenuOpen && !editingStickerText) return;
        int x = stickerMenuX;
        int y = stickerMenuY;
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 950);
        if (editingStickerText) {
            gg.fill(x, y, x + 116, y + 22, 0xFFFFFFFF);
            gg.fill(x + 2, y + 2, x + 114, y + 20, 0xFF303030);
            String shown = stickerText.isEmpty() ? "Type text, press Enter" : stickerText;
            gg.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(shown, 108), x + 5, y + 7, 0xFFFFFFFF, false);
        } else {
            drawOutlinedMenuRow(gg, x, y, 78, 18,
                    mouseX >= x && mouseX <= x + 78 && mouseY >= y && mouseY < y + 18);
            drawOutlinedMenuRow(gg, x, y + 20, 78, 18,
                    mouseX >= x && mouseX <= x + 78 && mouseY >= y + 20 && mouseY < y + 38);
            gg.drawString(minecraft.font, "Add text", x + 7, y + 6, 0xFFFFFFFF, false);
            gg.drawString(minecraft.font, "Add icon", x + 7, y + 24, 0xFFFFFFFF, false);
        }
        gg.pose().popPose();
    }

    private void drawOutlinedMenuRow(GuiGraphics gg, int x, int y, int width, int height, boolean hovered) {
        gg.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        gg.fill(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? 0xFF3A3A3A : 0xEE151515);
    }

    private void renderNodes(GuiGraphics gg, QuestEntryData hovered, CanvasBounds visibleBounds) {
        for (QuestEntryData quest : questsInBounds(visibleBounds)) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            if (!visibleBounds.intersects(x, y, x + NODE_SIZE, y + NODE_SIZE)) continue;
            boolean selected = quest == hovered || quest == selectedQuest || quest == connectorSource;
            String textureKey = normalizeBackground(quest.treeBackground) + (selected ? "-selected" : "");
            gg.blit(NODE_TEXTURES.getOrDefault(textureKey, NODE_TEXTURES.get("square")),
                    x, y, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
            ItemStack icon = questIcons.computeIfAbsent(quest, value -> QuestEditorItemIcons.iconStackFromId(value.icon));
            if (!icon.isEmpty()) gg.renderItem(icon, x + 8, y + 8);
            if (quest == connectorSource) drawNodeSelection(gg, x, y, 0xFFFFFFFF);
            if (originQuests.contains(quest)) {
                gg.blit(ORIGIN_STAR, x + NODE_SIZE - 8, y - 2, 0, 0, 10, 10, 10, 10);
            }
            if (!quest.importWarnings.isEmpty()) {
                gg.blit(IMPORT_WARNING, x + NODE_SIZE - 5, y + NODE_SIZE - 13, 0, 0, 4, 11, 4, 11);
            }
        }
    }

    private boolean isOverImportWarning(QuestEntryData quest, double mouseX, double mouseY) {
        if (quest.importWarnings.isEmpty()) return false;
        double x = canvasX(mouseX);
        double y = canvasY(mouseY);
        int iconX = nodeX(quest) + NODE_SIZE - 5;
        int iconY = nodeY(quest) + NODE_SIZE - 13;
        return x >= iconX && x <= iconX + 4 && y >= iconY && y <= iconY + 11;
    }

    private void renderImportWarningTooltip(GuiGraphics gg, QuestEntryData quest, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.literal("FTB import warning").getVisualOrderText());
        int lineWidth = Math.max(80, Math.min(240, width - 24));
        for (String warning : quest.importWarnings) {
            lines.addAll(minecraft.font.split(Component.literal(warning), lineWidth));
        }
        QuestEditorTooltips.renderFormatted(gg, minecraft.font, lines, mouseX, mouseY);
    }

    private void applyCanvasTransform(GuiGraphics gg) {
        gg.pose().translate(getX() + width / 2f, getY() + height / 2f, 0);
        gg.pose().scale(zoom, zoom, 1f);
        gg.pose().translate(panX, panY, 0);
    }

    private CanvasBounds visibleCanvasBounds() {
        float halfWidth = width / (2f * zoom);
        float halfHeight = height / (2f * zoom);
        return new CanvasBounds(-panX - halfWidth - NODE_SIZE, -panY - halfHeight - NODE_SIZE,
                -panX + halfWidth + NODE_SIZE, -panY + halfHeight + NODE_SIZE);
    }

    private boolean connectorVisible(Connector connector, CanvasBounds visibleBounds) {
        for (int index = 1; index < connector.points.size(); index++) {
            Point start = connector.points.get(index - 1);
            Point end = connector.points.get(index);
            if (visibleBounds.intersects(Math.min(start.x, end.x), Math.min(start.y, end.y),
                    Math.max(start.x, end.x), Math.max(start.y, end.y))) return true;
        }
        return false;
    }

    private void renderTreeTooltip(GuiGraphics gg, Component text, int mouseX, int mouseY) {
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 900);
        QuestEditorTooltips.render(gg, minecraft.font, text, mouseX, mouseY);
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
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 400);
        gg.blit(ZOOM_OUT, x, y, 0, 0, 7, 7, 7, 7);
        gg.blit(ZOOM_CENTRE, x + 10, y - 1, 0, 0, 9, 9, 9, 9);
        gg.blit(ZOOM_IN, x + 22, y, 0, 0, 7, 7, 7, 7);
        gg.pose().popPose();
    }

    private boolean isOverZoomControls(double mouseX, double mouseY) {
        int x = getX() + width - 31;
        int y = getY() + 2;
        return mouseX >= x && mouseX <= x + 29 && mouseY >= y && mouseY <= y + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // tool modes take priority over normal selection and panning
        if (!active || !visible) return false;
        if (stickerMenuOpen && button == 0) {
            if (mouseX >= stickerMenuX && mouseX <= stickerMenuX + 78 && mouseY >= stickerMenuY && mouseY <= stickerMenuY + 38) {
                if (mouseY < stickerMenuY + 20) {
                    stickerMenuOpen = false;
                    if (onStickerTextEditRequested != null) {
                        onStickerTextEditRequested.accept(-1,
                                new QuestData.Sticker("text", "", pendingStickerX, pendingStickerY));
                    } else {
                        editingStickerText = true;
                        stickerText = "";
                    }
                } else {
                    stickerMenuOpen = false;
                    if (onStickerIconRequested != null) onStickerIconRequested.accept(pendingStickerX, pendingStickerY);
                }
                return true;
            }
            stickerMenuOpen = false;
        }
        if (editingStickerText) return true;
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (button == 1 && questAt(mouseX, mouseY) == null) {
            pendingStickerX = canvasNodeX(mouseX);
            pendingStickerY = canvasNodeY(mouseY);
            stickerMenuX = Mth.floor(mouseX);
            stickerMenuY = Mth.floor(mouseY);
            stickerMenuOpen = true;
            return true;
        }
        if (button != 0) return false;
        if (handleZoomClick(mouseX, mouseY)) return true;

        if (tool == Tool.MOVE && selectedStickerIndex >= 0 && resizeHandleContains(selectedStickerIndex, mouseX, mouseY)) {
            QuestData.Sticker sticker = stickers.get(selectedStickerIndex);
            float[] bounds = stickerBounds(sticker);
            resizingStickerIndex = selectedStickerIndex;
            draggedStickerIndex = -1;
            resizeCenterX = (bounds[0] + bounds[2]) / 2d;
            resizeCenterY = (bounds[1] + bounds[3]) / 2d;
            resizeStartDistance = Math.max(1d, Math.hypot(canvasX(mouseX) - resizeCenterX, canvasY(mouseY) - resizeCenterY));
            resizeStartScale = sticker.scale();
            draggedStickerMoved = false;
            return true;
        }

        int clickedStickerIndex = stickerAt(mouseX, mouseY);
        if (clickedStickerIndex >= 0 && tool != Tool.DELETE && tool != Tool.CONNECT && tool != Tool.CREATE) {
            QuestData.Sticker sticker = stickers.get(clickedStickerIndex);
            long now = Util.getMillis();
            if (!"icon".equals(sticker.type()) && clickedStickerIndex == lastStickerClickIndex
                    && now - lastStickerClickMs <= STICKER_DOUBLE_CLICK_MS) {
                lastStickerClickIndex = -1;
                lastStickerClickMs = 0L;
                draggedStickerIndex = -1;
                draggingCanvas = false;
                if (onStickerTextEditRequested != null) onStickerTextEditRequested.accept(clickedStickerIndex, sticker);
                return true;
            }
            lastStickerClickIndex = clickedStickerIndex;
            lastStickerClickMs = now;
            selectedStickerIndex = clickedStickerIndex;
            if (tool == Tool.NONE) return true;
        } else if (clickedStickerIndex < 0) {
            lastStickerClickIndex = -1;
            lastStickerClickMs = 0L;
        }

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
            if (clickedStickerIndex >= 0) {
                removeSticker(clickedStickerIndex);
                return true;
            }
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
            int stickerIndex = stickerAt(mouseX, mouseY);
            if (stickerIndex >= 0) {
                selectedStickerIndex = stickerIndex;
                draggedStickerIndex = stickerIndex;
                QuestData.Sticker sticker = stickers.get(stickerIndex);
                stickerDragOffsetX = canvasNodeX(mouseX) - sticker.x();
                stickerDragOffsetY = canvasNodeY(mouseY) - sticker.y();
                draggedStickerMoved = false;
            } else if (quest != null) {
                selectedStickerIndex = -1;
                draggedQuest = quest;
                draggedQuestMoved = false;
            } else {
                selectedStickerIndex = -1;
                draggingCanvas = true;
            }
            return true;
        }

        if (quest != null) {
            setFocused(true);
            selectedQuest = quest;
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
        if (tool == Tool.MOVE && draggedStickerIndex >= 0 && draggedStickerIndex < stickers.size()) {
            QuestData.Sticker sticker = stickers.get(draggedStickerIndex);
            int nextX = canvasNodeX(mouseX) - stickerDragOffsetX;
            int nextY = canvasNodeY(mouseY) - stickerDragOffsetY;
            if (nextX != sticker.x() || nextY != sticker.y()) {
                List<QuestData.Sticker> updated = new ArrayList<>(stickers);
                updated.set(draggedStickerIndex, new QuestData.Sticker(sticker.type(), sticker.value(), nextX, nextY, sticker.scale()));
                stickers = List.copyOf(updated);
                draggedStickerMoved = true;
            }
            return true;
        }
        if (tool == Tool.MOVE && resizingStickerIndex >= 0 && resizingStickerIndex < stickers.size()) {
            QuestData.Sticker sticker = stickers.get(resizingStickerIndex);
            double distance = Math.hypot(canvasX(mouseX) - resizeCenterX, canvasY(mouseY) - resizeCenterY);
            float nextScale = Mth.clamp((float) (resizeStartScale * distance / resizeStartDistance), 0.25f, 4f);
            if (Math.abs(nextScale - sticker.scale()) > 0.001f) {
                List<QuestData.Sticker> updated = new ArrayList<>(stickers);
                updated.set(resizingStickerIndex, new QuestData.Sticker(sticker.type(), sticker.value(), sticker.x(), sticker.y(), nextScale));
                stickers = List.copyOf(updated);
                draggedStickerMoved = true;
            }
            return true;
        }
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
                updateQuestPosition(draggedQuest, nextX, nextY);
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
        if (draggedStickerMoved && onStickersChanged != null) onStickersChanged.accept(stickers);
        draggedQuest = null;
        draggedQuestMoved = false;
        draggedStickerIndex = -1;
        resizingStickerIndex = -1;
        draggedStickerMoved = false;
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
        if (editingStickerText) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!stickerText.isBlank()) addSticker(new QuestData.Sticker("text", stickerText, pendingStickerX, pendingStickerY));
                editingStickerText = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingStickerText = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !stickerText.isEmpty()) {
                stickerText = stickerText.substring(0, stickerText.offsetByCodePoints(stickerText.length(), -1));
                return true;
            }
            return true;
        }
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

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!editingStickerText) return false;
        if (!Character.isISOControl(codePoint) && stickerText.length() < 80) stickerText += codePoint;
        return true;
    }

    private void addSticker(QuestData.Sticker sticker) {
        List<QuestData.Sticker> updated = new ArrayList<>(stickers);
        updated.add(sticker);
        stickers = List.copyOf(updated);
        if (onStickersChanged != null) onStickersChanged.accept(stickers);
    }

    private void removeSticker(int index) {
        if (index < 0 || index >= stickers.size()) return;
        List<QuestData.Sticker> updated = new ArrayList<>(stickers);
        updated.remove(index);
        stickers = List.copyOf(updated);
        selectedStickerIndex = -1;
        draggedStickerIndex = -1;
        if (onStickersChanged != null) onStickersChanged.accept(stickers);
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
        int cellX = Math.floorDiv(Mth.floor(canvasX), SPATIAL_CELL_SIZE);
        int cellY = Math.floorDiv(Mth.floor(canvasY), SPATIAL_CELL_SIZE);
        List<QuestEntryData> candidates = questSpatialIndex.getOrDefault(packPoint(cellX, cellY), List.of());
        for (int index = candidates.size() - 1; index >= 0; index--) {
            QuestEntryData quest = candidates.get(index);
            int x = nodeX(quest);
            int y = nodeY(quest);
            if (canvasX >= x && canvasX <= x + NODE_SIZE && canvasY >= y && canvasY <= y + NODE_SIZE) return quest;
        }
        return null;
    }

    private int stickerAt(double mouseX, double mouseY) {
        double canvasX = canvasX(mouseX);
        double canvasY = canvasY(mouseY);
        for (int index = stickers.size() - 1; index >= 0; index--) {
            QuestData.Sticker sticker = stickers.get(index);
            float[] bounds = stickerBounds(sticker);
            if (canvasX >= bounds[0] && canvasX <= bounds[2]
                    && canvasY >= bounds[1] && canvasY <= bounds[3]) return index;
        }
        return -1;
    }

    private float[] stickerBounds(QuestData.Sticker sticker) {
        if ("icon".equals(sticker.type())) {
            float radius = 8f * sticker.scale();
            return new float[]{sticker.x() - radius, sticker.y() - radius, sticker.x() + radius, sticker.y() + radius};
        }
        float width = QuestStickerTextRenderer.width(minecraft.font, sticker.value()) * sticker.scale();
        float height = QuestStickerTextRenderer.height(minecraft.font, sticker.value()) * sticker.scale();
        return new float[]{sticker.x(), sticker.y(), sticker.x() + width, sticker.y() + height};
    }

    private boolean resizeHandleContains(int index, double mouseX, double mouseY) {
        if (index < 0 || index >= stickers.size()) return false;
        float[] bounds = stickerBounds(stickers.get(index));
        double x = canvasX(mouseX);
        double y = canvasY(mouseY);
        float tolerance = 5f / Math.max(0.15f, zoom);
        return nearCorner(x, y, bounds[0], bounds[1], tolerance)
                || nearCorner(x, y, bounds[2], bounds[1], tolerance)
                || nearCorner(x, y, bounds[0], bounds[3], tolerance)
                || nearCorner(x, y, bounds[2], bounds[3], tolerance);
    }

    private boolean nearCorner(double x, double y, float cornerX, float cornerY, float tolerance) {
        return Math.abs(x - cornerX) <= tolerance && Math.abs(y - cornerY) <= tolerance;
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
        Point position = questPositions.get(quest);
        return position == null ? -NODE_SIZE / 2 : position.x;
    }

    private int nodeY(QuestEntryData quest) {
        Point position = questPositions.get(quest);
        return position == null ? -NODE_SIZE / 2 : position.y;
    }

    private void updateQuestPosition(QuestEntryData quest, int centerX, int centerY) {
        Point previous = questPositions.get(quest);
        if (previous != null) removeFromSpatialIndex(quest, previous);
        Point updated = new Point(centerX - NODE_SIZE / 2, centerY - NODE_SIZE / 2);
        questPositions.put(quest, updated);
        addToSpatialIndex(quest, updated);
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

    private Connector connector(QuestEntryData dependency, QuestEntryData quest, List<RoutedConnector> routed) {
        int x1 = nodeX(dependency) + NODE_SIZE / 2;
        int y1 = nodeY(dependency) + NODE_SIZE / 2;
        int x2 = nodeX(quest) + NODE_SIZE / 2;
        int y2 = nodeY(quest) + NODE_SIZE / 2;
        List<Connector> candidates = new ArrayList<>();
        candidates.add(route(new Point(x1, y1), new Point(x2, y2)));
        candidates.add(route(new Point(x1, y1), new Point(x2, y1), new Point(x2, y2)));
        candidates.add(route(new Point(x1, y1), new Point(x1, y2), new Point(x2, y2)));

        int middleX = Math.round((x1 + x2) / 2f / GRID_SIZE) * GRID_SIZE;
        int middleY = Math.round((y1 + y2) / 2f / GRID_SIZE) * GRID_SIZE;
        int[] offsets = {0, GRID_SIZE, -GRID_SIZE, GRID_SIZE * 2, -GRID_SIZE * 2,
                GRID_SIZE * 3, -GRID_SIZE * 3, GRID_SIZE * 4, -GRID_SIZE * 4};
        for (int offset : offsets) {
            int bendX = middleX + offset;
            candidates.add(route(new Point(x1, y1), new Point(bendX, y1),
                    new Point(bendX, y2), new Point(x2, y2)));
            int bendY = middleY + offset;
            candidates.add(route(new Point(x1, y1), new Point(x1, bendY),
                    new Point(x2, bendY), new Point(x2, y2)));
        }

        Connector best = candidates.get(0);
        long bestScore = Long.MAX_VALUE;
        for (Connector candidate : candidates) {
            long score = routeScore(candidate, dependency, quest, routed);
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private Connector simpleConnector(QuestEntryData dependency, QuestEntryData quest) {
        int x1 = nodeX(dependency) + NODE_SIZE / 2;
        int y1 = nodeY(dependency) + NODE_SIZE / 2;
        int x2 = nodeX(quest) + NODE_SIZE / 2;
        int y2 = nodeY(quest) + NODE_SIZE / 2;
        int middleX = Math.round((x1 + x2) / 2f / GRID_SIZE) * GRID_SIZE;
        return route(new Point(x1, y1), new Point(middleX, y1), new Point(middleX, y2), new Point(x2, y2));
    }

    private Connector route(Point... rawPoints) {
        List<Point> points = new ArrayList<>();
        for (Point point : rawPoints) {
            if (point == null) continue;
            if (!points.isEmpty() && points.get(points.size() - 1).equals(point)) continue;
            points.add(point);
        }
        return new Connector(List.copyOf(points));
    }

    private long routeScore(Connector candidate, QuestEntryData dependency, QuestEntryData quest,
                            List<RoutedConnector> routed) {
        Set<Long> pixels = routePixels(candidate);
        long score = Math.max(0, candidate.points.size() - 2) * 12L;
        for (int index = 1; index < candidate.points.size(); index++) {
            Point start = candidate.points.get(index - 1);
            Point end = candidate.points.get(index);
            score += Math.abs(end.x - start.x) + Math.abs(end.y - start.y);
        }

        for (QuestEntryData other : quests) {
            if (other == dependency || other == quest) continue;
            int left = nodeX(other) - 4;
            int top = nodeY(other) - 4;
            int right = left + NODE_SIZE + 8;
            int bottom = top + NODE_SIZE + 8;
            for (long packed : pixels) {
                int x = (int) (packed >> 32);
                int y = (int) packed;
                if (x >= left && x <= right && y >= top && y <= bottom) score += 8_000L;
            }
        }

        for (RoutedConnector existing : routed) {
            Set<Long> existingPixels = existing.pixels;
            for (long packed : pixels) {
                int x = (int) (packed >> 32);
                int y = (int) packed;
                if (nearEndpoint(x, y, candidate) || nearEndpoint(x, y, existing.connector)) continue;
                if (existingPixels.contains(packed)) {
                    score += 2_000L;
                    continue;
                }
                for (int offsetX = -2; offsetX <= 2; offsetX++) {
                    for (int offsetY = -2; offsetY <= 2; offsetY++) {
                        if (existingPixels.contains(packPoint(x + offsetX, y + offsetY))) score += 20L;
                    }
                }
            }
        }
        return score;
    }

    private boolean nearEndpoint(int x, int y, Connector connector) {
        if (connector.points.isEmpty()) return false;
        Point first = connector.points.get(0);
        Point last = connector.points.get(connector.points.size() - 1);
        int radius = NODE_SIZE / 2 + 4;
        return Math.abs(x - first.x) <= radius && Math.abs(y - first.y) <= radius
                || Math.abs(x - last.x) <= radius && Math.abs(y - last.y) <= radius;
    }

    private Set<Long> routePixels(Connector connector) {
        Set<Long> pixels = new HashSet<>();
        for (int index = 1; index < connector.points.size(); index++) {
            Point start = connector.points.get(index - 1);
            Point end = connector.points.get(index);
            rasterRouteLine(start.x, start.y, end.x, end.y, pixels);
        }
        return pixels;
    }

    private void rasterRouteLine(int x1, int y1, int x2, int y2, Set<Long> pixels) {
        int distance = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (distance > MAX_CONNECTOR_RASTER_STEPS) {
            for (int step = 0; step <= MAX_CONNECTOR_RASTER_STEPS; step++) {
                double progress = step / (double) MAX_CONNECTOR_RASTER_STEPS;
                int x = (int) Math.round(x1 + (x2 - x1) * progress);
                int y = (int) Math.round(y1 + (y2 - y1) * progress);
                pixels.add(packPoint(x, y));
            }
            return;
        }
        int dx = Math.abs(x2 - x1);
        int sx = x1 < x2 ? 1 : -1;
        int dy = -Math.abs(y2 - y1);
        int sy = y1 < y2 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            pixels.add(packPoint(x1, y1));
            if (x1 == x2 && y1 == y2) return;
            int twice = error * 2;
            if (twice >= dy) { error += dy; x1 += sx; }
            if (twice <= dx) { error += dx; y1 += sy; }
        }
    }

    private long packPoint(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private void drawConnector(GuiGraphics gg, RenderedConnector connector) {
        int color = connector.incompatible ? 0xFFFF3030 : 0xFFFFFFFF;
        for (int index = 1; index < connector.connector.points.size(); index++) {
            Point start = connector.connector.points.get(index - 1);
            Point end = connector.connector.points.get(index);
            drawConnectorSegment(gg, start, end, CONNECTOR_OUTER_WIDTH, 0xFF000000);
            drawConnectorSegment(gg, start, end, CONNECTOR_INNER_WIDTH, color);
        }
    }

    private static void drawConnectorSegment(GuiGraphics gg, Point start, Point end, int thickness, int color) {
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        int length = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        int half = thickness / 2;
        gg.pose().pushPose();
        gg.pose().translate(start.x, start.y, 0);
        gg.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        gg.fill(0, -half, length + 1, thickness - half, color);
        gg.pose().popPose();
    }

    private RenderedConnector renderedConnector(QuestEntryData dependency, QuestEntryData quest, boolean incompatible,
                                                List<RoutedConnector> routed) {
        Connector connector = connector(dependency, quest, routed);
        return new RenderedConnector(dependency, quest, connector, incompatible);
    }

    private void rebuildCaches() {
        // rebuild positions once after data changes instead of during rendering
        Map<String, QuestEntryData> byId = new HashMap<>();
        Map<QuestEntryData, Point> positions = new IdentityHashMap<>();
        Set<QuestEntryData> origins = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (int index = 0; index < quests.size(); index++) {
            QuestEntryData quest = quests.get(index);
            if (quest.id != null && !quest.id.isBlank()) byId.put(quest.id, quest);
            int centerX = parse(quest.treeX, (index % 4 - 2) * 48);
            int centerY = parse(quest.treeY, (index / 4) * 48);
            positions.put(quest, new Point(centerX - NODE_SIZE / 2, centerY - NODE_SIZE / 2));
            if (parseBoolean(quest.treeOrigin)) origins.add(quest);
        }
        questsById = byId;
        questPositions = positions;
        questIcons = new IdentityHashMap<>();
        rebuildSpatialIndex();
        originQuests = origins;
        rebuildConnectorCache();
    }

    private void rebuildConnectorCache() {
        // large packs use straight links to avoid expensive route searches
        List<RenderedConnector> connectors = new ArrayList<>();
        List<RoutedConnector> routed = new ArrayList<>();
        for (QuestEntryData quest : quests) {
            Set<String> incompatible = new HashSet<>(incompatibleDependencies(quest));
            for (String dependencyId : dependencies(quest)) {
                QuestEntryData dependency = questById(dependencyId);
                if (dependency != null) {
                    boolean detailedRouting = quests.size() <= DETAILED_ROUTING_MAX_QUESTS
                            && connectors.size() < DETAILED_ROUTING_MAX_CONNECTORS;
                    RenderedConnector connector = detailedRouting
                            ? renderedConnector(dependency, quest, incompatible.contains(dependencyId), routed)
                            : new RenderedConnector(dependency, quest, simpleConnector(dependency, quest),
                            incompatible.contains(dependencyId));
                    connectors.add(connector);
                    if (detailedRouting) {
                        routed.add(new RoutedConnector(connector.connector, routePixels(connector.connector)));
                    }
                }
            }
        }
        renderedConnectors = List.copyOf(connectors);
    }

    private void rebuildSpatialIndex() {
        // bucket nodes so hover and selection checks stay local
        Map<Long, List<QuestEntryData>> spatialIndex = new HashMap<>();
        questSpatialIndex = spatialIndex;
        for (QuestEntryData quest : quests) {
            Point position = questPositions.get(quest);
            if (position != null) addToSpatialIndex(quest, position);
        }
    }

    private void addToSpatialIndex(QuestEntryData quest, Point position) {
        int minCellX = Math.floorDiv(position.x, SPATIAL_CELL_SIZE);
        int minCellY = Math.floorDiv(position.y, SPATIAL_CELL_SIZE);
        int maxCellX = Math.floorDiv(position.x + NODE_SIZE, SPATIAL_CELL_SIZE);
        int maxCellY = Math.floorDiv(position.y + NODE_SIZE, SPATIAL_CELL_SIZE);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                questSpatialIndex.computeIfAbsent(packPoint(cellX, cellY), ignored -> new ArrayList<>()).add(quest);
            }
        }
    }

    private void removeFromSpatialIndex(QuestEntryData quest, Point position) {
        int minCellX = Math.floorDiv(position.x, SPATIAL_CELL_SIZE);
        int minCellY = Math.floorDiv(position.y, SPATIAL_CELL_SIZE);
        int maxCellX = Math.floorDiv(position.x + NODE_SIZE, SPATIAL_CELL_SIZE);
        int maxCellY = Math.floorDiv(position.y + NODE_SIZE, SPATIAL_CELL_SIZE);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                long key = packPoint(cellX, cellY);
                List<QuestEntryData> cell = questSpatialIndex.get(key);
                if (cell == null) continue;
                cell.remove(quest);
                if (cell.isEmpty()) questSpatialIndex.remove(key);
            }
        }
    }

    private List<QuestEntryData> questsInBounds(CanvasBounds bounds) {
        int minCellX = Math.floorDiv(Mth.floor(bounds.left), SPATIAL_CELL_SIZE);
        int minCellY = Math.floorDiv(Mth.floor(bounds.top), SPATIAL_CELL_SIZE);
        int maxCellX = Math.floorDiv(Mth.floor(bounds.right), SPATIAL_CELL_SIZE);
        int maxCellY = Math.floorDiv(Mth.floor(bounds.bottom), SPATIAL_CELL_SIZE);
        long cellCount = (long) (maxCellX - minCellX + 1) * (maxCellY - minCellY + 1);
        if (cellCount > quests.size()) {
            return quests.stream().filter(quest -> {
                Point position = questPositions.get(quest);
                return position != null && bounds.intersects(position.x, position.y,
                        position.x + NODE_SIZE, position.y + NODE_SIZE);
            }).toList();
        }
        Set<QuestEntryData> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<QuestEntryData> visible = new ArrayList<>();
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                for (QuestEntryData quest : questSpatialIndex.getOrDefault(packPoint(cellX, cellY), List.of())) {
                    if (seen.add(quest)) visible.add(quest);
                }
            }
        }
        return visible;
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
        List<QuestEntryData> snapCandidates = quests.size() <= DETAILED_ROUTING_MAX_QUESTS
                ? quests : questsInBounds(new CanvasBounds(rawX - 128, rawY - 128, rawX + 128, rawY + 128));
        for (QuestEntryData quest : snapCandidates) {
            if (quest == moving) continue;
            int otherX = nodeX(quest) + NODE_SIZE / 2;
            int otherY = nodeY(quest) + NODE_SIZE / 2;
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

    private record CanvasBounds(float left, float top, float right, float bottom) {
        private boolean intersects(float otherLeft, float otherTop, float otherRight, float otherBottom) {
            return otherRight >= left && otherLeft <= right && otherBottom >= top && otherTop <= bottom;
        }
    }

    private record Connector(List<Point> points) {
    }

    private record RoutedConnector(Connector connector, Set<Long> pixels) {
    }

    private record DependencyEdge(QuestEntryData dependency, QuestEntryData quest) {
    }

    private record RenderedConnector(QuestEntryData dependency, QuestEntryData quest, Connector connector,
                                     boolean incompatible) {
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
