package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.dto.ForeignKeyResponse;
import com.furkan.democrudapi.service.SchemaMetadataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/schema")
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
    @GetMapping("/foreign-keys")
    public List<ForeignKeyResponse> foreignKeys() {
        return schemaMetadataService.foreignKeys();
    }
}
