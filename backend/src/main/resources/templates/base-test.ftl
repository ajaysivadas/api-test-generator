package ${packageName};

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.qameta.allure.restassured.AllureRestAssured;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Base test class with REST Assured configuration.
 * Auto-generated framework.
 */
public class BaseTest {

    private static RequestSpecification requestSpec;
    protected static Properties config = new Properties();

    @BeforeSuite
    public void loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                config.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }

        String baseUrl = config.getProperty("base.url", "http://localhost:8080");

        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured());

        // Configure authentication
        String authType = config.getProperty("auth.type", "none");
        switch (authType) {
            case "bearer":
                String token = config.getProperty("auth.token", "");
                builder.addHeader("Authorization", "Bearer " + token);
                break;
            case "basic":
                String username = config.getProperty("auth.username", "");
                String password = config.getProperty("auth.password", "");
                builder.addHeader("Authorization", "Basic " +
                        java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
                break;
            default:
                break;
        }

        requestSpec = builder.build();
    }

    public static RequestSpecification getRequestSpec() {
        return requestSpec;
    }
}
