package com.furkan.democrudapi.internal;

import com.furkan.democrudapi.config.BugProperties;
import com.furkan.democrudapi.dto.BugToggleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/toggle")
public class InternalToggleController {

    private static final Logger log = LoggerFactory.getLogger(InternalToggleController.class);

    private final BugProperties bugProperties;

    public InternalToggleController(BugProperties bugProperties) {
        this.bugProperties = bugProperties;
    }

    @GetMapping
    public BugToggleResponse current() {
        return new BugToggleResponse(bugProperties.isNPlusOne());
    }

    @PostMapping("/n-plus-one")
    public BugToggleResponse setNPlusOne(@RequestParam boolean enabled) {
        bugProperties.setNPlusOne(enabled);
        log.info("Toggled bug flag n-plus-one to {}", enabled);
        return new BugToggleResponse(bugProperties.isNPlusOne());
    }
}