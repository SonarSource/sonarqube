/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.agentic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.agentic.request.AgenticJobsSearchRestRequest;
import org.sonar.server.v2.api.agentic.response.AgenticJobsSearchRestResponse;
import org.sonar.server.v2.api.model.RestPage;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.AGENTIC_JOBS_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(AGENTIC_JOBS_ENDPOINT)
@RestController
@Tag(name = "Agentic")
public interface AgenticJobsController {

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Search agentic jobs", description = """
    List agentic orchestration jobs (Hunter and Remediation agents), most recent first.
    Requires system administration permission.
    """)
  AgenticJobsSearchRestResponse search(
    @Valid @ParameterObject AgenticJobsSearchRestRequest agenticJobsSearchRestRequest,
    @Valid @ParameterObject RestPage restPage);
}
