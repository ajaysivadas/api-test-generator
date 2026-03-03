<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "Https://testng.org/testng-1.0.dtd">

<suite name="${serviceName}">

<#list testGroups as group>
    <!-- ${group.name} Tests -->
    <test name="${group.name} Tests" parallel="classes" thread-count="${group.threadCount}">
        <classes>
<#list group.testClasses as testClass>
            <class name="MarketFeed.Api_Test.${serviceName}.${group.groupName}.${testClass}"/>
</#list>
        </classes>
    </test>

</#list>
</suite>
