/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.projects.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.projects.request.BoundProjectCreateRestRequest;
import org.sonar.server.v2.api.projects.response.BoundProjectCreateRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.BOUND_PROJECTS_ENDPOINT;

@RequestMapping(BOUND_PROJECTS_ENDPOINT)
@RestController
@Tag(name = "Dop Translation")
public interface BoundProjectsController {

  @PostMapping
  @Operation(
    summary = "Create a SonarQube project with the information from the provided DevOps platform project.",
    description = """
      Create a SonarQube project with the information from the provided DevOps platform project.
      Autoconfigure Pull-Request decoration mechanism.
      Requires the 'Create Projects' permission and setting a Personal Access Token with api/alm_integrations/set_pat for a user who will be using this endpoint
      """,
    responses = {
      @ApiResponse(responseCode = "201", description = "Project successfully created and bound to DevOps platform"),
      @ApiResponse(responseCode = "400", description = "Bad request - personal access token for the DevOps platform setting is missing", content = @Content),
      @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions - requires 'Create Projects' permission", content = @Content),
      @ApiResponse(
        responseCode = "500",
        description = "Internal server error - failed to fetch repository from DevOps platform (e.g., repository not found, invalid PAT, or insufficient permissions)",
        content = @Content)
    })
  @ResponseStatus(HttpStatus.CREATED)
  BoundProjectCreateRestResponse createBoundProject(@Valid @RequestBody BoundProjectCreateRestRequest request);

  @PutMapping
  @Operation(
    summary = "Create or update a bound SonarQube project",
    description = """
      Create a SonarQube project bound to a DevOps platform repository, or update the binding if the project already exists.
      This is an idempotent operation. If the project already exists with the same key, its binding will be updated.
      Autoconfigure Pull-Request decoration mechanism.
      Requires the 'Create Projects' permission and setting a Personal Access Token with api/alm_integrations/set_pat for a user who will be using this endpoint
      """,
    responses = {
      @ApiResponse(responseCode = "200", description = "Project successfully created or binding updated"),
      @ApiResponse(responseCode = "400", description = "Bad request - personal access token for the DevOps platform setting is missing", content = @Content),
      @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions - requires 'Create Projects' permission", content = @Content),
      @ApiResponse(
        responseCode = "500",
        description = "Internal server error - failed to fetch repository from DevOps platform (e.g., repository not found, invalid PAT, or insufficient permissions)",
        content = @Content)
    })
  @ResponseStatus(HttpStatus.OK)
  BoundProjectCreateRestResponse createOrUpdateBoundProject(@Valid @RequestBody BoundProjectCreateRestRequest request);

}
