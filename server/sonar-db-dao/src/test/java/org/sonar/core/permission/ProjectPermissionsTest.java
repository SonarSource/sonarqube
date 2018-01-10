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

import org.junit.Test;
import org.sonar.api.web.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectPermissionsTest {

  @Test
  public void all_permissions() {
    assertThat(ProjectPermissions.ALL).containsExactly(UserRole.ADMIN, UserRole.CODEVIEWER, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION, UserRole.USER);
  }

  @Test
  public void all_permissions_as_string() {
    assertThat(ProjectPermissions.ALL_ON_ONE_LINE).isEqualTo("admin, codeviewer, issueadmin, scan, user");
  }
}
