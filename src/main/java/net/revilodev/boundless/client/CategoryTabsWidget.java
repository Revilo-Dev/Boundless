
package net.revilodev.boundless.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class CategoryTabsWidget extends AbstractWidget {

    // tab textures
    private static final ResourceLocation TAB =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab.png");

    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/tab_selected.png");

    // category scroll button textures
    private static final ResourceLocation MOVE_DOWN =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_down.png");

    private static final ResourceLocation MOVE_UP =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_up.png");

    private static final ResourceLocation MOVE_DOWN_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_down-highlighted.png");

    private static final ResourceLocation MOVE_UP_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/arrow_up-highlighted.png");

    private static final ResourceLocation ATTENTION =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/icons/attention.png");

    // show five tabs in the sliding window
    private static final int PAGE_SIZE = 5;

    // scroll button sizes
    private static final int CONTROL_ICON_BASE = 16;
    private static final float CONTROL_ICON_SCALE = 1.0f;
    private static final int CONTROL_ICON = (int) (CONTROL_ICON_BASE * CONTROL_ICON_SCALE);
    private static final int CONTROL_GAP = 3;
    private static final int CONTROL_X_SHIFT = 3;

    private final Minecraft mc = Minecraft.getInstance();

    // called when a tab gets selected
    private final Consumer<String> onSelect;

    private final List<QuestData.Category> categories = new ArrayList<>();

    private String selected = "";
    private int firstVisibleIndex = 0;

    private static final int TAB_W = 35;
    private static final int TAB_H = 27;

    private int cellW = TAB_W;
    private int cellH = TAB_H;
    private int gap = 2;

    // tooltip gets drawn after the main widget
    private Component pendingTooltip;
    private int pendingTooltipX;
    private int pendingTooltipY;

    // lines the tabs up on the right side
    private int tabRenderX() {
        return getX() + Math.max(0, width - cellW);
    }

    public CategoryTabsWidget(int x, int y, int w, int h, Consumer<String> onSelect) {
        super(x, y, w, h, Component.empty());
        this.onSelect = onSelect;
    }

    // update the widget position and size
    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;
    }

    // reload the category list
    public void setCategories(List<QuestData.Category> list) {
        categories.clear();

        for (QuestData.Category c : list) {
            if (c == null) continue;

            // all is handled on its own
            if ("all".equalsIgnoreCase(c.id)) continue;

            // skip categories disabled in the config
            if (Config.isCategoryDisabled(c.id)) continue;

            // Categories gated by a quest should not expose a tab until that quest is redeemed.
            if (mc.player != null && !QuestData.isCategoryUnlocked(c, mc.player)) continue;

            categories.add(c);
        }

        if (!categories.isEmpty()) {
            boolean hasSelected = false;

            // check if the current category still exists
            for (QuestData.Category c : categories) {
                if (c.id.equalsIgnoreCase(selected)) {
                    hasSelected = true;
                    break;
                }
            }

            // use the 1st category if the old one is gone
            if (!hasSelected) {
                selected = categories.get(0).id;
            }
        } else {
            selected = "";
            firstVisibleIndex = 0;
            return;
        }

        ensureSelectedVisible();
    }

    // change the selected category
    public void setSelected(String id) {
        this.selected = id == null ? "" : id;
        ensureSelectedVisible();
    }

    public String getSelectedId() {
        return selected;
    }

    public String getSelectedName() {
        if (selected == null || selected.isBlank()) return "";

        for (QuestData.Category c : categories) {
            if (c.id.equalsIgnoreCase(selected)) {
                return c.name;
            }
        }

        return "";
    }

    // select the 1st category in the list
    public String selectFirstCategory() {
        if (categories.isEmpty()) {
            selected = "";
            firstVisibleIndex = 0;
            return "";
        }

        selected = categories.get(0).id;
        ensureSelectedVisible();

        return selected;
    }

    // call this after the screen has finished rendering
    // this keeps the tooltip above the rest of the gui
    public void renderHoverTooltipOnTop(GuiGraphics gg) {
        if (pendingTooltip == null) return;

        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 500.0F);
        gg.renderTooltip(mc.font, pendingTooltip, pendingTooltipX, pendingTooltipY);
        gg.pose().popPose();

        pendingTooltip = null;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        pendingTooltip = null;

        int visibleCount = visibleTabCount();
        if (visibleCount <= 0) return;

        clampFirstVisible();

        int x = tabRenderX();
        int y = getY();

        // work out which categories are in the current window
        int start = firstVisibleIndex;
        int end = Math.min(categories.size(), start + visibleCount);

        for (int idx = start; idx < end; idx++) {
            QuestData.Category c = categories.get(idx);

            int i = idx - start;
            int top = y + i * (cellH + gap);

            boolean sel = !selected.isBlank() && c.id.equalsIgnoreCase(selected);
            ResourceLocation tex = sel ? TAB_SELECTED : TAB;

            // selected tabs move 1 pixel to the left
            int renderX = x + 2 - (sel ? 1 : 0);

            gg.blit(tex, renderX, top, 0, 0, TAB_W, TAB_H, TAB_W, TAB_H);

            // draw the category item inside the tab
            c.iconItem().ifPresent(it ->
                    gg.renderItem(new ItemStack(it), renderX + 10, top + 5)
            );
            if (hasRedeemableQuest(c)) {
                gg.blit(ATTENTION, renderX + 27, top + 2, 0, 0, 4, 11, 4, 11);
            }

            boolean hover =
                    mouseX >= x &&
                            mouseX < x + cellW &&
                            mouseY >= top &&
                            mouseY < top + cellH;

            // save the tooltip so it can be drawn later
            if (hover) {
                pendingTooltip = Component.literal(c.name);
                pendingTooltipX = mouseX;
                pendingTooltipY = mouseY;
            }
        }

        renderScrollControls(gg, mouseX, mouseY, visibleCount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;

        // only use left click
        if (button != 0) return false;

        int visibleCount = visibleTabCount();
        if (visibleCount <= 0) return false;

        clampFirstVisible();

        int x = tabRenderX();
        int y = getY();

        int start = firstVisibleIndex;
        int end = Math.min(categories.size(), start + visibleCount);

        // check if a category tab was clicked
        for (int idx = start; idx < end; idx++) {
            int i = idx - start;
            int top = y + i * (cellH + gap);

            if (mouseX >= x &&
                    mouseX < x + cellW &&
                    mouseY >= top &&
                    mouseY < top + cellH) {

                String id = categories.get(idx).id;
                selected = id;

                if (onSelect != null) {
                    onSelect.accept(id);
                }

                return true;
            }
        }

        // move the five-tab window one category at a time
        if (categories.size() > PAGE_SIZE) {
            if (isOverUp(mouseX, mouseY) && firstVisibleIndex > 0) {
                firstVisibleIndex--;
                return true;
            }

            if (isOverDown(mouseX, mouseY, visibleCount) && firstVisibleIndex < maxFirstVisibleIndex()) {
                firstVisibleIndex++;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (!visible || !active || categories.size() <= PAGE_SIZE || scrollY == 0.0D || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int previous = firstVisibleIndex;
        firstVisibleIndex += scrollY > 0.0D ? -1 : 1;
        clampFirstVisible();
        return previous != firstVisibleIndex;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int visibleCount = visibleTabCount();
        int left = tabRenderX();
        int top = categories.size() > PAGE_SIZE ? upControlY() : getY();
        int bottom = categories.size() > PAGE_SIZE ? downControlY(visibleCount) + CONTROL_ICON : getY() + visibleCount * (cellH + gap);
        return visible && mouseX >= left && mouseX < left + cellW && mouseY >= top && mouseY < bottom;
    }

    // always show up to 5 tabs
    private int visibleTabCount() {
        return PAGE_SIZE;
    }

    private int maxFirstVisibleIndex() {
        return Math.max(0, categories.size() - PAGE_SIZE);
    }

    private void clampFirstVisible() {
        firstVisibleIndex = Math.max(0, Math.min(firstVisibleIndex, maxFirstVisibleIndex()));
    }

    // move the sliding window so the selected category remains visible
    private void ensureSelectedVisible() {
        if (selected == null || selected.isBlank() || categories.isEmpty()) {
            clampFirstVisible();
            return;
        }

        int selectedIndex = -1;

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id.equalsIgnoreCase(selected)) {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex < 0) {
            clampFirstVisible();
            return;
        }

        if (selectedIndex < firstVisibleIndex) {
            firstVisibleIndex = selectedIndex;
        } else if (selectedIndex >= firstVisibleIndex + PAGE_SIZE) {
            firstVisibleIndex = selectedIndex - PAGE_SIZE + 1;
        }
        clampFirstVisible();
    }

    private void renderScrollControls(GuiGraphics gg, int mouseX, int mouseY, int visibleCount) {
        if (categories.size() <= PAGE_SIZE) return;

        int iconX = controlsIconX();
        int upY = upControlY();
        int downY = downControlY(visibleCount);

        boolean hoverUp = isOverUp(mouseX, mouseY);
        boolean hoverDown = isOverDown(mouseX, mouseY, visibleCount);

        // use highlighted textures while hovering
        drawScaledIcon(
                gg,
                hoverUp ? MOVE_UP_HIGHLIGHTED : MOVE_UP,
                iconX,
                upY
        );

        drawScaledIcon(
                gg,
                hoverDown ? MOVE_DOWN_HIGHLIGHTED : MOVE_DOWN,
                iconX,
                downY
        );

        if (hasRedeemableQuestBefore(firstVisibleIndex)) {
            gg.blit(ATTENTION, iconX + CONTROL_ICON + 1, upY - 1, 0, 0, 4, 11, 4, 11);
        }
        if (hasRedeemableQuestAfter(firstVisibleIndex + visibleCount)) {
            gg.blit(ATTENTION, iconX + CONTROL_ICON + 1, downY - 1, 0, 0, 4, 11, 4, 11);
        }

        // show direction tooltips when the control can be used
        if (hoverUp && firstVisibleIndex > 0) {
            pendingTooltip =
                    Component.translatable("ui.boundless.pagination.previous");

            pendingTooltipX = mouseX;
            pendingTooltipY = mouseY;
        } else if (hoverDown && firstVisibleIndex < maxFirstVisibleIndex()) {
            pendingTooltip =
                    Component.translatable("ui.boundless.pagination.next");

            pendingTooltipX = mouseX;
            pendingTooltipY = mouseY;
        }
    }

    // check if the mouse is over the up button
    private boolean isOverUp(double mouseX, double mouseY) {
        int iconX = controlsIconX();
        int controlsY = upControlY();

        return mouseX >= iconX &&
                mouseX < iconX + CONTROL_ICON &&
                mouseY >= controlsY &&
                mouseY < controlsY + CONTROL_ICON;
    }

    // check if the mouse is over the down button
    private boolean isOverDown(double mouseX, double mouseY, int visibleCount) {
        int iconX = controlsIconX();
        int downY = downControlY(visibleCount);

        return mouseX >= iconX &&
                mouseX < iconX + CONTROL_ICON &&
                mouseY >= downY &&
                mouseY < downY + CONTROL_ICON;
    }

    // center the page buttons under the tabs
    private int controlsIconX() {
        return tabRenderX() +
                (cellW - CONTROL_ICON) / 2 +
                CONTROL_X_SHIFT;
    }

    private int upControlY() {
        return getY() - CONTROL_ICON - CONTROL_GAP;
    }

    private int downControlY(int visibleCount) {
        return getY() + visibleCount * (cellH + gap) + 1;
    }

    private boolean hasRedeemableQuestBefore(int endExclusive) {
        for (int index = 0; index < Math.min(endExclusive, categories.size()); index++) {
            if (hasRedeemableQuest(categories.get(index))) return true;
        }
        return false;
    }

    private boolean hasRedeemableQuestAfter(int startInclusive) {
        for (int index = Math.max(0, startInclusive); index < categories.size(); index++) {
            if (hasRedeemableQuest(categories.get(index))) return true;
        }
        return false;
    }

    private boolean hasRedeemableQuest(QuestData.Category category) {
        if (category == null || mc.player == null) return false;
        for (QuestData.Quest quest : QuestData.all()) {
            if (quest == null || !category.id.equalsIgnoreCase(quest.category)) continue;
            QuestTracker.Status status = QuestTracker.getStatus(quest, mc.player);
            if (status == QuestTracker.Status.REDEEMED || status == QuestTracker.Status.REJECTED) continue;
            if (!QuestTracker.dependenciesMet(quest, mc.player)) continue;
            if (status == QuestTracker.Status.COMPLETED || QuestTracker.isReady(quest, mc.player)
                    || QuestTracker.canAcknowledge(quest, mc.player)) return true;
        }
        return false;
    }


    // draw an icon using the set scale
    private void drawScaledIcon(
            GuiGraphics gg,
            ResourceLocation tex,
            int x,
            int y
    ) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0.0F);
        gg.pose().scale(CONTROL_ICON_SCALE, CONTROL_ICON_SCALE, 1.0F);

        gg.blit(
                tex,
                0,
                0,
                0,
                0,
                CONTROL_ICON_BASE,
                CONTROL_ICON_BASE,
                CONTROL_ICON_BASE,
                CONTROL_ICON_BASE
        );

        gg.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
