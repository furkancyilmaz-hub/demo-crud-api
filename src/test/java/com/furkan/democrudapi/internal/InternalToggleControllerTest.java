package com.furkan.democrudapi.internal;

import com.furkan.democrudapi.config.BugProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalToggleController.class)
@Import(BugProperties.class)
class InternalToggleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BugProperties bugProperties;

    @Test
    void shouldReturnCurrentFlagState() throws Exception {
        bugProperties.setNPlusOne(true);

        mockMvc.perform(get("/internal/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nPlusOne").value(true));
    }

    @Test
    void shouldDisableNPlusOneWhenToggledOff() throws Exception {
        bugProperties.setNPlusOne(true);

        mockMvc.perform(post("/internal/toggle/n-plus-one").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nPlusOne").value(false));

        assertThat(bugProperties.isNPlusOne()).isFalse();
    }

    @Test
    void shouldEnableNPlusOneWhenToggledOn() throws Exception {
        bugProperties.setNPlusOne(false);

        mockMvc.perform(post("/internal/toggle/n-plus-one").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nPlusOne").value(true));

        assertThat(bugProperties.isNPlusOne()).isTrue();
    }
}