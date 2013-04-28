/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.platform;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class UserSessionTest {
  @Test
  public void should_never_return_null_session() throws Exception {
    UserSession.remove();

    UserSession session = UserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void should_get_session() throws Exception {
    UserSession.set(123, "karadoc");

    UserSession session = UserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.userId()).isEqualTo(123);
    assertThat(session.login()).isEqualTo("karadoc");
    assertThat(session.isLoggedIn()).isTrue();
  }

  @Test
  public void should_get_anonymous_session() throws Exception {
    UserSession.set(null, null);

    UserSession session = UserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }
}
