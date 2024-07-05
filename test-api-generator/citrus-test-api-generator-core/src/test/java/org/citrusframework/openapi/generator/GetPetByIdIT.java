package org.citrusframework.openapi.generator;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.context.TestContext;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpClientBuilder;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.citrusframework.message.Message;
import org.citrusframework.openapi.generator.rest.petstore.spring.PetStoreBeanConfiguration;
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.citrusframework.message.MessageType.JSON;
import static org.citrusframework.openapi.generator.rest.petstore.request.PetApi.openapiPetstore;
import static org.citrusframework.validation.PathExpressionValidationContext.Builder.pathExpression;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@CitrusSpringSupport
@ContextConfiguration(classes = {PetStoreBeanConfiguration.class, CitrusSpringConfig.class, GetPetByIdIT.Config.class})
class GetPetByIdIT {

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private HttpServer httpServer;

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
                                .withPetId(2002L)
                                .withCorrelationIds("5599")
                                .withVerbose(true)
                        )
        );

        respondPet(runner);

        var expectedResponse = new File("src/test/resources/org/citrusframework/openapi/generator/GeneratedApiTest/payloads/getPetByIdControlMessage1.json");
        runner.$(
                openapiPetstore(httpClient)
                        .getPetById()
                        .receive()
                        .message()
                        .validate((Message message, TestContext context) -> {
                            assertThat(expectedResponse).exists().content().satisfies(expectedContent -> {
                                assertThat(message.getPayload(String.class)).isEqualToIgnoringWhitespace(expectedContent);
                            });
                        })
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

    @TestConfiguration
    public static class Config {

        private final int port = SocketUtils.findAvailableTcpPort(8080);

        @Bean
        public HttpClient httpClient() {
            return new HttpClientBuilder()
                    .requestUrl("http://localhost:%d".formatted(port))
                    .build();
        }

        @Bean
        public HttpServer httpServer() {
            return new HttpServerBuilder()
                    .port(port)
                    // .endpointAdapter(endpointAdapter)
                    .timeout(5000L)
                    .autoStart(true)
                    .defaultStatus(NO_CONTENT)
                    .build();
        }
    }
}
