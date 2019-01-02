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
package org.sonar.server.authentication;

import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.db.permission.OrganizationPermission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class SafeModeUserSessionTest {

  private SafeModeUserSession underTest = new SafeModeUserSession();

  @Test
  public void session_is_anonymous() {
    assertThat(underTest.getLogin()).isNull();
    assertThat(underTest.getUuid()).isNull();
    assertThat(underTest.isLoggedIn()).isFalse();
    assertThat(underTest.getName()).isNull();
    assertThat(underTest.getUserId()).isNull();
    assertThat(underTest.getGroups()).isEmpty();
  }

  @Test
  public void session_has_no_permissions() {
    assertThat(underTest.isRoot()).isFalse();
    assertThat(underTest.isSystemAdministrator()).isFalse();
    assertThat(underTest.hasPermissionImpl(OrganizationPermission.ADMINISTER, "foo")).isFalse();
    assertThat(underTest.hasProjectUuidPermission(UserRole.USER, "foo")).isFalse();
    assertThat(underTest.hasMembership(newOrganizationDto())).isFalse();
  }
}
