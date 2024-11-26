package org.citrusframework.restdocs.http;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedBodyHttpRequestTest extends AbstractHttpRequestTest<CachedBodyHttpRequest> {

    private static final byte[] BODY = "foo".getBytes();

    @Mock
    private HttpRequest delegate;

    private AutoCloseable openedMocks;

    @Override
    protected HttpRequest getDelegate() {
        return delegate;
    }

    @BeforeMethod
    public void beforeMethodSetup() {
        openedMocks = MockitoAnnotations.openMocks(this);

        fixture = new CachedBodyHttpRequest(delegate, BODY);
    }

    @AfterMethod
    public void afterMethodTeardown() throws Exception {
        openedMocks.close();
    }

    @Test
    public void getBodyReturnsBody() {
        assertThat(fixture.getBody())
                .isEqualTo(BODY);
    }
}
