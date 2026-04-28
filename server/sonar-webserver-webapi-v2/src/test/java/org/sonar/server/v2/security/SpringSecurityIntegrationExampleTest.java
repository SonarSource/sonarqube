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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.UserSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test demonstrates how Spring Security integration works with the UserSession bridge.
 * It shows how the SecurityContext is populated using standard Spring Security tokens
 * and provides access to both UserSession and AuthenticatedUser abstractions.
 *
 * Note: Testing @PreAuthorize annotations requires a Spring context with AOP support,
 * which is tested in integration tests. These are unit tests focused on the bridge mechanism.
 */
class SpringSecurityIntegrationExampleTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatedUser_shouldBeAccessibleViaSecurityContextUtils() {
    UserSession userSession = new MockUserSession("john.doe")
      .setUuid("user-uuid-123");
    setSecurityContext(userSession);

    AuthenticatedUser authenticatedUser = org.sonar.server.v2.security.SecurityContextUtils.requireAuthenticatedUser();

    assertThat(authenticatedUser.getId()).isEqualTo("user-uuid-123");
    assertThat(authenticatedUser.getLogin()).isEqualTo("john.doe");
    assertThat(authenticatedUser.getName()).isEqualTo("john.doe");
    assertThat(authenticatedUser.isLoggedIn()).isTrue();
  }

  @Test
  void authentication_shouldProvideAccessToUserSession() {
    UserSession expectedUserSession = new MockUserSession("john.doe");
    setSecurityContext(expectedUserSession);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    SonarUserDetails userDetails = (SonarUserDetails) authentication.getPrincipal();

    UserSession actualUserSession = userDetails.getUserSession();
    assertThat(actualUserSession).isSameAs(expectedUserSession);
    assertThat(actualUserSession.getLogin()).isEqualTo("john.doe");
  }

  @Test
  void authentication_shouldSeparateUuidAndLogin_forSonarSpringIntegration() {
    String expectedUuid = "abc-123-def-456";
    String expectedLogin = "alice.smith";
    UserSession userSession = new MockUserSession(expectedLogin)
      .setUuid(expectedUuid);
    setSecurityContext(userSession);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    SonarUserDetails userDetails = (SonarUserDetails) authentication.getPrincipal();

    assertThat(userDetails.getUsername()).isEqualTo(expectedUuid);
    assertThat(authentication.getName()).isEqualTo(expectedLogin);
    assertThat(userDetails.getLogin()).isEqualTo(expectedLogin);

    AuthenticatedUser authenticatedUser = org.sonar.server.v2.security.SecurityContextUtils.requireAuthenticatedUser();
    assertThat(authenticatedUser.getId()).isEqualTo(expectedUuid);
    assertThat(authenticatedUser.getLogin()).isEqualTo(expectedLogin);
  }

  @Test
  void authentication_withAuthenticatedUser_shouldHaveCorrectRoles() {
    UserSession userSession = new MockUserSession("admin")
      .setSystemAdministrator(true);
    setSecurityContext(userSession);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    SonarUserDetails userDetails = (SonarUserDetails) authentication.getPrincipal();

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getName()).isEqualTo("admin");
    assertThat(userDetails.getUsername()).isEqualTo("adminuuid");
    assertThat(userDetails.getLogin()).isEqualTo("admin");
    assertThat(authentication.getAuthorities())
      .extracting("authority")
      .contains("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void authentication_withAnonymousUser_shouldNotBeAuthenticated() {
    UserSession userSession = new AnonymousMockUserSession();
    setSecurityContext(userSession);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    assertThat(authentication.isAuthenticated()).isFalse();
    assertThat(authentication.getAuthorities()).isEmpty();
  }

  private void setSecurityContext(UserSession userSession) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    if (userSession.isSystemAdministrator()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    if (userSession.isLoggedIn()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }
    List<GrantedAuthority> groupAuthorities = userSession.getGroups().stream()
      .map(group -> new SimpleGrantedAuthority("GROUP_" + group.getName()))
      .collect(Collectors.toList());
    authorities.addAll(groupAuthorities);

    SonarUserDetails userDetails = new SonarUserDetails(userSession, authorities);

    // Override getName() to return login (used by sonar-spring's extractLogin)
    String login = userSession.getLogin();
    PreAuthenticatedAuthenticationToken authentication;
    if (userSession.isLoggedIn()) {
      authentication = new PreAuthenticatedAuthenticationToken(userDetails, null, authorities) {
        @Override
        public String getName() {
          return login;
        }
      };
    } else {
      authentication = new PreAuthenticatedAuthenticationToken(userDetails, null) {
        @Override
        public String getName() {
          return login;
        }
      };
      authentication.setAuthenticated(false);
    }

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
