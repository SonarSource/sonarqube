/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.core.permission;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Holds the constants representing the various global permissions that can be assigned to users & groups
 *
 */
public final class GlobalPermissions {

  public static final String SYSTEM_ADMIN = "admin";
  public static final String QUALITY_PROFILE_ADMIN = "profileadmin";
  public static final String DASHBOARD_SHARING = "shareDashboard";
  public static final String SCAN_EXECUTION = "scan";
  public static final String PREVIEW_EXECUTION = "dryRunScan";
  public static final String PROVISIONING = "provisioning";

  /**
   * All the global permissions values, ordered from {@link #SYSTEM_ADMIN} to {@link #PROVISIONING}.
   */
  public static final List<String> ALL = ImmutableList.of(SYSTEM_ADMIN, QUALITY_PROFILE_ADMIN, DASHBOARD_SHARING, SCAN_EXECUTION, PREVIEW_EXECUTION, PROVISIONING);

  private GlobalPermissions() {
    // only static methods
  }

}
