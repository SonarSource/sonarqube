/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.v2.api.github.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import javax.validation.Valid;
import org.sonar.server.v2.api.github.config.request.GithubConfigurationCreateRestRequest;
import org.sonar.server.v2.api.github.config.request.GithubConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.github.config.resource.GithubConfigurationResource;
import org.sonar.server.v2.api.github.config.response.GithubConfigurationSearchRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.GITHUB_CONFIGURATION_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;

@RequestMapping(GITHUB_CONFIGURATION_ENDPOINT)
@RestController
public interface GithubConfigurationController {

  @GetMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Fetch a GitHub configuration", description = """
    Fetch a GitHub configuration. Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GithubConfigurationResource getGithubConfiguration(
    @PathVariable("id") @Parameter(description = "The id of the configuration to fetch.", required = true, in = ParameterIn.PATH) String id);

  @GetMapping
  @Operation(summary = "Search GitHub configs", description = """
      Get the list of GitHub configurations.
      Note that a single configuration is supported at this time.
      Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GithubConfigurationSearchRestResponse searchGithubConfiguration();

  @PatchMapping(path = "/{id}", consumes = JSON_MERGE_PATCH_CONTENT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update a GitHub configuration", description = """
    Update a GitHub configuration. Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GithubConfigurationResource updateGithubConfiguration(@PathVariable("id") String id, @Valid @RequestBody GithubConfigurationUpdateRestRequest updateRequest);

  @PostMapping
  @Operation(summary = "Create GitHub configuration", description = """
      Create a new GitHub configuration.
      Note that only a single configuration can exist at a time.
      Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  GithubConfigurationResource createGithubConfiguration(@Valid @RequestBody GithubConfigurationCreateRestRequest createRequest);

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a GitHub configuration", description = """
    Delete a GitHub configuration.
    Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  void deleteGithubConfiguration(
    @PathVariable("id") @Parameter(description = "The id of the configuration to delete.", required = true, in = ParameterIn.PATH) String id);

}
