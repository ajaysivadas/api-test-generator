package MarketFeed.Api_Test.ApiExecutors.${serviceName};

import MarketFeed.Api_Test.Base.HttpRequests.HttpRequest;
import MarketFeed.Api_Test.Base.URI.BaseUri;
import MarketFeed.Api_Test.Base.URI.EndPoint;
import MarketFeed.Api_Test.Utils.Managers.JsonManager;
import io.restassured.response.ValidatableResponse;

import java.util.HashMap;

public class ${className} {

<#list methods as method>
<#if method.httpMethod == "POST" || method.httpMethod == "PUT" || method.httpMethod == "PATCH">
    public static synchronized ValidatableResponse ${method.methodName}(Object payload<#if method.hasPathParam>, String ${method.pathParamName}</#if>) {
        HashMap<String, String> headers = new HashMap<>();

        Object cleanedPayload = JsonManager.removeNullValuesAsMap(payload);

<#if method.hasPathParam>
        return HttpRequest.${method.httpMethod?lower_case}Request(BaseUri.${baseUriEnumName}, cleanedPayload, headers, EndPoint.${method.endpointEnumName}.addPathParam(${method.pathParamName}));
<#else>
        return HttpRequest.${method.httpMethod?lower_case}Request(BaseUri.${baseUriEnumName}, cleanedPayload, headers, EndPoint.${method.endpointEnumName});
</#if>
    }

<#elseif method.httpMethod == "DELETE">
    public static synchronized ValidatableResponse ${method.methodName}(<#if method.hasPathParam>String ${method.pathParamName}</#if>) {
        HashMap<String, String> headers = new HashMap<>();

<#if method.hasPathParam>
        return HttpRequest.deleteRequest(BaseUri.${baseUriEnumName}, headers, EndPoint.${method.endpointEnumName}.addPathParam(${method.pathParamName}));
<#else>
        return HttpRequest.deleteRequest(BaseUri.${baseUriEnumName}, headers, EndPoint.${method.endpointEnumName});
</#if>
    }

<#else>
    public static synchronized ValidatableResponse ${method.methodName}(<#if method.hasPathParam>String ${method.pathParamName}</#if>) {
        HashMap<String, String> headers = new HashMap<>();

<#if method.hasPathParam>
        return HttpRequest.getRequest(BaseUri.${baseUriEnumName}, headers, EndPoint.${method.endpointEnumName}.addPathParam(${method.pathParamName}));
<#else>
        return HttpRequest.getRequest(BaseUri.${baseUriEnumName}, headers, EndPoint.${method.endpointEnumName});
</#if>
    }

</#if>
</#list>
}
