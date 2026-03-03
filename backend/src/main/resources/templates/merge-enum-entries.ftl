====================================
  MERGE INSTRUCTIONS
====================================

Add the following entries to your existing enum files:

------------------------------------
  BaseUri.java
------------------------------------
Add this entry to: src/main/java/MarketFeed/Api_Test/Base/URI/BaseUri.java

    //${serviceName}
    ${baseUriEnumName}("${baseUriKey}"),

------------------------------------
  EndPoint.java
------------------------------------
Add these entries to: src/main/java/MarketFeed/Api_Test/Base/URI/EndPoint.java

    //${serviceName}
<#list endpointEntries as entry>
    ${entry.enumName}("${entry.path}"),
</#list>

====================================
