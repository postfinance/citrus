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

package org.citrusframework.openapi.yaml;

import org.citrusframework.TestActor;
import org.citrusframework.TestCase;
import org.citrusframework.TestCaseMetaInfo;
import org.citrusframework.actions.ReceiveMessageAction;
import org.citrusframework.actions.SendMessageAction;
import org.citrusframework.endpoint.EndpointAdapter;
import org.citrusframework.endpoint.direct.DirectEndpointAdapter;
import org.citrusframework.endpoint.direct.DirectSyncEndpointConfiguration;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageBuilder;
import org.citrusframework.http.message.HttpMessageHeaders;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.message.DefaultMessage;
import org.citrusframework.message.DefaultMessageQueue;
import org.citrusframework.message.Message;
import org.citrusframework.message.MessageQueue;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.util.SocketUtils;
import org.citrusframework.validation.DefaultMessageHeaderValidator;
import org.citrusframework.validation.DefaultTextEqualsMessageValidator;
import org.citrusframework.validation.context.DefaultValidationContext;
import org.citrusframework.validation.context.HeaderValidationContext;
import org.citrusframework.validation.json.JsonMessageValidationContext;
import org.citrusframework.validation.xml.XmlMessageValidationContext;
import org.citrusframework.yaml.YamlTestLoader;
import org.citrusframework.yaml.actions.YamlTestActionBuilder;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.endpoint.resolver.EndpointUriResolver.ENDPOINT_URI_HEADER_NAME;
import static org.citrusframework.endpoint.resolver.EndpointUriResolver.REQUEST_PATH_HEADER_NAME;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;
import static org.citrusframework.http.message.HttpMessageHeaders.*;
import static org.citrusframework.message.MessageHeaders.ID;
import static org.citrusframework.message.MessageHeaders.TIMESTAMP;
import static org.springframework.http.HttpMethod.GET;

/**
 * @author Christoph Deppisch
 */
public class OpenApiClientTest extends AbstractYamlActionTest {

    @BindToRegistry
    final TestActor testActor = Mockito.mock(TestActor.class);

    @BindToRegistry
    private final DefaultMessageHeaderValidator headerValidator = new DefaultMessageHeaderValidator();

    @BindToRegistry
    private final DefaultTextEqualsMessageValidator validator = new DefaultTextEqualsMessageValidator().enableTrim();

    private final int port = SocketUtils.findAvailableTcpPort(8080);
    private final String uri = "http://localhost:" + port + "/test";

    private HttpServer httpServer;
    private HttpClient httpClient;

    private final MessageQueue inboundQueue = new DefaultMessageQueue("inboundQueue");

    private final Queue<HttpMessage> responses = new ArrayBlockingQueue<>(6);

    @BeforeClass
    public void setupEndpoints() {
        EndpointAdapter endpointAdapter = new DirectEndpointAdapter(new DirectSyncEndpointConfiguration()) {
            @Override
            public Message handleMessageInternal(Message request) {
                inboundQueue.send(request);
                return responses.isEmpty() ? new HttpMessage().status(HttpStatus.OK) : responses.remove();
            }
        };

        httpServer = http().server()
                .port(port)
                .timeout(500L)
                .endpointAdapter(endpointAdapter)
                .autoStart(true)
                .name("httpServer")
                .build();
        httpServer.initialize();

        httpClient = http().client()
                .requestUrl(uri)
                .name("httpClient")
                .build();
    }

