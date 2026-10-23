/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.sonar.server.v2.api.projects.request.ProjectsSearchRestRequest;
import org.sonar.server.v2.api.projects.response.ProjectsSearchRestResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.PROJECTS_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(PROJECTS_ENDPOINT)
@Tag(name = "Components")
public interface ProjectsController {

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "Get projects", description = """
    Get projects using query parameters. At least one of ids or keys is required.
    Filters are combined with AND; tags match any supplied tag.
    Only projects for which the caller has Browse permission are returned.
    Results are ordered by creation time, then project ID, in ascending order.
    Set pageSize to 0 to return only the total number of matching projects.
    """)
  ProjectsSearchRestResponse getProjects(@Valid @ParameterObject ProjectsSearchRestRequest request);
}
