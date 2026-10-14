package bff.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JsonText {

    private JsonText() {
    }

    public static String read(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        if (bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String string(String json, String field) {
        String raw = raw(json, field);
        if (raw == null || "null".equals(raw)) {
            return null;
        }
        if (raw.startsWith("\"")) {
            return unquote(raw);
        }
        return raw;
    }

    public static Long longOrNull(String json, String field) {
        String raw = raw(json, field);
        if (raw == null || "null".equals(raw) || raw.isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Поле " + field + " не является числом", e);
        }
    }

    public static boolean bool(String json, String field) {
        return "true".equals(raw(json, field));
    }

    public static String raw(String json, String field) {
        if (json == null || field == null) {
            return null;
        }
        int n = json.length();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < n; i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                if (depth == 1 && isKeyPosition(json, i)) {
                    int end = endOfString(json, i);
                    String key = unquote(json.substring(i, end));
                    int j = skipWs(json, end);
                    if (j < n && json.charAt(j) == ':' && field.equals(key)) {
                        return sliceValue(json, skipWs(json, j + 1));
                    }
                    i = end - 1;
                    continue;
                }
                inString = true;
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
        }
        return null;
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }

    public static String number(Long value) {
        return value == null ? "null" : Long.toString(value);
    }

    private static String unquote(String quoted) {
        if (quoted.length() < 2 || quoted.charAt(0) != '"') {
            return quoted;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 1; i < quoted.length() - 1; i++) {
            char c = quoted.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i + 1 >= quoted.length() - 1) {
                break;
            }
            char next = quoted.charAt(++i);
            switch (next) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 >= quoted.length() - 1) {
                        throw new IllegalStateException("Неожиданный формат JSON");
                    }
                    String hex = quoted.substring(i + 1, i + 5);
                    out.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                }
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private static boolean isKeyPosition(String json, int quoteIndex) {
        for (int i = quoteIndex - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == '{' || c == ',';
        }
        return false;
    }

    private static int endOfString(String json, int quoteStart) {
        boolean escape = false;
        for (int i = quoteStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
        }
        throw new IllegalStateException("Неожиданный формат JSON");
    }

    private static String sliceValue(String json, int start) {
        if (start >= json.length()) {
            throw new IllegalStateException("Неожиданный формат JSON");
        }
        char first = json.charAt(start);
        if (first == '"') {
            return json.substring(start, endOfString(json, start));
        }
        if (first == '{' || first == '[') {
            int depth = 0;
            boolean inString = false;
            boolean escape = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (inString) {
                    if (escape) {
                        escape = false;
                    } else if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (c == '"') {
                    inString = true;
                    continue;
                }
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start, i + 1);
                    }
                }
            }
            throw new IllegalStateException("Неожиданный формат JSON");
        }
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                break;
            }
            i++;
        }
        return json.substring(start, i);
    }

    private static int skipWs(String json, int index) {
        int i = index;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        return i;
    }
}
