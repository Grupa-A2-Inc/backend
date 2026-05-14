package org.elearning.backend.security.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@org.springframework.test.context.ActiveProfiles("test")
class CsrfCookieFilterTest {

    @Test
    void doFilterInternal_withoutCsrfToken_stillContinuesChain() throws Exception {
        CsrfCookieFilter filter = new CsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withCsrfToken_readsTokenAndContinuesChain() throws Exception {
        CsrfCookieFilter filter = new CsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        CsrfToken csrfToken = mock(CsrfToken.class);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        filter.doFilterInternal(request, response, chain);

        verify(csrfToken).getToken();
        verify(chain).doFilter(request, response);
    }
}
