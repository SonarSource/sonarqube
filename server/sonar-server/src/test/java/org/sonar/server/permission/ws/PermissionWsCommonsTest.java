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

package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.BadRequestException;

import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.server.component.ComponentTesting.newProjectDto;
import static org.sonar.server.permission.ws.PermissionWsCommons.GLOBAL_PERMISSIONS_ONE_LINE;
import static org.sonar.server.permission.ws.PermissionWsCommons.PROJECT_PERMISSIONS_ONE_LINE;

public class PermissionWsCommonsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void succeed_with_right_values() {
    PermissionWsCommons.checkPermissionAndProjectParameters(UserRole.ADMIN, Optional.of(newProjectDto()));
    PermissionWsCommons.checkPermissionAndProjectParameters(UserRole.CODEVIEWER, Optional.of(newProjectDto()));
    PermissionWsCommons.checkPermissionAndProjectParameters(GlobalPermissions.SYSTEM_ADMIN, Optional.<ComponentDto>absent());
    PermissionWsCommons.checkPermissionAndProjectParameters(GlobalPermissions.PROVISIONING, Optional.<ComponentDto>absent());
    PermissionWsCommons.checkPermissionAndProjectParameters(GlobalPermissions.DASHBOARD_SHARING, Optional.<ComponentDto>absent());
  }

  @Test
  public void fail_if_project_and_global_permission() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(String.format("Incorrect value '%s' for project permissions. Values allowed: %s.", DASHBOARD_SHARING, PROJECT_PERMISSIONS_ONE_LINE));

    PermissionWsCommons.checkPermissionAndProjectParameters(DASHBOARD_SHARING, Optional.of(newProjectDto()));
  }

  @Test
  public void fail_if_no_project_and_project_permission() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(String.format("Incorrect value '%s' for global permissions. Values allowed: %s.", ISSUE_ADMIN, GLOBAL_PERMISSIONS_ONE_LINE));

    PermissionWsCommons.checkPermissionAndProjectParameters(ISSUE_ADMIN, Optional.<ComponentDto>absent());
  }
}
