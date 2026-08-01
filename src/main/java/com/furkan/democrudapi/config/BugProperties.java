package com.furkan.democrudapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Switches for the deliberate performance problems this demo exposes.
 * Fields are mutable on purpose: the flags are read on every request and can be
 * flipped at runtime through {@code /internal/toggle}.
 */
@Component
@ConfigurationProperties(prefix = "bug")
public class BugProperties {

    private boolean nPlusOne;
    private boolean missingIndex;

    public boolean isNPlusOne() {
        return nPlusOne;
    }

    public void setNPlusOne(boolean nPlusOne) {
        this.nPlusOne = nPlusOne;
    }

    public boolean isMissingIndex() {
        return missingIndex;
    }

    public void setMissingIndex(boolean missingIndex) {
        this.missingIndex = missingIndex;
    }
}