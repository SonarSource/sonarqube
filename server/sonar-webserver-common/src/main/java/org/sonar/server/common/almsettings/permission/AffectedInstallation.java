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
package org.sonar.server.common.almsettings.permission;

import java.util.List;

/**
 * A GitHub App installation that has not granted every permission the Remediation Agent requires.
 *
 * <p>Nothing here assumes what an installation is owned by: {@link #installationOwner()} is an organization on one
 * installation and a personal account on the next, and both are reported the same way. A project bound to a repository
 * under a personal account is blocked by that installation just as an organization's project would be.
 *
 * <p>An installation appears here only for permissions the app itself requests. A permission the app was never
 * configured to request cannot be approved on the installation settings page, so it is reported once, against the app,
 * and never repeated against every installation. Suspension is not a permission deficit either, and does not on its
 * own put an installation in this list.
 *
 * @param installationId     GitHub's numeric installation id, as a string — it is an opaque identifier to API callers
 * @param installationOwner  login of whoever the app is installed on
 * @param settingsUrl        the installation's settings page, where the missing permissions can be approved
 * @param missingPermissions the deficits to approve, in a stable order
 */
public record AffectedInstallation(String installationId, String installationOwner, String settingsUrl, List<PermissionDeficit> missingPermissions) {

  public AffectedInstallation {
    missingPermissions = List.copyOf(missingPermissions);
  }
}
