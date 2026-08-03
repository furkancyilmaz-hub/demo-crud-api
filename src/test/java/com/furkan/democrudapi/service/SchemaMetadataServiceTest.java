package com.furkan.democrudapi.service;

import com.furkan.democrudapi.config.SchemaMetadataProperties;
import com.furkan.democrudapi.dto.ForeignKeyResponse;
import com.furkan.democrudapi.repository.ForeignKeyMetadataRepository;
import com.furkan.democrudapi.repository.ForeignKeyMetadataRepository.ForeignKeyRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaMetadataServiceTest {

    @Mock
    private ForeignKeyMetadataRepository foreignKeyMetadataRepository;

    private SchemaMetadataService schemaMetadataService;

    @BeforeEach
    void setUp() {
        schemaMetadataService = new SchemaMetadataService(foreignKeyMetadataRepository,
                new SchemaMetadataProperties(Set.of("app_log")));
    }

    @Test
    void shouldReturnOneEntryPerForeignKeyColumn() {
        givenRows(row("customer", "proposal_id", "proposal", "id", "fk_customer_proposal", 1),
                row("payment", "customer_id", "customer", "id", "fk_payment_customer", 1));

        assertThat(schemaMetadataService.foreignKeys()).containsExactly(
                new ForeignKeyResponse("customer", "proposal_id", "proposal", "id"),
                new ForeignKeyResponse("payment", "customer_id", "customer", "id"));
    }

    @Test
    void shouldLowercaseTableAndColumnNames() {
        givenRows(row("CUSTOMER", "Proposal_Id", "Proposal", "ID", "FK_CUSTOMER_PROPOSAL", 1));

        assertThat(schemaMetadataService.foreignKeys()).containsExactly(
                new ForeignKeyResponse("customer", "proposal_id", "proposal", "id"));
    }

    @Test
    void shouldLowercaseWithRootLocaleRegardlessOfDefaultLocale() {
        // Under a Turkish default locale a bare toLowerCase() turns "ID" into "ıd".
        givenRows(row("IDENTITY", "ID", "PARENT", "ID", "fk_identity_parent", 1));

        assertThat(schemaMetadataService.foreignKeys()).containsExactly(
                new ForeignKeyResponse("identity", "id", "parent", "id"));
    }

    @Test
    void shouldExcludeInfrastructureTables() {
        givenRows(row("app_log", "customer_id", "customer", "id", "fk_app_log_customer", 1),
                row("customer", "log_id", "APP_LOG", "id", "fk_customer_app_log", 1),
                row("payment", "customer_id", "customer", "id", "fk_payment_customer", 1));

        assertThat(schemaMetadataService.foreignKeys()).containsExactly(
                new ForeignKeyResponse("payment", "customer_id", "customer", "id"));
    }

    @Test
    void shouldOrderByChildTableThenConstraintThenKeySequence() {
        // The driver orders by parent table, which would put payment first.
        givenRows(row("payment", "customer_id", "customer", "id", "fk_payment_customer", 1),
                row("customer", "proposal_id", "proposal", "id", "fk_customer_proposal", 1));

        assertThat(schemaMetadataService.foreignKeys())
                .extracting(ForeignKeyResponse::childTable)
                .containsExactly("customer", "payment");
    }

    @Test
    void shouldKeepCompositeForeignKeyColumnsInKeySequenceOrder() {
        givenRows(row("payment", "period", "invoice", "period", "fk_payment_invoice", 2),
                row("payment", "invoice_no", "invoice", "no", "fk_payment_invoice", 1));

        assertThat(schemaMetadataService.foreignKeys())
                .extracting(ForeignKeyResponse::childColumn)
                .containsExactly("invoice_no", "period");
    }

    @Test
    void shouldReturnEmptyListWhenSchemaHasNoForeignKeys() {
        givenRows();

        assertThat(schemaMetadataService.foreignKeys()).isEmpty();
    }

    private void givenRows(ForeignKeyRow... rows) {
        when(foreignKeyMetadataRepository.findImportedKeys()).thenReturn(List.of(rows));
    }

    private ForeignKeyRow row(String childTable, String childColumn, String parentTable,
            String parentColumn, String constraintName, int keySequence) {
        return new ForeignKeyRow(childTable, childColumn, parentTable, parentColumn,
                constraintName, (short) keySequence);
    }
}
