package MarketFeed.Api_Test.${serviceName}.${testGroup};

import MarketFeed.Api_Test.ApiExecutors.${serviceName}.${executorClassName};
import MarketFeed.Api_Test.Assertions.Assertions;
import MarketFeed.Api_Test.Base.TestData.HttpStatusCode;
<#if hasRequestPojo>
import MarketFeed.Api_Test.RequestPojo.${serviceName}.${requestPojoImport};
</#if>
<#if hasResponsePojo>
import MarketFeed.Api_Test.ResponsePojo.${serviceName}.${responsePojoImport};
</#if>
<#if hasPayloadBuilder>
import MarketFeed.Api_Test.Payloads.RequestPayload.${serviceName}.${payloadBuilderImport};
</#if>
import io.qameta.allure.*;
import io.restassured.response.ValidatableResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Epic("${epicName}")
@Feature("${featureName}")
@Story("${storyName}")
@Severity(SeverityLevel.CRITICAL)
public class ${className} {

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
<#elseif hasRequestPojo>
        ${responseVarName}Request = new ${requestPojoClass}();
        ${responseVarName}ValidatableResponse = ${executorClassName}.${executorMethodName}(${responseVarName}Request<#if hasPathParam>, "testId"</#if>);
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
