package com.geneaazul.gedcomanalyzer.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InetAddressUtilsTests {

    @Mock
    private HttpServletRequest request;

    @Test
    void getRemoteAddress_withXRealIp_returnsIt() {
        when(request.getHeader("X-REAL-IP")).thenReturn("10.0.0.1");
        assertThat(InetAddressUtils.getRemoteAddress(request)).contains("10.0.0.1");
    }

    @Test
    void getRemoteAddress_withXRealIpTakesPriorityOverXClientIp() {
        when(request.getHeader("X-REAL-IP")).thenReturn("10.0.0.1");
        assertThat(InetAddressUtils.getRemoteAddress(request)).contains("10.0.0.1");
    }

    @Test
    void getRemoteAddress_withXClientIpWhenNoXRealIp_returnsIt() {
        when(request.getHeader("X-REAL-IP")).thenReturn(null);
        when(request.getHeader("X-CLIENT-IP")).thenReturn("10.0.0.2");
        assertThat(InetAddressUtils.getRemoteAddress(request)).contains("10.0.0.2");
    }

    @Test
    void getRemoteAddress_withXForwardedFor_returnsFirstAddress() {
        when(request.getHeader("X-REAL-IP")).thenReturn(null);
        when(request.getHeader("X-CLIENT-IP")).thenReturn(null);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("10.0.0.3, 10.0.0.4, 10.0.0.5");
        assertThat(InetAddressUtils.getRemoteAddress(request)).contains("10.0.0.3");
    }

    @Test
    void getRemoteAddress_withXForwardedForWithLeadingWhitespace_trimsAndReturns() {
        when(request.getHeader("X-REAL-IP")).thenReturn(null);
        when(request.getHeader("X-CLIENT-IP")).thenReturn(null);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("  10.0.0.3  , 10.0.0.4");
        assertThat(InetAddressUtils.getRemoteAddress(request)).contains("10.0.0.3");
    }

    @Test
    void getRemoteAddress_fallsBackToRemoteAddr() {
        when(request.getRemoteAddr()).thenReturn("10.0.0.6");
        assertThat(InetAddressUtils.getRemoteAddress(request)).contains("10.0.0.6");
    }

    @Test
    void getRemoteAddress_withLocalhostIpv4_returnsEmpty() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        assertThat(InetAddressUtils.getRemoteAddress(request)).isEmpty();
    }

    @Test
    void getRemoteAddress_withLocalhostIpv6Short_returnsEmpty() {
        when(request.getRemoteAddr()).thenReturn("::1");
        assertThat(InetAddressUtils.getRemoteAddress(request)).isEmpty();
    }

    @Test
    void getRemoteAddress_withLocalhostIpv6Full_returnsEmpty() {
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        assertThat(InetAddressUtils.getRemoteAddress(request)).isEmpty();
    }

    @Test
    void getRemoteAddress_withLocalhostInXForwardedFor_returnsEmpty() {
        when(request.getHeader("X-REAL-IP")).thenReturn(null);
        when(request.getHeader("X-CLIENT-IP")).thenReturn(null);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("127.0.0.1");
        assertThat(InetAddressUtils.getRemoteAddress(request)).isEmpty();
    }

    @Test
    void getRemoteAddress_whenAllHeadersBlank_returnsEmpty() {
        when(request.getHeader("X-REAL-IP")).thenReturn("   ");
        when(request.getHeader("X-CLIENT-IP")).thenReturn("");
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);
        assertThat(InetAddressUtils.getRemoteAddress(request)).isEqualTo(Optional.empty());
    }

}
