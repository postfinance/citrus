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

package org.citrusframework.openapi.integration;

import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpClientBuilder;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.spi.Resources;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.citrusframework.util.SocketUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.citrusframework.message.MessageType.XML;
import static org.citrusframework.openapi.actions.OpenApiActionBuilder.openapi;
import static org.citrusframework.openapi.actions.OpenApiClientActionBuilder.OpenApiOperationBuilder.operation;

/**
 * @author Christoph Deppisch
 */
@Test
public class OpenApiClientIT extends TestNGCitrusSpringSupport {

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

    private final OpenApiSpecification petstoreSpec = OpenApiSpecification.from(
            Resources.create("classpath:org/citrusframework/openapi/petstore/petstore-v3.json"));

    @CitrusTest
    public void getPetById() {
        variable("petId", "1001");
        variable("verbose", "true");
        variable("correlationIds", "1234abcd");

        when(openapi(petstoreSpec)
                .client(httpClient)
                .send("getPetById")
                .fork(true)
                .message()
        );

        then(http().server(httpServer)
                .receive()
                .get("/pet/1001")
                .queryParam("verbose", "true")
                .message()
                .header("correlationIds", "1234abcd")
                .accept("@contains('application/json')@")
        );

        then(http().server(httpServer)
                .send()
                .response(HttpStatus.OK)
                .message()
                .body(Resources.create("classpath:org/citrusframework/openapi/petstore/pet.json"))
                .contentType("application/json"));

        then(openapi(petstoreSpec)
                .client(httpClient)
                .receive("getPetById", HttpStatus.OK));
    }

    @CitrusTest
    public void getPetById_requiredParamsShouldBeGeneratedIfNotProvided() {

        when(openapi(petstoreSpec)
                .client(httpClient)
                .send("getPetById")
                .fork(true)
        );

        then(http().server(httpServer)
                        .receive()
                        .get("@matches('/pet/\\d+')@")
                        .message()
        );

        variable("petId", "1001");
        then(http().server(httpServer)
                .send()
                .response(HttpStatus.OK)
                .message()
                .body(Resources.create("classpath:org/citrusframework/openapi/petstore/pet.json"))
                .contentType("application/json"));

        then(openapi(petstoreSpec)
                .client(httpClient)
                .receive("getPetById", HttpStatus.OK));
    }

    @CitrusTest
    public void getPetById_setParameterExplicitly() {
        when(openapi(petstoreSpec)
                .client(httpClient)
                .send(operation("getPetById")
                        .withParameter("petId", "1001")
                        .withParameter("correlationIds", "5599")
                        .withParameter("verbose", "true"))
                .fork(true));

        then(http().server(httpServer)
                .receive()
                .get("/pet/1001")
                .message()
                .queryParam("verbose", "true")
                .header("correlationIds", "5599")
                .accept("@contains('application/json')@"));

        then(http().server(httpServer)
                .send()
                .response(HttpStatus.OK)
                .message()
                .body(Resources.create("classpath:org/citrusframework/openapi/petstore/pet.json"))
                .contentType("application/json"));

        then(openapi(petstoreSpec)
                .client(httpClient)
                .receive("getPetById", HttpStatus.OK));
    }

    @CitrusTest
    public void getPetById_generated() {
        when(openapi(petstoreSpec)
                .client(httpClient)
                .send(operation("getPetById")
                        .withParameter("petId", "1001")
                        .withParameter("correlationIds", "5599")
                        .withParameter("verbose", "true"))
                .fork(true));

        then(http().server(httpServer)
                .receive()
                .get("/pet/1001")
                .message()
                .queryParam("verbose", "true")
                .header("correlationIds", "5599")
                .accept("@contains('application/json')@"));

        then(http().server(httpServer)
                .send()
                .response(HttpStatus.OK)
                .message()
                .body(Resources.create("classpath:org/citrusframework/openapi/petstore/pet.json"))
                .contentType("application/json"));

        then(openapi(petstoreSpec)
                .client(httpClient)
                .receive("getPetById", HttpStatus.OK));
    }

    @CitrusTest
    public void postAddPet() {
        variable("petId", "1001");

        when(openapi(petstoreSpec)
                .client(httpClient)
                .send("addPet")
                .fork(true));

        then(http().server(httpServer)
                .receive()
                .post("/pet")
                .message()
                .body("""
                                {
                                  "id": "@isNumber()@",
                                  "name": "@notEmpty()@",
                                  "category": {
                                    "id": "@isNumber()@",
                                    "name": "@notEmpty()@"
                                  },
                                  "photoUrls": "@notEmpty()@",
                                  "tags":  "@ignore@",
                                  "status": "@matches(sold|pending|available)@"
                                }
                        """)
                .contentType("application/json;charset=UTF-8"));

        then(http().server(httpServer)
                .send()
                .response(HttpStatus.CREATED)
                .message());

        then(openapi(petstoreSpec)
                .client(httpClient)
                .receive("addPet", HttpStatus.CREATED));
    }
}
