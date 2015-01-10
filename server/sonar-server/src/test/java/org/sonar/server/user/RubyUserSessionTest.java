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
package org.sonar.server.user;

import org.junit.Test;

import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class RubyUserSessionTest {
  @Test
  public void should_set_session() throws Exception {
    RubyUserSession.setSession(123, "karadoc", "Karadoc", newArrayList("sonar-users"), "fr");

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isEqualTo("karadoc");
    assertThat(session.name()).isEqualTo("Karadoc");
    assertThat(session.userId()).isEqualTo(123);
    assertThat(session.userGroups()).containsOnly("sonar-users", "Anyone");
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void should_set_anonymous_session() throws Exception {
    RubyUserSession.setSession(null, null, null, null, "fr");

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.name()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.userGroups()).containsOnly("Anyone");
    assertThat(session.isLoggedIn()).isFalse();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

}
