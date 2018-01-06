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
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.web.UserRole;

/**
 * Holds the constants representing the various component permissions that can be assigned to users & groups
 *
 */
public final class ProjectPermissions {
  /**
   * Permissions which are implicitly available for any user, any group and to group "AnyOne" on public components.
   */
  public static final Set<String> PUBLIC_PERMISSIONS = ImmutableSet.of(UserRole.USER, UserRole.CODEVIEWER);

  /**
   * All the component permissions values, ordered from {@link UserRole#USER} to {@link GlobalPermissions#SCAN_EXECUTION}.
   */
  public static final List<String> ALL = ImmutableList.of(UserRole.ADMIN,  UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION, UserRole.USER);

  public static final String ALL_ON_ONE_LINE = Joiner.on(", ").join(ProjectPermissions.ALL);

  private ProjectPermissions() {
    // static constants only
  }
}
