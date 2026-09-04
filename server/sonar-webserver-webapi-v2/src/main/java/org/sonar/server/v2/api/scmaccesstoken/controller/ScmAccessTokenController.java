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
package org.sonar.server.v2.api.scmaccesstoken.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sonar.server.v2.api.scmaccesstoken.response.ScmAccessTokenRestResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.SCM_ACCESS_TOKEN_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * DOP-agnostic successor to {@code GithubInstallationTokenController} (SONAR-31165) — provides a
 * scoped git credential for whichever DevOps Platform the project is bound to, dispatched server-side.
 * GitHub credentials are short-lived and minted per call; GitLab credentials are cached in memory per
 * SonarQube Server node and refreshed before expiry. Additive:
 * {@code github-installation-tokens} is left untouched.
 */
@RequestMapping(SCM_ACCESS_TOKEN_ENDPOINT)
@RestController
@Tag(name = "Dop Translation")
public interface ScmAccessTokenController {

  @PostMapping(produces = APPLICATION_JSON_VALUE)
  @Operation(operationId = "generateScmAccessToken", summary = "Provide a scoped SCM access token", description = """
    Provide a scoped git credential for the given project's bound DevOps Platform (GitHub or GitLab).
    GitHub credentials are short-lived and minted per call; GitLab credentials are cached in memory per
    SonarQube Server node and refreshed before expiry. Internal endpoint used by the agentic-workflows
    remediation orchestrator (SONAR-31165).
    Requires the 'Administer System' permission or trusted privileged-service authentication.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  ScmAccessTokenRestResponse generateScmAccessToken(
    @RequestParam("project") @Parameter(description = "Project key", required = true) String projectKey);

}
