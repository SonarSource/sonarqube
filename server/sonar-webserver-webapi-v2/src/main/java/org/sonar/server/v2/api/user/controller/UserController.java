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
import javax.validation.Valid;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.user.request.UsersSearchRestRequest;
import org.sonar.server.v2.api.user.response.UsersSearchRestResponse;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.USER_ENDPOINT;

@RequestMapping(USER_ENDPOINT)
@RestController
public interface UserController {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Users search", description = """
      Get a list of users. By default, only active users are returned.
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
  UsersSearchRestResponse search(@ParameterObject UsersSearchRestRequest usersSearchRestRequest, @Valid @ParameterObject RestPage restPage);
}
