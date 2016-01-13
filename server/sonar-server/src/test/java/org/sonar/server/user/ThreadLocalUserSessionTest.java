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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.tester.MockUserSession;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadLocalUserSessionTest {

  ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    // for test isolation
    threadLocalUserSession.remove();
  }

  @After
  public void tearDown() {
    // clean up for next test
    threadLocalUserSession.remove();
  }

  @Test
  public void getSession_get_anonymous_by_default() {
    UserSession session = threadLocalUserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.getLogin()).isNull();
    assertThat(session.getUserId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
    // default locale
    assertThat(session.locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void get_session() {
    threadLocalUserSession.set(new MockUserSession("karadoc").setUserId(123).setLocale(Locale.FRENCH));

    UserSession session = threadLocalUserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.getUserId()).isEqualTo(123);
    assertThat(session.getLogin()).isEqualTo("karadoc");
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

}
