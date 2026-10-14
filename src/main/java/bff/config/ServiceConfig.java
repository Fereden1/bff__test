package bff.config;

import java.io.InputStream;
import java.util.Properties;

public final class ServiceConfig {

    private static final Properties P = new Properties();

    static {
        try (InputStream in = ServiceConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                P.load(in);
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ServiceConfig() {
    }

    public static String user() {
        return P.getProperty("user.service.url", "http://localhost:8081");
    }

    public static String moderation() {
        return P.getProperty("moderation.service.url", "http://localhost:8082");
    }

    public static String scooter() {
        return P.getProperty("scooter.service.url", "http://localhost:8083");
    }

    public static String ride() {
        return P.getProperty("ride.service.url", "http://localhost:8084");
    }

    public static String document() {
        return P.getProperty("document.service.url", "http://localhost:8085");
    }
}
