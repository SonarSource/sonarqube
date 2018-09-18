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

package org.sonar.server.permission;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;

public class PermissionsHelper {

  private final Set<String> allPermissions;
  private final String allOnOneLine;

  public PermissionsHelper(ResourceTypes resourceTypes) {
    ArrayList<String> permissions = new ArrayList<>(Arrays.asList(UserRole.ADMIN, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN,
      GlobalPermissions.SCAN_EXECUTION, UserRole.USER));
    if (resourceTypes.isQualifierPresent(Qualifiers.VIEW)) {
      permissions.add(UserRole.PORTFOLIO_CREATOR);
    }
    if (resourceTypes.isQualifierPresent(Qualifiers.APP)) {
      permissions.add(UserRole.APPLICATION_CREATOR);
    }
    allPermissions = ImmutableSet.copyOf(permissions);
    allOnOneLine = Joiner.on(", ").join(this.allPermissions);
  }

  public Set<String> allPermissions() {
    return allPermissions;
  }

  public String allOnOneLine() {
    return allOnOneLine;
  }
}
