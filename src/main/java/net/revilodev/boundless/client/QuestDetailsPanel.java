package net.revilodev.boundless.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.compat.JeiCompat;
import net.revilodev.boundless.compat.LevelUpCompat;
import net.revilodev.boundless.network.BoundlessNetwork;
import net.revilodev.boundless.quest.QuestData;
import net.revilodev.boundless.quest.QuestItemSpec;
import net.revilodev.boundless.quest.QuestTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public final class QuestDetailsPanel extends AbstractWidget {

    private static final int LINE_ITEM_ROW = 22;
    private static final int BOTTOM_PADDING = 12;
    private static final int CONTENT_BOTTOM_MARGIN = 6;
    private static final int HEADER_HEIGHT = 19;
    private static final int DESC_CHAR_LIMIT = 180;
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("\\b[a-z0-9_.-]+:[a-z0-9_./-]+\\b");

    private static final ResourceLocation TEX_PIN =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pin.png");
    private static final ResourceLocation TEX_PIN_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/pin-hovered.png");
    private static final ResourceLocation TEX_UNPIN =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/unpin.png");
    private static final ResourceLocation TEX_UNPIN_HOVER =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/unpin-hovered.png");
    private static final ResourceLocation TEX_SCROLL =
            ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/scroll-icon.png");

    private final Minecraft mc = Minecraft.getInstance();
    private QuestData.Quest quest;

    private final BackButton back;
    private final CompleteButton complete;
    private final RejectButton reject;
    private final PinButton pin;
    private final ScrollButton scroll;
    private final Runnable onBack;

    private float scrollY = 0f;
    private int measuredContentHeight = 0;

    private boolean descExpanded = false;
    private boolean hideBackButton = false;

    private final List<DepClickRegion> depRegions = new ArrayList<>();
    private final List<ItemClickRegion> itemRegions = new ArrayList<>();
    private final Map<String, EditBox> inputBoxes = new HashMap<>();

    private static ResourceLocation safeParse(String id) {
        return id == null || id.isBlank() ? null : ResourceLocation.tryParse(id);
    }

    private static Item resolveItem(String id) {
        ResourceLocation rl = safeParse(id);
        return rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    private static final class DepClickRegion {
        final int x, y, w, h;
        final String questId;

        DepClickRegion(int x, int y, int w, int h, String questId) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.questId = questId;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public QuestDetailsPanel(int x, int y, int w, int h, Runnable onBack) {
        super(x, y, w, h, Component.empty());
        this.onBack = onBack;

        this.back = new BackButton(getX(), getY(), () -> {
            if (this.onBack != null) this.onBack.run();
        });
        this.back.visible = false;
        this.back.active = false;

        this.complete = new CompleteButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null) {
                if (QuestTracker.canRestartRepeatable(quest, mc.player)) {
                    PacketDistributor.sendToServer(new BoundlessNetwork.RestartRepeatable(quest.id));
                } else {
                    PacketDistributor.sendToServer(new BoundlessNetwork.Redeem(quest.id));
                }
                if (this.onBack != null) this.onBack.run();
            }
        });
        this.complete.visible = false;
        this.complete.active = false;

        this.reject = new RejectButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null && quest.optional) {
                PacketDistributor.sendToServer(new BoundlessNetwork.Reject(quest.id));
                if (this.onBack != null) this.onBack.run();
            }
        });
        this.reject.visible = false;
        this.reject.active = false;

        this.pin = new PinButton(getX(), getY());
        this.pin.visible = false;
        this.pin.active = false;

        this.scroll = new ScrollButton(getX(), getY(), () -> {
            if (quest != null && mc.player != null) {
                PacketDistributor.sendToServer(new BoundlessNetwork.CreateScroll(quest.id));
            }
        });
        this.scroll.visible = false;
        this.scroll.active = false;

        setBounds(x, y, w, h);
    }

    private static final class ItemClickRegion {
        final int x;
        final int y;
        final int w;
        final int h;
        final ItemStack stack;
        final boolean jeiEnabled;

        ItemClickRegion(int x, int y, int w, int h, ItemStack stack) {
            this(x, y, w, h, stack, true);
        }

        ItemClickRegion(int x, int y, int w, int h, ItemStack stack, boolean jeiEnabled) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.stack = stack;
            this.jeiEnabled = jeiEnabled;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public AbstractButton backButton() { return back; }
    public AbstractButton completeButton() { return complete; }
    public AbstractButton rejectButton() { return reject; }
    public AbstractButton pinButton() { return pin; }
    public AbstractButton scrollButton() { return scroll; }

    public void setHideBackButton(boolean v) { this.hideBackButton = v; }

    public void setBounds(int x, int y, int w, int h) {
        this.setX(x);
        this.setY(y);
        this.width = w;
        this.height = h;

        int cy = y + h - complete.getHeight() - 2;
        int cxCenter = x + (w - complete.getWidth()) / 2;

        back.setPosition(x + 2, cy);
        complete.setPosition(cxCenter, cy);
        reject.setPosition(x + w - reject.getWidth() - 2, cy);

        int actionX = x + w - pin.getWidth() - 5;
        int actionY = y + 4;
        pin.setPosition(actionX, actionY);
        scroll.setPosition(actionX, actionY);
    }

    public void setQuest(QuestData.Quest q) {
        this.quest = q;
        this.scrollY = 0f;
        this.descExpanded = false;
        this.reject.resetConfirmState();
        inputBoxes.clear();
        if (q != null) PinnedQuestHud.setCurrentQuestId(q.id);
    }

    public String currentQuestTitle() {
        return quest == null ? "" : quest.name;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || quest == null) return;

        depRegions.clear();
        itemRegions.clear();
        List<Component> hoveredTooltips = new ArrayList<>();
        for (EditBox box : inputBoxes.values()) {
            box.visible = false;
            box.active = false;
        }

        int x = this.getX();
        int y = this.getY();
        int w = this.width;

        int contentTop = y + HEADER_HEIGHT;
        int contentBottom = complete.getY() - CONTENT_BOTTOM_MARGIN;
        int viewportH = Math.max(0, contentBottom - contentTop);

        measuredContentHeight = measureContentHeight(w);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        scrollY = Mth.clamp(scrollY, 0f, maxScroll);

        quest.iconItem().ifPresent(item -> renderScaledItem(gg, new ItemStack(item), x + 4, y + 2));

        String title = quest.name;
        int nameWidth = w - 32 - 18;
        float titleScale = textScale();
        if (title != null && title.length() > 16) {
            int textW = mc.font.width(title);
            if (textW > 0) titleScale = Math.max(0.5f, Math.min(titleScale, (float) nameWidth / (float) textW));
        }
        if (titleScale < 1f) {
            gg.pose().pushPose();
            gg.pose().scale(titleScale, titleScale, 1f);
            float inv = 1f / titleScale;
            if (titleScale <= 0.5f && mc.font.width(title) * titleScale > nameWidth) {
                int wrapW = (int) (nameWidth * inv);
                gg.drawWordWrap(mc.font, Component.literal(title), (int) ((x + 26) * inv), (int) ((y + 6) * inv), wrapW, 0xFFFFFF);
            } else {
                gg.drawString(mc.font, title, (int) ((x + 26) * inv), (int) ((y + 6) * inv), 0xFFFFFF, false);
            }
            gg.pose().popPose();
        } else {
            gg.drawString(mc.font, title, x + 26, y + 6, 0xFFFFFF, false);
        }

        boolean showScrollButton = Config.enableQuestScrolls() && QuestTracker.canCreateScroll(quest, mc.player);
        boolean pinningEnabled = !Config.disableQuestPinning() && !showScrollButton;
        pin.visible = pinningEnabled;
        pin.active = pinningEnabled;
        if (pinningEnabled) {
            pin.render(gg, mouseX, mouseY, partialTick);
        }
        scroll.visible = showScrollButton;
        scroll.active = showScrollButton;
        if (showScrollButton) {
            scroll.render(gg, mouseX, mouseY, partialTick);
        }

        gg.enableScissor(x, contentTop, x + w, contentBottom);

        int[] curY = {contentTop + 3 - Mth.floor(scrollY)};

        if (!quest.description.isBlank()) {
            String full = quest.description;
            boolean needsMore = full.length() > DESC_CHAR_LIMIT;

            String shown = full;
            if (needsMore && !descExpanded) {
                int cut = full.lastIndexOf(" ", DESC_CHAR_LIMIT);
                if (cut < 0) cut = DESC_CHAR_LIMIT;
                shown = full.substring(0, cut) + "...";
            }

            Component shownComponent = formatColorCodes(shown, 0xCFCFCF);
            drawScaledWordWrap(gg, shownComponent, x + 4, curY[0], w - 8, 0xCFCFCF);
            addDescriptionItemRegions(stripColorTokens(shown), x + 4, curY[0], w - 8);
            int wrapHeight = scaledWrappedHeight(shownComponent, w - 8);

            if (needsMore) {
                int toggleY = curY[0] + wrapHeight + 2;
                String toggleText = descExpanded
                        ? Component.translatable("ui.boundless.questbook.read_less").getString()
                        : Component.translatable("ui.boundless.questbook.read_more").getString();
                int toggleW = scaledTextWidth(toggleText);
                int toggleX = x + (w - toggleW) / 2;

                drawScaledString(gg, toggleText, toggleX, toggleY, 0x55AAFF);

                depRegions.add(new DepClickRegion(
                        toggleX,
                        toggleY,
                        toggleW,
                        scaledLineHeight(),
                        "__desc_toggle__"
                ));

                curY[0] += wrapHeight + scaledLineHeight() + 6;
            } else {
                curY[0] += wrapHeight + 8;
            }
        }

        if (!quest.dependencies.isEmpty()) {
            Component requiresLabel = Component.translatable("ui.boundless.questbook.requires");
            drawScaledWordWrap(gg, requiresLabel, x + 4, curY[0], w - 8, 0xff9f0f);
            curY[0] += scaledWrappedHeight(requiresLabel, w - 8) + 2;

            for (String depId : quest.dependencies) {
                QuestData.Quest depQuest = QuestData.byId(depId).orElse(null);
                String depName = depQuest != null ? depQuest.name : depId;

                int lineY = curY[0];
                int textX = x + 24;
                int textW = scaledTextWidth(depName);
                int color = 0xFF5555;

                if (depQuest != null && mc.player != null) {
                    var status = QuestTracker.getStatus(depQuest, mc.player);
                    if (status == QuestTracker.Status.REDEEMED) color = 0x55FF55;
                }

                if (depQuest != null) {
                    depQuest.iconItem().ifPresent(icon -> renderScaledItem(gg, new ItemStack(icon), x + 4, lineY));
                }

                drawScaledString(gg, depName, textX, lineY + 4, color);
                int underlineY = lineY + 4 + scaledLineHeight();
                gg.fill(textX, underlineY, textX + textW, underlineY + 1, color);

                DepClickRegion region = new DepClickRegion(textX, lineY, textW, scaledLineHeight() + 6, depId);
                depRegions.add(region);
                if (region.contains(mouseX, mouseY)) hoveredTooltips.add(Component.translatable("ui.boundless.questbook.view"));

                curY[0] += scaledRowHeight();
            }
            curY[0] += 2;
        }

        if (quest.completion != null && !quest.completion.targets.isEmpty() && mc.player != null) {

            boolean printedCollectHeader = false;
            boolean printedSubmitHeader = false;
            boolean printedKillHeader = false;

            for (QuestData.Target t : quest.completion.targets) {
                boolean isSubmitTarget = t.isSubmit();
                boolean isItemLike = t.isItem() || t.isSubmit();

                if (t.isXp()) {
                    if (!printedSubmitHeader) {
                        drawScaledString(gg, Component.translatable("ui.boundless.questbook.submit"), x + 4, curY[0], 0x1d9633);
                        curY[0] += scaledLineHeight() + 2;
                        printedSubmitHeader = true;
                    }
                    int have = Math.min(QuestTracker.getXpAmount(mc.player, t.id), t.count);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;
                    String label = "levels".equals(QuestTracker.normalizeXpType(t.id))
                            ? Component.translatable("ui.boundless.questbook.levels").getString()
                            : Component.translatable("ui.boundless.questbook.xp").getString();
                    renderScaledItem(gg, new ItemStack(Items.EXPERIENCE_BOTTLE), x + 4, curY[0]);
                    drawScaledString(gg, label + ": " + have + "/" + t.count, x + 24, curY[0] + 4, color);
                    curY[0] += scaledRowHeight();
                    continue;
                }

                if (t.isLevelUpLevel()) {
                    if (!printedSubmitHeader) {
                        drawScaledString(gg, Component.translatable("ui.boundless.questbook.submit"), x + 4, curY[0], 0x1d9633);
                        curY[0] += scaledLineHeight() + 2;
                        printedSubmitHeader = true;
                    }
                    int have = Math.min(LevelUpCompat.getLevel(mc.player), t.count);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;
                    renderScaledItem(gg, new ItemStack(Items.EXPERIENCE_BOTTLE), x + 4, curY[0]);
                    drawScaledString(gg, Component.translatable("ui.boundless.questbook.levelup_level_progress", have, t.count), x + 24, curY[0] + 4, color);
                    curY[0] += scaledRowHeight();
                    continue;
                }

                if (t.isFieldInput()) {
                    drawScaledString(gg, Component.translatable("ui.boundless.questbook.input"), x + 4, curY[0], 0x1d9633);
                    curY[0] += scaledLineHeight() + 2;
                    String key = quest.id + ":field:" + t.id;
                    EditBox box = inputBoxes.computeIfAbsent(key, ignored -> createInputBox());
                    if (!box.isFocused()) {
                        box.setValue(QuestTracker.getFieldInputProgress(mc.player, key));
                    }
                    box.setHint(Component.literal(t.hint == null ? "" : t.hint));
                    box.setPosition(x + 4, curY[0]);
                    box.setWidth(w - 8);
                    box.visible = true;
                    box.active = true;
                    boolean ready = QuestTracker.isReady(quest, mc.player);
                    int borderColor = ready ? 0xFF55AA55 : 0xFF8E8E8E;
                    gg.fill(box.getX() - 1, box.getY() - 1, box.getX() + box.getWidth() + 1, box.getY(), borderColor);
                    gg.fill(box.getX() - 1, box.getY() + box.getHeight(), box.getX() + box.getWidth() + 1, box.getY() + box.getHeight() + 1, borderColor);
                    gg.fill(box.getX() - 1, box.getY() - 1, box.getX(), box.getY() + box.getHeight() + 1, borderColor);
                    gg.fill(box.getX() + box.getWidth(), box.getY() - 1, box.getX() + box.getWidth() + 1, box.getY() + box.getHeight() + 1, borderColor);
                    box.render(gg, mouseX, mouseY, partialTick);
                    curY[0] += box.getHeight() + 4;
                    continue;
                }

                if (isItemLike) {

                    if (isSubmitTarget) {
                        if (!printedSubmitHeader) {
                            drawScaledString(gg, Component.translatable("ui.boundless.questbook.submit"), x + 4, curY[0], 0x1d9633);
                            curY[0] += scaledLineHeight() + 2;
                            printedSubmitHeader = true;
                        }
                    } else {
                        if (!printedCollectHeader) {
                            drawScaledString(gg, Component.translatable("ui.boundless.questbook.collect"), x + 4, curY[0], 0x1d9633);
                            curY[0] += scaledLineHeight() + 2;
                            printedCollectHeader = true;
                        }
                    }

                    String raw = t.id;
                    QuestItemSpec spec = QuestItemSpec.parse(raw);
                    boolean isTagSyntax = spec.tag;
                    String key = spec.id;
                    ResourceLocation rl = safeParse(key);
                    if (rl == null) {
                        drawScaledString(gg, Component.translatable("ui.boundless.questbook.invalid_item_target"), x + 4, curY[0] + 4, 0xFF5555);
                        curY[0] += scaledRowHeight();
                        continue;
                    }
                    Item direct = spec.item();
                    boolean treatAsTag = isTagSyntax || direct == null;

                    int need = t.count;
                    int found = QuestTracker.getCountInInventory(t.id, mc.player);

                    String progressKey = quest.id + ":" + t.id;
                    int permFound = QuestTracker.getPermanentItemProgress(progressKey, found, need);

                    int shownCount = Math.min(permFound, need);
                    boolean ready = shownCount >= need;
                    int color = ready ? 0x55FF55 : 0xFF5555;

                    int px = x + 4;

                    Item iconItem;
                    if (treatAsTag) {
                        List<Item> tagItems = resolveTagItems(rl);
                        iconItem = tagItems.isEmpty() ? null :
                                tagItems.get((int) ((mc.level != null ? mc.level.getGameTime() : 0) / 20 % tagItems.size()));
                    } else {
                        iconItem = direct;
                    }

                    if (iconItem != null) {
                        ItemStack st = new ItemStack(iconItem);
                        renderScaledItem(gg, st, px, curY[0]);
                        itemRegions.add(new ItemClickRegion(px, curY[0], 16, 16, st.copy()));
                        if (mouseX >= px && mouseX <= px + 16 && mouseY >= curY[0] && mouseY <= curY[0] + 16) {
                            hoveredTooltips.add(st.getHoverName());
                        }
                        px += 20;
                    }

                    drawScaledString(gg, shownCount + "/" + need, px, curY[0] + 4, color);
                    curY[0] += scaledRowHeight();
                    continue;
                } else if (t.isEntity()) {
                    if (!printedKillHeader) {
                    drawScaledString(gg, Component.translatable("ui.boundless.questbook.kill"), x + 4, curY[0], 0x1d9633);
                        curY[0] += scaledLineHeight() + 2;
                        printedKillHeader = true;
                    }

                    ResourceLocation rl = safeParse(t.id);
                    if (rl == null) {
                        drawScaledString(gg, Component.translatable("ui.boundless.questbook.invalid_entity_target"), x + 4, curY[0] + 4, 0xFF5555);
                        curY[0] += scaledRowHeight();
                        continue;
                    }
                    EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
                    String eName = et == null ? rl.toString() : et.getDescription().getString();

                    int rawKills = QuestTracker.getKillCount(mc.player, t.id);
                    int have = Math.min(rawKills, t.count);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;

                    Item iconItem;
                    if (et != null) {
                        ResourceLocation eggRl = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath() + "_spawn_egg");
                        iconItem = BuiltInRegistries.ITEM.getOptional(eggRl).orElse(Items.DIAMOND_SWORD);
                    } else {
                        iconItem = Items.DIAMOND_SWORD;
                    }

                    ItemStack icon = new ItemStack(iconItem);
                    renderScaledItem(gg, icon, x + 4, curY[0]);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18) {
                        hoveredTooltips.add(Component.literal(eName));
                    }

                    drawScaledString(gg, have + "/" + t.count, x + 24, curY[0] + 4, color);
                    curY[0] += scaledRowHeight();
                } else if (t.isEffect()) {
                    drawScaledString(gg, Component.translatable("ui.boundless.questbook.have_effect"), x + 4, curY[0], 0x55FFFF);
                    curY[0] += scaledLineHeight() + 2;

                    ResourceLocation rl = safeParse(t.id);
                    if (rl == null) {
                        drawScaledString(gg, Component.translatable("ui.boundless.questbook.invalid_effect_target"), x + 4, curY[0] + 4, 0xFF5555);
                        curY[0] += scaledRowHeight();
                        continue;
                    }
                    MobEffect eff = BuiltInRegistries.MOB_EFFECT.getOptional(rl).orElse(null);
                    String eName = eff == null ? rl.toString() : Component.translatable(eff.getDescriptionId()).getString();
                    boolean has = QuestTracker.hasEffect(mc.player, t.id);
                    int color = has ? 0x55FF55 : 0xFF5555;

                    ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/effects/" + rl.getPath() + ".png");
                    renderScaledTextureIcon(gg, tex, x + 4, curY[0]);
                    drawScaledString(gg, eName, x + 26, curY[0] + 6, color);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18) {
                        hoveredTooltips.add(Component.literal(eName));
                    }

                    curY[0] += scaledRowHeight();
                } else if (t.isAdvancement()) {
                    drawScaledString(gg, Component.translatable("ui.boundless.questbook.achieve"), x + 4, curY[0], 0x55FFFF);
                    curY[0] += scaledLineHeight() + 2;

                    ResourceLocation rl = safeParse(t.id);
                    if (rl == null) {
                        drawScaledString(gg, Component.translatable("ui.boundless.questbook.invalid_advancement_target"), x + 4, curY[0] + 4, 0xFF5555);
                        curY[0] += scaledRowHeight();
                        continue;
                    }
                    ItemStack icon = new ItemStack(Items.MOJANG_BANNER_PATTERN);
                    String advName = rl.toString();

                    AdvancementHolder holder = null;

                    if (mc.getConnection() != null) {
                        holder = mc.getConnection().getAdvancements().get(rl);
                    }

                    if (holder == null && mc.hasSingleplayerServer()) {
                        var server = mc.getSingleplayerServer();
                        if (server != null) holder = server.getAdvancements().get(rl);
                    }

                    if (holder != null) {
                        var displayOpt = holder.value().display();
                        if (displayOpt.isPresent()) {
                            DisplayInfo di = displayOpt.get();
                            advName = di.getTitle().getString();
                            icon = di.getIcon();
                        }
                    }

                    boolean done = QuestTracker.hasAdvancement(mc.player, t.id);
                    int color = done ? 0x55FF55 : 0xFF5555;

                    renderScaledItem(gg, icon, x + 4, curY[0]);
                    drawScaledString(gg, advName, x + 26, curY[0] + 6, color);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18) {
                        hoveredTooltips.add(Component.literal(advName));
                    }

                    curY[0] += scaledRowHeight();
                } else if (t.isStat()) {
                    drawScaledString(gg, Component.translatable("ui.boundless.questbook.stat"), x + 4, curY[0], 0x1d9633);
                    curY[0] += scaledLineHeight() + 2;

                    int have = QuestTracker.getStatCount(mc.player, t.id);
                    int color = have >= t.count ? 0x55FF55 : 0xFF5555;

                    ItemStack icon = new ItemStack(Items.PAPER);
                    renderScaledItem(gg, icon, x + 4, curY[0]);
                    drawScaledString(gg, have + "/" + t.count, x + 24, curY[0] + 4, color);
                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= curY[0] && mouseY <= curY[0] + 18) {
                        hoveredTooltips.add(Component.literal(t.id));
                    }
                    curY[0] += scaledRowHeight();
                }
            }

            curY[0] += 2;
        }

        int lootCommandCount = commandLootRewardCount();
        boolean hasItemRewards = quest.rewards != null && quest.rewards.items != null && !quest.rewards.items.isEmpty();
        boolean hasCommandRewards = quest.rewards != null && quest.rewards.hasCommands() && quest.rewards.commands.size() > lootCommandCount;
        boolean hasFunctionRewards = quest.rewards != null && quest.rewards.hasFunctions();
        boolean hasLootTableRewards = (quest.rewards != null && quest.rewards.hasLootTables()) || lootCommandCount > 0;
        boolean hasExpReward = quest.rewards != null && quest.rewards.hasExp();

        boolean hasAnyReward = hasItemRewards || hasCommandRewards || hasFunctionRewards || hasLootTableRewards || hasExpReward;
        for (ItemClickRegion region : itemRegions) {
            if (region.contains(mouseX, mouseY) && region.stack != null && !region.stack.isEmpty()) {
                hoveredTooltips.add(region.stack.getHoverName());
                break;
            }
        }
        if (!hasAnyReward) {
            gg.disableScissor();
            for (Component tip : hoveredTooltips) gg.renderTooltip(mc.font, tip, mouseX, mouseY);
            updateBottomButtons();
            return;
        }

            Component rewardLabel = Component.translatable("ui.boundless.questbook.reward");
            drawScaledWordWrap(gg, rewardLabel, x + 4, curY[0], w - 8, 0xA8FFA8);
            curY[0] += scaledWrappedHeight(rewardLabel, w - 8) + 4;

        if (hasItemRewards) {
            for (QuestData.RewardEntry re : quest.rewards.items) {
                Item item = resolveItem(QuestItemSpec.stripComponents(re.item));
                int lineY = curY[0];
                if (item != null) {
                    ItemStack st = new ItemStack(item, Math.max(1, re.count));
                    renderScaledItem(gg, st, x + 4, lineY);
                    itemRegions.add(new ItemClickRegion(x + 4, lineY, 16, 16, st.copy(), false));
                    drawScaledString(gg, "x" + st.getCount(), x + 24, lineY + 6, 0xA8FFA8);
                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16) {
                        hoveredTooltips.add(st.getHoverName());
                    }
                } else {
                    drawScaledWordWrap(gg,
                            Component.literal("- " + re.item + " x" + Math.max(1, re.count)),
                            x + 4, lineY, w - 8, 0xA8FFA8);
                }
                curY[0] += scaledRowHeight();
            }
        }

        if (hasCommandRewards) {
            for (QuestData.CommandReward cr : quest.rewards.commands) {
                String lootTableId = lootTableIdFromCommand(cr.command);
                if (!lootTableId.isBlank()) continue;
                int lineY = curY[0];

                ItemStack icon = new ItemStack(Items.COMMAND_BLOCK);
                if (cr.icon != null && !cr.icon.isBlank()) {
                    Item it = resolveItem(cr.icon);
                    if (it != null) icon = new ItemStack(it);
                }

                String display = (cr.title != null && !cr.title.isBlank()) ? cr.title : cr.command;
                Component displayComponent = Component.literal(display);

                renderScaledItem(gg, icon, x + 4, lineY);
                drawScaledWordWrap(gg, displayComponent, x + 24, lineY + 4, w - 30, 0xA8FFA8);

                if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16) {
                    hoveredTooltips.add(Component.literal(cr.command));
                }

                curY[0] += Math.max(scaledRowHeight(), scaledWrappedHeight(displayComponent, w - 30) + 8);
            }
        }

        if (hasFunctionRewards) {
            for (QuestData.FunctionReward fr : quest.rewards.functions) {
                int lineY = curY[0];

                ItemStack icon = new ItemStack(Items.KNOWLEDGE_BOOK);
                if (fr.icon != null && !fr.icon.isBlank()) {
                    Item it = resolveItem(fr.icon);
                    if (it != null) icon = new ItemStack(it);
                }

                String display = (fr.title != null && !fr.title.isBlank()) ? fr.title : fr.function;
                Component displayComponent = Component.literal(display);

                renderScaledItem(gg, icon, x + 4, lineY);
                drawScaledWordWrap(gg, displayComponent, x + 24, lineY + 4, w - 30, 0xA8FFA8);

                if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16) {
                    hoveredTooltips.add(Component.literal(fr.function));
                }

                curY[0] += Math.max(scaledRowHeight(), scaledWrappedHeight(displayComponent, w - 30) + 8);
            }
        }

        if (hasLootTableRewards) {
            if (quest.rewards != null && quest.rewards.commands != null) {
                for (QuestData.CommandReward cr : quest.rewards.commands) {
                    String lootTableId = lootTableIdFromCommand(cr.command);
                    if (lootTableId.isBlank()) continue;
                    int lineY = curY[0];
                    ItemStack icon = lootTableIcon(lootTableId);
                    String pretty = prettyLootTableName(lootTableId);
                    String display = Component.translatable("ui.boundless.questbook.loot_table", pretty).getString();
                    Component displayComponent = Component.literal(display);

                    renderScaledItem(gg, icon, x + 4, lineY);
                    drawScaledWordWrap(gg, displayComponent, x + 24, lineY + 4, w - 30, 0xA8FFA8);

                    if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16) {
                        hoveredTooltips.add(Component.literal(pretty));
                    }

                    curY[0] += Math.max(scaledRowHeight(), scaledWrappedHeight(displayComponent, w - 30) + 8);
                }
            }
            for (QuestData.LootTableReward lr : quest.rewards.lootTables) {
                int lineY = curY[0];

                ItemStack icon = lootTableIcon(lr.lootTable);
                if (lr.icon != null && !lr.icon.isBlank()) {
                    Item it = resolveItem(lr.icon);
                    if (it != null) icon = new ItemStack(it);
                }

                String pretty = (lr.title != null && !lr.title.isBlank()) ? lr.title : prettyLootTableName(lr.lootTable);
                String display = Component.translatable("ui.boundless.questbook.loot_table", pretty).getString();
                Component displayComponent = Component.literal(display);

                renderScaledItem(gg, icon, x + 4, lineY);
                drawScaledWordWrap(gg, displayComponent, x + 24, lineY + 4, w - 30, 0xA8FFA8);

                if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16) {
                    hoveredTooltips.add(Component.literal(pretty));
                }

                curY[0] += Math.max(scaledRowHeight(), scaledWrappedHeight(displayComponent, w - 30) + 8);
            }
        }

        if (hasExpReward) {
            int lineY = curY[0];
            renderScaledItem(gg, new ItemStack(Items.EXPERIENCE_BOTTLE), x + 4, lineY);
            String txt = switch (quest.rewards.expType) {
                case "levels" -> Component.translatable("ui.boundless.questbook.levels_amount", quest.rewards.expAmount).getString();
                case "levelup" -> Component.translatable("ui.boundless.questbook.levelup_xp_amount", quest.rewards.expAmount).getString();
                default -> Component.translatable("ui.boundless.questbook.xp_amount", quest.rewards.expAmount).getString();
            };
            drawScaledString(gg, txt, x + 24, lineY + 6, 0xA8FFA8);
            if (mouseX >= x + 4 && mouseX <= x + 20 && mouseY >= lineY && mouseY <= lineY + 16) {
                hoveredTooltips.add(Component.literal(txt));
            }
            curY[0] += scaledRowHeight();
        }

        curY[0] += 2;

        gg.disableScissor();

        for (Component tip : hoveredTooltips) gg.renderTooltip(mc.font, tip, mouseX, mouseY);

        updateBottomButtons();
    }

    private void updateBottomButtons() {
        if (quest == null || mc.player == null) return;

        boolean depsMet = QuestTracker.dependenciesMet(quest, mc.player);
        QuestTracker.Status status = QuestTracker.getStatus(quest, mc.player);
        boolean canRepeat = QuestTracker.canRestartRepeatable(quest, mc.player);

        boolean red = status == QuestTracker.Status.REDEEMED;
        boolean rej = status == QuestTracker.Status.REJECTED;
        boolean done = red || rej;
        boolean ready = depsMet && !done
                && (status == QuestTracker.Status.COMPLETED || QuestTracker.isReady(quest, mc.player));

        complete.setMessage(Component.translatable(canRepeat ? "ui.boundless.questbook.repeat" : "quest.boundless.complete"));
        complete.active = canRepeat || ready;
        complete.visible = !rej && (canRepeat || !red);

        reject.setOptionalAllowed(quest.optional);
        reject.active = !done && !canRepeat && quest.optional;
        reject.visible = !done && !canRepeat;
        if (!reject.active || !reject.visible) reject.resetConfirmState();

        back.visible = !hideBackButton;
        back.active = !hideBackButton;
    }

    private int measureContentHeight(int panelWidth) {
        if (quest == null) return 0;

        int w = panelWidth;
        int y = 0;

        if (!quest.description.isBlank()) {
            String full = quest.description;
            boolean needsMore = full.length() > DESC_CHAR_LIMIT;

            String shown = full;
            if (needsMore && !descExpanded) {
                int cut = full.lastIndexOf(" ", DESC_CHAR_LIMIT);
                if (cut < 0) cut = DESC_CHAR_LIMIT;
                shown = full.substring(0, cut) + "...";
            }

            int wrapH = scaledWrappedHeight(formatColorCodes(shown, 0xCFCFCF), w - 8);
            if (needsMore) y += wrapH + scaledLineHeight() + 6;
            else y += wrapH + 8;
        }

        if (!quest.dependencies.isEmpty()) {
            y += scaledWrappedHeight(Component.translatable("ui.boundless.questbook.requires"), w - 8) + 2;
            y += quest.dependencies.size() * scaledRowHeight() + 2;
        }

        if (quest.completion != null && !quest.completion.targets.isEmpty()) {
            boolean printedCollectHeader = false;
            boolean printedSubmitHeader = false;
            boolean printedKillHeader = false;
            for (QuestData.Target target : quest.completion.targets) {
                if (target == null) continue;
                if (target.isXp() || target.isLevelUpLevel() || target.isSubmit() || target.isFieldInput()) {
                    if (!printedSubmitHeader) {
                        y += scaledLineHeight() + 2;
                        printedSubmitHeader = true;
                    }
                } else if (target.isItem()) {
                    if (!printedCollectHeader) {
                        y += scaledLineHeight() + 2;
                        printedCollectHeader = true;
                    }
                } else if (target.isEntity()) {
                    if (!printedKillHeader) {
                        y += scaledLineHeight() + 2;
                        printedKillHeader = true;
                    }
                } else if (target.isEffect() || target.isAdvancement() || target.isStat()) {
                    y += scaledLineHeight() + 2;
                }

                if (target.isFieldInput()) {
                    y += 20 + 4;
                } else {
                    y += scaledRowHeight();
                }
            }
            y += 2;
        }

        int lootCommandCount = commandLootRewardCount();
        boolean hasItemRewards = quest.rewards != null && quest.rewards.items != null && !quest.rewards.items.isEmpty();
        boolean hasCommandRewards = quest.rewards != null && quest.rewards.hasCommands() && quest.rewards.commands.size() > lootCommandCount;
        boolean hasFunctionRewards = quest.rewards != null && quest.rewards.hasFunctions();
        boolean hasLootTableRewards = (quest.rewards != null && quest.rewards.hasLootTables()) || lootCommandCount > 0;
        boolean hasExpReward = quest.rewards != null && quest.rewards.hasExp();

        if (hasItemRewards || hasCommandRewards || hasFunctionRewards || hasLootTableRewards || hasExpReward) {
            y += scaledWrappedHeight(Component.translatable("ui.boundless.questbook.reward"), w - 8) + 4;
            if (hasItemRewards) y += quest.rewards.items.size() * scaledRowHeight();
            if (hasCommandRewards) {
                for (QuestData.CommandReward cr : quest.rewards.commands) {
                    if (!lootTableIdFromCommand(cr.command).isBlank()) continue;
                    String display = (cr.title != null && !cr.title.isBlank()) ? cr.title : cr.command;
                    y += Math.max(scaledRowHeight(), scaledWrappedHeight(Component.literal(display), w - 30) + 8);
                }
            }
            if (hasFunctionRewards) {
                for (QuestData.FunctionReward fr : quest.rewards.functions) {
                    String display = (fr.title != null && !fr.title.isBlank()) ? fr.title : fr.function;
                    y += Math.max(scaledRowHeight(), scaledWrappedHeight(Component.literal(display), w - 30) + 8);
                }
            }
            if (hasLootTableRewards) {
                if (quest.rewards.commands != null) {
                    for (QuestData.CommandReward cr : quest.rewards.commands) {
                        String lootTableId = lootTableIdFromCommand(cr.command);
                        if (lootTableId.isBlank()) continue;
                        String pretty = prettyLootTableName(lootTableId);
                        String display = Component.translatable("ui.boundless.questbook.loot_table", pretty).getString();
                        y += Math.max(scaledRowHeight(), scaledWrappedHeight(Component.literal(display), w - 30) + 8);
                    }
                }
                for (QuestData.LootTableReward lr : quest.rewards.lootTables) {
                    String pretty = (lr.title != null && !lr.title.isBlank()) ? lr.title : prettyLootTableName(lr.lootTable);
                    String display = Component.translatable("ui.boundless.questbook.loot_table", pretty).getString();
                    y += Math.max(scaledRowHeight(), scaledWrappedHeight(Component.literal(display), w - 30) + 8);
                }
            }
            if (hasExpReward) y += scaledRowHeight();
            y += 2;
        }

        return y;
    }

    private int wrappedHeight(Component component, int maxWidth) {
        if (component == null || maxWidth <= 0) return 0;
        return mc.font.split(component, maxWidth).size() * mc.font.lineHeight;
    }

    private float textScale() {
        return Config.questTextScale();
    }

    private int scaledLineHeight() {
        return Math.max(1, Math.round(mc.font.lineHeight * textScale()));
    }

    private int scaledRowHeight() {
        return Math.max(LINE_ITEM_ROW, scaledLineHeight() + 10);
    }

    private int scaledWrapWidth(int physicalWidth) {
        float scale = textScale();
        return Math.max(1, (int) Math.floor(physicalWidth / scale));
    }

    private int scaledWrappedHeight(Component component, int physicalWidth) {
        if (component == null || physicalWidth <= 0) return 0;
        int wrapWidth = scaledWrapWidth(physicalWidth);
        return Math.max(1, Math.round(mc.font.split(component, wrapWidth).size() * mc.font.lineHeight * textScale()));
    }

    private int scaledTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.round(mc.font.width(text) * textScale());
    }

    private void drawScaledString(GuiGraphics gg, Component text, int x, int y, int color) {
        if (text == null) return;
        float scale = textScale();
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1f);
        float inv = 1f / scale;
        gg.drawString(mc.font, text, (int) (x * inv), (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    private void drawScaledString(GuiGraphics gg, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) return;
        float scale = textScale();
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1f);
        float inv = 1f / scale;
        gg.drawString(mc.font, text, (int) (x * inv), (int) (y * inv), color, false);
        gg.pose().popPose();
    }

    private void drawScaledWordWrap(GuiGraphics gg, Component text, int x, int y, int physicalWidth, int color) {
        if (text == null || physicalWidth <= 0) return;
        float scale = textScale();
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1f);
        float inv = 1f / scale;
        gg.drawWordWrap(mc.font, text, (int) (x * inv), (int) (y * inv), scaledWrapWidth(physicalWidth), color);
        gg.pose().popPose();
    }

    private void renderScaledItem(GuiGraphics gg, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        float scale = Config.questIconScale();
        int size = Math.max(1, Math.round(16 * scale));
        int dx = x + (16 - size) / 2;
        int dy = y + (16 - size) / 2;
        gg.pose().pushPose();
        gg.pose().translate(dx, dy, 0);
        gg.pose().scale(scale, scale, 1f);
        gg.renderItem(stack, 0, 0);
        gg.pose().popPose();
    }

    private void renderScaledTextureIcon(GuiGraphics gg, ResourceLocation texture, int x, int y) {
        if (texture == null) return;
        float scale = Config.questIconScale();
        int size = Math.max(1, Math.round(16 * scale));
        int dx = x + (16 - size) / 2;
        int dy = y + (16 - size) / 2;
        gg.pose().pushPose();
        gg.pose().translate(dx, dy, 0);
        gg.pose().scale(scale, scale, 1f);
        gg.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        gg.pose().popPose();
    }

    private void addDescriptionItemRegions(String text, int x, int y, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return;
        float scale = textScale();
        List<String> lines = wrapPlainText(text, scaledWrapWidth(maxWidth));
        int lineY = y;
        for (String line : lines) {
            Matcher matcher = ITEM_ID_PATTERN.matcher(line);
            while (matcher.find()) {
                String token = matcher.group();
                Item item = resolveItem(token);
                if (item == null) continue;
                int startX = x + Math.round(mc.font.width(line.substring(0, matcher.start())) * scale);
                int width = Math.round(mc.font.width(token) * scale);
                itemRegions.add(new ItemClickRegion(startX, lineY, width, scaledLineHeight(), new ItemStack(item)));
            }
            lineY += scaledLineHeight();
        }
    }

    private List<String> wrapPlainText(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && mc.font.width(candidate) > maxWidth) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private Component formatColorCodes(String raw, int defaultColor) {
        if (!Config.enableDescriptionColors()) {
            return Component.literal(stripColorTokens(raw)).withStyle(Style.EMPTY.withColor(defaultColor));
        }
        MutableComponent out = Component.empty();
        String text = raw == null ? "" : raw;
        int current = defaultColor;
        boolean bold = false;
        boolean italic = false;
        boolean encrypted = false;
        int segStart = 0;
        for (int i = 0; i < text.length(); i++) {
            if (startsWithColorToken(text, i)) {
                if (i > segStart) {
                    out.append(Component.literal(text.substring(segStart, i))
                            .withStyle(Style.EMPTY.withColor(current).withBold(bold).withItalic(italic).withObfuscated(encrypted)));
                }
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'l') {
                    bold = !bold;
                } else if (code == 'i') {
                    italic = !italic;
                } else if (code == 'e') {
                    encrypted = !encrypted;
                } else if (code == 'x') {
                    current = defaultColor;
                    bold = false;
                    italic = false;
                    encrypted = false;
                } else {
                    current = formatColor(code, defaultColor);
                }
                i += 1;
                segStart = i + 1;
            }
        }
        if (segStart < text.length()) {
            out.append(Component.literal(text.substring(segStart))
                    .withStyle(Style.EMPTY.withColor(current).withBold(bold).withItalic(italic).withObfuscated(encrypted)));
        }
        return out;
    }

    private String stripColorTokens(String raw) {
        String text = raw == null ? "" : raw;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (startsWithColorToken(text, i)) {
                i += 1;
                continue;
            }
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    private boolean startsWithColorToken(String text, int index) {
        return index + 1 < text.length()
                && text.charAt(index) == '/'
                && isColorTokenCode(text.charAt(index + 1));
    }

    private boolean isColorTokenCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case 'w', 'r', 'g', 'b', 'y', 'o', 'a', 'p', 'x', 'l', 'i', 'e' -> true;
            default -> false;
        };
    }

    private int formatColor(char code, int fallback) {
        return switch (Character.toLowerCase(code)) {
            case 'w' -> 0x55FFFF;
            case 'r' -> 0xFF5555;
            case 'g' -> 0x55FF55;
            case 'b' -> 0x5555FF;
            case 'y' -> 0xFFFF55;
            case 'o' -> 0xFFAA00;
            case 'a' -> 0xAAAAAA;
            case 'p' -> 0xAA55FF;
            case 'x' -> fallback;
            default -> fallback;
        };
    }

    private String lootTableIdFromCommand(String command) {
        if (command == null) return "";
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
        String prefix = "loot give @s loot ";
        if (!trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) return "";
        return trimmed.substring(prefix.length()).trim();
    }

    private int commandLootRewardCount() {
        if (quest == null || quest.rewards == null || quest.rewards.commands == null) return 0;
        int total = 0;
        for (QuestData.CommandReward cr : quest.rewards.commands) {
            if (cr != null && !lootTableIdFromCommand(cr.command).isBlank()) total++;
        }
        return total;
    }

    private String prettyLootTableName(String lootTableId) {
        if (lootTableId == null || lootTableId.isBlank()) return Component.translatable("ui.boundless.questbook.unknown").getString();
        String cleaned = lootTableId;
        int colon = cleaned.indexOf(':');
        if (colon >= 0 && colon + 1 < cleaned.length()) cleaned = cleaned.substring(colon + 1);
        if (cleaned.startsWith("chests/")) cleaned = cleaned.substring("chests/".length());
        else if (cleaned.startsWith("entities/")) cleaned = cleaned.substring("entities/".length());
        cleaned = cleaned.replace('/', ' ').replace('_', ' ').trim();
        if (cleaned.isBlank()) return Component.translatable("ui.boundless.questbook.unknown").getString();
        String[] parts = cleaned.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.toString();
    }

    private ItemStack lootTableIcon(String lootTableId) {
        if (lootTableId == null || lootTableId.isBlank()) return new ItemStack(Items.CHEST);
        String namespace = "minecraft";
        String path = lootTableId;
        int colon = lootTableId.indexOf(':');
        if (colon >= 0) {
            namespace = lootTableId.substring(0, colon);
            if (colon + 1 < lootTableId.length()) path = lootTableId.substring(colon + 1);
        }
        if (path.startsWith("entities/")) {
            String entityPath = path.substring("entities/".length());
            Item egg = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, entityPath + "_spawn_egg")).orElse(null);
            if (egg == null) {
                egg = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", "zombie_spawn_egg")).orElse(Items.ZOMBIE_SPAWN_EGG);
            }
            return new ItemStack(egg);
        }
        return new ItemStack(Items.CHEST);
    }

    private List<Item> resolveTagItems(ResourceLocation tagId) {
        List<Item> out = new ArrayList<>();
        var itemTag = net.minecraft.tags.TagKey.create(Registries.ITEM, tagId);
        for (Item it : BuiltInRegistries.ITEM) {
            if (it.builtInRegistryHolder().is(itemTag)) out.add(it);
        }
        if (out.isEmpty()) {
            var blockTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, tagId);
            for (Item it : BuiltInRegistries.ITEM) {
                if (it instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder().is(blockTag)) {
                    out.add(it);
                }
            }
        }
        return out;
    }

    private EditBox createInputBox() {
        EditBox box = new EditBox(mc.font, 0, 0, 40, 16, Component.empty());
        box.setMaxLength(128);
        box.setResponder(value -> {
            if (quest == null || quest.completion == null || quest.completion.targets == null || mc.player == null) return;
            for (QuestData.Target target : quest.completion.targets) {
                if (target == null || !target.isFieldInput()) continue;
                String key = quest.id + ":field:" + target.id;
                EditBox targetBox = inputBoxes.get(key);
                if (targetBox != box) continue;
                String normalized = value == null ? "" : value.trim();
                QuestTracker.setFieldInputProgress(mc.player, key, normalized);
                PacketDistributor.sendToServer(new BoundlessNetwork.UpdateFieldInput(quest.id, target.id, normalized));
                break;
            }
        });
        return box;
    }

    private boolean openJeiForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !JeiCompat.isJeiInstalled()) return false;
        return JeiCompat.showItem(stack, true) || JeiCompat.showItem(stack, false);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible || !this.active) return false;

        int contentTop = this.getY() + HEADER_HEIGHT;
        int contentBottom = complete.getY() - CONTENT_BOTTOM_MARGIN;

        if (mouseX < this.getX() || mouseX > this.getX() + this.width) return false;
        if (mouseY < contentTop || mouseY > contentBottom) return false;

        int viewportH = Math.max(0, contentBottom - contentTop);
        int maxScroll = Math.max(0, measuredContentHeight + BOTTOM_PADDING - viewportH);
        if (maxScroll <= 0) return false;

        scrollY = Mth.clamp(scrollY - (float) (delta * 12), 0f, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return mouseScrolled(mouseX, mouseY, deltaY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) return false;

        if (button == 0) {
            EditBox clicked = null;
            for (EditBox box : inputBoxes.values()) {
                if (!box.visible || !box.active) continue;
                boolean inside = mouseX >= box.getX() && mouseX <= box.getX() + box.getWidth()
                        && mouseY >= box.getY() && mouseY <= box.getY() + box.getHeight();
                if (inside) {
                    clicked = box;
                    break;
                }
            }
            if (clicked != null) {
                for (EditBox box : inputBoxes.values()) {
                    box.setFocused(box == clicked);
                }
                clicked.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            for (EditBox box : inputBoxes.values()) {
                box.setFocused(false);
            }
        }

        for (EditBox box : inputBoxes.values()) {
            if (!box.visible || !box.active) continue;
            if (box.mouseClicked(mouseX, mouseY, button)) return true;
        }

        if (pin.visible && pin.active && pin.isMouseOver(mouseX, mouseY)) {
            pin.onPress();
            return true;
        }

        if (scroll.visible && scroll.active && scroll.isMouseOver(mouseX, mouseY)) {
            scroll.onPress();
            return true;
        }

        for (DepClickRegion r : depRegions) {
            if (r.contains(mouseX, mouseY)) {
                if ("__desc_toggle__".equals(r.questId)) {
                    descExpanded = !descExpanded;
                    return true;
                }

                QuestData.Quest depQuest = QuestData.byId(r.questId).orElse(null);
                if (depQuest != null) {
                    setQuest(depQuest);
                    return true;
                }
            }
        }

        if (button == 0) {
            for (ItemClickRegion region : itemRegions) {
                if (region.jeiEnabled && region.contains(mouseX, mouseY) && openJeiForStack(region.stack)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : inputBoxes.values()) {
            if (!box.visible || !box.active) continue;
            if (box.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox box : inputBoxes.values()) {
            if (!box.visible || !box.active) continue;
            if (box.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {}

    private static final class PinButton extends AbstractButton {

        public PinButton(int x, int y) {
            super(x, y, 10, 10, Component.empty());
        }

        @Override
        public void onPress() {
            PinnedQuestHud.toggleCurrentQuest();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean pinned = PinnedQuestHud.isPinnedCurrentQuest();
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = pinned
                    ? (hovered ? TEX_UNPIN_HOVER : TEX_UNPIN)
                    : (hovered ? TEX_PIN_HOVER : TEX_PIN);

            gg.pose().pushPose();
            gg.pose().translate(getX(), getY(), 0);
            gg.pose().scale(0.5f, 0.5f, 1.0f);
            gg.blit(tex, 0, 0, 0, 0, 20, 20, 20, 20);
            gg.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class ScrollButton extends AbstractButton {
        private final Runnable onPress;

        private ScrollButton(int x, int y, Runnable onPress) {
            super(x, y, 10, 10, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            gg.pose().pushPose();
            gg.pose().translate(getX(), getY(), 0);
            gg.pose().scale(0.5f, 0.5f, 1.0f);
            gg.blit(TEX_SCROLL, 0, 0, 0, 0, 20, 20, 20, 20);
            gg.pose().popPose();
            if (this.isMouseOver(mouseX, mouseY)) {
                gg.renderTooltip(Minecraft.getInstance().font, Component.translatable("ui.boundless.questbook.create_scroll"), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class BackButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_button.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_back_highlighted.png");

        private final Runnable onPress;

        public BackButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = hovered ? TEX_HOVER : TEX_NORMAL;
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class CompleteButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_highlighted.png");
        private static final ResourceLocation TEX_DISABLED =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_complete_button_disabled.png");

        private final Runnable onPress;

        public CompleteButton(int x, int y, Runnable onPress) {
            super(x, y, 68, 20, Component.translatable("quest.boundless.complete"));
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            if (onPress != null) onPress.run();
            this.active = false;
            this.visible = false;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
            ResourceLocation tex = !this.active ? TEX_DISABLED : (hovered ? TEX_HOVER : TEX_NORMAL);
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            var font = Minecraft.getInstance().font;
            int textW = font.width(getMessage());
            int textX = getX() + (this.width - textW) / 2 + 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            int color = this.active ? 0xFFFFFF : 0x808080;
            gg.drawString(font, getMessage(), textX, textY, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class RejectButton extends AbstractButton {
        private static final ResourceLocation TEX_NORMAL =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject.png");
        private static final ResourceLocation TEX_HOVER =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_highlighted.png");
        private static final ResourceLocation TEX_DISABLED =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/quest_reject_disabled.png");
        private static final ResourceLocation TEX_CONFIRM =
                ResourceLocation.fromNamespaceAndPath("boundless", "textures/gui/sprites/editor/are_you_sure_button.png");

        private final Runnable onPress;
        private boolean optionalAllowed;
        private boolean confirmArmed;

        public RejectButton(int x, int y, Runnable onPress) {
            super(x, y, 24, 20, Component.empty());
            this.onPress = onPress;
        }

        public void setOptionalAllowed(boolean v) {
            this.optionalAllowed = v;
        }

        public void resetConfirmState() {
            this.confirmArmed = false;
        }

        @Override
        public void onPress() {
            if (!this.active) return;
            if (!this.confirmArmed) {
                this.confirmArmed = true;
                return;
            }
            this.confirmArmed = false;
            if (onPress != null) onPress.run();
            this.active = false;
            this.visible = false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.visible || button != 0) return false;
            boolean over = this.isMouseOver(mouseX, mouseY);
            if (this.confirmArmed && !over) {
                this.confirmArmed = false;
                return false;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            if (!this.active && this.confirmArmed) this.confirmArmed = false;
            ResourceLocation tex = !this.active ? TEX_DISABLED : (this.confirmArmed ? TEX_CONFIRM : (hovered ? TEX_HOVER : TEX_NORMAL));
            gg.blit(tex, getX(), getY(), 0, 0, this.width, this.height, this.width, this.height);

            if (hovered && !this.active && !optionalAllowed) {
                gg.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("ui.boundless.questbook.not_optional"),
                        mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
