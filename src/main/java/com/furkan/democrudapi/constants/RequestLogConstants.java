package com.furkan.democrudapi.constants;

import java.util.List;

public class RequestLogConstants {

    /**
     * Prefixes of the API documentation endpoints. Opening the Swagger page fires a dozen
     * asset requests on top of the spec itself; recorded, they would drown the real traffic
     * the analysis agent looks at.
     */
    public static final List<String> DOC_PATH_PREFIXES = List.of("/swagger-ui", "/v3/api-docs");

    /**
     * Marker of the single log line written when a request completes. The format is fixed —
     * the analysis agent parses it out of app_log, so it must not change.
     * Example: {@code REQUEST_COMPLETED method=GET path=/customers status=200 durationMs=1180}
     */
    public static final String REQUEST_COMPLETED_PREFIX = "REQUEST_COMPLETED";

    /** Requests below this path are not recorded: the agent's own queries would pollute the result. */
    public static final String INTERNAL_PATH_PREFIX = "/internal/";

    private RequestLogConstants() {
    }
}