    @AfterClass(alwaysRun = true)
    public void cleanupEndpoints() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Test
    public void shouldLoadOpenApiClientActions() throws IOException {
        YamlTestLoader testLoader = createTestLoader("classpath:org/citrusframework/openapi/yaml/openapi-client-test.yaml");

        context.setVariable("port", port);

        context.getReferenceResolver().bind("httpClient", httpClient);
        context.getReferenceResolver().bind("httpServer", httpServer);

        responses.add(new HttpMessage("""
                {
                  "id": 1000,
                  "name": "hasso",
                  "category": {
                    "id": 1000,
                    "name": "dog"
                  },
                  "photoUrls": [ "http://localhost:8080/photos/1000" ],
                  "tags": [
                    {
                      "id": 1000,
                      "name": "generated"
                    }
                  ],
                  "status": "available"
                }
                """).status(HttpStatus.OK).contentType("application/json"));
        responses.add(new HttpMessage().status(HttpStatus.CREATED));

        testLoader.load();

        TestCase result = testLoader.getTestCase();
        assertThat(result.getName()).isEqualTo("OpenApiClientTest");
        assertThat(result.getMetaInfo().getAuthor()).isEqualTo("Christoph");
        assertThat(result.getMetaInfo().getStatus()).isEqualTo(TestCaseMetaInfo.Status.FINAL);
        assertThat(result.getActions()).hasSize(4);

        assertThat(result.getTestAction(0).getClass()).isEqualTo(SendMessageAction.class);
        assertThat(result.getTestAction(0).getName()).isEqualTo("openapi:send-request");

        assertThat(result.getTestAction(1).getClass()).isEqualTo(ReceiveMessageAction.class);
        assertThat(result.getTestAction(1).getName()).isEqualTo("openapi:receive-response");

        int actionIndex = 0;

        var sendMessageAction = (SendMessageAction) result.getTestAction(actionIndex++);
        assertThat(sendMessageAction.isForkMode()).isFalse();
        assertThat(sendMessageAction.getEndpointUri()).isEqualTo("httpClient");

        String messageType = sendMessageAction.getMessageType();
        assertThat(sendMessageAction.getMessageBuilder())
                .isNotNull()
                .isInstanceOf(HttpMessageBuilder.class)
                .extracting(HttpMessageBuilder.class::cast)
                .satisfies(httpMessageBuilder -> {
                    assertThat(httpMessageBuilder.buildMessagePayload(context, messageType)).isEqualTo("");
                    assertThat(httpMessageBuilder.getMessage().getHeaders())
                            .hasSize(5)
                            .hasEntrySatisfying(ID, e -> assertThat(e).isNotNull())
                            .hasEntrySatisfying(TIMESTAMP, e -> assertThat(e).isNotNull())
                            .doesNotContainKey(HTTP_QUERY_PARAMS)
                            .doesNotContainKey(ENDPOINT_URI_HEADER_NAME)
                            .hasEntrySatisfying(HTTP_REQUEST_METHOD, e -> assertThat(e).isEqualTo(GET.name()))
                            .hasEntrySatisfying(REQUEST_PATH_HEADER_NAME, e -> assertThat(e).isEqualTo("/pet/${petId}"))
                            .hasEntrySatisfying(HTTP_REQUEST_URI, e -> assertThat(e).isEqualTo("/pet/${petId}"));
                });

        Message controlMessage = new DefaultMessage("");
        Message request = inboundQueue.receive();
        headerValidator.validateMessage(request, controlMessage, context, new HeaderValidationContext());
        validator.validateMessage(request, controlMessage, context, new DefaultValidationContext());

        var receiveMessageAction = (ReceiveMessageAction) result.getTestAction(actionIndex++);
        assertThat(receiveMessageAction.getValidationContexts()).hasSize(3);
        assertThat(receiveMessageAction.getReceiveTimeout()).isEqualTo(0L);
        assertThat(receiveMessageAction.getValidationContexts().get(0)).isInstanceOf(XmlMessageValidationContext.class);
        assertThat(receiveMessageAction.getValidationContexts().get(1)).isInstanceOf(JsonMessageValidationContext.class);
        assertThat(receiveMessageAction.getValidationContexts().get(2)).isInstanceOf(HeaderValidationContext.class);

        var httpMessageBuilder = ((HttpMessageBuilder) receiveMessageAction.getMessageBuilder());
        assertThat(httpMessageBuilder).isNotNull();

        assertThat(httpMessageBuilder.buildMessagePayload(context, receiveMessageAction.getMessageType())).isEqualTo("{\"id\": \"@isNumber()@\",\"category\": {\"id\": \"@isNumber()@\",\"name\": \"@notEmpty()@\"},\"name\": \"@notEmpty()@\",\"photoUrls\": \"@ignore@\",\"tags\": \"@ignore@\",\"status\": \"@matches(available|pending|sold)@\"}");
        assertThat(httpMessageBuilder.getMessage().getHeaders()).hasSize(5);
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(ID)).isNotNull();
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(TIMESTAMP)).isNotNull();
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(HttpMessageHeaders.HTTP_STATUS_CODE)).isEqualTo(200);
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(HttpMessageHeaders.HTTP_REASON_PHRASE)).isEqualTo("OK");
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(HttpMessageHeaders.HTTP_CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(receiveMessageAction.getEndpoint()).isNull();
        assertThat(receiveMessageAction.getEndpointUri()).isEqualTo("httpClient");
        assertThat(receiveMessageAction.getMessageProcessors()).isEmpty();
        assertThat(receiveMessageAction.getControlMessageProcessors()).isEmpty();

        sendMessageAction = (SendMessageAction) result.getTestAction(actionIndex++);
        assertThat(sendMessageAction.isForkMode()).isFalse();
        httpMessageBuilder = ((HttpMessageBuilder) sendMessageAction.getMessageBuilder());
        assertThat(httpMessageBuilder).isNotNull();
        assertThat(httpMessageBuilder.buildMessagePayload(context, messageType).toString().startsWith("{\"id\": ")).isTrue();
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(ID)).isNotNull();
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(TIMESTAMP)).isNotNull();

        Map<String, Object> requestHeaders = httpMessageBuilder.buildMessageHeaders(context);
        assertThat(requestHeaders).hasSize(4);
        assertThat(requestHeaders.get(HTTP_REQUEST_METHOD)).isEqualTo(HttpMethod.POST.name());
        assertThat(requestHeaders.get(REQUEST_PATH_HEADER_NAME)).isEqualTo("/pet");
        assertThat(requestHeaders.get(HTTP_REQUEST_URI)).isEqualTo("/pet");
        assertThat(requestHeaders.get(HttpMessageHeaders.HTTP_CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(sendMessageAction.getEndpoint()).isNull();
        assertThat(sendMessageAction.getEndpointUri()).isEqualTo("httpClient");

        receiveMessageAction = (ReceiveMessageAction) result.getTestAction(actionIndex);
        assertThat(receiveMessageAction.getValidationContexts()).hasSize(3);
        assertThat(receiveMessageAction.getValidationContexts().get(0)).isInstanceOf(XmlMessageValidationContext.class);
        assertThat(receiveMessageAction.getValidationContexts().get(1)).isInstanceOf(JsonMessageValidationContext.class);
        assertThat(receiveMessageAction.getValidationContexts().get(2)).isInstanceOf(HeaderValidationContext.class);

        httpMessageBuilder = ((HttpMessageBuilder) receiveMessageAction.getMessageBuilder());
        assertThat(httpMessageBuilder).isNotNull();
        assertThat(httpMessageBuilder.buildMessagePayload(context, receiveMessageAction.getMessageType())).isEqualTo("");
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(ID)).isNotNull();
        assertThat(httpMessageBuilder.getMessage().getHeaders().get(TIMESTAMP)).isNotNull();
        Map<String, Object> responseHeaders = httpMessageBuilder.buildMessageHeaders(context);
        assertThat(responseHeaders).hasSize(2);
        assertThat(responseHeaders.get(HttpMessageHeaders.HTTP_STATUS_CODE)).isEqualTo(201);
        assertThat(responseHeaders.get(HttpMessageHeaders.HTTP_REASON_PHRASE)).isEqualTo("CREATED");
        assertThat(receiveMessageAction.getEndpoint()).isNull();
        assertThat(receiveMessageAction.getEndpointUri()).isEqualTo("httpClient");

        assertThat(receiveMessageAction.getVariableExtractors()).isEmpty();
    }

    @Test
    public void shouldLookupTestActionBuilder() {
        assertThat(YamlTestActionBuilder.lookup("openapi").isPresent()).isTrue();
        assertThat(YamlTestActionBuilder.lookup("openapi").get()).isOfAnyClassIn(OpenApi.class);
    }
}
