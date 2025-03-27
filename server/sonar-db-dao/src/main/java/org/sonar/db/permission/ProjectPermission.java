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
package org.sonar.db.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum ProjectPermission {

  USER("user"),
  ADMIN("admin"),
  CODEVIEWER("codeviewer"),
  ISSUE_ADMIN("issueadmin"),
  SECURITYHOTSPOT_ADMIN("securityhotspotadmin"),
  SCAN("scan");

  /**
   * Permissions which are implicitly available for any user, any group on public projects.
   */
  public static final Set<ProjectPermission> PUBLIC_PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(ProjectPermission.USER, ProjectPermission.CODEVIEWER));

  private final String key;

  ProjectPermission(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  @Override
  public String toString() {
    return key;
  }

  public static ProjectPermission fromKey(String key) {
    for (ProjectPermission p : values()) {
      if (p.getKey().equals(key)) {
        return p;
      }
    }
    throw new IllegalArgumentException("Unsupported project permission: " + key);
  }

  public static boolean contains(String key) {
    return Arrays.stream(values()).anyMatch(v -> v.getKey().equals(key));
  }

  public static boolean isPublic(ProjectPermission permission) {
    return PUBLIC_PERMISSIONS.contains(permission);
  }

  public static boolean isPublic(String permissionKey) {
    return PUBLIC_PERMISSIONS.stream().anyMatch(p -> p.getKey().equals(permissionKey));
  }

}
