package MarketFeed.Api_Test.RequestPojo.${serviceName}<#if subPackage?has_content>.${subPackage}</#if>;

<#if imports?has_content>
<#list imports as import>
import ${import};
</#list>

</#if>
public class ${className} {

<#list fields as field>
    private ${field.javaType} ${field.name};
</#list>

<#list fields as field>
    public ${field.javaType} get${field.name?cap_first}() {
        return ${field.name};
    }

    public void set${field.name?cap_first}(${field.javaType} ${field.name}) {
        this.${field.name} = ${field.name};
    }

</#list>
}
