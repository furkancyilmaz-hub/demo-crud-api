package com.furkan.democrudapi.dto;

/**
 * One foreign key column pair: {@code childTable.childColumn} references
 * {@code parentTable.parentColumn}. A composite foreign key yields one entry per column pair.
 */
public record ForeignKeyResponse(String childTable, String childColumn,
                                 String parentTable, String parentColumn) {
}
