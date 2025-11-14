/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.Test;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.component.ComponentTypesRule;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionServiceImplTest {

  private final ComponentTypesRule resourceTypesRule = new ComponentTypesRule().setRootQualifiers("APP", "VW");
  private final PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule);

  @Test
  public void globalPermissions_must_be_ordered() {
    assertThat(underTest.getGlobalPermissions())
      .extracting(GlobalPermission::getKey)
      .containsExactlyInAnyOrder("admin", "gateadmin", "profileadmin", "provisioning", "scan", "applicationcreator", "portfoliocreator");
  }

  @Test
  public void projectPermissions_must_be_ordered() {
    assertThat(underTest.getAllProjectPermissions())
      .extracting(ProjectPermission::getKey)
      .containsExactlyInAnyOrder("admin", "codeviewer", "issueadmin", "securityhotspotadmin", "scan", "user");
  }
}
