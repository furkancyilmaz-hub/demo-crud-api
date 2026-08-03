package com.furkan.democrudapi.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads foreign key definitions straight from JDBC metadata, so the result follows the live
 * schema instead of a list maintained in code. Nothing is cached: adding a constraint to the
 * database must show up on the next call.
 *
 * <p>Tables are enumerated first and {@link DatabaseMetaData#getImportedKeys} is then called per
 * table. The JDBC contract does not allow a null table name there; the PostgreSQL driver happens
 * to accept one and would answer in a single round trip, but that is driver-specific behaviour
 * and this schema has few enough tables for the loop to be free.
 */
@Repository
public class ForeignKeyMetadataRepository {

    private static final Logger log = LoggerFactory.getLogger(ForeignKeyMetadataRepository.class);

    /** Used only when the connection reports no current schema. */
    private static final String DEFAULT_SCHEMA = "public";

    private static final String[] TABLE_TYPES = {"TABLE"};
    private static final String ALL_TABLES_PATTERN = "%";

    private final JdbcTemplate jdbcTemplate;

    public ForeignKeyMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * All foreign keys declared within the current schema, as the database reports them —
     * unfiltered, unsorted and in the database's own letter case.
     */
    public List<ForeignKeyRow> findImportedKeys() {
        return jdbcTemplate.execute((ConnectionCallback<List<ForeignKeyRow>>) connection -> {
            String schema = resolveSchema(connection.getSchema());
            log.debug("Reading foreign keys of schema={}", schema);

            DatabaseMetaData metaData = connection.getMetaData();
            List<ForeignKeyRow> rows = new ArrayList<>();
            for (String table : readTableNames(metaData, schema)) {
                appendImportedKeys(metaData, schema, table, rows);
            }
            return rows;
        });
    }

    private String resolveSchema(String currentSchema) {
        return currentSchema == null || currentSchema.isBlank() ? DEFAULT_SCHEMA : currentSchema;
    }

    private List<String> readTableNames(DatabaseMetaData metaData, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, schema, ALL_TABLES_PATTERN, TABLE_TYPES)) {
            while (rs.next()) {
                // The schema argument is a pattern, so a name containing '_' would also match
                // sibling schemas. Keep only exact matches.
                if (schema.equals(rs.getString("TABLE_SCHEM"))) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        return tables;
    }

    private void appendImportedKeys(DatabaseMetaData metaData, String schema, String table,
            List<ForeignKeyRow> rows) throws SQLException {
        try (ResultSet rs = metaData.getImportedKeys(null, schema, table)) {
            while (rs.next()) {
                // A cross-schema reference would otherwise leak a table outside the
                // application schema into the response.
                if (!schema.equals(rs.getString("PKTABLE_SCHEM"))) {
                    continue;
                }
                rows.add(new ForeignKeyRow(
                        rs.getString("FKTABLE_NAME"),
                        rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKTABLE_NAME"),
                        rs.getString("PKCOLUMN_NAME"),
                        rs.getString("FK_NAME"),
                        rs.getShort("KEY_SEQ")));
            }
        }
    }

    /**
     * One column pair of one foreign key constraint.
     *
     * @param keySequence position of this column within the constraint, 1-based
     */
    public record ForeignKeyRow(String childTable, String childColumn, String parentTable,
                                String parentColumn, String constraintName, short keySequence) {
    }
}