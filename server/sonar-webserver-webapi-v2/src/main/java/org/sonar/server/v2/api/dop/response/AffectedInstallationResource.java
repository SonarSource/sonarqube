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
package org.sonar.server.v2.api.dop.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = """
  A GitHub App installation that has not approved every permission the Remediation Agent requires. Only permissions the
  app itself requests appear here; a permission the app does not request is reported once in 'appMissingPermissions'
  instead, because no installation owner can approve it. Suspension alone does not put an installation in this list.
  """)
public record AffectedInstallationResource(
  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Identifier of the GitHub App installation")
  String installationId,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Login of whoever the app is installed on. May be an organization or a personal account.")
  String installationOwner,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "URL of the installation settings page, where the missing permissions can be approved")
  String settingsUrl,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Permissions this installation still has to approve")
  List<PermissionDeficitResource> missingPermissions
) {
}
