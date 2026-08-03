package com.furkan.democrudapi.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static com.furkan.democrudapi.constants.CorrelationConstants.CORRELATION_ID_HEADER;
import static com.furkan.democrudapi.constants.CorrelationConstants.CORRELATION_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void captureFilterLog() {
        logAppender = new ListAppender<>();
        logAppender.start();
        filterLogger().addAppender(logAppender);
    }

    @AfterEach
    void clearMdcAndLog() {
        filterLogger().detachAppender(logAppender);
        MDC.clear();
    }

    @Test
    void shouldPreserveIncomingCorrelationId() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/api/customers");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(CORRELATION_ID_HEADER, "caller-id-123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderMissing() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/customers");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(any(), captor.capture());
        assertThatCode(() -> UUID.fromString(captor.getValue())).doesNotThrowAnyException();
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderBlank() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("   ");
        when(request.getRequestURI()).thenReturn("/api/customers");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(any(), captor.capture());
        assertThatCode(() -> UUID.fromString(captor.getValue())).doesNotThrowAnyException();
    }

    @Test
    void shouldMakeCorrelationIdAvailableInMdcDuringChainExecution() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/api/customers");
        String[] mdcValueDuringChain = new String[1];
        doAnswer(invocation -> {
            mdcValueDuringChain[0] = MDC.get(CORRELATION_ID_MDC_KEY);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(mdcValueDuringChain[0]).isEqualTo("caller-id-123");
    }

    @Test
    void shouldClearMdcAfterSuccessfulChain() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/api/customers");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldClearMdcEvenWhenChainThrows() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/api/customers");
        doThrow(new ServletException("boom")).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldLogRequestCompletedInFixedFormat() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/customers/detail");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage())
                .matches("REQUEST_COMPLETED method=GET path=/api/customers/detail status=200 durationMs=\\d+");
    }

    @Test
    void shouldLogRequestCompletedWithCorrelationIdStillInMdc() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/api/customers");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(logAppender.list.get(0).getMDCPropertyMap()).containsEntry(CORRELATION_ID_MDC_KEY, "caller-id-123");
    }

    @Test
    void shouldNotLogRequestCompletedForInternalEndpoints() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/internal/logs");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void shouldNotLogRequestCompletedForSwaggerUi() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void shouldNotLogRequestCompletedForApiDocs() throws ServletException, IOException {
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("caller-id-123");
        when(request.getRequestURI()).thenReturn("/v3/api-docs");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(logAppender.list).isEmpty();
    }

    private Logger filterLogger() {
        return (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
    }
}
