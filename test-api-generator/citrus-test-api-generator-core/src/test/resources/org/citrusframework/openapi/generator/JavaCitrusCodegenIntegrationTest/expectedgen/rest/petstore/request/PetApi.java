package org.citrusframework.openapi.generator.rest.petstore.request;

import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.actions.OpenApiClientActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientActionBuilder.OpenApiOperationBuilder;
import org.citrusframework.openapi.actions.OpenApiClientRequestActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientResponseActionBuilder;

import java.util.function.UnaryOperator;

import static org.citrusframework.spi.Resources.create;

public class PetApi {
    private static final OpenApiSpecification petstoreSpec = OpenApiSpecification.from(
            create("src/test/resources/apis/petstore.yaml")
    );

    public static PetApi openapiPetstore(HttpClient httpClient) {
        return new PetApi(httpClient);
    }

    private final HttpClient httpClient;

    private PetApi(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public PetstoreAction<GetPetByIdRequest> getPetById() {
        return petstoreAction(new GetPetByIdRequest());
    }

    private <B extends OperationRequestBuilder> PetstoreAction<B> petstoreAction(B requestBuilder) {
        return new PetstoreAction<>(httpClient, petstoreSpec, requestBuilder);
    }

    /**
     * getPetById (GET /pet/{petId})
     * Find pet by ID
     **/
    public static class GetPetByIdRequest extends OperationRequestBuilder {
        @Override
        public String getOperationId() {
            return "getPetById";
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
    }

    public static abstract class OperationRequestBuilder {
        protected final OpenApiOperationBuilder openApiOperation = OpenApiOperationBuilder.operation(getOperationId());

        public abstract String getOperationId();

        public OpenApiOperationBuilder build() {
            return openApiOperation;
        }
    }

    public static class PetstoreAction<T extends OperationRequestBuilder> extends OpenApiClientActionBuilder {
        private final T operation;

        private PetstoreAction(Endpoint httpClient, OpenApiSpecification specification, T operation) {
            super(httpClient, specification);
            this.operation = operation;
        }

        public OpenApiClientRequestActionBuilder send(UnaryOperator<T> builderProvider) {
            var builder = builderProvider.apply(operation);
            var send = send(builder.build());
            send.fork(true);
            return send;
        }

        public OpenApiClientResponseActionBuilder receive() {
            return receive(operation.getOperationId(), "200");
        }
    }
}
