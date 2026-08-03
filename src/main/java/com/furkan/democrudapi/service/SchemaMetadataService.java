package com.furkan.democrudapi.service;

import com.furkan.democrudapi.config.SchemaMetadataProperties;
import com.furkan.democrudapi.dto.ForeignKeyResponse;
import com.furkan.democrudapi.repository.ForeignKeyMetadataRepository;
import com.furkan.democrudapi.repository.ForeignKeyMetadataRepository.ForeignKeyRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns raw JDBC foreign key metadata into the response contract: lowercase identifiers,
 * infrastructure tables removed, deterministic order.
 */
@Service
@Transactional(readOnly = true)
public class SchemaMetadataService {

    /**
     * Sorted by child table so callers see the owning side first; then by constraint and column
     * position, which keeps the columns of a composite key together and in declared order.
     */
    private static final Comparator<ForeignKeyRow> RESPONSE_ORDER = Comparator
            .comparing(ForeignKeyRow::childTable)
            .thenComparing(ForeignKeyRow::constraintName, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(ForeignKeyRow::keySequence);

    private final ForeignKeyMetadataRepository foreignKeyMetadataRepository;
    private final SchemaMetadataProperties properties;

    public SchemaMetadataService(ForeignKeyMetadataRepository foreignKeyMetadataRepository,
            SchemaMetadataProperties properties) {
        this.foreignKeyMetadataRepository = foreignKeyMetadataRepository;
        this.properties = properties;
    }

    public List<ForeignKeyResponse> foreignKeys() {
        Set<String> excluded = lowercaseAll(properties.excludedTables());

        return foreignKeyMetadataRepository.findImportedKeys().stream()
                .map(this::normalize)
                .filter(row -> !excluded.contains(row.childTable()) && !excluded.contains(row.parentTable()))
                .sorted(RESPONSE_ORDER)
                .map(row -> new ForeignKeyResponse(row.childTable(), row.childColumn(),
                        row.parentTable(), row.parentColumn()))
                .toList();
    }

    private ForeignKeyRow normalize(ForeignKeyRow row) {
        return new ForeignKeyRow(lowercase(row.childTable()), lowercase(row.childColumn()),
                lowercase(row.parentTable()), lowercase(row.parentColumn()),
                row.constraintName(), row.keySequence());
    }

    private Set<String> lowercaseAll(Set<String> names) {
        if (names == null) {
            return Set.of();
        }
        return names.stream().map(this::lowercase).collect(Collectors.toSet());
    }

    /**
     * Always folds with {@link Locale#ROOT}. Under a Turkish default locale {@code "ID"} would
     * otherwise become {@code "ıd"} and silently corrupt every identifier containing an I.
     */
    private String lowercase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}