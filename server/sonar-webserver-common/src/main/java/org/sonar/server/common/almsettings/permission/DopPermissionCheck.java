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
 * Structured result of a DevOps Platform permission check.
 */
public record DopPermissionCheck(PermissionCheckStatus status) {

  public static DopPermissionCheck sufficient() {
    return new DopPermissionCheck(PermissionCheckStatus.SUFFICIENT);
  }

  public static DopPermissionCheck insufficient() {
    return new DopPermissionCheck(PermissionCheckStatus.INSUFFICIENT);
  }

  public static DopPermissionCheck unknown() {
    return new DopPermissionCheck(PermissionCheckStatus.UNKNOWN);
  }

  public static DopPermissionCheck checkFailed() {
    return new DopPermissionCheck(PermissionCheckStatus.CHECK_FAILED);
  }
}
