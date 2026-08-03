package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.ForeignKeyResponse;
import com.furkan.democrudapi.service.SchemaMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/schema")
@Tag(name = "Internal — schema", description = "Database schema metadata consumed by the N+1 analysis")
public class InternalSchemaController {

    private final SchemaMetadataService schemaMetadataService;

    public InternalSchemaController(SchemaMetadataService schemaMetadataService) {
        this.schemaMetadataService = schemaMetadataService;
    }

    /**
     * Foreign key relations of the application schema, read from live database metadata.
     * Takes no parameters: the whole schema is small and bounded, and the analysis agent
     * needs the complete graph.
     */
    @Operation(summary = "List the foreign key relations of the application schema")
    @GetMapping("/foreign-keys")
    public List<ForeignKeyResponse> foreignKeys() {
        return schemaMetadataService.foreignKeys();
    }
}
