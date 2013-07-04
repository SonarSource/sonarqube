/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.user;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * Holds the constants representing the various global permissions that can be assigned to users & groups
 *
 * @since 3.7
 */
public class Permission {

  public static final Permission SYSTEM_ADMIN = new Permission("admin");
  public static final Permission QUALITY_PROFILE_ADMIN = new Permission("profileadmin");
  public static final Permission DASHBOARD_SHARING = new Permission("shareDashboard");
  public static final Permission SCAN_EXECUTION = new Permission("scan");
  public static final Permission DRY_RUN_EXECUTION = new Permission("dryRunScan");

  private final String key;
  // Use linked hash map to preserve order
  private static Map<String, Permission> allGlobal = new LinkedHashMap<String, Permission>();

  static {
    allGlobal.put(SYSTEM_ADMIN.key, SYSTEM_ADMIN);
    allGlobal.put(QUALITY_PROFILE_ADMIN.key, QUALITY_PROFILE_ADMIN);
    allGlobal.put(DASHBOARD_SHARING.key, DASHBOARD_SHARING);
    allGlobal.put(SCAN_EXECUTION.key, SCAN_EXECUTION);
    allGlobal.put(DRY_RUN_EXECUTION.key, DRY_RUN_EXECUTION);
  }

  private Permission(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  public static Map<String, Permission> allGlobal() {
    return allGlobal;
  }

  public static boolean isValid(String permission) {
    return allGlobal.containsKey(permission);
  }
}
