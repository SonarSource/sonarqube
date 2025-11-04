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
package org.sonar.server.v2.api.gitlab.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.gitlab.config.request.GitlabConfigurationCreateRestRequest;
import org.sonar.server.v2.api.gitlab.config.request.GitlabConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.gitlab.config.resource.GitlabConfigurationResource;
import org.sonar.server.v2.api.gitlab.config.response.GitlabConfigurationSearchRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.GITLAB_CONFIGURATION_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(GITLAB_CONFIGURATION_ENDPOINT)
@RestController
@Tag(name = "Dop Translation")
public interface GitlabConfigurationController {

  @GetMapping(path = "/{id}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Fetch a GitLab configuration", description = """
    Fetch a GitLab configuration. Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GitlabConfigurationResource getGitlabConfiguration(
    @PathVariable("id") @Parameter(description = "The id of the configuration to fetch.", required = true, in = ParameterIn.PATH) String id);

  @GetMapping
  @Operation(summary = "Search GitLab configs", description = """
      Get the list of GitLab configurations.
      Note that a single configuration is supported at this time.
      Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GitlabConfigurationSearchRestResponse searchGitlabConfiguration();

  @PatchMapping(path = "/{id}", consumes = JSON_MERGE_PATCH_CONTENT_TYPE, produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update a Gitlab configuration", description = """
    Update a Gitlab configuration. Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GitlabConfigurationResource updateGitlabConfiguration(@PathVariable("id") String id, @Valid @RequestBody GitlabConfigurationUpdateRestRequest updateRequest);

  @PostMapping
  @Operation(summary = "Create Gitlab configuration", description = """
      Create a new Gitlab configuration.
      Note that only a single configuration can exist at a time.
      Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GitlabConfigurationResource create(@Valid @RequestBody GitlabConfigurationCreateRestRequest createRequest);

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a GitLab configuration", description = """
    Delete a GitLab configuration.
    Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  void deleteGitlabConfiguration(
    @PathVariable("id") @Parameter(description = "The id of the configuration to delete.", required = true, in = ParameterIn.PATH) String id);

}
