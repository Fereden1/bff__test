package bff.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

public final class JsonUtil {

    private static final ObjectMapper M = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {
    }

    public static String toJson(Object v) throws IOException {
        return M.writeValueAsString(v);
    }

    public static void write(Writer w, Object v) throws IOException {
        M.writeValue(w, v);
    }

    public static <T> T from(InputStream in, Class<T> type) throws IOException {
        return M.readValue(in, type);
    }

    public static <T> T from(String json, Class<T> type) throws IOException {
        return M.readValue(json, type);
    }
}
