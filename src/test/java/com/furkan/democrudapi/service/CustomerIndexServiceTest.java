package com.furkan.democrudapi.service;

import com.furkan.democrudapi.config.BugProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CustomerIndexServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Spy
    private BugProperties bugProperties = new BugProperties();

    @InjectMocks
    private CustomerIndexService customerIndexService;

    @Test
    void shouldCreateIndexThenAnalyzeWhenDisablingMissingIndexBug() {
        bugProperties.setMissingIndex(true);

        customerIndexService.setMissingIndexEnabled(false);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).execute("CREATE INDEX idx_customer_city ON customer(city)");
        inOrder.verify(jdbcTemplate).execute("ANALYZE customer");
        assertThat(bugProperties.isMissingIndex()).isFalse();
    }

    @Test
    void shouldDropIndexWhenEnablingMissingIndexBug() {
        bugProperties.setMissingIndex(false);

        customerIndexService.setMissingIndexEnabled(true);

        verify(jdbcTemplate).execute("DROP INDEX IF EXISTS idx_customer_city");
        assertThat(bugProperties.isMissingIndex()).isTrue();
    }

    @Test
    void shouldDoNothingWhenRequestedStateMatchesCurrentState() {
        bugProperties.setMissingIndex(true);

        customerIndexService.setMissingIndexEnabled(true);

        verifyNoInteractions(jdbcTemplate);
        assertThat(bugProperties.isMissingIndex()).isTrue();
    }
}
