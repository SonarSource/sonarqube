/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  /** Asked through the public entry points, so this still holds if the protected hooks move again. */
  @Test
  public void session_has_no_permissions() {
    EntityDto entity = mock(EntityDto.class);
    when(entity.getUuid()).thenReturn("foo");
    when(entity.getAuthUuid()).thenReturn("foo");
    ComponentDto portfolio = mock(ComponentDto.class);
    when(portfolio.uuid()).thenReturn("foo");

    assertThat(underTest.shouldResetPassword()).isFalse();
    assertThat(underTest.isSystemAdministrator()).isFalse();
    assertThat(underTest.hasPermission(GlobalPermission.ADMINISTER)).isFalse();
    assertThat(underTest.hasEntityPermission(ProjectPermission.USER, "foo")).isFalse();
    assertThat(underTest.hasEntityPermission(ProjectPermission.USER, entity)).isFalse();
    assertThat(underTest.hasChildProjectsPermission(ProjectPermission.USER, entity)).isFalse();
    assertThat(underTest.hasPortfolioChildProjectsPermission(ProjectPermission.USER, portfolio)).isFalse();
    assertThat(underTest.hasComponentUuidPermission(ProjectPermission.USER, "foo")).isFalse();
  }
}
