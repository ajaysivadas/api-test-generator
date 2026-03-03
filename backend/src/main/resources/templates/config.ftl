package ${packageName};

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * API Configuration loader.
 * Auto-generated framework.
 */
public class ApiConfig {

    private static final Properties properties = new Properties();
    private static ApiConfig instance;

    private ApiConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public static synchronized ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
        }
        return instance;
    }

    public String getBaseUrl() {
        return properties.getProperty("base.url", "http://localhost:8080");
    }

    public String getApiVersion() {
        return properties.getProperty("api.version", "1.0");
    }

    public String getAuthType() {
        return properties.getProperty("auth.type", "none");
    }

    public String getAuthToken() {
        return properties.getProperty("auth.token", "");
    }

    public int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("connection.timeout", "30000"));
    }

    public int getReadTimeout() {
        return Integer.parseInt(properties.getProperty("read.timeout", "30000"));
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
