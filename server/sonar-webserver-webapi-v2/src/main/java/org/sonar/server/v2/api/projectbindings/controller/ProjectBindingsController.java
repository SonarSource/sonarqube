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
package org.sonar.server.v2.api.projectbindings.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.projectbindings.model.ProjectBinding;
import org.sonar.server.v2.api.projectbindings.request.ProjectBindingsSearchRestRequest;
import org.sonar.server.v2.api.projectbindings.response.ProjectBindingsSearchRestResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.PROJECT_BINDINGS_ENDPOINT;

@RequestMapping(PROJECT_BINDINGS_ENDPOINT)
@RestController
public interface ProjectBindingsController {

  @GetMapping(path = "/{id}")
  @Operation(
    operationId = "getProjectBinding",
    summary = "Fetch a single Project Binding",
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")})
  )
  ProjectBinding getProjectBinding(
    @PathVariable("id") @Parameter(description = "The id of the project-bindings to fetch.", required = true, in = ParameterIn.PATH) String id
  );

  @GetMapping()
  @Operation(
    operationId = "getProjectBindingByProjectId",
    summary = "Search across project bindings",
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")})
  )
  ProjectBindingsSearchRestResponse searchProjectBindings (
    @Valid @ParameterObject ProjectBindingsSearchRestRequest projectBindingsSearchRestRequest,
    @Valid @ParameterObject RestPage restPage
  );

}
