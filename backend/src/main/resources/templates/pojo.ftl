package ${packageName};

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
<#list imports as import>
import ${import};
</#list>

/**
 * ${description}
 * Auto-generated POJO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ${className} {

<#list fields as field>
    <#if field.description?has_content>
    /** ${field.description} */
    </#if>
    @JsonProperty("${field.jsonName}")
    private ${field.javaType} ${field.name};

</#list>
}
