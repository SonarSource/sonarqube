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
package org.sonar.server.authentication;

import org.junit.Test;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.permission.GlobalPermission;

import static org.assertj.core.api.Assertions.assertThat;

public class SafeModeUserSessionTest {

  private SafeModeUserSession underTest = new SafeModeUserSession();

  @Test
  public void session_is_anonymous() {
    assertThat(underTest.getLogin()).isNull();
    assertThat(underTest.getUuid()).isNull();
    assertThat(underTest.isLoggedIn()).isFalse();
    assertThat(underTest.shouldResetPassword()).isFalse();
    assertThat(underTest.getName()).isNull();
    assertThat(underTest.getGroups()).isEmpty();
    assertThat(underTest.isActive()).isFalse();
    assertThat(underTest.isAuthenticatedBrowserSession()).isFalse();
  }

  @Test
  public void session_has_no_permissions() {
    assertThat(underTest.shouldResetPassword()).isFalse();
    assertThat(underTest.isSystemAdministrator()).isFalse();
    assertThat(underTest.hasPermissionImpl(GlobalPermission.ADMINISTER)).isFalse();
    assertThat(underTest.hasEntityUuidPermission(ProjectPermission.USER, "foo")).isFalse();
    assertThat(underTest.hasChildProjectsPermission(ProjectPermission.USER, "foo")).isFalse();
    assertThat(underTest.hasPortfolioChildProjectsPermission(ProjectPermission.USER, "foo")).isFalse();
  }
}
