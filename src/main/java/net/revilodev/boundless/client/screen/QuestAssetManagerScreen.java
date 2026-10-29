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
import net.revilodev.boundless.client.QuestPanelBackgroundRenderer;
import net.revilodev.boundless.client.editor.QuestEditorCustomIcons;
import net.revilodev.boundless.client.editor.QuestEditorControls;
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
import java.util.concurrent.CompletableFuture;

@OnlyIn(Dist.CLIENT)
public final class QuestAssetManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 270;
    private static final int PANEL_HEIGHT = 166;
    private static final int ROW_HEIGHT = 28;
    private static final int LIST_HEIGHT = 76;
    private static final int LIST_COLUMNS = 2;
    private static final int COLUMN_GAP = 2;
    private static final ResourceLocation MEDIA_ICON = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/sprites/editor/inventory.png");
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "boundless", "textures/gui/quest_panel.png");

    private final Screen parent;
    private AssetType type = AssetType.ASSET;
    private List<AssetEntry> entries = List.of();
    private AssetEntry selected;
    private int scroll;
    private int panelX;
    private int panelY;
    private Button deleteButton;
    private Button uploadButton;
    private Button renameButton;
    private String status = "";
    private int statusColor = 0xFFA0FFA0;
    private boolean loading;
    private int loadGeneration;

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
        addRenderableWidget(new AssetTabButton(panelX + 8, tabY, 80, AssetType.ASSET));
        addRenderableWidget(new AssetTabButton(panelX + 95, tabY, 80, AssetType.MEDIA));
        addRenderableWidget(new AssetTabButton(panelX + 182, tabY, 80, AssetType.EMOJI));
        addRenderableWidget(new QuestEditorControls.BackButton(
                panelX - 24, panelY + PANEL_HEIGHT - 23, () -> minecraft.setScreen(parent)));
        renameButton = addRenderableWidget(Button.builder(Component.literal("Rename"), button -> renameSelected())
                .bounds(panelX + 8, panelY + PANEL_HEIGHT - 35, 52, 20).build());
        uploadButton = addRenderableWidget(Button.builder(uploadLabel(), button -> upload())
                .bounds(panelX + 66, panelY + PANEL_HEIGHT - 35, 92, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteSelected())
                .bounds(panelX + 164, panelY + PANEL_HEIGHT - 35, 98, 20).build());
        renameButton.active = false;
        deleteButton.active = false;
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
        return Component.literal("Upload");
    }

    private void reloadEntries() {
        reloadEntries(null);
    }

    private void reloadEntries(Runnable afterLoad) {
        int generation = ++loadGeneration;
        loading = true;
        status = "Loading " + type.label.toLowerCase(Locale.ROOT) + "...";
        statusColor = 0xFFAAAAAA;
        AssetType requestedType = type;
        CompletableFuture.supplyAsync(() -> scanEntries(requestedType), Util.backgroundExecutor())
                .thenAccept(found -> {
                    if (minecraft == null) return;
                    minecraft.execute(() -> {
                        if (generation != loadGeneration || minecraft.screen != this) return;
                        entries = found;
                        loading = false;
                        status = "";
                        scroll = Math.max(0, Math.min(scroll, maxScroll()));
                        if (selected != null) {
                            selected = entries.stream().filter(entry -> entry.path.equals(selected.path)).findFirst().orElse(null);
                        }
                        if (afterLoad != null) afterLoad.run();
                    });
                });
    }

    private List<AssetEntry> scanEntries(AssetType requestedType) {
        String usage = usageCorpus();
        List<AssetEntry> found = new ArrayList<>();
        Path root = requestedType.root();
        try {
            Files.createDirectories(root);
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> requestedType.accepts(path.getFileName().toString()))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> found.add(assetEntry(requestedType, root, path, usage)));
            }
        } catch (IOException exception) {
            return List.of();
        }
        return List.copyOf(found);
    }

    private AssetEntry assetEntry(AssetType assetType, Path root, Path path, String usage) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String withoutExtension = relative.substring(0, relative.lastIndexOf('.')).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
        String reference = switch (assetType) {
            case ASSET -> QuestEditorCustomIcons.PREFIX + withoutExtension;
            case MEDIA -> relative;
            case EMOJI -> withoutExtension;
        };
        int[] dimensions = dimensions(path);
        boolean used = switch (assetType) {
            case ASSET -> containsReference(usage, reference) || containsReference(usage, relative);
            case MEDIA -> containsReference(usage, relative);
            case EMOJI -> containsReference(usage, ":" + reference + ":")
                    || containsReference(usage, QuestEditorEmojis.iconId(reference));
        };
        return new AssetEntry(path, relative, reference, dimensions[0], dimensions[1], used);
    }

    private boolean containsReference(String content, String reference) {
        if (content == null || content.isEmpty() || reference == null || reference.isEmpty()) return false;
        int from = 0;
        while (from < content.length()) {
            int index = content.indexOf(reference, from);
            if (index < 0) return false;
            int end = index + reference.length();
            boolean leftBoundary = index == 0 || !isReferenceCharacter(content.charAt(index - 1));
            boolean rightBoundary = end == content.length() || !isReferenceCharacter(content.charAt(end));
            if (leftBoundary && rightBoundary) return true;
            from = index + 1;
        }
        return false;
    }

    private boolean isReferenceCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-' || value == '.' || value == '/';
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
            if (type == AssetType.EMOJI) fileName = fileName.toLowerCase(Locale.ROOT);
            final String uploadedFileName = fileName;
            Path target = root.resolve(uploadedFileName).normalize();
            if (!target.startsWith(root)) throw new IOException("Invalid asset path");
            if (!source.equals(target)) Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            clearAssetCaches();
            reloadEntries(() -> {
                selected = entries.stream().filter(entry -> entry.path.equals(target)).findFirst().orElse(null);
                status = uploadedFileName + " uploaded";
                statusColor = 0xFFA0FFA0;
            });
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

    private void renameSelected() {
        if (selected == null) return;
        AssetEntry entry = selected;
        String requested = TinyFileDialogs.tinyfd_inputBox(
                "Rename " + type.label.toLowerCase(Locale.ROOT), "New file name", entry.path.getFileName().toString());
        if (requested == null) return;
        requested = requested.trim();
        if (requested.isBlank() || requested.contains("/") || requested.contains("\\")) {
            status = "Enter a valid file name";
            statusColor = 0xFFFF7070;
            return;
        }
        String oldName = entry.path.getFileName().toString();
        int extensionIndex = oldName.lastIndexOf('.');
        String extension = extensionIndex >= 0 ? oldName.substring(extensionIndex).toLowerCase(Locale.ROOT) : "";
        if (requested.lastIndexOf('.') < 1) requested += extension;
        if (!type.accepts(requested)) {
            status = "Unsupported file extension";
            statusColor = 0xFFFF7070;
            return;
        }
        String sanitizedFileName = requested.replaceAll("[^A-Za-z0-9_.-]", "_");
        if (type == AssetType.EMOJI) sanitizedFileName = sanitizedFileName.toLowerCase(Locale.ROOT);
        final String fileName = sanitizedFileName;
        try {
            Path root = type.root().toAbsolutePath().normalize();
            Path source = entry.path.toAbsolutePath().normalize();
            Path target = source.resolveSibling(fileName).normalize();
            if (!source.startsWith(root) || !target.startsWith(root)) throw new IOException("Invalid asset path");
            if (Files.exists(target) && !source.equals(target)) {
                status = fileName + " already exists";
                statusColor = 0xFFFF7070;
                return;
            }
            if (!oldName.equals(fileName) && source.equals(target)) {
                Path temporary = source.resolveSibling(".boundless-rename-" + Long.toUnsignedString(System.nanoTime()) + extension);
                Files.move(source, temporary);
                Files.move(temporary, target);
            } else if (!source.equals(target)) {
                Files.move(source, target);
            }
            clearAssetCaches();
            reloadEntries(() -> {
                selected = entries.stream().filter(found -> found.path.equals(target)).findFirst().orElse(null);
                status = oldName + " renamed to " + fileName;
                statusColor = 0xFFA0FFA0;
            });
        } catch (IOException exception) {
            status = "Could not rename " + oldName;
            statusColor = 0xFFFF7070;
        }
    }

    private void clearAssetCaches() {
        QuestEditorCustomIcons.clear();
        QuestEditorEmojis.clearCache();
        QuestMediaRenderer.clear();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        graphics.fill(0, 0, width, height, 0xA0000000);
        QuestPanelBackgroundRenderer.render(graphics, PANEL_TEXTURE, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, panelY + 11, 0xFFFFFFFF);
        int listX = panelX + 8;
        int listY = panelY + 47;
        int listWidth = PANEL_WIDTH - 16;
        int columnWidth = (listWidth - COLUMN_GAP) / LIST_COLUMNS;
        graphics.fill(listX, listY, listX + listWidth, listY + LIST_HEIGHT, 0xFF111111);
        AssetEntry hovered = null;
        int visibleRows = LIST_HEIGHT / ROW_HEIGHT;
        for (int row = 0; row < visibleRows; row++) {
            for (int column = 0; column < LIST_COLUMNS; column++) {
                int index = (scroll + row) * LIST_COLUMNS + column;
                if (index >= entries.size()) break;
                AssetEntry entry = entries.get(index);
                int x = listX + column * (columnWidth + COLUMN_GAP);
                int y = listY + row * ROW_HEIGHT;
                boolean over = mouseX >= x && mouseX < x + columnWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
                boolean chosen = entry.equals(selected);
                int border = chosen ? 0xFFFFFFFF : 0xFF555555;
                graphics.fill(x + 1, y + 1, x + columnWidth - 1, y + ROW_HEIGHT - 1, border);
                graphics.fill(x + 2, y + 2, x + columnWidth - 2, y + ROW_HEIGHT - 2,
                        over ? 0xFF3A3A3A : 0xFF242424);
                renderPreview(graphics, entry, x + 5, y + 4);
                graphics.drawString(font, font.plainSubstrByWidth(entry.relative, columnWidth - 36), x + 31, y + 5,
                        0xFFFFFFFF, false);
                String details = entry.width + "×" + entry.height + "  " + (entry.used ? "Used" : "Unused");
                graphics.drawString(font, font.plainSubstrByWidth(details, columnWidth - 36), x + 31, y + 16,
                        entry.used ? 0xFF80FF80 : 0xFFAAAAAA, false);
                if (over) hovered = entry;
            }
        }
        if (!loading && entries.isEmpty()) graphics.drawCenteredString(font, "No " + type.label.toLowerCase(Locale.ROOT) + " found",
                width / 2, listY + 62, 0xFFAAAAAA);
        if (!status.isBlank()) graphics.drawCenteredString(font, status, width / 2, panelY + PANEL_HEIGHT - 48, statusColor);
        renameButton.active = selected != null;
        deleteButton.active = selected != null;
        deleteButton.setMessage(Component.literal("Delete"));
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
        graphics.pose().popPose();
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
        int listWidth = PANEL_WIDTH - 16;
        int columnWidth = (listWidth - COLUMN_GAP) / LIST_COLUMNS;
        if (mouseX < listX || mouseX >= listX + listWidth || mouseY < listY || mouseY >= listY + LIST_HEIGHT) return false;
        int row = (int) ((mouseY - listY) / ROW_HEIGHT);
        int column = Math.min(LIST_COLUMNS - 1, (int) ((mouseX - listX) / (columnWidth + COLUMN_GAP)));
        int columnX = listX + column * (columnWidth + COLUMN_GAP);
        if (mouseX >= columnX + columnWidth) return false;
        int index = (scroll + row) * LIST_COLUMNS + column;
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
        int rows = (entries.size() + LIST_COLUMNS - 1) / LIST_COLUMNS;
        return Math.max(0, rows - LIST_HEIGHT / ROW_HEIGHT);
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

    private final class AssetTabButton extends AbstractButton {
        private final AssetType assetType;

        private AssetTabButton(int x, int y, int width, AssetType assetType) {
            super(x, y, width, 20, Component.literal(assetType.label));
            this.assetType = assetType;
        }

        @Override
        public void onPress() {
            if (type != assetType) setType(assetType);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = type == assetType;
            ResourceLocation sprite = !selected
                    ? ResourceLocation.withDefaultNamespace("widget/button_disabled")
                    : ResourceLocation.withDefaultNamespace(isMouseOver(mouseX, mouseY)
                    ? "widget/button_highlighted" : "widget/button");
            graphics.blitSprite(sprite, getX(), getY(), width, height);
            int textWidth = font.width(getMessage());
            graphics.drawString(font, getMessage(), getX() + (width - textWidth) / 2, getY() + 6,
                    selected ? 0xFFFFFFFF : 0xFF808080, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
        }
    }
}
