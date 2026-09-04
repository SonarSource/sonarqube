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

/**
 * Outcome of validating a DevOps Platform configuration against the write permissions the Remediation Agent needs.
 */
public enum PermissionCheckStatus {
  /** All required write permissions are granted. */
  SUFFICIENT,
  /** The configuration is missing at least one required write permission. */
  INSUFFICIENT,
  /** The permissions could not be determined (e.g. the platform does not expose token scopes). */
  UNKNOWN,
  /** The check itself failed (invalid credentials, rate limiting, platform unreachable, ...). */
  CHECK_FAILED,
  /** GitLab only: the credential belongs to a bot user (Project/Group Access Token), which can't be exchanged for the short-lived user token the Remediation Agent needs. */
  UNSUPPORTED_TOKEN_TYPE
}
