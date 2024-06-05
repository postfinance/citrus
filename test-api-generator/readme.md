# TAT-1291

## Possible solution

```java

@CitrusTest
public void getPetById() {
    // native
    when(openapi(petstoreSpec)
            .client(httpClient)
            .withParameter("petId", "1001") // TODO: to be implemented
            .send("getPetById")
            .fork(true));
    // generated - TODO: to be implemented
    when(openapiPetstore()
            .client()
            .getPetById()
            .withPetId("1001")
            .send()
            .fork(true));
}
```

```xml

<spring:beans
        xmlns="http://citrusframework.org/schema/xml/testcase"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:spring="http://www.springframework.org/schema/beans"
        xmlns:petstore="http://www.citrusframework.org/citrus-test-schema/petstore-api"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd
    http://citrusframework.org/schema/xml/testcase
    http://citrusframework.org/schema/xml/testcase/citrus-testcase.xsd
    http://www.citrusframework.org/citrus-test-schema/petstore-api
    http://www.citrusframework.org/citrus-test-schema/petstore-api/petstore-api.xsd"
>
    <testcase name="">
        <!-- native -->
        <variables>
            <variable name="petstoreSpec" value="classpath:org/citrusframework/openapi/petstore/petstore-v3.json"/>
            <variable name="petId" value="1001"/>
        </variables>
        <actions>
            <openapi specification="${petstoreSpec}" client="httpClient">
                <send-request operation="getPetById"/>
            </openapi>

            <openapi specification="${petstoreSpec}" client="httpClient">
                <receive-response operation="getPetById" status="200">
                    <validate>
                        <json-path expression="$.FooMessage.foo" value="newValue"/>
                    </validate>
                </receive-response>
            </openapi>

            <!-- current generated -->
            <petstore:getPetByIdRequest petId="1234">
                <petstore:response status="201">
                    <petstore:json-path expression="$.FooMessage.foo" value="newValue"/>
                </petstore:response>
            </petstore:getPetByIdRequest>

            <!-- generated, adapted to the citrus way -->
            <!-- TODO: to be implemented -->
            <petstore:getPetByIdRequest petId="1234">
                <petstore:send-request/>
            </petstore:getPetByIdRequest>
            <petstore:getPetByIdRequest petId="1234">
                <petstore:receive-response status="201">
                    <validate>
                        <json-path expression="$.FooMessage.foo" value="newValue"/>
                    </validate>
                </petstore:receive-response>
            </petstore:getPetByIdRequest>
        </actions>
    </testcase>
</spring:beans>
```
