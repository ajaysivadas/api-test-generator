<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd" >
<suite name="${suiteName}" verbose="1" parallel="classes" thread-count="3">
    <test name="API Tests">
        <classes>
<#list testClasses as testClass>
            <class name="${packageName}.${testClass}"/>
</#list>
        </classes>
    </test>
</suite>
