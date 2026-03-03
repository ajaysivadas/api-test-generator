package ${packageName};

import ${basePackage}.models.${requestPojoClass};

public class ${className} {

    public static ${requestPojoClass} createPayload() {
        return ${requestPojoClass}.builder()
<#list fields as field>
<#if field.javaType == "String">
                .${field.name}("test${field.name?cap_first}")
<#elseif field.javaType == "Integer" || field.javaType == "int">
                .${field.name}(1)
<#elseif field.javaType == "Long" || field.javaType == "long">
                .${field.name}(1L)
<#elseif field.javaType == "Double" || field.javaType == "double">
                .${field.name}(1.0)
<#elseif field.javaType == "Float" || field.javaType == "float">
                .${field.name}(1.0f)
<#elseif field.javaType == "Boolean" || field.javaType == "boolean">
                .${field.name}(true)
<#else>
                // TODO: Set ${field.name} value
</#if>
</#list>
                .build();
    }

    public static ${requestPojoClass} createPayload(${className}Field field) {
        ${requestPojoClass} request = createPayload();
        switch (field) {
<#list fields as field>
<#if field.javaType == "String">
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
<#elseif field.javaType == "Integer" || field.javaType == "int">
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
<#elseif field.javaType == "Long" || field.javaType == "long">
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
<#elseif field.javaType == "Double" || field.javaType == "double">
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
<#elseif field.javaType == "Float" || field.javaType == "float">
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
<#elseif field.javaType == "Boolean" || field.javaType == "boolean">
            case ${field.enumName}:
                request.set${field.name?cap_first}(null);
                break;
<#else>
            case ${field.enumName}:
                // TODO: Null out ${field.name}
                break;
</#if>
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
