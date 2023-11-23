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
package org.sonar.server.v2.api.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import javax.validation.Valid;
import org.sonar.server.v2.api.membership.request.GroupMembershipCreateRestRequest;
import org.sonar.server.v2.api.membership.request.GroupsMembershipSearchRestRequest;
import org.sonar.server.v2.api.membership.response.GroupsMembershipSearchRestResponse;
import org.sonar.server.v2.api.membership.response.GroupMembershipRestResponse;
import org.sonar.server.v2.api.model.RestPage;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.GROUP_MEMBERSHIPS_ENDPOINT;

@RequestMapping(GROUP_MEMBERSHIPS_ENDPOINT)
@RestController
public interface GroupMembershipController {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Search across group memberships", description = """
      Get the list of groups and members matching the query.
    """)
  GroupsMembershipSearchRestResponse search(
    @Valid @ParameterObject GroupsMembershipSearchRestRequest groupsSearchRestRequest,
    @Valid @ParameterObject RestPage restPage);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add a group membership", description = "Add a user to a group.")
  GroupMembershipRestResponse create(@Valid @RequestBody GroupMembershipCreateRestRequest request);

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove a group membership", description = "Remove a user from a group")
  void delete(@PathVariable("id") @Parameter(description = "The ID of the group membership to delete.", required = true, in = ParameterIn.PATH) String id);


}
