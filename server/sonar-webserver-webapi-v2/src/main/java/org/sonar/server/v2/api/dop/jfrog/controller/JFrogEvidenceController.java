/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubeStatement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH;
import static org.sonar.server.v2.WebApiEndpoints.JFROG_EVIDENCE_ENDPOINT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(value = JFROG_EVIDENCE_ENDPOINT, produces = APPLICATION_JSON_VALUE)
@ResponseStatus(OK)
@RestController
@Tag(name = "DevOps Platform Integration")
public interface JFrogEvidenceController {

  String GET_EVIDENCE_SUMMARY = "Get JFrog evidence for a Compute Engine task";
  String GET_EVIDENCE_DESCRIPTION = """
    Returns a JFrog evidence statement for the specified Compute Engine task.
    The evidence contains quality gate status and conditions in the in-toto Statement format.
    Requires 'Browse' permission on the project associated with the task.""";
  String TASK_ID_DESCRIPTION = "The UUID of the Compute Engine task";

  @GetMapping(value = "/{taskId}")
  @Operation(summary = GET_EVIDENCE_SUMMARY, description = GET_EVIDENCE_DESCRIPTION)
  SonarQubeStatement getEvidence(
    @PathVariable(value = "taskId") @Parameter(description = TASK_ID_DESCRIPTION, required = true, in = PATH) String taskId);

}
