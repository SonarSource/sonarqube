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
package org.sonar.server.v2.api.dop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Nullable;
import org.sonar.server.v2.api.dop.response.PermissionChecksRestResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.INTERNAL;
import static org.sonar.server.v2.WebApiEndpoints.PERMISSION_CHECKS_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(PERMISSION_CHECKS_ENDPOINT)
@RestController
@Tag(name = "Dop Translation")
public interface PermissionChecksController {

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @Operation(operationId = "checkDopPermissions", summary = "Check DevOps Platform permissions for the Remediation Agent", description = """
    Validates whether the configured DevOps Platforms (GitHub, GitLab, Azure DevOps) grant the write permissions the
    SonarQube Remediation Agent needs to clone a repository, push a branch and open a pull/merge request. Results are
    cached for a short time. Internal endpoint used by the Remediation Agent UI.

    Without 'project' it checks configurations instance-wide and requires the 'Administer System' permission or
    trusted privileged-service authentication; 'configuration' narrows that to a single configuration. With 'project'
    it checks the platform bound to that project and requires 'Browse' permission on the project; a GitHub project is
    checked against the installation covering its own repository, so two projects sharing one configuration can get
    different results. 'project' and 'configuration' cannot be combined.

    For GitHub, the response also reports permissions the app itself is not configured to request, and — for
    administrator requests — every installation that has not approved the permissions it does request, whether an
    organization or a personal account owns it. The counts and list are valid only when 'installationCheckStatus' is
    COMPLETE. If the scan cannot finish, 'installationCheckStatus' is FAILED and 'status' is CHECK_FAILED. The
    response does not include the counts or list.
    """,
    extensions = @Extension(properties = {@ExtensionProperty(name = INTERNAL, value = "true")}))
  PermissionChecksRestResponse checkPermissions(
    @RequestParam(value = "project", required = false) @Parameter(description = "Key of the project whose bound DevOps Platform should be checked") @Nullable String projectKey,

    @RequestParam(value = "configuration", required = false) @Parameter(description = """
      Key of a single DevOps Platform configuration to check, instead of all of them. Cannot be combined with 'project'.
      """) @Nullable String configurationKey,

    @RequestParam(value = "refresh", required = false, defaultValue = "false") @Parameter(description = """
      Bypass the cache and run a live check, then store its result. Requires 'configuration' and the
      'Administer System' permission.
      """) boolean refresh);

}
