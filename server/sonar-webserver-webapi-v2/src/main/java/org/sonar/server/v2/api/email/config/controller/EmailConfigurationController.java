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
package org.sonar.server.v2.api.email.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import javax.validation.Valid;
import org.sonar.server.v2.api.email.config.request.EmailConfigurationCreateRestRequest;
import org.sonar.server.v2.api.email.config.request.EmailConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationResource;
import org.sonar.server.v2.api.email.config.response.EmailConfigurationSearchRestResponse;
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

import static org.sonar.server.v2.WebApiEndpoints.EMAIL_CONFIGURATION_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;

@RequestMapping(EMAIL_CONFIGURATION_ENDPOINT)
@RestController
public interface EmailConfigurationController {

  @PostMapping
  @Operation(summary = "Create an email configuration", description = """
      Create a new email configuration.
      Note that only a single configuration can exist at a time.
      Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  EmailConfigurationResource createEmailConfiguration(@Valid @RequestBody EmailConfigurationCreateRestRequest createRequest);

  @GetMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Fetch an email configuration", description = """
    Fetch a Email configuration. Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  EmailConfigurationResource getEmailConfiguration(
    @PathVariable("id") @Parameter(description = "The id of the configuration to fetch.", required = true, in = ParameterIn.PATH) String id);

  @GetMapping
  @Operation(summary = "Search email configurations", description = """
      Get the list of email configurations.
      Note that a single configuration is supported at this time.
      Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  EmailConfigurationSearchRestResponse searchEmailConfigurations();

  @PatchMapping(path = "/{id}", consumes = JSON_MERGE_PATCH_CONTENT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update an email configuration", description = """
    Update an email configuration. Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  EmailConfigurationResource updateEmailConfiguration(@PathVariable("id") String id, @Valid @RequestBody EmailConfigurationUpdateRestRequest updateRequest);

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete an email configuration", description = """
    Delete an email configuration.
    Requires 'Administer System' permission.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  void deleteEmailConfiguration(
    @PathVariable("id") @Parameter(description = "The id of the configuration to delete.", required = true, in = ParameterIn.PATH) String id);

}
