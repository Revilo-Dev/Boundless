package net.revilodev.boundless.util;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class ResourceLocations {
    private ResourceLocations() {
    }

    public static ResourceLocation id(String namespace, String path) {
        return Objects.requireNonNull(ResourceLocation.tryBuild(namespace, path), namespace + ":" + path);
    }

    public static ResourceLocation parse(String id) {
        return Objects.requireNonNull(ResourceLocation.tryParse(id), id);
    }
}
