package com.furkan.democrudapi.service;

import com.furkan.democrudapi.dto.ProposalCreateRequest;
import com.furkan.democrudapi.dto.ProposalDetailResponse;
import com.furkan.democrudapi.dto.ProposalResponse;
import com.furkan.democrudapi.dto.ProposalUpdateRequest;
import com.furkan.democrudapi.entity.Customer;
import com.furkan.democrudapi.entity.CustomerStatus;
import com.furkan.democrudapi.entity.Proposal;
import com.furkan.democrudapi.entity.ProposalStatus;
import com.furkan.democrudapi.exception.DuplicateProposalNoException;
import com.furkan.democrudapi.exception.ResourceInUseException;
import com.furkan.democrudapi.exception.ResourceNotFoundException;
import com.furkan.democrudapi.repository.CustomerRepository;
import com.furkan.democrudapi.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ProposalService proposalService;

    @Test
    void shouldCreateProposalWhenProposalNoIsUnique() {
        ProposalCreateRequest request = new ProposalCreateRequest(
                "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        Proposal saved = newProposal(1L, request.proposalNo(), request.status(), request.issueDate(), request.totalPremium());
        when(proposalRepository.existsByProposalNo(request.proposalNo())).thenReturn(false);
        when(proposalRepository.save(any(Proposal.class))).thenReturn(saved);

        ProposalResponse response = proposalService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.proposalNo()).isEqualTo("PN-001");
    }

    @Test
    void shouldThrowDuplicateProposalNoExceptionWhenProposalNoAlreadyExists() {
        ProposalCreateRequest request = new ProposalCreateRequest(
                "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        when(proposalRepository.existsByProposalNo(request.proposalNo())).thenReturn(true);

        assertThatThrownBy(() -> proposalService.create(request))
                .isInstanceOf(DuplicateProposalNoException.class);
    }

    @Test
    void shouldThrowDuplicateProposalNoExceptionWhenSaveThrowsDataIntegrityViolation() {
        ProposalCreateRequest request = new ProposalCreateRequest(
                "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        when(proposalRepository.existsByProposalNo(request.proposalNo())).thenReturn(false);
        when(proposalRepository.save(any(Proposal.class))).thenThrow(new DataIntegrityViolationException("conflict"));

        assertThatThrownBy(() -> proposalService.create(request))
                .isInstanceOf(DuplicateProposalNoException.class);
    }

    @Test
    void shouldReturnProposalWhenGetByIdWithExistingId() {
        Proposal proposal = newProposal(1L, "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        ProposalResponse response = proposalService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenGetByIdWithMissingId() {
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnPagedProposalsWhenListCalled() {
        Proposal proposal = newProposal(1L, "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        Page<Proposal> page = new PageImpl<>(List.of(proposal));
        when(proposalRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<ProposalResponse> result = proposalService.list(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldIncludeCustomersWhenListingDetail() {
        Proposal proposal = newProposal(1L, "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        ReflectionTestUtils.setField(proposal, "customers",
                List.of(newCustomer(10L, proposal, "Jane Doe"), newCustomer(11L, proposal, "John Doe")));
        when(proposalRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(proposal)));

        Page<ProposalDetailResponse> result = proposalService.listDetail(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().customers())
                .extracting(ProposalDetailResponse.CustomerSummary::fullName)
                .containsExactly("Jane Doe", "John Doe");
    }

    @Test
    void shouldUpdateProposalWhenIdExists() {
        Proposal proposal = newProposal(1L, "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        ProposalUpdateRequest request = new ProposalUpdateRequest(
                ProposalStatus.APPROVED, LocalDate.now(), BigDecimal.ONE);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        ProposalResponse response = proposalService.update(1L, request);

        assertThat(response.status()).isEqualTo(ProposalStatus.APPROVED);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUpdatingMissingProposal() {
        ProposalUpdateRequest request = new ProposalUpdateRequest(
                ProposalStatus.APPROVED, LocalDate.now(), BigDecimal.ONE);
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteProposalWhenNoCustomersExist() {
        Proposal proposal = newProposal(1L, "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(customerRepository.existsByProposalId(1L)).thenReturn(false);

        proposalService.delete(1L);

        verify(proposalRepository).delete(proposal);
    }

    @Test
    void shouldThrowResourceInUseExceptionWhenDeletingProposalWithCustomers() {
        Proposal proposal = newProposal(1L, "PN-001", ProposalStatus.DRAFT, LocalDate.now(), BigDecimal.TEN);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(customerRepository.existsByProposalId(1L)).thenReturn(true);

        assertThatThrownBy(() -> proposalService.delete(1L))
                .isInstanceOf(ResourceInUseException.class);
    }

    private Proposal newProposal(Long id, String proposalNo, ProposalStatus status, LocalDate issueDate,
                                  BigDecimal totalPremium) {
        Proposal proposal = new Proposal(proposalNo, status, issueDate, totalPremium);
        ReflectionTestUtils.setField(proposal, "id", id);
        return proposal;
    }

    private Customer newCustomer(Long id, Proposal proposal, String fullName) {
        Customer customer = new Customer(proposal, "12345678901", fullName, "Istanbul", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }
}
