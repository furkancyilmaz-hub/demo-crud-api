package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.exception.InvalidExplainQueryException;
import com.furkan.democrudapi.service.ExplainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalExplainController.class)
class InternalExplainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExplainService explainService;

    @Test
    void shouldReturn200WithRawPlanJsonWhenQueryValid() throws Exception {
        when(explainService.explain(anyString())).thenReturn("{\"Plan\":{\"Node Type\":\"Seq Scan\"}}");

        mockMvc.perform(post("/internal/explain").content("SELECT * FROM customer WHERE city='Ankara'"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"Plan\":{\"Node Type\":\"Seq Scan\"}}"));
    }

    @Test
    void shouldReturn400WhenServiceRejectsQuery() throws Exception {
        when(explainService.explain(anyString()))
                .thenThrow(new InvalidExplainQueryException("Only SELECT or WITH queries are allowed"));

        mockMvc.perform(post("/internal/explain").content("DROP TABLE customer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only SELECT or WITH queries are allowed"));
    }
}
