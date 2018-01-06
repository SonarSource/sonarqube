/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.user;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadLocalUserSessionTest {

  private ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    // for test isolation
    threadLocalUserSession.unload();
  }

  @After
  public void tearDown() {
    // clean up for next test
    threadLocalUserSession.unload();
  }

  @Test
  public void get_session_for_user() {
    GroupDto group = GroupTesting.newGroupDto();
    MockUserSession expected = new MockUserSession("karadoc")
      .setUserId(123)
      .setGroups(group);
    threadLocalUserSession.set(expected);

    UserSession session = threadLocalUserSession.get();
    assertThat(session).isSameAs(expected);
    assertThat(threadLocalUserSession.getUserId()).isEqualTo(123);
    assertThat(threadLocalUserSession.getLogin()).isEqualTo("karadoc");
    assertThat(threadLocalUserSession.isLoggedIn()).isTrue();
    assertThat(threadLocalUserSession.getGroups()).extracting(GroupDto::getId).containsOnly(group.getId());
  }

  @Test
  public void get_session_for_anonymous() {
    AnonymousMockUserSession expected = new AnonymousMockUserSession();
    threadLocalUserSession.set(expected);

    UserSession session = threadLocalUserSession.get();
    assertThat(session).isSameAs(expected);
    assertThat(threadLocalUserSession.getLogin()).isNull();
    assertThat(threadLocalUserSession.getUserId()).isNull();
    assertThat(threadLocalUserSession.isLoggedIn()).isFalse();
    assertThat(threadLocalUserSession.getGroups()).isEmpty();
  }

  @Test
  public void throw_UnauthorizedException_when_no_session() {
    thrown.expect(UnauthorizedException.class);
    threadLocalUserSession.get();
  }

}
