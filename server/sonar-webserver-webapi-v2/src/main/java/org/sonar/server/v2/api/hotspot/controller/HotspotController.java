package org.sonar.server.v2.api.hotspot.controller;

import static org.sonar.server.v2.WebApiEndpoints.HOTSPOT_EXPIRE_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import org.sonar.server.v2.api.hotspot.response.ExpireHotspotsRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(HOTSPOT_EXPIRE_ENDPOINT)
@RestController
public interface HotspotController {

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Expire security hotspot exceptions", description = """
            Expire security hotspot exceptions that have reached their expiry date.
            Automatically transitions expired hotspots from EXCEPTION status back to TO_REVIEW status. Requires 'Administer System' permission.
            """, extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
    ExpireHotspotsRestResponse expireHotspots();
}