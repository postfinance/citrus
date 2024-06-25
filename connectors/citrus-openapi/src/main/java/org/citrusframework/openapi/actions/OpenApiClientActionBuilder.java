/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.openapi.actions;

import org.citrusframework.TestAction;
import org.citrusframework.TestActionBuilder;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.spi.ReferenceResolver;
import org.citrusframework.spi.ReferenceResolverAware;
import org.citrusframework.util.ObjectHelper;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Action executes http client operations such as sending requests and receiving responses.
 *
 * @author Christoph Deppisch
 * @since 4.1
 */
public class OpenApiClientActionBuilder implements TestActionBuilder.DelegatingTestActionBuilder<TestAction>, ReferenceResolverAware {

    private final OpenApiSpecification specification;

    /** Bean reference resolver */
    private ReferenceResolver referenceResolver;

    /** Target http client instance */
    private Endpoint httpClient;
    private String httpClientUri;

    private TestActionBuilder<?> delegate;

    /**
     * Default constructor.
     */
    public OpenApiClientActionBuilder(Endpoint httpClient, OpenApiSpecification specification) {
        this.httpClient = httpClient;
        this.specification = specification;
    }

    /**
     * Default constructor.
     */
    public OpenApiClientActionBuilder(String httpClientUri, OpenApiSpecification specification) {
        this.httpClientUri = httpClientUri;
        this.specification = specification;
    }

    public static class OpenApiOperationBuilder {
        private final String operationId;
        private final Map<String, Object> parameters = new HashMap<>();

        public OpenApiOperationBuilder(String operationId) {
            this.operationId = operationId;
        }

        public static OpenApiOperationBuilder operation(String operationId) {
            return new OpenApiOperationBuilder(operationId);
        }

        public OpenApiOperationBuilder withParameter(String name, Object value) {
            parameters.put(name, value);
            return this;
        }

        public OpenApiOperationBuilder withParameters(Map<String, Object> parameters) {
            this.parameters.putAll(parameters);
            return this;
        }
    }

    public OpenApiClientRequestActionBuilder send(OpenApiOperationBuilder openApiOperationBuilder) {
        var builder = OpenApiClientRequestActionBuilder.create(
                specification,
                openApiOperationBuilder.operationId
        );
        if (httpClient != null) {
            builder.endpoint(httpClient);
        } else {
            builder.endpoint(httpClientUri);
        }

        builder.name("openapi:send-request");
        builder.withReferenceResolver(referenceResolver);
        openApiOperationBuilder.parameters.forEach(builder.getMessageBuilder()::withParameter);

        this.delegate = builder;
        return builder;
    }

    /**
     * Sends Http requests as client.
     */
    public OpenApiClientRequestActionBuilder send(String operationId) {
        return send(OpenApiOperationBuilder.operation(operationId));
    }

    /**
     * Receives Http response messages as client.
     * Uses default Http status 200 OK.
     */
    public OpenApiClientResponseActionBuilder receive(String operationId) {
        return receive(operationId, HttpStatus.OK);
    }

    /**
     * Receives Http response messages as client.
     */
    public OpenApiClientResponseActionBuilder receive(String operationId, HttpStatus status) {
        return receive(operationId, String.valueOf(status.value()));
    }

    /**
     * Receives Http response messages as client.
     */
    public OpenApiClientResponseActionBuilder receive(String operationId, String statusCode) {
        OpenApiClientResponseActionBuilder builder = new OpenApiClientResponseActionBuilder(specification, operationId, statusCode);
        if (httpClient != null) {
            builder.endpoint(httpClient);
        } else {
            builder.endpoint(httpClientUri);
        }

        builder.name("openapi:receive-response");
        builder.withReferenceResolver(referenceResolver);
        this.delegate = builder;
        return builder;
    }

    /**
     * Sets the bean reference resolver.
     * @param referenceResolver
     */
    public OpenApiClientActionBuilder withReferenceResolver(ReferenceResolver referenceResolver) {
        this.referenceResolver = referenceResolver;
        return this;
    }

    @Override
    public TestAction build() {
        ObjectHelper.assertNotNull(delegate, "Missing delegate action to build");
        return delegate.build();
    }

    @Override
    public TestActionBuilder<?> getDelegate() {
        return delegate;
    }

    /**
     * Specifies the referenceResolver.
     * @param referenceResolver
     */
    @Override
    public void setReferenceResolver(ReferenceResolver referenceResolver) {
        if (referenceResolver == null) {
            this.referenceResolver = referenceResolver;

            if (delegate instanceof ReferenceResolverAware) {
                ((ReferenceResolverAware) delegate).setReferenceResolver(referenceResolver);
            }
        }
    }
}
