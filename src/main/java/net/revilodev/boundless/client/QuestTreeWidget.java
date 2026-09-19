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
import java.util.List;
import java.util.function.Consumer;

// shared quest tree canvas
@OnlyIn(Dist.CLIENT)
public final class QuestTreeWidget extends AbstractWidget {
    private static final ResourceLocation ZOOM_IN = texture("button-zoom-in");
    private static final ResourceLocation ZOOM_OUT = texture("button-zoom-out");
    private static final ResourceLocation ZOOM_CENTRE = texture("button-zoom-centre");
    private static final ResourceLocation CONNECTOR_COMPLETE = texture("connector-complete");
    private static final ResourceLocation CONNECTOR_INCOMPLETE = texture("connector-incomplete");
    private static final ResourceLocation ICON_COMPLETE = texture("icon-complete");
    private static final ResourceLocation ICON_ATTENTION = texture("icon-attention");
    private static final int NODE_SIZE = 32;
    private static final int CONNECTOR_W = 6;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Consumer<QuestData.Quest> onQuestSelected;
    private List<QuestData.Quest> quests = List.of();
    private float zoom = 1f;
    private float panX;
    private float panY;
    private boolean dragging;

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
        panX = 0f;
        panY = 0f;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        gg.enableScissor(getX(), getY(), getX() + width, getY() + height);
        renderConnectors(gg);
        renderNodes(gg, mouseX, mouseY);
        gg.disableScissor();
        renderZoomButtons(gg);
    }

    private void renderConnectors(GuiGraphics gg) {
        for (QuestData.Quest quest : quests) {
            for (String dependencyId : quest.dependencies) {
                QuestData.Quest dependency = QuestData.byId(dependencyId).orElse(null);
                if (dependency == null) continue;
                int x1 = nodeX(dependency) + nodeSize() / 2;
                int y1 = nodeY(dependency) + nodeSize();
                int x2 = nodeX(quest) + nodeSize() / 2;
                int y2 = nodeY(quest);
                boolean complete = isComplete(dependency);
                ResourceLocation texture = complete ? CONNECTOR_COMPLETE : CONNECTOR_INCOMPLETE;
                drawStraightConnector(gg, x1, y1, x2, y2, complete ? 0xFFFFD700 : 0xFF808080);
            }
        }
    }

    private void renderNodes(GuiGraphics gg, int mouseX, int mouseY) {
        for (QuestData.Quest quest : quests) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            boolean complete = isComplete(quest);
            int nodeSize = nodeSize();
            gg.blit(nodeTexture(quest.treeBackground, complete), x, y, 0, 0, nodeSize, nodeSize, NODE_SIZE, NODE_SIZE);
            quest.iconItem().ifPresent(item -> renderNodeItem(gg, new net.minecraft.world.item.ItemStack(item), x, y));
            if (complete) {
                gg.blit(ICON_COMPLETE, x + Mth.floor(18 * zoom), y + Mth.floor(18 * zoom), 0, 0,
                        Math.max(1, Mth.floor(11 * zoom)), Math.max(1, Mth.floor(11 * zoom)), 11, 11);
            } else if (minecraft.player != null && !QuestTracker.dependenciesMet(quest, minecraft.player)) {
                gg.blit(ICON_ATTENTION, x + Mth.floor(23 * zoom), y + Mth.floor(10 * zoom), 0, 0,
                        Math.max(1, Mth.floor(6 * zoom)), Math.max(1, Mth.floor(13 * zoom)), 6, 13);
            }
            if (mouseX >= x && mouseX <= x + nodeSize && mouseY >= y && mouseY <= y + nodeSize) {
                gg.renderTooltip(minecraft.font, Component.literal(quest.name), mouseX, mouseY);
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

    private int nodeX(QuestData.Quest quest) {
        int index = quests.indexOf(quest);
        int baseX = quest.treeX >= 0 ? quest.treeX : (index % 4) * 46;
        return getX() + Mth.floor((baseX + panX) * zoom);
    }

    private int nodeY(QuestData.Quest quest) {
        int index = quests.indexOf(quest);
        int baseY = quest.treeY >= 0 ? quest.treeY : (index / 4) * 54;
        return getY() + Mth.floor((baseY + panY) * zoom);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0) return false;
        int zoomX = getX() + width - 31;
        int zoomY = getY() + 3;
        if (mouseY >= zoomY - 1 && mouseY <= zoomY + 9) {
            if (mouseX >= zoomX && mouseX <= zoomX + 7) zoomAt(mouseX, mouseY, zoom - .1f);
            else if (mouseX >= zoomX + 10 && mouseX <= zoomX + 19) centreView();
            else if (mouseX >= zoomX + 22 && mouseX <= zoomX + 29) zoomAt(mouseX, mouseY, zoom + .1f);
            else return false;
            return true;
        }
        for (QuestData.Quest quest : quests) {
            int x = nodeX(quest);
            int y = nodeY(quest);
            if (mouseX >= x && mouseX <= x + nodeSize() && mouseY >= y && mouseY <= y + nodeSize()) {
                if (onQuestSelected != null) onQuestSelected.accept(quest);
                return true;
            }
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
