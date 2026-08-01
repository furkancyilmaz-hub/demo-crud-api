package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.config.BugProperties;
import com.furkan.democrudapi.dto.BugToggleResponse;
import com.furkan.democrudapi.service.CustomerIndexService;
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
    private final CustomerIndexService customerIndexService;

    public InternalToggleController(BugProperties bugProperties, CustomerIndexService customerIndexService) {
        this.bugProperties = bugProperties;
        this.customerIndexService = customerIndexService;
    }

    @GetMapping
    public BugToggleResponse current() {
        return new BugToggleResponse(bugProperties.isNPlusOne(), bugProperties.isMissingIndex());
    }

    @PostMapping("/n-plus-one")
    public BugToggleResponse setNPlusOne(@RequestParam boolean enabled) {
        bugProperties.setNPlusOne(enabled);
        log.info("Toggled bug flag n-plus-one to {}", enabled);
        return new BugToggleResponse(bugProperties.isNPlusOne(), bugProperties.isMissingIndex());
    }

    @PostMapping("/missing-index")
    public BugToggleResponse setMissingIndex(@RequestParam boolean enabled) {
        customerIndexService.setMissingIndexEnabled(enabled);
        return new BugToggleResponse(bugProperties.isNPlusOne(), bugProperties.isMissingIndex());
    }
}