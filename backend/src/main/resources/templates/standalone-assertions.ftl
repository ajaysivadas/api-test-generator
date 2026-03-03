package ${packageName};

import io.qameta.allure.Allure;
import io.restassured.response.ValidatableResponse;

public class Assertions {

    public static void assertStatusCode(ValidatableResponse response, HttpStatusCode expectedStatus, String message) {
        int actualStatus = response.extract().statusCode();
        if (actualStatus != expectedStatus.getCode()) {
            Allure.addAttachment("Expected Status", String.valueOf(expectedStatus.getCode()));
            Allure.addAttachment("Actual Status", String.valueOf(actualStatus));
            throw new AssertionError(message + " - Expected: " + expectedStatus.getCode() + ", Actual: " + actualStatus);
        }
    }

    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            Allure.addAttachment("Assertion Failed", message + " - Object was null");
            throw new AssertionError(message + " - Expected non-null value but got null");
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            Allure.addAttachment("Expected", String.valueOf(expected));
            Allure.addAttachment("Actual", String.valueOf(actual));
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    public static void assertResponseTime(ValidatableResponse response, long maxMillis, String message) {
        long actualTime = response.extract().time();
        if (actualTime > maxMillis) {
            Allure.addAttachment("Max Allowed (ms)", String.valueOf(maxMillis));
            Allure.addAttachment("Actual Time (ms)", String.valueOf(actualTime));
            throw new AssertionError(message + " - Expected response within " + maxMillis + "ms but took " + actualTime + "ms");
        }
    }
}
