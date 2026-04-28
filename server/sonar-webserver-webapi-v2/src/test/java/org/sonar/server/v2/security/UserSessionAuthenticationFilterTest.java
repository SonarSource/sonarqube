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

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSessionAuthenticationFilterTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private ThreadLocalUserSession threadLocalUserSession;
  private UserSessionAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    threadLocalUserSession = new ThreadLocalUserSession();
    filter = new UserSessionAuthenticationFilter(threadLocalUserSession);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    threadLocalUserSession.unload();
  }

  @Test
  void doFilterInternal_withUserSession_shouldPopulateSecurityContext() throws Exception {
    UserSession userSession = new MockUserSession("john.doe");
    threadLocalUserSession.set(userSession);

    filter.doFilterInternal(request, response, (req, res) -> {
      // Verify SecurityContext is populated during filter chain execution
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      assertThat(authentication).isNotNull();
      assertThat(authentication).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
      assertThat(authentication.getName()).isEqualTo("john.doe");
      assertThat(authentication.isAuthenticated()).isTrue();
    });
  }

  @Test
  void doFilterInternal_withAnonymousSession_shouldPopulateSecurityContextWithAnonymous() throws Exception {
    UserSession userSession = new AnonymousMockUserSession();
    threadLocalUserSession.set(userSession);

    filter.doFilterInternal(request, response, (req, res) -> {
      // Verify SecurityContext is populated during filter chain execution
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      assertThat(authentication).isNotNull();
      assertThat(authentication).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
      assertThat(authentication.isAuthenticated()).isFalse();
    });
  }

  @Test
  void doFilterInternal_withoutUserSession_shouldSetAnonymousAuthentication() throws Exception {
    // No UserSession set in ThreadLocal

    filter.doFilterInternal(request, response, filterChain);

    // Should set anonymous authentication so Spring Security's authorization works correctly
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication)
      .isNotNull()
      .isInstanceOf(AnonymousAuthenticationToken.class);
    assertThat(authentication.getName()).isEqualTo("anonymousUser");

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_leavesSecurityContextForSpringSecurityToManage() throws Exception {
    UserSession userSession = new MockUserSession("john.doe");
    threadLocalUserSession.set(userSession);

    filter.doFilterInternal(request, response, filterChain);

    // SecurityContext is NOT cleared by our filter - Spring Security's SecurityContextHolderFilter
    // handles cleanup automatically. This allows Spring Security's authorization filters to work correctly.
    // During tests, we must manually clear it in tearDown.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication)
      .isNotNull()
      .isInstanceOf(PreAuthenticatedAuthenticationToken.class);
  }

  @Test
  void doFilterInternal_withException_letsExceptionPropagate() throws Exception {
    UserSession userSession = new MockUserSession("john.doe");
    threadLocalUserSession.set(userSession);

    RuntimeException expectedException = new RuntimeException("Test exception");

    try {
      filter.doFilterInternal(request, response, (req, res) -> {
        throw expectedException;
      });
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(expectedException);
    }

    // Our filter does NOT handle cleanup - Spring Security's SecurityContextHolderFilter
    // handles that. The SecurityContext remains populated until Spring Security clears it.
    // In tests, we must manually clear in tearDown.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
  }
}
