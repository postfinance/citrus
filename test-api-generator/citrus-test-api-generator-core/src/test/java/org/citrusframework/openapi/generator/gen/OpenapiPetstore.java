package org.citrusframework.openapi.generator.gen;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.actions.OpenApiClientActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientRequestActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientResponseActionBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.citrusframework.spi.Resources.create;
import static org.springframework.http.HttpStatus.OK;

// TODO move to mustache File
public class OpenapiPetstore {
    private static final OpenApiSpecification petstoreSpec = OpenApiSpecification.from(
            create("src/test/resources/apis/petstore.yaml")
    );

    public static OpenapiPetstoreBuilder openapiPetstore(HttpClient httpClient) {
        return new OpenapiPetstoreBuilder(httpClient);
    }

    public static class OpenapiPetstoreBuilder {

        private final HttpClient httpClient;

        OpenapiPetstoreBuilder(HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        public GetPetByIdBuilder getPetById() {
            return new GetPetByIdBuilder(httpClient, petstoreSpec);
        }

        public static class GetPetByIdBuilder extends OpenApiClientActionBuilder {
            public static final String OPERATION_ID = "getPetById";
            private final Map<String, Object> variables = new HashMap<>();

            public GetPetByIdBuilder(Endpoint httpClient, OpenApiSpecification specification) {
                super(httpClient, specification);
            }

            public GetPetByIdBuilder withPetId(String petId) {
                variables.put("petId", petId);
                return this;
            }

            public GetPetByIdBuilder withCorrelationIds(String correlationIds) {
                variables.put("correlationIds", correlationIds);
                return this;
            }

            public GetPetByIdBuilder withVerbose(boolean verbose) {
                variables.put("verbose", verbose);
                return this;
            }

            public OpenApiClientRequestActionBuilder send() {
                var openApiOperation = OpenApiOperationBuilder.operation(OPERATION_ID).withParameters(variables);
                var send = send(openApiOperation);
                send.fork(true);
                return send;
            }

            public OpenApiClientResponseActionBuilder receive(TestCaseRunner runner) {
                return receive(OPERATION_ID, OK);
            }

            public static class GetPetByIdMessageBuilder extends OpenApiClientRequestActionBuilder {

                protected GetPetByIdMessageBuilder(OpenApiClientRequestActionBuilder.OpenApiClientRequestMessageBuilder messageBuilder) {
                    super(messageBuilder);
                }

                public GetPetByIdMessageBuilder create() {
                    return null;
                }
            }
        }
    }
}
