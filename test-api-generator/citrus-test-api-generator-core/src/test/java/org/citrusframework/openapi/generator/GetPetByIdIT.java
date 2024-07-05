package org.citrusframework.openapi.generator;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpClientBuilder;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.citrusframework.openapi.generator.rest.petstore.spring.PetStoreBeanConfiguration;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.citrusframework.message.MessageType.JSON;
import static org.citrusframework.openapi.generator.rest.petstore.request.PetApi.openapiPetstore;
import static org.citrusframework.validation.PathExpressionValidationContext.Builder.pathExpression;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@CitrusSpringSupport
@ContextConfiguration(classes = {PetStoreBeanConfiguration.class, CitrusSpringConfig.class})
class GetPetByIdIT {

    private final int port = SocketUtils.findAvailableTcpPort(8080);

//    private final MessageQueue inboundQueue = new DefaultMessageQueue("inboundQueue");
//    private final EndpointAdapter endpointAdapter = new DirectEndpointAdapter(direct()
//            .synchronous()
//            .timeout(5000L)
//            .queue(inboundQueue)
//            .build());

    @BindToRegistry
    private final HttpClient httpClient = new HttpClientBuilder()
            .requestUrl("http://localhost:%d".formatted(port))
            .build();

    // TODO TAT-1291 the GetPetByIdIT class gets instantiated twice, but the second time
    //  the httpServer, respectively the "endpoint", is not initialized correctly...
    //  why is the Test Class instantiated twice (once per testmethod?) anyway?!
    @BindToRegistry
    private final HttpServer httpServer = new HttpServerBuilder()
            .port(port)
            // .endpointAdapter(endpointAdapter)
            .timeout(5000L)
            .autoStart(true)
            .defaultStatus(NO_CONTENT)
            .build();

    @BeforeEach
    public void beforeTest() {
    }

    @Test
    @CitrusTest
    void testByJsonPath(@CitrusResource TestCaseRunner runner) {

        runner.$(
                openapiPetstore(httpClient)
                        .getPetById()
                        .send(request -> request
                                .withPetId(2002L)
                                .withCorrelationIds("5599")
                                .withVerbose(true)
                        )
        );

        respondPet(runner);

        runner.$(
                openapiPetstore(httpClient)
                        .getPetById()
                        .receive()
                        .message()
                        .validate(
                                pathExpression()
                                        .jsonPath("$.name", "Snoopy")
                                        .jsonPath("$.id", 2002)
                        )
        );
    }

    @Test
    @CitrusTest
    void testJsonFileBody(@CitrusResource TestCaseRunner runner) {

        runner.$(
                openapiPetstore(httpClient)
                        .getPetById()
                        .send(request -> request
                                // TODO
                                // .withBodyFile("org/citrusframework/openapi/generator/GeneratedApiTest/payloads/getPetByIdControlMessage1.json")
                                .withPetId(2002L)
                                .withCorrelationIds("5599")
                                .withVerbose(true)
                        )
        );

        respondPet(runner);

        runner.$(
                openapiPetstore(httpClient)
                        .getPetById()
                        .receive()
                        .message()
                        .validate(
                                pathExpression()
                                        .jsonPath("$.name", "Snoopy")
                                        .jsonPath("$.id", 2002)
                        )
        );
    }

    private void respondPet(TestCaseRunner runner) {
        runner.$(http().server(httpServer)
                .receive()
                .get("/pet/2002")
                .message()
                .queryParam("verbose", "true")
                .header("correlationIds", "5599")
                .accept("@contains('application/json')@"));

        runner.$(http().server(httpServer)
                .send()
                .response(OK)
                .message()
                .body("""
                        {
                          "id": ${petId},
                          "name": "Snoopy",
                          "tags": [],
                          "photoUrls": [],
                          "category": {
                            "name": "a name",
                            "id": 112233
                          },
                          "status": "available"
                        }
                        """)
                .contentType("application/json").type(JSON));
    }

   /* @Test
    @CitrusTest
    void testValidationFailureByResource(@CitrusResource TestCaseRunner runner) {

        // Given
        getPetByIdRequest.setPetId("1234");

        // Then
        getPetByIdRequest.setResponseStatus(HttpStatus.OK.value());
        getPetByIdRequest.setResponseReasonPhrase(HttpStatus.OK.getReasonPhrase());
        // Assert body by resource
        getPetByIdRequest.setResource(
            "org/citrusframework/openapi/generator/GeneratedApiTest/payloads/getPetByIdControlMessage2.json");

        // When
        runner.$(assertException()
            .exception(org.citrusframework.exceptions.CitrusRuntimeException.class)
            .message("Values not equal for entry: '$['name']', expected 'Garfield' but was 'Snoopy'")
            .when(
                getPetByIdRequest
            )
        );
    }

    @Test
    @CitrusTest
    void validateByVariable(@CitrusResource TestContext testContext,
        @CitrusResource TestCaseRunner runner) {

        // Given
        getPetByIdRequest.setPetId("1234");

        // Then
        getPetByIdRequest.setResponseStatus(HttpStatus.OK.value());
        getPetByIdRequest.setResponseReasonPhrase(HttpStatus.OK.getReasonPhrase());

        // Assert load data into variables
        getPetByIdRequest.setResponseVariable(Map.of("$", "RESPONSE", "$.id", "ID"));

        // When
        runner.$(getPetByIdRequest);

        // Then
        assertThat(testContext)
            .satisfies(
                c -> assertThat(c.getVariable("RESPONSE"))
                    .isNotNull(),
                c -> assertThat(c.getVariable("ID"))
                    .isNotNull()
                    .isEqualTo("12")
            );
    }

    @Test
    @CitrusTest
    void validateReceivedResponse(@CitrusResource TestContext testContext) {

        // Given
        getPetByIdRequest.setPetId("1234");

        // When
        getPetByIdRequest.sendRequest(testContext);

        // Then
        Message receiveResponse = getPetByIdRequest.receiveResponse(testContext);
        assertThat(receiveResponse)
            .isNotNull()
            .extracting(Message::getPayload)
            .asString()
            .isEqualToIgnoringWhitespace(defaultResponse);
        assertThat(receiveResponse.getHeaders())
            .containsEntry("citrus_http_status_code", 200)
            .containsEntry("citrus_http_reason_phrase", "OK");
    }*/
}
