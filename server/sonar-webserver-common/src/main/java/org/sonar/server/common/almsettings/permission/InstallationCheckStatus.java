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
 * Whether the per-installation part of a GitHub permission check produced a trustworthy answer.
 *
 * <p>Separate from {@link PermissionCheckStatus}, which reports the verdict itself: the app configuration can be
 * checked successfully while the installation scan fails, and the two findings call for different actions from an
 * administrator (SONAR-32166).
 */
public enum InstallationCheckStatus {
  /** Every installation page was read; the affected count and list are exhaustive. */
  COMPLETE,
  /** The scan could not be completed, so no count or list can be trusted — including an empty one. */
  FAILED,
  /**
   * Project checks only: the app is not installed on that project's repository. A definite answer, not a failure —
   * there is no installation whose permissions could be approved until the app is installed there.
   */
  NOT_INSTALLED,
  /** No installation scan was attempted: the platform is not GitHub, or the check stopped before reaching it. */
  NOT_RUN
}
