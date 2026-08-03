package com.furkan.democrudapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the acceptance criterion of SP0013: every endpoint without exception has to show up
 * in the generated spec. A full context is unavoidable here — the question under test is
 * exactly which handlers springdoc discovers, which a slice test cannot answer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    private static final String[] EXPECTED_PATHS = {
            "/api/customers",
            "/api/customers/{id}",
            "/api/customers/detail",
            "/api/customers/overview",
            "/api/customers/search",
            "/api/customers/{id}/payments",
            "/api/payments",
            "/api/payments/{id}",
            "/api/proposals",
            "/api/proposals/{id}",
            "/api/proposals/detail",
            "/internal/logs",
            "/internal/requests",
            "/internal/schema/foreign-keys",
    };

    private static final int EXPECTED_OPERATION_COUNT = 23;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeEveryControllerPathInTheApiDocs() throws Exception {
        JsonNode paths = fetchApiDocs().get("paths");

        for (String path : EXPECTED_PATHS) {
            assertThat(paths.has(path)).as("path %s is missing from the api docs", path).isTrue();
        }
        assertThat(paths.size()).isEqualTo(EXPECTED_PATHS.length);
    }

    @Test
    void shouldExposeEveryOperationInTheApiDocs() throws Exception {
        JsonNode paths = fetchApiDocs().get("paths");

        int operations = 0;
        for (JsonNode pathItem : paths) {
            operations += pathItem.size();
        }

        assertThat(operations).isEqualTo(EXPECTED_OPERATION_COUNT);
    }

    private JsonNode fetchApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }
}
