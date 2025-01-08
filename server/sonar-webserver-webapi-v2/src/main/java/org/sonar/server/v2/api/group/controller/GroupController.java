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
package org.sonar.server.v2.api.group.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.group.request.GroupCreateRestRequest;
import org.sonar.server.v2.api.group.request.GroupUpdateRestRequest;
import org.sonar.server.v2.api.group.request.GroupsSearchRestRequest;
import org.sonar.server.v2.api.group.response.GroupRestResponse;
import org.sonar.server.v2.api.group.response.GroupsSearchRestResponse;
import org.sonar.server.v2.api.model.RestPage;
import org.springdoc.core.annotations.ParameterObject;
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

import static org.sonar.server.v2.WebApiEndpoints.GROUPS_ENDPOINT;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;

@RequestMapping(GROUPS_ENDPOINT)
@RestController
public interface GroupController {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Group search", description = """
      Get the list of groups.
      The results are sorted alphabetically by group name.
    """)
  GroupsSearchRestResponse search(
    @Valid @ParameterObject GroupsSearchRestRequest groupsSearchRestRequest,
    @Valid @ParameterObject RestPage restPage);

  @GetMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Fetch a single group", description = "Fetch a single group.")
  GroupRestResponse fetchGroup(@PathVariable("id") @Parameter(description = "The id of the group to fetch.", required = true, in = ParameterIn.PATH) String id);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new group", description = "Create a new group.")
  GroupRestResponse create(@Valid @RequestBody GroupCreateRestRequest request);

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Deletes a group", description = "Deletes a group.")
  void deleteGroup(@PathVariable("id") @Parameter(description = "The ID of the group to delete.", required = true, in = ParameterIn.PATH) String id);

  @PatchMapping(path = "/{id}", consumes = JSON_MERGE_PATCH_CONTENT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update a group", description = """
    Update a group name or description.
    """)
  GroupRestResponse updateGroup(@PathVariable("id") String id, @Valid @RequestBody GroupUpdateRestRequest updateRequest);
}
