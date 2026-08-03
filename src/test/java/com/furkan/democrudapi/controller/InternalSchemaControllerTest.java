package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.ForeignKeyResponse;
import com.furkan.democrudapi.service.SchemaMetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalSchemaController.class)
class InternalSchemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchemaMetadataService schemaMetadataService;

    @Test
    void shouldReturnForeignKeysAsJsonArray() throws Exception {
        when(schemaMetadataService.foreignKeys()).thenReturn(List.of(
                new ForeignKeyResponse("customer", "proposal_id", "proposal", "id"),
                new ForeignKeyResponse("payment", "customer_id", "customer", "id")));

        mockMvc.perform(get("/internal/schema/foreign-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].childTable").value("customer"))
                .andExpect(jsonPath("$[0].childColumn").value("proposal_id"))
                .andExpect(jsonPath("$[0].parentTable").value("proposal"))
                .andExpect(jsonPath("$[0].parentColumn").value("id"))
                .andExpect(jsonPath("$[1].childTable").value("payment"))
                .andExpect(jsonPath("$[1].parentTable").value("customer"));
    }

    @Test
    void shouldReturnEmptyArrayWhenSchemaHasNoForeignKeys() throws Exception {
        when(schemaMetadataService.foreignKeys()).thenReturn(List.of());

        mockMvc.perform(get("/internal/schema/foreign-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
