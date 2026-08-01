package com.furkan.democrudapi.service;

import com.furkan.democrudapi.exception.InvalidExplainQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExplainServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExplainService explainService;

    @Test
    void shouldRejectBlankQuery() {
        assertThatThrownBy(() -> explainService.explain("  "))
                .isInstanceOf(InvalidExplainQueryException.class)
                .hasMessage("Query must not be blank");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldRejectQueryNotStartingWithSelectOrWith() {
        assertThatThrownBy(() -> explainService.explain("DROP TABLE customer"))
                .isInstanceOf(InvalidExplainQueryException.class)
                .hasMessage("Only SELECT or WITH queries are allowed");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldRejectQueryContainingSemicolon() {
        assertThatThrownBy(() -> explainService.explain("SELECT 1; DROP TABLE customer"))
                .isInstanceOf(InvalidExplainQueryException.class)
                .hasMessage("Query must not contain multiple statements");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldRejectQueryContainingForbiddenKeyword() {
        assertThatThrownBy(() -> explainService.explain("WITH x AS (DELETE FROM customer RETURNING *) SELECT * FROM x"))
                .isInstanceOf(InvalidExplainQueryException.class)
                .hasMessage("Query contains a forbidden keyword: DELETE");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void shouldAllowSelectQueryWithColumnNameResemblingForbiddenKeyword() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("{\"Plan\":{}}");

        String result = explainService.explain("SELECT created_at FROM customer");

        assertThat(result).isEqualTo("{\"Plan\":{}}");
    }

    @Test
    void shouldRunStatementTimeoutThenExplainAndReturnPlanAsIs() {
        when(jdbcTemplate.queryForObject("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT 1", String.class))
                .thenReturn("{\"Plan\":{}}");

        String result = explainService.explain("SELECT 1");

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).execute("SET LOCAL statement_timeout = '5s'");
        inOrder.verify(jdbcTemplate).queryForObject("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT 1", String.class);
        assertThat(result).isEqualTo("{\"Plan\":{}}");
    }
}
