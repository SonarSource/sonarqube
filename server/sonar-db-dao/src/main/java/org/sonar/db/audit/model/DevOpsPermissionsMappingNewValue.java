/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.audit.model;

import com.google.common.annotations.VisibleForTesting;

public class DevOpsPermissionsMappingNewValue extends NewValue {

  @VisibleForTesting
  public static final String ALL_PERMISSIONS = "all";
  private final String devOpsPlatform;
  private final String githubRole;
  private final String sonarqubePermission;

  public DevOpsPermissionsMappingNewValue(String devOpsPlatform, String githubRole, String sonarqubePermission) {
    this.devOpsPlatform = devOpsPlatform;
    this.githubRole = githubRole;
    this.sonarqubePermission = sonarqubePermission;
  }

  public static DevOpsPermissionsMappingNewValue withAllPermissions(String devOpsPlatform, String githubRole) {
    return new DevOpsPermissionsMappingNewValue(devOpsPlatform, githubRole, ALL_PERMISSIONS);
  }

  @VisibleForTesting
  public String getGithubRole() {
    return githubRole;
  }

  @VisibleForTesting
  public String getSonarqubePermission() {
    return sonarqubePermission;
  }

  @VisibleForTesting
  public String getDevOpsPlatform() {
    return devOpsPlatform;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"devOpsPlatform\": ", this.devOpsPlatform, true);
    addField(sb, "\"devOpsRole\": ", this.githubRole, true);
    addField(sb, "\"sonarqubePermissions\": ", this.sonarqubePermission, true);
    endString(sb);
    return sb.toString();
  }

}
