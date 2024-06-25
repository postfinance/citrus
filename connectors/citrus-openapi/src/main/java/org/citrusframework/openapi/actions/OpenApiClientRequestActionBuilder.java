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

import io.apicurio.datamodels.openapi.models.*;
import org.citrusframework.CitrusSettings;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.actions.HttpClientRequestActionBuilder;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageBuilder;
import org.citrusframework.message.Message;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.model.OasModelHelper;
import org.springframework.http.HttpMethod;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static org.citrusframework.CitrusSettings.VARIABLE_PREFIX;
import static org.citrusframework.CitrusSettings.VARIABLE_SUFFIX;
import static org.citrusframework.openapi.OpenApiTestDataGenerator.createOutboundPayload;
import static org.citrusframework.openapi.OpenApiTestDataGenerator.createRandomValueExpression;
import static org.citrusframework.openapi.model.OasModelHelper.*;

/**
 * @author Christoph Deppisch
 * @since 4.1
 */
public class OpenApiClientRequestActionBuilder extends HttpClientRequestActionBuilder {

    // TODO remove?
    public OpenApiClientRequestMessageBuilder getMessageBuilder() {
        return messageBuilder;
    }

    private final OpenApiClientRequestMessageBuilder messageBuilder;

    protected OpenApiClientRequestActionBuilder(OpenApiClientRequestMessageBuilder messageBuilder) {
        super(messageBuilder, messageBuilder.httpMessage);
        this.messageBuilder = messageBuilder;
    }

    public static OpenApiClientRequestActionBuilder create(OpenApiSpecification openApiSpec, String operationId) {
        var messageBuilder = new OpenApiClientRequestMessageBuilder(new HttpMessage(), openApiSpec, operationId, Map.of());
        return new OpenApiClientRequestActionBuilder(messageBuilder);
    }

    protected static class OpenApiClientRequestMessageBuilder extends HttpMessageBuilder {

        private final Map<String, Object> parameters = new HashMap<>();
        private final OpenApiSpecification openApiSpec;
        private final String operationId;

        private final HttpMessage httpMessage;

        public OpenApiClientRequestMessageBuilder(
                HttpMessage httpMessage,
                OpenApiSpecification openApiSpec,
                String operationId,
                Map<String, Object> parameters
        ) {
            super(httpMessage);
            this.openApiSpec = openApiSpec;
            this.operationId = operationId;
            this.httpMessage = httpMessage;
            this.parameters.putAll(parameters);
        }

        private record OasItem(
                OasOperation operation,
                OasPathItem pathItem,
                HttpMethod method
        ) {
            public static OasItem create(String operationId, OasDocument oasDocument) {
                OasItem item = null;

                for (OasPathItem path : OasModelHelper.getPathItems(oasDocument.paths)) {
                    Optional<Map.Entry<String, OasOperation>> operationEntry = OasModelHelper.getOperationMap(path).entrySet().stream()
                            .filter(op -> operationId.equals(op.getValue().operationId))
                            .findFirst();

                    if (operationEntry.isPresent()) {
                        item = new OasItem(
                                operationEntry.get().getValue(),
                                path,
                                HttpMethod.valueOf(operationEntry.get().getKey().toUpperCase(Locale.US))
                        );
                        break;
                    }
                }

                if (item == null) {
                    throw new CitrusRuntimeException(
                            "Unable to locate operation with id '%s' in OpenAPI specification %s"
                                    .formatted(operationId, oasDocument.getAttributeNames())
                    );
                }
                return item;
            }
        }

        public OpenApiClientRequestMessageBuilder withParameter(String name, Object number) {
            parameters.put(name, number);
            return this;
        }

        @Override
        public Message build(TestContext context, String messageType) {
            // TODO: TAT-1291 - make parameter substitution more explicit?
            context.addVariables(parameters);
            OasDocument oasDocument = openApiSpec.getOpenApiDoc(context);
            var item = OasItem.create(operationId, oasDocument);

            getRequiredOrPresentParametersIn("header", item, context).forEach(param ->
                            httpMessage.setHeader(
                                    param.getName(),
                                    context.getVariables().containsKey(param.getName())
                                            ? CitrusSettings.VARIABLE_PREFIX + param.getName() + CitrusSettings.VARIABLE_SUFFIX
                                            :
                                            createRandomValueExpression(
                                                    (OasSchema) param.schema,
                                                    getSchemaDefinitions(oasDocument),
                                                    false,
                                                    openApiSpec
                                            )
                    )
            );

            getRequiredOrPresentParametersIn("query", item, context)
                    .forEach(param -> httpMessage.queryParam(
                            param.getName(),
                            createRandomValueExpression(param.getName(), (OasSchema) param.schema, context)
                    ));

            getRequestBodySchema(oasDocument, item.operation)
                    .ifPresent(oasSchema -> httpMessage.setPayload(
                            createOutboundPayload(oasSchema, getSchemaDefinitions(oasDocument), openApiSpec)
                    ));

            getRequestContentType(item.operation).ifPresent(httpMessage::contentType);
            httpMessage.path(getSubstitutedPath(context, item));
            httpMessage.method(item.method);

            return super.build(context, messageType);
        }

        private static Stream<OasParameter> getRequiredOrPresentParametersIn(String header, OasItem item, TestContext context) {
            return item.operation.getParametersIn(header).stream()
                    .filter(onlyRequiredOrPresentParameters(context));
        }

        private static Predicate<OasParameter> onlyRequiredOrPresentParameters(TestContext context) {
            return param -> TRUE.equals(param.required) || context.getVariables().containsKey(param.getName());
        }

        private static String getSubstitutedPath(TestContext context, OasItem item) {
            String randomizedPath = item.pathItem.getPath();
            List<OasParameter> pathParams = item.operation.getParametersIn("path");

            for (OasParameter parameter : pathParams) {
                String parameterValue;
                if (context.getVariables().containsKey(parameter.getName())) {
                    parameterValue = "\\" + VARIABLE_PREFIX + parameter.getName() + VARIABLE_SUFFIX;
                } else {
                    parameterValue = createRandomValueExpression((OasSchema) parameter.schema);
                }
                randomizedPath = Pattern.compile("\\{" + parameter.getName() + "}")
                        .matcher(randomizedPath)
                        .replaceAll(parameterValue);
            }
            return randomizedPath;
        }
    }
}
