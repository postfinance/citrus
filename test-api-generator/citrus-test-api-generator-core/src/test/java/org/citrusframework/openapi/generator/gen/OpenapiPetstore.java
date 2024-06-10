package org.citrusframework.openapi.generator.gen;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.actions.OpenApiClientActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientRequestActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientResponseActionBuilder;

import static org.citrusframework.spi.Resources.create;
import static org.springframework.http.HttpStatus.OK;

// TODO move to mustache File
public class OpenapiPetstore {
    private static final OpenApiSpecification petstoreSpec = OpenApiSpecification.from(
            create("src/test/resources/apis/petstore.yaml")
    );

    public static OpenapiPetstoreBuilder openapiPetstore(HttpClient httpClient, TestCaseRunner runner) {
        return new OpenapiPetstoreBuilder(httpClient, runner);
    }

    public static class OpenapiPetstoreBuilder {

        private final HttpClient httpClient;
        private final TestCaseRunner runner;

        OpenapiPetstoreBuilder(HttpClient httpClient, TestCaseRunner runner) {
            this.httpClient = httpClient;
            // TODO the runner is only needed to set the request params as test-variables, which is ugly...
            //      see comment in `withPetId()`
            this.runner = runner;
        }

        public GetPetByIdBuilder getPetById() {
            return new GetPetByIdBuilder(httpClient, petstoreSpec, runner);
        }

        public static class GetPetByIdBuilder extends OpenApiClientActionBuilder {
            public static final String OPERATION_ID = "getPetById";
            private final TestCaseRunner runner;

            public GetPetByIdBuilder(Endpoint httpClient, OpenApiSpecification specification, TestCaseRunner runner) {
                super(httpClient, specification);
                this.runner = runner;
            }

            public GetPetByIdBuilder withPetId(String petId) {
                // TODO? move that to super and make it more explicit to set params (e.g. not implicitly via testcase-variables)
                runner.variable("petId", petId);
                return this;
            }

            public OpenApiClientRequestActionBuilder send() {
                return send(OPERATION_ID);
            }

            public OpenApiClientResponseActionBuilder receive() {
                return receive(OPERATION_ID, OK);
            }
        }
    }
}
