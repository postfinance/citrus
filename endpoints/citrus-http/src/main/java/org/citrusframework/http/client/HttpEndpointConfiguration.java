/*
 * Copyright 2006-2013 the original author or authors.
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

package org.citrusframework.http.client;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.citrusframework.endpoint.AbstractPollableEndpointConfiguration;
import org.citrusframework.endpoint.resolver.DynamicEndpointUriResolver;
import org.citrusframework.endpoint.resolver.EndpointUriResolver;
import org.citrusframework.http.interceptor.LoggingClientInterceptor;
import org.citrusframework.http.message.HttpMessageConverter;
import org.citrusframework.message.DefaultMessageCorrelator;
import org.citrusframework.message.ErrorHandlingStrategy;
import org.citrusframework.message.MessageCorrelator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class HttpEndpointConfiguration extends AbstractPollableEndpointConfiguration {

    /**
     * Http url as service destination
     */
    private String requestUrl;

    /**
     * Request method
     */
    private RequestMethod requestMethod = RequestMethod.POST;

    /**
     * The request charset
     */
    private String charset = "UTF-8";

    /**
     * Default content type
     */
    private String contentType = "text/plain";

    /**
     * The rest template
     */
    private RestTemplate restTemplate;

    /**
     * Http client builder
     */
    private HttpClientBuilder httpClient;

    /**
     * Request factory
     */
    private ClientHttpRequestFactory requestFactory;

    /**
     * Resolves dynamic endpoint uri
     */
    private EndpointUriResolver endpointUriResolver = new DynamicEndpointUriResolver();

    /**
     * Header mapper
     */
    private HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

    /**
     * The message converter
     */
    private HttpMessageConverter messageConverter = new HttpMessageConverter();

    /**
     * Endpoint clientInterceptors
     */
    private List<ClientHttpRequestInterceptor> clientInterceptors = new ArrayList<>();

    /**
     * Should http errors be handled within endpoint consumer or simply throw exception
     */
    private ErrorHandlingStrategy errorHandlingStrategy = ErrorHandlingStrategy.PROPAGATE;

    /**
     * Response error handler
     */
    private ResponseErrorHandler errorHandler;

    /**
     * Reply message correlator
     */
    private MessageCorrelator correlator = new DefaultMessageCorrelator();

    /**
     * Auto add default accept header with os supported content-types
     */
    private boolean defaultAcceptHeader = true;

    /**
     * Should handle http attributes
     */
    private boolean handleAttributeHeaders = false;

    /**
     * Should handle http cookies
     */
    private boolean handleCookies = false;

    /**
     * Default status code returned by http server
     */
    private int defaultStatusCode = HttpStatus.OK.value();

    /**
     * List of media types that should be handled with binary content processing
     */
    private List<MediaType> binaryMediaTypes = asList(
        MediaType.APPLICATION_OCTET_STREAM,
        MediaType.APPLICATION_PDF,
        MediaType.IMAGE_GIF,
        MediaType.IMAGE_JPEG,
        MediaType.IMAGE_PNG,
        MediaType.valueOf("application/zip")
    );

    public HttpEndpointConfiguration() {
        clientInterceptors.add(new LoggingClientInterceptor());
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String url) {
        this.requestUrl = url;
    }

    public void setEndpointUriResolver(EndpointUriResolver endpointUriResolver) {
        this.endpointUriResolver = endpointUriResolver;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        clientInterceptors.addAll(restTemplate.getInterceptors());
        restTemplate.setInterceptors(clientInterceptors);
        this.restTemplate = restTemplate;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
        this.headerMapper = headerMapper;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ErrorHandlingStrategy getErrorHandlingStrategy() {
        return errorHandlingStrategy;
    }

    public void setErrorHandlingStrategy(ErrorHandlingStrategy errorHandlingStrategy) {
        this.errorHandlingStrategy = errorHandlingStrategy;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public String getCharset() {
        return charset;
    }

    public String getContentType() {
        return contentType;
    }

    public RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            restTemplate.setInterceptors(clientInterceptors);
        }

        restTemplate.setRequestFactory(getRequestFactory());
        restTemplate.setErrorHandler(getErrorHandler());

        if (!defaultAcceptHeader) {
            restTemplate.getMessageConverters().stream()
                .filter(StringHttpMessageConverter.class::isInstance)
                .map(StringHttpMessageConverter.class::cast)
                .forEach(converter -> converter.setWriteAcceptCharset(defaultAcceptHeader));
        }

        return restTemplate;
    }

    public EndpointUriResolver getEndpointUriResolver() {
        return endpointUriResolver;
    }

    public HeaderMapper<HttpHeaders> getHeaderMapper() {
        return headerMapper;
    }

    public List<ClientHttpRequestInterceptor> getClientInterceptors() {
        return clientInterceptors;
    }

    public void setClientInterceptors(List<ClientHttpRequestInterceptor> clientInterceptors) {
        this.clientInterceptors = clientInterceptors;
        getRestTemplate().setInterceptors(clientInterceptors);
    }

    public void setCorrelator(MessageCorrelator correlator) {
        this.correlator = correlator;
    }

    public MessageCorrelator getCorrelator() {
        return correlator;
    }

    public ClientHttpRequestFactory getRequestFactory() {
        if (requestFactory == null) {
            requestFactory = new HttpComponentsClientHttpRequestFactory(getHttpClient().build());
        }

        return requestFactory;
    }

    public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    public HttpClientBuilder getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClientBuilder.create().useSystemProperties();
        }

        return httpClient;
    }

    public void setHttpClient(HttpClientBuilder httpClient) {
        this.httpClient = httpClient;
    }

    public HttpMessageConverter getMessageConverter() {
        return messageConverter;
    }

    public void setMessageConverter(HttpMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    public void setDefaultAcceptHeader(boolean defaultAcceptHeader) {
        this.defaultAcceptHeader = defaultAcceptHeader;
    }

    public boolean isDefaultAcceptHeader() {
        return defaultAcceptHeader;
    }

    public boolean isHandleAttributeHeaders() {
        return handleAttributeHeaders;
    }

    public void setHandleAttributeHeaders(boolean handleAttributeHeaders) {
        this.handleAttributeHeaders = handleAttributeHeaders;
    }

    public boolean isHandleCookies() {
        return handleCookies;
    }

    public void setHandleCookies(boolean handleCookies) {
        this.handleCookies = handleCookies;
    }

    public ResponseErrorHandler getErrorHandler() {
        if (errorHandler == null) {
            errorHandler = new HttpResponseErrorHandler(errorHandlingStrategy);
        }

        return errorHandler;
    }

    public void setErrorHandler(ResponseErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public int getDefaultStatusCode() {
        return defaultStatusCode;
    }

    public void setDefaultStatusCode(int defaultStatusCode) {
        this.defaultStatusCode = defaultStatusCode;
    }

    public List<MediaType> getBinaryMediaTypes() {
        return binaryMediaTypes;
    }

    public void setBinaryMediaTypes(List<MediaType> binaryMediaTypes) {
        this.binaryMediaTypes = binaryMediaTypes;
    }

    public static class Builder {

        private HttpEndpointConfiguration config;

        public Builder() {
            config = new HttpEndpointConfiguration();
        }

        public Builder requestUrl(String requestUrl) {
            config.requestUrl = requestUrl;
            return this;
        }

        public Builder requestMethod(RequestMethod requestMethod) {
            config.requestMethod = requestMethod;
            return this;
        }

        public Builder charset(String charset) {
            config.charset = charset;
            return this;
        }

        public Builder contentType(String contentType) {
            config.contentType = contentType;
            return this;
        }

        public Builder restTemplate(RestTemplate restTemplate) {
            config.restTemplate = restTemplate;
            return this;
        }

        public Builder httpClient(HttpClientBuilder httpClient) {
            config.httpClient = httpClient;
            return this;
        }

        public Builder requestFactory(ClientHttpRequestFactory requestFactory) {
            config.requestFactory = requestFactory;
            return this;
        }

        public Builder endpointUriResolver(EndpointUriResolver endpointUriResolver) {
            config.endpointUriResolver = endpointUriResolver;
            return this;
        }

        public Builder headerMapper(HeaderMapper<HttpHeaders> headerMapper) {
            config.headerMapper = headerMapper;
            return this;
        }

        public Builder messageConverter(HttpMessageConverter messageConverter) {
            config.messageConverter = messageConverter;
            return this;
        }

        public Builder clientInterceptors(List<ClientHttpRequestInterceptor> clientInterceptors) {
            config.clientInterceptors = clientInterceptors;
            return this;
        }

        public Builder errorHandlingStrategy(ErrorHandlingStrategy errorHandlingStrategy) {
            config.errorHandlingStrategy = errorHandlingStrategy;
            return this;
        }

        public Builder errorHandler(ResponseErrorHandler errorHandler) {
            config.errorHandler = errorHandler;
            return this;
        }

        public Builder correlator(MessageCorrelator correlator) {
            config.correlator = correlator;
            return this;
        }

        public Builder defaultAcceptHeader(boolean defaultAcceptHeader) {
            config.defaultAcceptHeader = defaultAcceptHeader;
            return this;
        }

        public Builder handleAttributeHeaders(boolean handleAttributeHeaders) {
            config.handleAttributeHeaders = handleAttributeHeaders;
            return this;
        }

        public Builder handleCookies(boolean handleCookies) {
            config.handleCookies = handleCookies;
            return this;
        }

        public Builder defaultStatusCode(int defaultStatusCode) {
            config.defaultStatusCode = defaultStatusCode;
            return this;
        }

        public Builder binaryMediaTypes(List<MediaType> binaryMediaTypes) {
            config.binaryMediaTypes = binaryMediaTypes;
            return this;
        }

        public HttpEndpointConfiguration build() {
            return config;
        }
    }
}
