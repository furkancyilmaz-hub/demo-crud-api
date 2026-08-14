package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.ProposalResponse;
import com.furkan.democrudapi.entity.ProposalStatus;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.service.ProposalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers {@code GET /api/proposals/search}, in particular that the literal {@code /search}
 * segment is routed to the new handler rather than to the {@code /{id}} template.
 */
@WebMvcTest(ProposalController.class)
class ProposalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalService proposalService;

    @Test
    void shouldReturnSingleProposalWhenProposalNoExists() throws Exception {
        when(proposalService.getByProposalNo("PN-001")).thenReturn(new ProposalResponse(
                1L, "PN-001", ProposalStatus.DRAFT, LocalDate.of(2026, 1, 5), new BigDecimal("1500.00")));

        mockMvc.perform(get("/api/proposals/search").param("proposalNo", "PN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.proposalNo").value("PN-001"));
    }

    @Test
    void shouldReturn404WhenProposalNoDoesNotExist() throws Exception {
        when(proposalService.getByProposalNo("PN-999"))
                .thenThrow(new ResourceNotFoundException("Proposal not found: PN-999"));

        mockMvc.perform(get("/api/proposals/search").param("proposalNo", "PN-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenProposalNoParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/proposals/search"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(proposalService);
    }
}
