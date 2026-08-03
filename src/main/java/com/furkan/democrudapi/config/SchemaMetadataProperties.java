package com.furkan.democrudapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Set;

/**
 * Tables kept out of the schema metadata endpoints. The foreign key list itself is never
 * hardcoded — it always comes from live database metadata — but which tables count as
 * infrastructure rather than application schema is a policy input, so it lives in config.
 *
 * @param excludedTables table names to drop, matched case-insensitively
 */
@ConfigurationProperties(prefix = "schema-metadata")
public record SchemaMetadataProperties(@DefaultValue("app_log") Set<String> excludedTables) {
}
