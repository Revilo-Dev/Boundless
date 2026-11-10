package net.revilodev.boundless.client;

import com.mojang.math.Axis;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestItemSpec;
import net.revilodev.boundless.quest.QuestTracker;
import net.revilodev.boundless.client.editor.QuestEditorItemIcons;
import net.revilodev.boundless.client.editor.QuestStickerTextRenderer;
import net.revilodev.boundless.client.editor.QuestTreeConnectorRouting;
import net.revilodev.boundless.client.editor.QuestTreeConnectorRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// shared quest tree canvas
@OnlyIn(Dist.CLIENT)
// renders the playable quest tree and keeps its view state
public final class QuestTreeWidget extends AbstractWidget {
    private static final ResourceLocation ZOOM_IN = texture("button-zoom-in");
    private static final ResourceLocation ZOOM_OUT = texture("button-zoom-out");
    private static final ResourceLocation ZOOM_CENTRE = texture("button-zoom-centre");
    private static final ResourceLocation ICON_COMPLETE = texture("icon-complete");
    private static final ResourceLocation ICON_ATTENTION = texture("icon-attention");
    private static final int NODE_SIZE = 32;
    private static final Map<String, ViewState> SAVED_VIEWS = new HashMap<>();

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Consumer<QuestData.Quest> onQuestSelected;
    private List<QuestData.Quest> quests = List.of();
    private List<RenderedConnector> renderedConnectors = List.of();
    private final Map<String, Integer> questIndexes = new HashMap<>();
    private List<QuestData.Sticker> stickers = List.of();
    private float zoom = 1f;
    private float panX;
    private float panY;
    private boolean dragging;
    private String selectedQuestId = "";
    private String background = "";
    private float backgroundTileScale = 0.5f;
    private boolean backgroundDarkening;
    private float backgroundDarkeningOpacity = 0.45f;
    private boolean tooltipSuppressed;
    private String viewKey = "all";
    private boolean viewInitialized;
    private boolean centreWhenQuestsLoad;
    private final Map<String, QuestState> questStates = new HashMap<>();
    private long questStateTick = Long.MIN_VALUE;
    private long connectorGeneration;
    private boolean connectorLayoutRequested;

    public QuestTreeWidget(int x, int y, int width, int height, Consumer<QuestData.Quest> onQuestSelected) {
        super(x, y, width, height, Component.empty());
        this.onQuestSelected = onQuestSelected;
    }

    // update the visible quest nodes
    public void setQuests(List<QuestData.Quest> quests) {
        setQuests(quests, true);
    }

    public void setQuests(List<QuestData.Quest> quests, boolean categoryLoaded) {
        List<QuestData.Quest> source = quests == null ? List.of() : quests;
        if (this.quests.equals(source)) {
            if (categoryLoaded && !connectorLayoutRequested) rebuildConnectorCacheAsync();
            if (centreWhenQuestsLoad && !this.quests.isEmpty()) {
                centreWhenQuestsLoad = false;
                centreView();
            }
            return;
        }
        this.quests = List.copyOf(source);
        questIndexes.clear();
        for (int index = 0; index < this.quests.size(); index++) {
            QuestData.Quest quest = this.quests.get(index);
            if (quest != null && quest.id != null) questIndexes.put(quest.id, index);
        }
        connectorGeneration++;
        connectorLayoutRequested = false;
        renderedConnectors = List.of();
        if (categoryLoaded) rebuildConnectorCacheAsync();
        if (centreWhenQuestsLoad && !this.quests.isEmpty()) {
            centreWhenQuestsLoad = false;
            centreView();
        }
        invalidateQuestStates();
    }

    public void setViewKey(String viewKey) {
        String nextKey = viewKey == null || viewKey.isBlank() ? "all" : viewKey;
        if (viewInitialized && this.viewKey.equals(nextKey)) return;
        if (viewInitialized) saveView();
        this.viewKey = nextKey;
        ViewState saved = SAVED_VIEWS.get(this.viewKey);
        if (saved == null) {
            zoom = 1f;
            panX = 0f;
            panY = 0f;
            centreWhenQuestsLoad = true;
        } else {
            zoom = saved.zoom;
            panX = saved.panX;
            panY = saved.panY;
            centreWhenQuestsLoad = false;
        }
        viewInitialized = true;
    }

