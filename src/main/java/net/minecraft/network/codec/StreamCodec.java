package net.minecraft.network.codec;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface StreamCodec<B, V> {
    void encode(B buffer, V value);

    V decode(B buffer);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public void encode(B buffer, V value) {
                encoder.accept(buffer, value);
            }

            @Override
            public V decode(B buffer) {
                return decoder.apply(buffer);
            }
        };
    }
}
