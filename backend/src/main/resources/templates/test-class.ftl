package ${packageName};

import ${basePackage}.assertions.Assertions;
import ${basePackage}.assertions.HttpStatusCode;
import ${basePackage}.base.BaseTest;
import ${basePackage}.executors.${executorClassName};
<#if hasRequestPojo>
import ${basePackage}.models.${requestPojoClass};
</#if>
<#if hasResponsePojo>
import ${basePackage}.models.${responsePojoClass};
</#if>
<#if hasPayloadBuilder>
import ${basePackage}.payloads.${payloadBuilderClass};
</#if>
import io.qameta.allure.*;
import io.restassured.response.ValidatableResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Epic("${epicName}")
@Feature("${featureName}")
@Story("${storyName}")
@Severity(SeverityLevel.CRITICAL)
public class ${className} extends BaseTest {

    private static ValidatableResponse ${responseVarName}ValidatableResponse;
<#if hasResponsePojo>
    private static ${responsePojoClass} ${responseVarName}Response;
</#if>
<#if hasRequestPojo>
    private static ${requestPojoClass} ${responseVarName}Request;
</#if>

    private void ${apiCallMethodName}() {
<#if hasRequestPojo && hasPayloadBuilder>
        ${responseVarName}Request = ${payloadBuilderClass}.createPayload();
        ${responseVarName}ValidatableResponse = ${executorClassName}.${executorMethodName}(${responseVarName}Request<#if hasPathParam>, "testId"</#if>);
<#elseif hasRequestBody>
        ${responseVarName}ValidatableResponse = ${executorClassName}.${executorMethodName}(new java.util.HashMap<>()<#if hasPathParam>, "testId"</#if>);
<#elseif hasPathParam>
        ${responseVarName}ValidatableResponse = ${executorClassName}.${executorMethodName}("testId");
<#else>
        ${responseVarName}ValidatableResponse = ${executorClassName}.${executorMethodName}();
</#if>
<#if hasResponsePojo>
        ${responseVarName}Response = ${responseVarName}ValidatableResponse.extract().response().as(${responsePojoClass}.class);
</#if>
    }

    @BeforeClass
    public void beforeTest() {
        ${apiCallMethodName}();
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify status code should be ${expectedStatus}")
    public void verify_Status_Code_Should_Be_${expectedStatus}() {
        Assertions.assertStatusCode(${responseVarName}ValidatableResponse, HttpStatusCode.${httpStatusCodeEnum},
                "Status code should be ${expectedStatus}");
    }

<#if hasResponsePojo>
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify response body is not null")
    public void verify_Response_Body_Is_Not_Null() {
        Assertions.assertNotNull(${responseVarName}Response,
                "Response body should not be null");
    }

</#if>
}
