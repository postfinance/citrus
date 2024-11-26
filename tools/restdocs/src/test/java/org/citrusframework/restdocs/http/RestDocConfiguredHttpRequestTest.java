package org.citrusframework.restdocs.http;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpRequest;
import org.springframework.restdocs.RestDocumentationContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RestDocConfiguredHttpRequestTest extends AbstractHttpRequestTest<RestDocConfiguredHttpRequest> {

    @Mock
    private HttpRequest delegate;

    @Mock
    private RestDocumentationContext restDocumentationContext;

    private Map<String, Object> configuration;

    private AutoCloseable openedMocks;

    @Override
    protected HttpRequest getDelegate() {
        return delegate;
    }

    @BeforeMethod
    public void beforeMethodSetup() {
        openedMocks = MockitoAnnotations.openMocks(this);
        configuration = new HashMap<>();

        fixture = new RestDocConfiguredHttpRequest(delegate, restDocumentationContext, configuration);
    }

    @AfterMethod
    public void afterMethodTeardown() throws Exception {
        openedMocks.close();
    }

    @Test
    public void getContextReturnsContext() {
        assertThat(fixture.getContext())
                .isEqualTo(restDocumentationContext);
    }

    @Test
    public void getConfigurationReturnsConfiguration() {
        assertThat(fixture.getConfiguration())
                .isEqualTo(configuration);
    }

    @Test
    public void getRequestReturnsDelegate() {
        assertThat(fixture.getRequest())
                .isEqualTo(delegate);
    }
}
