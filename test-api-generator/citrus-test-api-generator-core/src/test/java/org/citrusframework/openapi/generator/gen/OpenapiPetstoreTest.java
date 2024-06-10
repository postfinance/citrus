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
        runner.$(openapiPetstore(httpClient, runner)
                .getPetById()
                .withPetId("1001")
                .send()
                // TODO? set `.fork(true)` in the `.send()`
                .fork(true));

        respondPet(runner);

        runner.$(openapiPetstore(httpClient, runner)
                .getPetById()
                .receive()
                .message()
                .type(JSON)
                .contentType("application/json")
        );
    }

    private void respondPet(TestCaseRunner runner) {
        runner.$(http().server(httpServer)
                .receive()
                .get("/pet/1001")
                .message()
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