    public void setStickers(List<QuestData.Sticker> stickers) {
        List<QuestData.Sticker> source = stickers == null ? List.of() : stickers;
        if (this.stickers.equals(source)) return;
        this.stickers = List.copyOf(source);
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

    public void setTooltipSuppressed(boolean tooltipSuppressed) {
        this.tooltipSuppressed = tooltipSuppressed;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void selectQuest(String questId) {
        selectedQuestId = questId == null ? "" : questId;
    }

    // reset view to the canvas centre
    public void centreView() {
        zoom = 1f;
        QuestData.Quest origin = quests.stream().filter(quest -> quest.treeOrigin).findFirst().orElse(null);
        panX = origin == null ? 0f : -origin.treeX;
        panY = origin == null ? 0f : -origin.treeY;
        saveView();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // render links before nodes so icons always stay readable
        if (!visible) return;
        QuestData.Quest hovered = tooltipSuppressed || isOverZoomControls(mouseX, mouseY) ? null : questAt(mouseX, mouseY);
        QuestCategoryBackgroundRenderer.render(gg, background, backgroundDarkening, backgroundDarkeningOpacity,
                backgroundTileScale,
                getX(), getY(), width, height);
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        gg.pose().pushPose();
        applyCanvasTransform(gg);
        renderStickers(gg);
        renderConnectors(gg, hovered);
        renderNodes(gg, hovered);
        gg.pose().popPose();
        gg.flush();
        gg.disableScissor();
        renderZoomButtons(gg);
        if (hovered != null) {
            gg.pose().pushPose();
            gg.pose().translate(0, 0, 10000);
            gg.renderComponentTooltip(minecraft.font, questTooltip(hovered), mouseX, mouseY);
            gg.pose().popPose();
        }
    }

    private void renderConnectors(GuiGraphics gg, QuestData.Quest hovered) {
        for (RenderedConnector connector : renderedConnectors) {
            if (!isQuestShown(connector.quest) || !isQuestShown(connector.dependency)) continue;
            boolean highlighted = hovered != null && (hovered.id.equals(connector.quest.id)
                    || hovered.id.equals(connector.dependency.id));
            QuestTracker.Status dependencyStatus = questState(connector.dependency).status;
            boolean completed = dependencyStatus == QuestTracker.Status.REDEEMED;
            drawConnector(gg, connector.connector, connector.incompatible, completed, highlighted);
        }
    }

    private void rebuildConnectorCacheAsync() {
        long generation = ++connectorGeneration;
        connectorLayoutRequested = true;
        List<QuestTreeConnectorRouting.Node> nodes = new ArrayList<>();
        for (QuestData.Quest quest : quests) {
            if (quest == null || quest.id == null || quest.id.isBlank()) continue;
            nodes.add(new QuestTreeConnectorRouting.Node(quest.id, nodeX(quest), nodeY(quest)));
        }
        List<QuestTreeConnectorRouting.Edge> edges = new ArrayList<>();
        for (QuestData.Quest quest : quests) {
            if (quest == null || quest.id == null || quest.id.isBlank()) continue;
            for (String dependencyId : quest.dependencies) {
                if (questIndexes.containsKey(dependencyId)) {
                    edges.add(new QuestTreeConnectorRouting.Edge(dependencyId, quest.id));
                }
            }
        }
        List<QuestTreeConnectorRouting.Node> nodeSnapshot = List.copyOf(nodes);
        List<QuestTreeConnectorRouting.Edge> edgeSnapshot = List.copyOf(edges);
        CompletableFuture.supplyAsync(() -> QuestTreeConnectorRouting.route(nodeSnapshot, edgeSnapshot))
                .exceptionally(ignored -> List.of())
                .thenAccept(routed -> minecraft.execute(() -> applyConnectorRoutes(generation, routed)));
    }

    private void applyConnectorRoutes(long generation, List<QuestTreeConnectorRouting.RoutedEdge> routedEdges) {
        if (generation != connectorGeneration) return;
        Map<String, QuestData.Quest> byId = new HashMap<>();
        for (QuestData.Quest quest : quests) {
            if (quest != null && quest.id != null) byId.put(quest.id, quest);
        }
        List<RenderedConnector> connectors = new ArrayList<>();
        for (QuestTreeConnectorRouting.RoutedEdge routed : routedEdges) {
            QuestData.Quest dependency = byId.get(routed.edge().fromId());
            QuestData.Quest quest = byId.get(routed.edge().toId());
            if (dependency == null || quest == null) continue;
            List<Point> points = routed.points().stream().map(point -> new Point(point.x(), point.y())).toList();
            connectors.add(new RenderedConnector(dependency, quest, new Connector(points),
                    quest.incompatibleDependencies.contains(dependency.id)));
        }
        renderedConnectors = List.copyOf(connectors);
    }

    private void renderNodes(GuiGraphics gg, QuestData.Quest hovered) {
        for (QuestData.Quest quest : quests) {
            if (!isQuestShown(quest)) continue;
            int x = nodeX(quest);
            int y = nodeY(quest);
            QuestState state = questState(quest);
            boolean complete = isComplete(quest);
            boolean selected = selectedQuestId.equals(quest.id);
            gg.blit(nodeTexture(quest.treeBackground, complete, selected || quest == hovered),
                    x, y, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
            QuestEditorItemIcons.renderIcon(gg, quest.icon, x + 8, y + 8);
            if (complete) {
                gg.blit(ICON_COMPLETE, x + 18, y + 18, 0, 0, 11, 11, 11, 11);
            } else if (state.redeemable) {
                gg.blit(ICON_ATTENTION, x + 23, y + 10, 0, 0, 6, 13, 6, 13);
            }
        }
    }

    private void renderStickers(GuiGraphics gg) {
        for (QuestData.Sticker sticker : stickers) {
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
        }
    }

    private void applyCanvasTransform(GuiGraphics gg) {
        gg.pose().translate(getX() + width / 2f, getY() + height / 2f, 0);
        gg.pose().scale(zoom, zoom, 1f);
        gg.pose().translate(panX, panY, 0);
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

    private int nodeX(QuestData.Quest quest) {
        int index = questIndexes.getOrDefault(quest.id, 0);
        int baseX = quest.treeX != -1 ? quest.treeX : (index % 4 - 2) * 48;
        return baseX - NODE_SIZE / 2;
    }

    private int nodeY(QuestData.Quest quest) {
        int index = questIndexes.getOrDefault(quest.id, 0);
        int baseY = quest.treeY != -1 ? quest.treeY : (index / 4) * 48;
        return baseY - NODE_SIZE / 2;
    }

    private void zoomAt(double mouseX, double mouseY, float targetZoom) {
        // keep the point under the cursor fixed while zooming
        float nextZoom = Mth.clamp(targetZoom, .15f, 2f);
        if (nextZoom == zoom) return;
        float centerX = getX() + width / 2f;
        float centerY = getY() + height / 2f;
        float canvasX = (float) ((mouseX - centerX) / zoom - panX);
        float canvasY = (float) ((mouseY - centerY) / zoom - panY);
        zoom = nextZoom;
        panX = (float) ((mouseX - centerX) / zoom - canvasX);
        panY = (float) ((mouseY - centerY) / zoom - canvasY);
        saveView();
    }

    private boolean isOverZoomControls(double mouseX, double mouseY) {
        int x = getX() + width - 31;
        int y = getY() + 2;
        return mouseX >= x && mouseX <= x + 29 && mouseY >= y && mouseY <= y + 10;
    }

    private void drawConnector(GuiGraphics gg, Connector connector, boolean incompatible, boolean completed, boolean highlighted) {
        int color = highlighted ? QuestTreeConnectorRenderer.HIGHLIGHTED_COLOR
                : incompatible ? QuestTreeConnectorRenderer.INCOMPATIBLE_COLOR
                : completed ? QuestTreeConnectorRenderer.COMPLETED_COLOR
                : QuestTreeConnectorRenderer.NORMAL_COLOR;
        for (int index = 1; index < connector.points.size(); index++) {
            Point start = connector.points.get(index - 1);
            Point end = connector.points.get(index);
            QuestTreeConnectorRenderer.drawOutlineSegment(gg, start.x, start.y, end.x, end.y);
        }
        for (int index = 1; index < connector.points.size(); index++) {
            Point start = connector.points.get(index - 1);
            Point end = connector.points.get(index);
            QuestTreeConnectorRenderer.drawInnerSegment(gg, start.x, start.y, end.x, end.y, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        if (button == 1) {
            if (isOverZoomControls(mouseX, mouseY)) return false;
            selectedQuestId = "";
            centreOn(mouseX, mouseY);
            return true;
        }
        if (button != 0) return false;
        int zoomX = getX() + width - 31;
        int zoomY = getY() + 3;
        if (mouseY >= zoomY - 1 && mouseY <= zoomY + 9) {
            if (mouseX >= zoomX && mouseX <= zoomX + 7) zoomAt(mouseX, mouseY, zoom - .1f);
            else if (mouseX >= zoomX + 10 && mouseX <= zoomX + 19) centreView();
            else if (mouseX >= zoomX + 22 && mouseX <= zoomX + 29) zoomAt(mouseX, mouseY, zoom + .1f);
            else return false;
            return true;
        }
        QuestData.Quest quest = questAt(mouseX, mouseY);
        if (quest != null) {
            selectedQuestId = quest.id;
            if (onQuestSelected != null) onQuestSelected.accept(quest);
            return true;
        }
        selectedQuestId = "";
        dragging = true;
        setFocused(true);
        return true;
    }

    private void centreOn(double mouseX, double mouseY) {
        float centerX = getX() + width / 2f;
        float centerY = getY() + height / 2f;
        float canvasX = (float) ((mouseX - centerX) / zoom - panX);
        float canvasY = (float) ((mouseY - centerY) / zoom - panY);
        panX = -canvasX;
        panY = -canvasY;
        saveView();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging || button != 0) return false;
        panX += (float) dragX / zoom;
        panY += (float) dragY / zoom;
        saveView();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!visible || !active || !isMouseOver(mouseX, mouseY)) return false;
        zoomAt(mouseX, mouseY, zoom + (float) deltaY * .1f);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !active) return false;
        double centerX = getX() + width / 2D;
        double centerY = getY() + height / 2D;
        if (keyCode == GLFW.GLFW_KEY_KP_ADD
                || keyCode == GLFW.GLFW_KEY_EQUAL && Screen.hasShiftDown()) {
            zoomAt(centerX, centerY, zoom + .1f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoomAt(centerX, centerY, zoom - .1f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_EQUAL) {
            centreView();
            return true;
        }
        return false;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/tree/" + name + ".png");
    }

    private static ResourceLocation nodeTexture(String background, boolean complete, boolean selected) {
        String shape = background == null || background.isBlank() ? "square" : background.replace('_', '-');
        return texture(shape + (complete ? "-complete" : "-incomplete") + (selected ? "-selected" : ""));
    }

    private boolean isComplete(QuestData.Quest quest) {
        return questState(quest).status == QuestTracker.Status.REDEEMED;
    }

    private boolean isQuestShown(QuestData.Quest quest) {
        if (quest == null || minecraft.player == null) return quest != null;
        QuestState state = questState(quest);
        if (!QuestFilterBar.treeAllowCompleted() && state.status == QuestTracker.Status.REDEEMED) return false;
        if (!QuestFilterBar.treeAllowRejected() && state.status == QuestTracker.Status.REJECTED) return false;
        return QuestFilterBar.treeAllowLocked() || state.dependenciesMet;
    }

    private QuestState questState(QuestData.Quest quest) {
        if (quest == null || minecraft.player == null) {
            return new QuestState(QuestTracker.Status.INCOMPLETE, true, false);
        }
        long tick = minecraft.level == null ? Long.MIN_VALUE : minecraft.level.getGameTime();
        if (questStateTick != tick) {
            questStates.clear();
            questStateTick = tick;
        }
        return questStates.computeIfAbsent(quest.id, ignored -> new QuestState(
                QuestTracker.getStatus(quest, minecraft.player),
                QuestTracker.dependenciesMet(quest, minecraft.player),
                QuestTracker.isRedeemable(quest, minecraft.player)
        ));
    }

    private void invalidateQuestStates() {
        // progress changes invalidate colours without rebuilding layout
        questStates.clear();
        questStateTick = Long.MIN_VALUE;
    }

    private void saveView() {
        // remember pan and zoom separately for each category tree
        if (!viewInitialized || viewKey == null || viewKey.isBlank()) return;
        SAVED_VIEWS.put(viewKey, new ViewState(zoom, panX, panY));
    }

    private List<Component> questTooltip(QuestData.Quest quest) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(quest.name));
        if (quest.completion == null || quest.completion.targets.isEmpty()) return lines;
        for (QuestData.Target target : quest.completion.targets) {
            lines.add(requirementLine(quest, target));
        }
        return lines;
    }

    private Component requirementLine(QuestData.Quest quest, QuestData.Target target) {
        if (target == null) return Component.translatable("ui.boundless.questbook.unknown");
        boolean complete = minecraft.player != null && QuestTracker.isTargetSatisfied(quest, target, minecraft.player);
        MutableComponent line = objectiveLabel(target).withStyle(style -> style.withColor(objectiveLabelColor(target)));
        return line.append(Component.literal(" " + objectiveText(quest, target))
                .withStyle(style -> style.withColor(complete ? Config.completedTextColor() : Config.incompleteTextColor())));
    }

    private MutableComponent objectiveLabel(QuestData.Target target) {
        if (target.isAdvancement()) return Component.translatable("ui.boundless.questbook.advance");
        if (target.isItem()) return Component.translatable("ui.boundless.questbook.collect");
        if (target.isSubmit() || target.isXp() || target.isLevelUpLevel()) return Component.translatable("ui.boundless.questbook.submit");
        if (target.isEntity()) return Component.translatable("ui.boundless.questbook.kill");
        if (target.isEffect()) return Component.translatable("ui.boundless.questbook.have_effect");
        if (target.isObserve()) return Component.translatable("ui.boundless.questbook.observe");
        if (target.isBiome()) return Component.translatable("ui.boundless.questbook.biome");
        if (target.isDimension()) return Component.translatable("ui.boundless.questbook.dimension");
        return Component.translatable("quest.boundless.complete").append(":");
    }

    private int objectiveLabelColor(QuestData.Target target) {
        if (target.isItem()) return Config.collectTextColor();
        if (target.isSubmit() || target.isXp() || target.isLevelUpLevel() || target.isFieldInput()) return Config.submitTextColor();
        if (target.isEntity()) return Config.killTextColor();
        if (target.isEffect()) return Config.effectTextColor();
        if (target.isAdvancement()) return Config.achieveTextColor();
        if (target.isObserve()) return Config.observeTextColor();
        if (target.isBiome()) return Config.biomeTextColor();
        if (target.isDimension()) return Config.dimensionTextColor();
        return Config.requiresTextColor();
    }

    private String objectiveText(QuestData.Quest quest, QuestData.Target target) {
        String name = objectiveName(target);
        if (minecraft.player == null) return name;
        if (target.isItem() || target.isSubmit()) {
            int have = Math.min(QuestTracker.getTrackedItemProgress(quest, target, minecraft.player), target.count);
            return name + " " + have + "/" + target.count;
        }
        if (target.isEntity()) {
            int have = Math.min(QuestTracker.getAcceptedKillCount(target, minecraft.player), target.count);
            return name + " " + have + "/" + target.count;
        }
        if (target.isXp()) {
            int have = Math.min(QuestTracker.getXpAmount(minecraft.player, target.id), target.count);
            return name + " " + have + "/" + target.count;
        }
        return name;
    }

    private String objectiveName(QuestData.Target target) {
        if (target.hint != null && !target.hint.isBlank()) return target.hint;
        if (target.isAdvancement()) {
            ResourceLocation id = ResourceLocation.tryParse(target.id);
            AdvancementHolder holder = id == null || minecraft.getConnection() == null
                    ? null : minecraft.getConnection().getAdvancements().get(id);
            if (holder != null) {
                DisplayInfo display = holder.value().display().orElse(null);
                if (display != null) return display.getTitle().getString();
            }
        }
        if (target.isItem() || target.isSubmit()) {
            QuestItemSpec spec = QuestItemSpec.parse(target.id);
            Item item = spec.item();
            if (item != null) return new ItemStack(item).getHoverName().getString();
        }
        if (target.isEntity()) {
            ResourceLocation id = ResourceLocation.tryParse(target.id);
            if (id != null) {
                var entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
                if (entity != null) return entity.getDescription().getString();
            }
        }
        return target.id == null || target.id.isBlank()
                ? Component.translatable("ui.boundless.questbook.unknown").getString() : target.id;
    }

    private QuestData.Quest questAt(double mouseX, double mouseY) {
        double canvasX = canvasX(mouseX);
        double canvasY = canvasY(mouseY);
        for (int index = quests.size() - 1; index >= 0; index--) {
            QuestData.Quest quest = quests.get(index);
            if (!isQuestShown(quest)) continue;
            int x = nodeX(quest);
            int y = nodeY(quest);
            if (canvasX >= x && canvasX <= x + NODE_SIZE && canvasY >= y && canvasY <= y + NODE_SIZE) return quest;
        }
        return null;
    }

    private double canvasX(double screenX) {
        return (screenX - (getX() + width / 2D)) / zoom - panX;
    }

    private double canvasY(double screenY) {
        return (screenY - (getY() + height / 2D)) / zoom - panY;
    }

    private record Point(int x, int y) {
    }

    private record Connector(List<Point> points) {
    }

    private record RenderedConnector(QuestData.Quest dependency, QuestData.Quest quest, Connector connector,
                                     boolean incompatible) {
    }

    private record ViewState(float zoom, float panX, float panY) {
    }

    private record QuestState(QuestTracker.Status status, boolean dependenciesMet, boolean redeemable) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
