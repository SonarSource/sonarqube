/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.permission;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Holds the constants representing the various global permissions that can be assigned to users & groups
 *
 * @deprecated replaced by enum {@link org.sonar.db.permission.OrganizationPermission}
 */
@Deprecated
public final class GlobalPermissions {

  public static final String SYSTEM_ADMIN = "admin";
  public static final String QUALITY_PROFILE_ADMIN = "profileadmin";
  public static final String QUALITY_GATE_ADMIN = "gateadmin";
  public static final String SCAN_EXECUTION = "scan";
  public static final String PROVISIONING = "provisioning";

  /**
   * All the global permissions values, ordered from {@link #SYSTEM_ADMIN} to {@link #PROVISIONING}.
   */
  public static final List<String> ALL = ImmutableList.of(
    SYSTEM_ADMIN, QUALITY_PROFILE_ADMIN, QUALITY_GATE_ADMIN, SCAN_EXECUTION, PROVISIONING);
  public static final String ALL_ON_ONE_LINE = Joiner.on(", ").join(GlobalPermissions.ALL);

  private GlobalPermissions() {
    // only static methods
  }

}
