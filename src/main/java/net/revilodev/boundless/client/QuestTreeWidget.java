package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// shared quest tree canvas
@OnlyIn(Dist.CLIENT)
public final class QuestTreeWidget extends AbstractWidget {
    private static final ResourceLocation ZOOM_IN = texture("button-zoom-in");
    private static final ResourceLocation ZOOM_OUT = texture("button-zoom-out");
    private static final ResourceLocation ZOOM_CENTRE = texture("button-zoom-centre");
    private static final ResourceLocation ICON_COMPLETE = texture("icon-complete");
    private static final ResourceLocation ICON_ATTENTION = texture("icon-attention");
    private static final int NODE_SIZE = 32;
    private static final int CONNECTOR_OUTER_WIDTH = 5;
    private static final int CONNECTOR_INNER_WIDTH = 3;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Consumer<QuestData.Quest> onQuestSelected;
    private List<QuestData.Quest> quests = List.of();
    private float zoom = 1f;
    private float panX;
    private float panY;
    private boolean dragging;
    private String selectedQuestId = "";

    public QuestTreeWidget(int x, int y, int width, int height, Consumer<QuestData.Quest> onQuestSelected) {
        super(x, y, width, height, Component.empty());
        this.onQuestSelected = onQuestSelected;
    }

    // update the visible quest nodes
    public void setQuests(List<QuestData.Quest> quests) {
        this.quests = quests == null ? List.of() : List.copyOf(quests);
    }

    // reset view to the canvas centre
    public void centreView() {
        zoom = 1f;
        QuestData.Quest origin = quests.stream().filter(quest -> quest.treeOrigin).findFirst().orElse(null);
        panX = origin == null ? 0f : -origin.treeX;
        panY = origin == null ? 0f : -origin.treeY;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        QuestData.Quest hovered = questAt(mouseX, mouseY);
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        gg.pose().pushPose();
        applyCanvasTransform(gg);
        renderConnectors(gg);
        renderNodes(gg);
        gg.pose().popPose();
        gg.disableScissor();
        renderZoomButtons(gg);
        if (hovered != null) {
            gg.pose().pushPose();
            gg.pose().translate(0, 0, 900);
            gg.renderTooltip(minecraft.font, Component.literal(hovered.name), mouseX, mouseY);
            gg.pose().popPose();
        }
    }

    private void renderConnectors(GuiGraphics gg) {
        for (QuestData.Quest quest : quests) {
            for (String dependencyId : quest.dependencies) {
                QuestData.Quest dependency = QuestData.byId(dependencyId).orElse(null);
                if (dependency == null) continue;
                int x1 = nodeX(dependency) + NODE_SIZE / 2;
                int y1 = nodeY(dependency) + NODE_SIZE / 2;
                int x2 = nodeX(quest) + NODE_SIZE / 2;
                int y2 = nodeY(quest) + NODE_SIZE / 2;
                boolean highlighted = selectedQuestId.isBlank()
                        || selectedQuestId.equals(quest.id) || selectedQuestId.equals(dependencyId);
                drawConnector(gg, connector(x1, y1, x2, y2),
                        quest.incompatibleDependencies.contains(dependencyId), highlighted);
            }
        }
    }

    private void renderNodes(GuiGraphics gg) {
        for (QuestData.Quest quest : quests) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            boolean complete = isComplete(quest);
            gg.blit(nodeTexture(quest.treeBackground, complete), x, y, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
            quest.iconItem().ifPresent(item -> gg.renderItem(new net.minecraft.world.item.ItemStack(item), x + 8, y + 8));
            if (complete) {
                gg.blit(ICON_COMPLETE, x + 18, y + 18, 0, 0, 11, 11, 11, 11);
            } else if (minecraft.player != null && !QuestTracker.dependenciesMet(quest, minecraft.player)) {
                gg.blit(ICON_ATTENTION, x + 23, y + 10, 0, 0, 6, 13, 6, 13);
            }
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
        gg.blit(ZOOM_OUT, x, y, 0, 0, 7, 7, 7, 7);
        gg.blit(ZOOM_CENTRE, x + 10, y - 1, 0, 0, 9, 9, 9, 9);
        gg.blit(ZOOM_IN, x + 22, y, 0, 0, 7, 7, 7, 7);
    }

    private int nodeX(QuestData.Quest quest) {
        int index = quests.indexOf(quest);
        int baseX = quest.treeX != -1 ? quest.treeX : (index % 4 - 2) * 48;
        return baseX - NODE_SIZE / 2;
    }

    private int nodeY(QuestData.Quest quest) {
        int index = quests.indexOf(quest);
        int baseY = quest.treeY != -1 ? quest.treeY : (index / 4) * 48;
        return baseY - NODE_SIZE / 2;
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

    private Connector connector(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        List<Point> points = new ArrayList<>();
        points.add(new Point(x1, y1));
        if (dx != 0 && dy != 0 && !isDiagonal(dx, dy)) points.add(new Point(x2, y1));
        points.add(new Point(x2, y2));
        return new Connector(List.copyOf(points));
    }

    private void drawConnector(GuiGraphics gg, Connector connector, boolean incompatible, boolean highlighted) {
        Set<Long> outer = new HashSet<>();
        Set<Long> inner = new HashSet<>();
        for (int index = 1; index < connector.points.size(); index++) {
            Point start = connector.points.get(index - 1);
            Point end = connector.points.get(index);
            rasterLine(start.x, start.y, end.x, end.y, outer, inner);
        }
        for (long packed : outer) {
            if (inner.contains(packed)) continue;
            int x = (int) (packed >> 32);
            int y = (int) packed;
            gg.fill(x, y, x + 1, y + 1, 0xFF000000);
        }
        int color = incompatible
                ? (highlighted ? 0xFFFF3030 : 0xFF681C1C)
                : (highlighted ? 0xFFFFFFFF : 0xFF686868);
        for (long packed : inner) {
            int x = (int) (packed >> 32);
            int y = (int) packed;
            gg.fill(x, y, x + 1, y + 1, color);
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
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
        dragging = true;
        setFocused(true);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging || button != 0) return false;
        panX += (float) dragX / zoom;
        panY += (float) dragY / zoom;
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

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/tree/" + name + ".png");
    }

    private static ResourceLocation nodeTexture(String background, boolean complete) {
        String shape = background == null || background.isBlank() ? "square" : background.replace('_', '-');
        return texture(shape + (complete ? "-complete" : "-incomplete"));
    }

    private boolean isComplete(QuestData.Quest quest) {
        if (minecraft.player == null) return false;
        QuestTracker.Status status = QuestTracker.getStatus(quest, minecraft.player);
        return status == QuestTracker.Status.COMPLETED || status == QuestTracker.Status.REDEEMED;
    }

    private static boolean isDiagonal(int dx, int dy) {
        int longest = Math.max(Math.abs(dx), Math.abs(dy));
        return Math.abs(Math.abs(dx) - Math.abs(dy)) <= Math.max(12, longest / 5);
    }

    private QuestData.Quest questAt(double mouseX, double mouseY) {
        double canvasX = canvasX(mouseX);
        double canvasY = canvasY(mouseY);
        for (int index = quests.size() - 1; index >= 0; index--) {
            QuestData.Quest quest = quests.get(index);
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
