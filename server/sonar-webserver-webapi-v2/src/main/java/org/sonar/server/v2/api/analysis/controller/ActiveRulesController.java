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
package org.sonar.server.v2.api.analysis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.sonar.server.rule.ActiveRuleRestReponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.ACTIVE_RULES_ENDPOINT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(value = ACTIVE_RULES_ENDPOINT, produces = APPLICATION_JSON_VALUE)
@ResponseStatus(OK)
@RestController
public interface ActiveRulesController {

  String PROJECT_KEY_PARAM_DESCRIPTION = "Project Key";

  @GetMapping
  @Operation(summary = "Get all active rules for a specific project", description = "Used by the scanner-engine to get all active rules for a given project.")
  List<ActiveRuleRestReponse.ActiveRule> getActiveRules(@RequestParam(value = "projectKey") @Parameter(description = PROJECT_KEY_PARAM_DESCRIPTION) String projectKey);

}
