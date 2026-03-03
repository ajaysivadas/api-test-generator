package MarketFeed.Api_Test.Payloads.RequestPayload.${serviceName};

import MarketFeed.Api_Test.RequestPojo.${serviceName}.${requestPojoClass};
<#if imports?has_content>
<#list imports as import>
import ${import};
</#list>
</#if>

public class ${className} {

    public static ${requestPojoClass} createPayload() {
        ${requestPojoClass} request = new ${requestPojoClass}();
<#list fields as field>
<#if field.javaType == "String">
        request.set${field.name?cap_first}("test${field.name?cap_first}");
<#elseif field.javaType == "Integer" || field.javaType == "int">
        request.set${field.name?cap_first}(1);
<#elseif field.javaType == "Long" || field.javaType == "long">
        request.set${field.name?cap_first}(1L);
<#elseif field.javaType == "Double" || field.javaType == "double">
        request.set${field.name?cap_first}(1.0);
<#elseif field.javaType == "Float" || field.javaType == "float">
        request.set${field.name?cap_first}(1.0f);
<#elseif field.javaType == "Boolean" || field.javaType == "boolean">
        request.set${field.name?cap_first}(true);
<#else>
        // TODO: Set ${field.name} value
</#if>
</#list>
        return request;
    }

    public static ${requestPojoClass} createPayload(${className}Field field) {
        ${requestPojoClass} request = createPayload();
        switch (field) {
<#list fields as field>
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
</#list>
        }
        return request;
    }

    public enum ${className}Field {
<#list fields as field>
        ${field.enumName}<#if field?has_next>,<#else>;</#if>
</#list>
    }
}
