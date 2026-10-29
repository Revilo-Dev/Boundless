package net.revilodev.boundless.client.editor;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class FtbSnbtParser {
    private static final Pattern NUMBER = Pattern.compile(
            "[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?[bBsSlLfFdD]?");

    private final String source;
    private int position;

    private FtbSnbtParser(String source) {
        this.source = source == null ? "" : source;
    }

    static CompoundTag parse(String source) {
        FtbSnbtParser parser = new FtbSnbtParser(source);
        if (parser.next() != '{') throw parser.error("Expected '{'");
        CompoundTag result = parser.readCompound();
        parser.skipSeparators();
        if (parser.position < parser.source.length()) throw parser.error("Unexpected trailing content");
        return result;
    }

    private CompoundTag readCompound() {
        CompoundTag result = new CompoundTag();
        while (true) {
            skipSeparators();
            if (peek() == '}') {
                position++;
                return result;
            }
            String key = readKey();
            skipWhitespaceAndComments();
            char separator = nextRaw();
            if (separator != ':' && separator != '=') throw error("Expected ':' after key " + key);
            result.put(key, readValue());
        }
    }

    private String readKey() {
        char first = peek();
        if (first == '"' || first == '\'') {
            position++;
            return readQuoted(first);
        }
        String key = readWord();
        if (key.isBlank()) throw error("Expected a key");
        return key;
    }

    private Tag readValue() {
        skipWhitespaceAndComments();
        char first = nextRaw();
        if (first == '{') return readCompound();
        if (first == '[') return readListOrArray();
        if (first == '"' || first == '\'') return StringTag.valueOf(readQuoted(first));
        position--;
        String word = readWord();
        if (word.isBlank()) throw error("Expected a value");
        if (word.equalsIgnoreCase("true")) return ByteTag.ONE;
        if (word.equalsIgnoreCase("false")) return ByteTag.ZERO;
        if (word.equalsIgnoreCase("Infinity") || word.equalsIgnoreCase("+Infinity")) {
            return DoubleTag.valueOf(Double.POSITIVE_INFINITY);
        }
        if (word.equalsIgnoreCase("-Infinity")) return DoubleTag.valueOf(Double.NEGATIVE_INFINITY);
        if (word.equalsIgnoreCase("NaN")) return DoubleTag.valueOf(Double.NaN);
        Tag number = number(word);
        return number == null ? StringTag.valueOf(word) : number;
    }

    private Tag readListOrArray() {
        int contentStart = position;
        skipWhitespaceAndComments();
        if (position + 1 < source.length() && source.charAt(position + 1) == ';') {
            char arrayType = Character.toUpperCase(source.charAt(position));
            if (arrayType == 'B' || arrayType == 'I' || arrayType == 'L') {
                position += 2;
                return readArray(arrayType);
            }
        }
        position = contentStart;
        ListTag result = new ListTag();
        while (true) {
            skipSeparators();
            if (peek() == ']') {
                position++;
                return result;
            }
            Tag value = readValue();
            if (!result.isEmpty() && result.getElementType() != value.getId()) {
                throw error("Lists cannot mix value types");
            }
            result.add(value);
        }
    }

    private Tag readArray(char type) {
        List<Number> values = new ArrayList<>();
        while (true) {
            skipSeparators();
            if (peek() == ']') {
                position++;
                break;
            }
            Tag value = readValue();
            if (!(value instanceof NumericTag numeric)) throw error("Array values must be numeric");
            values.add(numeric.getAsNumber());
        }
        return switch (type) {
            case 'I' -> new IntArrayTag(values.stream().mapToInt(Number::intValue).toArray());
            case 'L' -> new LongArrayTag(values.stream().mapToLong(Number::longValue).toArray());
            default -> {
                byte[] bytes = new byte[values.size()];
                for (int index = 0; index < bytes.length; index++) bytes[index] = values.get(index).byteValue();
                yield new ByteArrayTag(bytes);
            }
        };
    }

    private String readQuoted(char quote) {
        StringBuilder result = new StringBuilder();
        while (position < source.length()) {
            char current = source.charAt(position++);
            if (current == quote) return result.toString();
            if (current != '\\') {
                result.append(current);
                continue;
            }
            if (position >= source.length()) throw error("Unterminated escape sequence");
            char escaped = source.charAt(position++);
            switch (escaped) {
                case '"', '\'', '\\', '/' -> result.append(escaped);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> result.append(readUnicodeEscape());
                default -> result.append(escaped);
            }
        }
        throw error("Unterminated string");
    }

    private char readUnicodeEscape() {
        if (position + 4 > source.length()) throw error("Invalid Unicode escape");
        String digits = source.substring(position, position + 4);
        position += 4;
        try {
            return (char) Integer.parseInt(digits, 16);
        } catch (NumberFormatException exception) {
            throw error("Invalid Unicode escape");
        }
    }

    private String readWord() {
        skipWhitespaceAndComments();
        int start = position;
        while (position < source.length()) {
            char current = source.charAt(position);
            if (Character.isWhitespace(current) || current == ',' || current == ':' || current == '='
                    || current == '{' || current == '}' || current == '[' || current == ']') break;
            position++;
        }
        return source.substring(start, position);
    }

    private char peek() {
        skipWhitespaceAndComments();
        if (position >= source.length()) throw error("Unexpected end of input");
        return source.charAt(position);
    }

    private char next() {
        skipWhitespaceAndComments();
        return nextRaw();
    }

    private char nextRaw() {
        if (position >= source.length()) throw error("Unexpected end of input");
        return source.charAt(position++);
    }

    private void skipSeparators() {
        while (true) {
            skipWhitespaceAndComments();
            if (position < source.length() && source.charAt(position) == ',') {
                position++;
                continue;
            }
            return;
        }
    }

    private void skipWhitespaceAndComments() {
        while (position < source.length()) {
            char current = source.charAt(position);
            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }
            if (current == '#') {
                skipLine();
                continue;
            }
            if (current == '/' && position + 1 < source.length()) {
                char next = source.charAt(position + 1);
                if (next == '/') {
                    position += 2;
                    skipLine();
                    continue;
                }
                if (next == '*') {
                    position += 2;
                    while (position + 1 < source.length()
                            && !(source.charAt(position) == '*' && source.charAt(position + 1) == '/')) position++;
                    position = Math.min(source.length(), position + 2);
                    continue;
                }
            }
            return;
        }
    }

    private void skipLine() {
        while (position < source.length() && source.charAt(position) != '\n' && source.charAt(position) != '\r') {
            position++;
        }
    }

    private static Tag number(String word) {
        if (!NUMBER.matcher(word).matches()) return null;
        char suffix = Character.toLowerCase(word.charAt(word.length() - 1));
        boolean hasSuffix = suffix == 'b' || suffix == 's' || suffix == 'l' || suffix == 'f' || suffix == 'd';
        String number = hasSuffix ? word.substring(0, word.length() - 1) : word;
        try {
            return switch (suffix) {
                case 'b' -> ByteTag.valueOf(Byte.parseByte(number));
                case 's' -> ShortTag.valueOf(Short.parseShort(number));
                case 'l' -> LongTag.valueOf(Long.parseLong(number));
                case 'f' -> FloatTag.valueOf(Float.parseFloat(number));
                case 'd' -> DoubleTag.valueOf(Double.parseDouble(number));
                default -> {
                    if (number.contains(".") || number.contains("e") || number.contains("E")) {
                        yield DoubleTag.valueOf(Double.parseDouble(number));
                    }
                    long value = Long.parseLong(number);
                    yield value == (int) value ? IntTag.valueOf((int) value) : LongTag.valueOf(value);
                }
            };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private IllegalArgumentException error(String message) {
        int line = 1;
        for (int index = 0; index < Math.min(position, source.length()); index++) {
            if (source.charAt(index) == '\n') line++;
        }
        return new IllegalArgumentException(message + " at line " + line + ", position " + position);
    }
}
