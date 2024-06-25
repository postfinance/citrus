package org.citrusframework.openapi.generator.gen;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.endpoint.EndpointConfiguration;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpClientBuilder;
import org.citrusframework.http.client.HttpEndpointConfiguration;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.junit.jupiter.spring.CitrusSpringExtension;
import org.citrusframework.openapi.generator.gen.OpenapiPetstoreTest.Config;
import org.citrusframework.openapi.generator.rest.petstore.spring.PetStoreBeanConfiguration;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.citrusframework.message.MessageType.JSON;
import static org.citrusframework.openapi.generator.gen.OpenapiPetstore.openapiPetstore;
import static org.citrusframework.validation.PathExpressionValidationContext.Builder.pathExpression;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(CitrusSpringExtension.class)
@SpringBootTest(classes = {PetStoreBeanConfiguration.class, CitrusSpringConfig.class, Config.class})
class OpenapiPetstoreTest {

    @BeforeEach
    public void beforeTest() {
    }

    private final int port = SocketUtils.findAvailableTcpPort(8080);

    @BindToRegistry
    private final HttpServer httpServer = new HttpServerBuilder()
            .port(port)
            .timeout(5000L)
            .autoStart(true)
            .defaultStatus(HttpStatus.NO_CONTENT)
            .build();

    @BindToRegistry
    private final HttpClient httpClient = new HttpClientBuilder()
            .requestUrl("http://localhost:%d".formatted(port))
            .build();

    @Test
    @CitrusTest
    void testFluentGeneratedOpenapiAction(@CitrusResource TestCaseRunner runner) {
        runner.$(openapiPetstore(httpClient)
                .getPetById()
                .send(request -> request
                        .withPetId("2002")
                        .withCorrelationIds("5599")
                        .withVerbose(true))
        );

        respondPet(runner);

        runner.$(openapiPetstore(httpClient)
                .getPetById()
                .receive()
                .message()
                .validate(pathExpression()
                        .jsonPath("$.id", "2002")
                        .jsonPath("$.category.id", "2002")
                        .jsonPath("$.tags[0].name", "generated")
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
                          "name": "citrus:randomEnumValue('hasso','cutie','fluffy')",
                          "category": {
                            "id": ${petId},
                            "name": "citrus:randomEnumValue('dog', 'cat', 'fish')"
                          },
                          "photoUrls": [ "http://localhost:8080/photos/${petId}" ],
                          "tags": [
                            {
                              "id": ${petId},
                              "name": "generated"
                            }
                          ],
                          "status": "citrus:randomEnumValue('available', 'pending', 'sold')"
                        }
                        """)
                .contentType("application/json").type(JSON));
    }

    @TestConfiguration
    public static class Config {

        @Bean(name = {"applicationServiceClient", "petStoreEndpoint"})
        public HttpClient applicationServiceClient() {
            HttpClient client = mock(HttpClient.class);
            EndpointConfiguration endpointConfiguration = mock(EndpointConfiguration.class);
            when(client.getEndpointConfiguration()).thenReturn(new HttpEndpointConfiguration());
            when(endpointConfiguration.getTimeout()).thenReturn(5000L);
            return client;
        }
    }
}
