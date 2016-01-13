/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.platform.Platform;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RubyUserSessionTest {
  Platform platform = mock(Platform.class);
  ComponentContainer componentContainer = mock(ComponentContainer.class);
  ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();
  RubyUserSession underTest = new RubyUserSession(platform);

  @Before
  public void setUp() {
    // for test isolation
    threadLocalUserSession.remove();

    when(platform.getContainer()).thenReturn(componentContainer);
    when(componentContainer.getComponentByType(ThreadLocalUserSession.class)).thenReturn(threadLocalUserSession);
  }

  @After
  public void tearDown() {
    // clean up for next test
    threadLocalUserSession.remove();
  }

  @Test
  public void should_set_session() {
    underTest.setSessionImpl(123, "karadoc", "Karadoc", newArrayList("sonar-users"), "fr");

    UserSession session = threadLocalUserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.getLogin()).isEqualTo("karadoc");
    assertThat(session.getName()).isEqualTo("Karadoc");
    assertThat(session.getUserId()).isEqualTo(123);
    assertThat(session.getUserGroups()).containsOnly("sonar-users", "Anyone");
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void should_set_anonymous_session() {
    underTest.setSessionImpl(null, null, null, null, "fr");

    UserSession session = threadLocalUserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.getLogin()).isNull();
    assertThat(session.getName()).isNull();
    assertThat(session.getUserId()).isNull();
    assertThat(session.getUserGroups()).containsOnly("Anyone");
    assertThat(session.isLoggedIn()).isFalse();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

}
