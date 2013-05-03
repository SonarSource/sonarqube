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

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;

public class UserSessionTest {
  @Test
  public void should_get_anonymous_session_by_default() throws Exception {
    UserSession.remove();

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
    // default locale
    assertThat(session.locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void should_set_session_from_jror() throws Exception {
    UserSession.setSession(123, "karadoc", "fr");

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isEqualTo("karadoc");
    assertThat(session.userId()).isEqualTo(123);
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void should_set_anonymous_session_from_jror() throws Exception {
    UserSession.setSession(null, null, "fr");

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void should_get_session() throws Exception {
    UserSession.set(new UserSession(123, "karadoc", Locale.FRENCH));

    UserSession session = UserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.userId()).isEqualTo(123);
    assertThat(session.login()).isEqualTo("karadoc");
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }
}
