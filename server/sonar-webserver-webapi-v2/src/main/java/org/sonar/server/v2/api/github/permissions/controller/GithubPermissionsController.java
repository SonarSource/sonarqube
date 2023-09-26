/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.github.permissions.controller;

import io.swagger.v3.oas.annotations.Operation;
import javax.validation.Valid;
import org.sonar.server.v2.WebApiEndpoints;
import org.sonar.server.v2.api.github.permissions.model.RestGithubPermissionsMapping;
import org.sonar.server.v2.api.github.permissions.request.GithubPermissionMappingUpdateRequest;
import org.sonar.server.v2.api.github.permissions.response.GithubPermissionsMappingRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;

@RequestMapping(WebApiEndpoints.GITHUB_PERMISSIONS_ENDPOINT)
@RestController
public interface GithubPermissionsController {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Fetch the GitHub permissions mapping", description = "Requires Administer System permission.")
  GithubPermissionsMappingRestResponse fetchAll();

  @PatchMapping(path = "/{githubRole}", consumes = JSON_MERGE_PATCH_CONTENT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update a single Github permission mapping", description = "Update a single Github permission mapping")
  RestGithubPermissionsMapping updateMapping(@PathVariable("githubRole") String githubRole, @Valid @RequestBody GithubPermissionMappingUpdateRequest request);

  @DeleteMapping(path = "/{githubRole}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a single Github permission mapping", description = "Delete a single Github permission mapping")
  void deleteMapping(@PathVariable("githubRole") String githubRole);

}
