package com.furkan.democrudapi.controller;

import com.furkan.democrudapi.service.ExplainService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/internal/explain")
public class InternalExplainController {

    private final ExplainService explainService;

    public InternalExplainController(ExplainService explainService) {
        this.explainService = explainService;
    }

    @PostMapping
    public ResponseEntity<String> explain(HttpServletRequest request) throws IOException {
        String sql = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        String plan = explainService.explain(sql);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(plan);
    }
}
