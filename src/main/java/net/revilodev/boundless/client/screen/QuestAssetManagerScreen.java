package net.revilodev.boundless.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.revilodev.boundless.Config;
import net.revilodev.boundless.client.QuestMediaRenderer;
import net.revilodev.boundless.client.editor.QuestEditorCustomIcons;
import net.revilodev.boundless.client.editor.QuestEditorEmojis;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class QuestAssetManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 270;
    private static final int PANEL_HEIGHT = 206;
    private static final int ROW_HEIGHT = 28;
    private static final int LIST_HEIGHT = 140;
    private static final ResourceLocation MEDIA_ICON = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/sprites/editor/inventory.png");

    private final Screen parent;
    private AssetType type = AssetType.ASSET;
    private List<AssetEntry> entries = List.of();
    private AssetEntry selected;
    private int scroll;
    private int panelX;
    private int panelY;
    private HoldDeleteButton deleteButton;
    private Button uploadButton;
    private String status = "";
    private int statusColor = 0xFFA0FFA0;

    public QuestAssetManagerScreen(Screen parent) {
        super(Component.literal("Asset Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null
                || (!minecraft.hasSingleplayerServer() && !minecraft.player.hasPermissions(2))) {
            if (minecraft != null) minecraft.setScreen(parent);
            return;
        }
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        int tabY = panelY + 22;
        addRenderableWidget(Button.builder(Component.literal("Assets"), button -> setType(AssetType.ASSET))
                .bounds(panelX + 8, tabY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Media"), button -> setType(AssetType.MEDIA))
                .bounds(panelX + 95, tabY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Emojis"), button -> setType(AssetType.EMOJI))
                .bounds(panelX + 182, tabY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> minecraft.setScreen(parent))
                .bounds(panelX + 8, panelY + PANEL_HEIGHT - 25, 52, 20).build());
        uploadButton = addRenderableWidget(Button.builder(uploadLabel(), button -> upload())
                .bounds(panelX + 66, panelY + PANEL_HEIGHT - 25, 92, 20).build());
        deleteButton = addRenderableWidget(new HoldDeleteButton(panelX + 164, panelY + PANEL_HEIGHT - 25, 98, 20,
                this::deleteSelected));
        reloadEntries();
    }

    private void setType(AssetType type) {
        this.type = type;
        selected = null;
        scroll = 0;
        status = "";
        if (uploadButton != null) uploadButton.setMessage(uploadLabel());
        reloadEntries();
    }

    private Component uploadLabel() {
        return Component.literal("Upload " + type.label.toLowerCase(Locale.ROOT));
    }

    private void reloadEntries() {
        String usage = usageCorpus();
        List<AssetEntry> found = new ArrayList<>();
        Path root = type.root();
        try {
            Files.createDirectories(root);
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> type.accepts(path.getFileName().toString()))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> found.add(assetEntry(root, path, usage)));
            }
        } catch (IOException exception) {
            status = "Could not read asset folder";
            statusColor = 0xFFFF7070;
        }
        entries = List.copyOf(found);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        if (selected != null) {
            selected = entries.stream().filter(entry -> entry.path.equals(selected.path)).findFirst().orElse(null);
        }
    }

    private AssetEntry assetEntry(Path root, Path path, String usage) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String withoutExtension = relative.substring(0, relative.lastIndexOf('.')).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
        String reference = switch (type) {
            case ASSET -> QuestEditorCustomIcons.PREFIX + withoutExtension;
            case MEDIA -> relative;
            case EMOJI -> withoutExtension;
        };
        int[] dimensions = dimensions(path);
        boolean used = switch (type) {
            case ASSET -> usage.contains(reference) || usage.contains(relative);
            case MEDIA -> usage.contains(relative) || usage.contains(path.getFileName().toString());
            case EMOJI -> usage.contains(":" + reference + ":")
                    || usage.contains(QuestEditorEmojis.iconId(reference));
        };
        return new AssetEntry(path, relative, reference, dimensions[0], dimensions[1], used);
    }

    private String usageCorpus() {
        StringBuilder content = new StringBuilder();
        Path root = Config.questPacksRoot();
        try {
            Files.createDirectories(root);
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                            return name.endsWith(".json") || name.endsWith(".json5");
                        })
                        .forEach(path -> {
                            try {
                                content.append(Files.readString(path)).append('\n');
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
        return content.toString();
    }

    private int[] dimensions(Path path) {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) return new int[]{0, 0};
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return new int[]{0, 0};
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException ignored) {
            return new int[]{0, 0};
        }
    }

    private void upload() {
        try {
            Path root = type.root().toAbsolutePath().normalize();
            Files.createDirectories(root);
            String selectedFile;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                List<String> patterns = type.patterns();
                PointerBuffer filters = stack.mallocPointer(patterns.size());
                for (String pattern : patterns) filters.put(stack.UTF8(pattern));
                filters.flip();
                selectedFile = TinyFileDialogs.tinyfd_openFileDialog(
                        "Upload " + type.label.toLowerCase(Locale.ROOT), root.toString(), filters,
                        type.description, false);
            }
            if (selectedFile == null || selectedFile.isBlank()) return;
            Path source = Path.of(selectedFile).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source) || !type.accepts(source.getFileName().toString())) {
                throw new IOException("Unsupported file type");
            }
            int[] size = dimensions(source);
            if (size[0] <= 0 || size[1] <= 0) throw new IOException("Unreadable image");
            if (type == AssetType.EMOJI && size[0] != size[1]) {
                status = "Emoji images must use a 1:1 aspect ratio";
                statusColor = 0xFFFF7070;
                return;
            }
            String fileName = source.getFileName().toString().replaceAll("[^A-Za-z0-9_.-]", "_");
            Path target = root.resolve(fileName).normalize();
            if (!target.startsWith(root)) throw new IOException("Invalid asset path");
            if (!source.equals(target)) Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            clearAssetCaches();
            reloadEntries();
            selected = entries.stream().filter(entry -> entry.path.equals(target)).findFirst().orElse(null);
            status = fileName + " uploaded";
            statusColor = 0xFFA0FFA0;
        } catch (Exception exception) {
            status = "Could not upload " + type.label.toLowerCase(Locale.ROOT);
            statusColor = 0xFFFF7070;
        }
    }

    private void deleteSelected() {
        if (selected == null) return;
        String name = selected.relative;
        try {
            Path root = type.root().toAbsolutePath().normalize();
            Path target = selected.path.toAbsolutePath().normalize();
            if (!target.startsWith(root)) throw new IOException("Invalid asset path");
            Files.deleteIfExists(target);
            selected = null;
            clearAssetCaches();
            reloadEntries();
            status = name + " deleted";
            statusColor = 0xFFA0FFA0;
        } catch (IOException exception) {
            status = "Could not delete " + name;
            statusColor = 0xFFFF7070;
        }
    }

    private void clearAssetCaches() {
        QuestEditorCustomIcons.clear();
        QuestEditorEmojis.clearCache();
        QuestMediaRenderer.clear();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF171717);
        graphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + PANEL_HEIGHT - 1, 0xFF2B2B2B);
        graphics.drawCenteredString(font, title, width / 2, panelY + 7, 0xFFFFFFFF);
        int listX = panelX + 8;
        int listY = panelY + 47;
        graphics.fill(listX, listY, listX + PANEL_WIDTH - 16, listY + LIST_HEIGHT, 0xFF111111);
        AssetEntry hovered = null;
        int visibleRows = LIST_HEIGHT / ROW_HEIGHT;
        for (int row = 0; row < visibleRows && scroll + row < entries.size(); row++) {
            AssetEntry entry = entries.get(scroll + row);
            int y = listY + row * ROW_HEIGHT;
            boolean over = mouseX >= listX && mouseX < listX + PANEL_WIDTH - 16 && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean chosen = entry.equals(selected);
            int border = chosen ? 0xFFFFFFFF : 0xFF555555;
            graphics.fill(listX + 1, y + 1, listX + PANEL_WIDTH - 17, y + ROW_HEIGHT - 1, border);
            graphics.fill(listX + 2, y + 2, listX + PANEL_WIDTH - 18, y + ROW_HEIGHT - 2,
                    over ? 0xFF3A3A3A : 0xFF242424);
            renderPreview(graphics, entry, listX + 5, y + 4);
            graphics.drawString(font, font.plainSubstrByWidth(entry.relative, PANEL_WIDTH - 58), listX + 31, y + 5,
                    0xFFFFFFFF, false);
            String details = entry.width + "×" + entry.height + "  " + (entry.used ? "Used" : "Unused");
            graphics.drawString(font, details, listX + 31, y + 16, entry.used ? 0xFF80FF80 : 0xFFAAAAAA, false);
            if (over) hovered = entry;
        }
        if (entries.isEmpty()) graphics.drawCenteredString(font, "No " + type.label.toLowerCase(Locale.ROOT) + " found",
                width / 2, listY + 62, 0xFFAAAAAA);
        if (!status.isBlank()) graphics.drawCenteredString(font, status, width / 2, panelY + PANEL_HEIGHT + 5, statusColor);
        deleteButton.active = selected != null;
        deleteButton.setMessage(Component.literal(selected == null ? "Select an asset" : "Hold to delete"));
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) {
            List<Component> tooltip = List.of(
                    Component.literal(hovered.relative),
                    Component.literal(type.label),
                    Component.literal("Dimensions: " + hovered.width + " × " + hovered.height).withStyle(ChatFormatting.GRAY),
                    Component.literal(hovered.used ? "Used" : "Unused")
                            .withStyle(hovered.used ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderPreview(GuiGraphics graphics, AssetEntry entry, int x, int y) {
        ResourceLocation texture = switch (type) {
            case ASSET -> QuestEditorCustomIcons.texture(entry.reference);
            case EMOJI -> QuestEditorEmojis.texture(entry.reference);
            case MEDIA -> MEDIA_ICON;
        };
        if (texture == null) return;
        int sourceWidth = type == AssetType.MEDIA ? 20 : Math.max(1, entry.width);
        int sourceHeight = type == AssetType.MEDIA ? 20 : Math.max(1, entry.height);
        graphics.blit(texture, x, y, 0, 0, 20, 20, sourceWidth, sourceHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        int listX = panelX + 8;
        int listY = panelY + 47;
        if (mouseX < listX || mouseX >= listX + PANEL_WIDTH - 16 || mouseY < listY || mouseY >= listY + LIST_HEIGHT) return false;
        int row = (int) ((mouseY - listY) / ROW_HEIGHT);
        int index = scroll + row;
        if (index >= 0 && index < entries.size()) selected = entries.get(index);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scroll;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        return old != scroll || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - LIST_HEIGHT / ROW_HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private enum AssetType {
        ASSET("Assets", List.of("*.png"), "PNG images"),
        MEDIA("Media", List.of("*.png", "*.jpg", "*.jpeg", "*.gif"), "Images and looping GIFs"),
        EMOJI("Emojis", List.of("*.png"), "Square PNG images");

        private final String label;
        private final List<String> patterns;
        private final String description;

        AssetType(String label, List<String> patterns, String description) {
            this.label = label;
            this.patterns = patterns;
            this.description = description;
        }

        private Path root() {
            return switch (this) {
                case ASSET -> QuestEditorCustomIcons.directory();
                case MEDIA -> Config.questMediaRoot();
                case EMOJI -> QuestEditorEmojis.directory();
            };
        }

        private List<String> patterns() {
            return patterns;
        }

        private boolean accepts(String name) {
            String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
            return patterns.stream().anyMatch(pattern -> lower.endsWith(pattern.substring(1)));
        }
    }

    private record AssetEntry(Path path, String relative, String reference, int width, int height, boolean used) {
    }

    private static final class HoldDeleteButton extends AbstractButton {
        private static final long HOLD_TIME_MS = 3000L;
        private final Runnable onDelete;
        private boolean holding;
        private long startedAt;

        private HoldDeleteButton(int x, int y, int width, int height, Runnable onDelete) {
            super(x, y, width, height, Component.literal("Select an asset"));
            this.onDelete = onDelete;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            holding = true;
            startedAt = Util.getMillis();
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) holding = false;
            return true;
        }

        @Override
        public void onPress() {
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (holding && (!active || !visible || !isMouseOver(mouseX, mouseY))) holding = false;
            float progress = holding ? Math.min(1f, (Util.getMillis() - startedAt) / (float) HOLD_TIME_MS) : 0f;
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (progress > 0f) {
                graphics.fill(getX() + 2, getY() + height - 3,
                        getX() + 2 + Math.round((width - 4) * progress), getY() + height - 1, 0xFFFF3030);
            }
            if (progress >= 1f) {
                holding = false;
                if (onDelete != null) onDelete.run();
            }
            if (active && isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(Minecraft.getInstance().font, Component.literal("Hold for 3 seconds to delete"), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }
}
