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
package org.sonar.server.v2.api.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import javax.validation.Valid;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.user.response.UserRestResponse;
import org.sonar.server.v2.api.user.request.UserCreateRestRequest;
import org.sonar.server.v2.api.user.request.UserUpdateRestRequest;
import org.sonar.server.v2.api.user.request.UsersSearchRestRequest;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.sonar.server.v2.WebApiEndpoints.USER_ENDPOINT;

@RequestMapping(USER_ENDPOINT)
@RestController
public interface UserController {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Users search", description = """
      Get a list of users. By default, only active users are returned.
      The following fields are only returned when user has Administer System permission or for logged-in in user :
        'email',
        'externalIdentity',
        'externalProvider',
        'groups',
        'lastConnectionDate',
        'sonarLintLastConnectionDate',
        'tokensCount'.
      Field 'sonarqubeLastConnectionDate' is only updated every hour, so it may not be accurate, for instance when a user authenticates many times in less than one hour.
      The results are sorted alphabetically by login.
    """)
  UsersSearchRestResponse search(
    @Valid @ParameterObject UsersSearchRestRequest usersSearchRestRequest,
    @RequestParam(name = "groupId!") @Nullable @Schema(description = "Filter users not belonging to group. Only available for system administrators.",
      extensions = @Extension(properties = {@ExtensionProperty(name = "internal", value = "true")}), hidden = true) String excludedGroupId,
    @Valid @ParameterObject RestPage restPage);

  @DeleteMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Deactivate a user", description = "Deactivates a user. Requires Administer System permission.")
  void deactivate(
    @PathVariable("id") @Parameter(description = "The ID of the user to delete.", required = true, in = ParameterIn.PATH) String id,
    @RequestParam(value = "anonymize", required = false, defaultValue = "false") @Parameter(description = "Anonymize user in addition to deactivating it.") Boolean anonymize);

  @GetMapping(path = "/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Fetch a single user", description = """
    Fetch a single user.
    The following fields are only returned when user has Administer System permission or for logged-in in user :
        'email'
        'externalIdentity'
        'externalProvider'
        'groups'
        'lastConnectionDate'
        'sonarLintLastConnectionDate'
        'tokensCount'
      Field 'sonarqubeLastConnectionDate' is only updated every hour, so it may not be accurate, for instance when a user authenticates many times in less than one hour.
    """)
  UserRestResponse fetchUser(@PathVariable("id") @Parameter(description = "The id of the user to fetch.", required = true, in = ParameterIn.PATH) String id);

  @PatchMapping(path = "/{id}", consumes = JSON_MERGE_PATCH_CONTENT_TYPE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update a user", description = """
    Update a user.
    Allows updating user's name, email and SCM accounts.
    """)
  UserRestResponse updateUser(@PathVariable("id") String id, @Valid @RequestBody UserUpdateRestRequest updateRequest);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "User creation", description = """
      Create a user.
      If a deactivated user account exists with the given login, it will be reactivated.
      Requires Administer System permission.
    """)
  UserRestResponse create(@Valid @RequestBody UserCreateRestRequest userCreateRestRequest);

}
