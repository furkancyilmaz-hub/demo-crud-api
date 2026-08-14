package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.CustomerResponse;
import com.furkan.democrudapi.entity.CustomerStatus;
import com.furkan.democrudapi.service.CustomerService;
import com.furkan.democrudapi.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the parameter-combination rules of {@code GET /api/customers/search}, which are the
 * only piece of logic that lives in the controller rather than the service.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final String IDENTITY_NO = "12345678901";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldSearchByCityWhenOnlyCityProvided() throws Exception {
        when(customerService.searchByCity(eq("Ankara"), any())).thenReturn(pageOf("Ankara"));

        mockMvc.perform(get("/api/customers/search").param("city", "Ankara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].city").value("Ankara"));
    }

    @Test
    void shouldSearchByIdentityNoWhenProposalIdAndIdentityNoProvided() throws Exception {
        when(customerService.searchByIdentityNo(eq(1L), eq(IDENTITY_NO), any())).thenReturn(pageOf("Istanbul"));

        mockMvc.perform(get("/api/customers/search")
                        .param("proposalId", "1")
                        .param("identityNo", IDENTITY_NO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].identityNo").value(IDENTITY_NO));
    }

    @Test
    void shouldReturn400WhenNoSearchCriteriaProvided() throws Exception {
        mockMvc.perform(get("/api/customers/search"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void shouldReturn400WhenIdentityNoProvidedWithoutProposalId() throws Exception {
        mockMvc.perform(get("/api/customers/search").param("identityNo", IDENTITY_NO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void shouldReturn400WhenProposalIdProvidedWithoutIdentityNo() throws Exception {
        mockMvc.perform(get("/api/customers/search").param("proposalId", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void shouldReturn400WhenCityIsCombinedWithIdentityNo() throws Exception {
        mockMvc.perform(get("/api/customers/search")
                        .param("city", "Ankara")
                        .param("proposalId", "1")
                        .param("identityNo", IDENTITY_NO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void shouldReturn400WhenCityIsBlank() throws Exception {
        mockMvc.perform(get("/api/customers/search").param("city", "   "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    private Page<CustomerResponse> pageOf(String city) {
        return new PageImpl<>(List.of(new CustomerResponse(
                10L, 1L, IDENTITY_NO, "Jane Doe", city, CustomerStatus.ACTIVE, List.of())));
    }
}
