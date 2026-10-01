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
package org.sonar.server.v2.security;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.assertj.core.api.Assertions.assertThat;

class UserSessionSnapshotTest {

  private final ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();

  @AfterEach
  void tearDown() {
    threadLocalUserSession.unload();
    SecurityContextHolder.clearContext();
  }

  @Test
  void take_whenCallerIsApiV2Request_shouldUnwrapSecurityContextBackedUserSession() {
    UserSession realSession = new MockUserSession("john");
    SecurityContextHolder.setContext(new SecurityContextImpl(authenticationOf(realSession)));
    SecurityContextBackedUserSession callerSession = new SecurityContextBackedUserSession();
    threadLocalUserSession.set(callerSession);

    try (UserSessionSnapshot ignored = UserSessionSnapshot.take(threadLocalUserSession)) {
      assertThat(threadLocalUserSession.get()).isSameAs(realSession);
    }

    assertThat(threadLocalUserSession.get()).isSameAs(callerSession);
  }

  @Test
  void take_whenCallerIsNotApiV2Request_shouldKeepUserSession() {
    UserSession callerSession = new MockUserSession("john");
    threadLocalUserSession.set(callerSession);

    try (UserSessionSnapshot ignored = UserSessionSnapshot.take(threadLocalUserSession)) {
      assertThat(threadLocalUserSession.get()).isSameAs(callerSession);
    }

    assertThat(threadLocalUserSession.get()).isSameAs(callerSession);
  }

  @Test
  void close_shouldRestoreCallerSecurityContext() {
    SecurityContext callerSecurityContext = new SecurityContextImpl(new TestingAuthenticationToken("john", null));
    SecurityContextHolder.setContext(callerSecurityContext);

    UserSessionSnapshot underTest = UserSessionSnapshot.take(threadLocalUserSession);
    SecurityContextHolder.clearContext();
    underTest.close();

    assertThat(SecurityContextHolder.getContext()).isSameAs(callerSecurityContext);
  }

  @Test
  void close_whenCallerHadNoSessionNorAuthentication_shouldClearBoth() {
    UserSessionSnapshot underTest = UserSessionSnapshot.take(threadLocalUserSession);
    threadLocalUserSession.set(new MockUserSession("john"));
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("john", null));

    underTest.close();

    assertThat(threadLocalUserSession.hasSession()).isFalse();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  private static TestingAuthenticationToken authenticationOf(UserSession userSession) {
    return new TestingAuthenticationToken(new SonarUserDetails(userSession, List.of()), null);
  }
}
