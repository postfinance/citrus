package org.citrusframework.openapi.generator.gen;

import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.actions.OpenApiClientActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientRequestActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientResponseActionBuilder;

import java.util.function.UnaryOperator;

import static org.citrusframework.spi.Resources.create;

// TODO move to mustache File
public class OpenapiPetstore {
    private static final OpenApiSpecification petstoreSpec = OpenApiSpecification.from(
            create("src/test/resources/apis/petstore.yaml")
    );

    public static OpenapiPetstore openapiPetstore(HttpClient httpClient) {
        return new OpenapiPetstore(httpClient);
    }

    private final HttpClient httpClient;

    private OpenapiPetstore(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public GetPetByIdAction getPetById() {
        return new GetPetByIdAction(httpClient, petstoreSpec);
    }

    public static class GetPetByIdAction extends OpenApiClientActionBuilder {
        public static final String OPERATION_ID = "getPetById";

        public GetPetByIdAction(Endpoint httpClient, OpenApiSpecification specification) {
            super(httpClient, specification);
        }

        public OpenApiClientRequestActionBuilder send(UnaryOperator<GetPetByIdRequest> builderProvider) {
            var builder = builderProvider.apply(new GetPetByIdRequest());
            var send = send(builder.build());
            send.fork(true);
            return send;
        }

        public OpenApiClientResponseActionBuilder receive() {
            return receive(OPERATION_ID, "200");
        }

        public static class GetPetByIdRequest {
            private final OpenApiOperationBuilder openApiOperation = OpenApiOperationBuilder.operation(OPERATION_ID);

            public static GetPetByIdRequest getPetByIdRequest() {
                return new GetPetByIdRequest();
            }

            public GetPetByIdRequest withPetId(String petId) {
                openApiOperation.withParameter("petId", petId);
                return this;
            }

            public GetPetByIdRequest withCorrelationIds(String correlationIds) {
                openApiOperation.withParameter("correlationIds", correlationIds);
                return this;
            }

            public GetPetByIdRequest withVerbose(boolean verbose) {
                openApiOperation.withParameter("verbose", verbose);
                return this;
            }

            public OpenApiOperationBuilder build() {
                return openApiOperation;
            }
        }
    }
}
