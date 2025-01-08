/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.v2.api.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.LIVENESS_ENDPOINT;

@RequestMapping(LIVENESS_ENDPOINT)
@RestController
public interface LivenessController {

  @GetMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Provide liveness of SonarQube, meant to be used as a liveness probe on Kubernetes", description = """
      Require 'Administer System' permission or authentication with passcode.

      When SonarQube is fully started, liveness check for database connectivity, Compute Engine status, and, except for DataCenter Edition, if ElasticSearch is Green or Yellow.

      When SonarQube is on Safe Mode (for example when a database migration is running), liveness check only for database connectivity
    """)
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "This SonarQube node is alive"),
    @ApiResponse(description = "This SonarQube node is not alive and should be rescheduled"),
  })
  void livenessCheck(
    @Parameter(description = "Passcode can be provided, see SonarQube documentation") @RequestHeader(value = "X-Sonar-Passcode", required = false) String requestPassCode);
}
