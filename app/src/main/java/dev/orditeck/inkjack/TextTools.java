package dev.orditeck.inkjack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure text operations, independent of Android and OCR engines. */
public final class TextTools {
    private TextTools() {}
    public static final int MAX_COLLECTION_CHARS = 200_000;
    public enum Kind { EMAIL, URL, PHONE, HANDLE, HASHTAG }
    private static final Pattern[] PATTERNS = {
        Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:https?://|www\\.)[^\\s<>]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?<![\\w])\\+?\\d[\\d ().-]{5,}\\d(?![\\w])"),
        Pattern.compile("(?<![\\w@.])@[\\p{L}\\p{N}_]+"),
        Pattern.compile("(?<![\\w#])#[\\p{L}\\p{N}_]+")
    };
    public static List<String> entities(String text, Kind kind) {
        Set<String> found = new LinkedHashSet<>();
        var matcher = PATTERNS[kind.ordinal()].matcher(text);
        while (matcher.find()) {
            String value = matcher.group().replaceAll("[.,;:!?]+$", "");
            if (kind == Kind.PHONE && value.replaceAll("\\D", "").length() < 7) continue;
            found.add(value);
        }
        return new ArrayList<>(found);
    }
    public static String join(List<String> chunks) {
        return String.join("\n", chunks).strip();
    }
    public static final class Collection {
        private final LinkedHashSet<String> chunks = new LinkedHashSet<>();
        private int length;
        public synchronized boolean add(String text) {
            String value = text.strip();
            if (value.isEmpty() || chunks.contains(value)) return false;
            if (length + value.length() + 2 > MAX_COLLECTION_CHARS) throw new IllegalStateException("Collection is full. Copy it, then clear it to keep collecting.");
            chunks.add(value);
            length += value.length() + 2;
            return true;
        }
        public synchronized String text() { return String.join("\n\n", chunks); }
        public synchronized int size() { return chunks.size(); }
        public synchronized void clear() { chunks.clear(); length = 0; }
    }
}
