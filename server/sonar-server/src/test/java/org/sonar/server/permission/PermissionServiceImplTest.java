/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.OrganizationPermission;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionServiceImplTest {

  private ResourceTypesRule resourceTypesRule = new ResourceTypesRule().setRootQualifiers("APP", "VW");
  private PermissionServiceImpl underTest = new PermissionServiceImpl(resourceTypesRule);

  @Test
  public void organizationPermissions_must_be_ordered() {
    assertThat(underTest.getAllOrganizationPermissions())
      .extracting(OrganizationPermission::getKey)
      .containsExactly("admin", "gateadmin", "profileadmin", "provisioning", "scan", "applicationcreator", "portfoliocreator");
  }

  @Test
  public void projectPermissions_must_be_ordered() {
    assertThat(underTest.getAllProjectPermissions())
      .containsExactly("admin", "codeviewer", "issueadmin", "securityhotspotadmin", "scan", "user");
  }
}
