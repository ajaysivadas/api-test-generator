package ${packageName};

import ${basePackage}.base.BaseTest;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.response.ValidatableResponse;

import static io.restassured.RestAssured.given;

public class ${className} {

<#list methods as method>
<#if method.httpMethod == "POST" || method.httpMethod == "PUT" || method.httpMethod == "PATCH">
    public static synchronized ValidatableResponse ${method.methodName}(Object payload<#if method.hasPathParam>, String ${method.pathParamName}</#if>) {
        return given()
                .spec(BaseTest.getRequestSpec())
                .filter(new AllureRestAssured())
                .contentType("application/json")
                .body(payload)
        <#if method.hasPathParam>
                .pathParam("${method.pathParamName}", ${method.pathParamName})
        </#if>
            .when()
                .${method.httpMethod?lower_case}("${method.path}")
            .then();
    }

<#elseif method.httpMethod == "DELETE">
    public static synchronized ValidatableResponse ${method.methodName}(<#if method.hasPathParam>String ${method.pathParamName}</#if>) {
        return given()
                .spec(BaseTest.getRequestSpec())
                .filter(new AllureRestAssured())
        <#if method.hasPathParam>
                .pathParam("${method.pathParamName}", ${method.pathParamName})
        </#if>
            .when()
                .delete("${method.path}")
            .then();
    }

<#else>
    public static synchronized ValidatableResponse ${method.methodName}(<#if method.hasPathParam>String ${method.pathParamName}</#if>) {
        return given()
                .spec(BaseTest.getRequestSpec())
                .filter(new AllureRestAssured())
        <#if method.hasPathParam>
                .pathParam("${method.pathParamName}", ${method.pathParamName})
        </#if>
            .when()
                .get("${method.path}")
            .then();
    }

</#if>
</#list>
}
