package org.citrusframework.restdocs.http;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public abstract class AbstractHttpRequestTest<F extends HttpRequest> {

    protected F fixture;

    protected abstract HttpRequest getDelegate();

    @Test
    public void getHeadersReturnsHeaders() {
        var httpHeaders = mock(HttpHeaders.class);
        doReturn(httpHeaders).when(getDelegate()).getHeaders();

        assertThat(fixture.getHeaders())
                .isEqualTo(httpHeaders);
    }

    @Test
    public void getMethodReturnsMethod() {
        var httpMethod = mock(HttpMethod.class);
        doReturn(httpMethod).when(getDelegate()).getMethod();

        assertThat(fixture.getMethod())
                .isEqualTo(httpMethod);
    }

    @Test
    public void getURIReturnsURI() {
        var uri = mock(URI.class);
        doReturn(uri).when(getDelegate()).getURI();

        assertThat(fixture.getURI())
                .isEqualTo(uri);
    }

    @Test
    public void getAttributesReturnsAttributes() {
        var attributes = new HashMap<String, Object>();
        doReturn(attributes).when(getDelegate()).getAttributes();

        assertThat(fixture.getAttributes())
                .isEqualTo(attributes);
    }
}
