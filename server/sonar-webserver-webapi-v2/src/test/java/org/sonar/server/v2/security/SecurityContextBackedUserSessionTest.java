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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.UserSession;
import java.util.ArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for SecurityContextBackedUserSession - ensures all methods properly delegate
 * to the UserSession stored in SecurityContext.
 */
class SecurityContextBackedUserSessionTest {

  private SecurityContextBackedUserSession securityContextBackedUserSession;
  private UserSession mockUserSession;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    securityContextBackedUserSession = new SecurityContextBackedUserSession();
    mockUserSession = mock(UserSession.class);
    // Stub common methods required by setUpSecurityContext
    when(mockUserSession.isLoggedIn()).thenReturn(true);
    when(mockUserSession.getGroups()).thenReturn(List.of());
    when(mockUserSession.isSystemAdministrator()).thenReturn(false);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setUpSecurityContext(UserSession userSession) {
    // Create standard Spring Security token (matching UserSessionAuthenticationFilter behavior)
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

    // Principal: SonarUserDetails (standard Spring Security UserDetails)
    SonarUserDetails userDetails = new SonarUserDetails(userSession, authorities);

    PreAuthenticatedAuthenticationToken authentication;
    if (userSession.isLoggedIn()) {
      authentication = new PreAuthenticatedAuthenticationToken(userDetails, null, authorities);
    } else {
      // Anonymous user - create unauthenticated token
      authentication = new PreAuthenticatedAuthenticationToken(userDetails, null);
      authentication.setAuthenticated(false);
    }

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  void getLogin_shouldDelegateToSecurityContext() {
    when(mockUserSession.getLogin()).thenReturn("john.doe");
    setUpSecurityContext(mockUserSession);

    String login = securityContextBackedUserSession.getLogin();

    // These methods are called multiple times in setUpSecurityContext, so we just verify the result
    assertThat(login).isEqualTo("john.doe");
  }

  @Test
  void getUuid_shouldDelegateToSecurityContext() {
    when(mockUserSession.getUuid()).thenReturn("user-uuid-123");
    setUpSecurityContext(mockUserSession);

    String uuid = securityContextBackedUserSession.getUuid();

    assertThat(uuid).isEqualTo("user-uuid-123");
  }

  @Test
  void getName_shouldDelegateToSecurityContext() {
    when(mockUserSession.getName()).thenReturn("John Doe");
    setUpSecurityContext(mockUserSession);

    String name = securityContextBackedUserSession.getName();

    assertThat(name).isEqualTo("John Doe");
  }

  @Test
  void getLastSonarlintConnectionDate_shouldDelegateToSecurityContext() {
    when(mockUserSession.getLastSonarlintConnectionDate()).thenReturn(12345L);
    setUpSecurityContext(mockUserSession);

    Long date = securityContextBackedUserSession.getLastSonarlintConnectionDate();

    assertThat(date).isEqualTo(12345L);
    verify(mockUserSession).getLastSonarlintConnectionDate();
  }

  @Test
  void getGroups_shouldDelegateToSecurityContext() {
    // Use real MockUserSession because UserSessionAuthentication constructor calls getGroups()
    GroupDto group = new GroupDto().setUuid("group-1").setName("developers");
    MockUserSession realSession = new MockUserSession("user").setGroups(group);
    setUpSecurityContext(realSession);

    Collection<GroupDto> groups = securityContextBackedUserSession.getGroups();

    assertThat(groups).containsExactly(group);
  }

  @Test
  void shouldResetPassword_shouldDelegateToSecurityContext() {
    when(mockUserSession.shouldResetPassword()).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean shouldReset = securityContextBackedUserSession.shouldResetPassword();

    assertThat(shouldReset).isTrue();
    verify(mockUserSession).shouldResetPassword();
  }

  @Test
  void getIdentityProvider_shouldDelegateToSecurityContext() {
    when(mockUserSession.getIdentityProvider()).thenReturn(Optional.of(UserSession.IdentityProvider.GITHUB));
    setUpSecurityContext(mockUserSession);

    Optional<UserSession.IdentityProvider> provider = securityContextBackedUserSession.getIdentityProvider();

    assertThat(provider).contains(UserSession.IdentityProvider.GITHUB);
    verify(mockUserSession).getIdentityProvider();
  }

  @Test
  void getExternalIdentity_shouldDelegateToSecurityContext() {
    UserSession.ExternalIdentity externalId = new UserSession.ExternalIdentity("ext-123", "external-login");
    when(mockUserSession.getExternalIdentity()).thenReturn(Optional.of(externalId));
    setUpSecurityContext(mockUserSession);

    Optional<UserSession.ExternalIdentity> identity = securityContextBackedUserSession.getExternalIdentity();

    assertThat(identity).contains(externalId);
    verify(mockUserSession).getExternalIdentity();
  }

  @Test
  void isLoggedIn_shouldDelegateToSecurityContext() {
    // Use real MockUserSession because UserSessionAuthentication constructor calls isLoggedIn()
    MockUserSession realSession = new MockUserSession("user");
    setUpSecurityContext(realSession);

    boolean loggedIn = securityContextBackedUserSession.isLoggedIn();

    assertThat(loggedIn).isTrue();
  }

  @Test
  void checkLoggedIn_shouldDelegateToSecurityContext() {
    when(mockUserSession.checkLoggedIn()).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkLoggedIn();

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkLoggedIn();
  }

  @Test
  void hasPermission_shouldDelegateToSecurityContext() {
    when(mockUserSession.hasPermission(GlobalPermission.ADMINISTER)).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasPermission(GlobalPermission.ADMINISTER);

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasPermission(GlobalPermission.ADMINISTER);
  }

  @Test
  void checkPermission_shouldDelegateToSecurityContext() {
    when(mockUserSession.checkPermission(GlobalPermission.ADMINISTER)).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkPermission(GlobalPermission.ADMINISTER);

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkPermission(GlobalPermission.ADMINISTER);
  }

  @Test
  void hasComponentPermission_shouldDelegateToSecurityContext() {
    ComponentDto component = new ComponentDto().setUuid("comp-1");
    when(mockUserSession.hasComponentPermission(ProjectPermission.ADMIN, component)).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasComponentPermission(ProjectPermission.ADMIN, component);

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasComponentPermission(ProjectPermission.ADMIN, component);
  }

  @Test
  void hasEntityPermission_withEntityDto_shouldDelegateToSecurityContext() {
    EntityDto entity = mock(EntityDto.class);
    when(mockUserSession.hasEntityPermission(ProjectPermission.ADMIN, entity)).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasEntityPermission(ProjectPermission.ADMIN, entity);

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasEntityPermission(ProjectPermission.ADMIN, entity);
  }

  @Test
  void hasEntityPermission_withUuid_shouldDelegateToSecurityContext() {
    when(mockUserSession.hasEntityPermission(ProjectPermission.ADMIN, "entity-uuid")).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasEntityPermission(ProjectPermission.ADMIN, "entity-uuid");

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasEntityPermission(ProjectPermission.ADMIN, "entity-uuid");
  }

  @Test
  void isSystemAdministrator_shouldDelegateToSecurityContext() {
    // Use real MockUserSession because UserSessionAuthentication constructor calls isSystemAdministrator()
    MockUserSession realSession = new MockUserSession("user").setSystemAdministrator(true);
    setUpSecurityContext(realSession);

    boolean isAdmin = securityContextBackedUserSession.isSystemAdministrator();

    assertThat(isAdmin).isTrue();
  }

  @Test
  void checkIsSystemAdministrator_shouldDelegateToSecurityContext() {
    when(mockUserSession.checkIsSystemAdministrator()).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkIsSystemAdministrator();

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkIsSystemAdministrator();
  }

  @Test
  void isActive_shouldDelegateToSecurityContext() {
    when(mockUserSession.isActive()).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean isActive = securityContextBackedUserSession.isActive();

    assertThat(isActive).isTrue();
    verify(mockUserSession).isActive();
  }

  @Test
  void isAuthenticatedBrowserSession_shouldDelegateToSecurityContext() {
    when(mockUserSession.isAuthenticatedBrowserSession()).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean isBrowserSession = securityContextBackedUserSession.isAuthenticatedBrowserSession();

    assertThat(isBrowserSession).isTrue();
    verify(mockUserSession).isAuthenticatedBrowserSession();
  }

  @Test
  void hasChildProjectsPermission_withComponent_shouldDelegateToSecurityContext() {
    ComponentDto component = new ComponentDto().setUuid("app-1");
    when(mockUserSession.hasChildProjectsPermission(ProjectPermission.USER, component)).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasChildProjectsPermission(ProjectPermission.USER, component);

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasChildProjectsPermission(ProjectPermission.USER, component);
  }

  @Test
  void hasChildProjectsPermission_withEntity_shouldDelegateToSecurityContext() {
    EntityDto entity = mock(EntityDto.class);
    when(mockUserSession.hasChildProjectsPermission(ProjectPermission.USER, entity)).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasChildProjectsPermission(ProjectPermission.USER, entity);

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasChildProjectsPermission(ProjectPermission.USER, entity);
  }

  @Test
  void hasPortfolioChildProjectsPermission_shouldDelegateToSecurityContext() {
    ComponentDto component = new ComponentDto().setUuid("portfolio-1");
    when(mockUserSession.hasPortfolioChildProjectsPermission(ProjectPermission.USER, component)).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasPortfolioChildProjectsPermission(ProjectPermission.USER, component);

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasPortfolioChildProjectsPermission(ProjectPermission.USER, component);
  }

  @Test
  void hasComponentUuidPermission_shouldDelegateToSecurityContext() {
    when(mockUserSession.hasComponentUuidPermission(ProjectPermission.ADMIN, "comp-uuid")).thenReturn(true);
    setUpSecurityContext(mockUserSession);

    boolean hasPermission = securityContextBackedUserSession.hasComponentUuidPermission(ProjectPermission.ADMIN, "comp-uuid");

    assertThat(hasPermission).isTrue();
    verify(mockUserSession).hasComponentUuidPermission(ProjectPermission.ADMIN, "comp-uuid");
  }

  @Test
  void keepAuthorizedComponents_shouldDelegateToSecurityContext() {
    ComponentDto comp1 = new ComponentDto().setUuid("comp-1");
    ComponentDto comp2 = new ComponentDto().setUuid("comp-2");
    List<ComponentDto> components = List.of(comp1, comp2);
    when(mockUserSession.keepAuthorizedComponents(ProjectPermission.CODEVIEWER, components)).thenReturn(List.of(comp1));
    setUpSecurityContext(mockUserSession);

    List<ComponentDto> result = securityContextBackedUserSession.keepAuthorizedComponents(ProjectPermission.CODEVIEWER, components);

    assertThat(result).containsExactly(comp1);
    verify(mockUserSession).keepAuthorizedComponents(ProjectPermission.CODEVIEWER, components);
  }

  @Test
  void keepAuthorizedEntities_shouldDelegateToSecurityContext() {
    EntityDto entity1 = mock(EntityDto.class);
    EntityDto entity2 = mock(EntityDto.class);
    List<EntityDto> entities = List.of(entity1, entity2);
    when(mockUserSession.keepAuthorizedEntities(ProjectPermission.CODEVIEWER, entities)).thenReturn(List.of(entity1));
    setUpSecurityContext(mockUserSession);

    List<EntityDto> result = securityContextBackedUserSession.keepAuthorizedEntities(ProjectPermission.CODEVIEWER, entities);

    assertThat(result).containsExactly(entity1);
    verify(mockUserSession).keepAuthorizedEntities(ProjectPermission.CODEVIEWER, entities);
  }

  @Test
  void checkComponentPermission_shouldDelegateAndReturnSelf() {
    ComponentDto component = new ComponentDto().setUuid("comp-1");
    when(mockUserSession.checkComponentPermission(ProjectPermission.ADMIN, component)).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkComponentPermission(ProjectPermission.ADMIN, component);

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkComponentPermission(ProjectPermission.ADMIN, component);
  }

  @Test
  void checkEntityPermission_shouldDelegateAndReturnSelf() {
    EntityDto entity = mock(EntityDto.class);
    when(mockUserSession.checkEntityPermission(ProjectPermission.ADMIN, entity)).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkEntityPermission(ProjectPermission.ADMIN, entity);

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkEntityPermission(ProjectPermission.ADMIN, entity);
  }

  @Test
  void checkEntityPermissionOrElseThrowResourceForbiddenException_shouldDelegateAndReturnSelf() {
    EntityDto entity = mock(EntityDto.class);
    when(mockUserSession.checkEntityPermissionOrElseThrowResourceForbiddenException(ProjectPermission.ADMIN, entity)).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkEntityPermissionOrElseThrowResourceForbiddenException(ProjectPermission.ADMIN, entity);

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkEntityPermissionOrElseThrowResourceForbiddenException(ProjectPermission.ADMIN, entity);
  }

  @Test
  void checkChildProjectsPermission_withComponent_shouldDelegateAndReturnSelf() {
    ComponentDto component = new ComponentDto().setUuid("app-1");
    when(mockUserSession.checkChildProjectsPermission(ProjectPermission.USER, component)).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkChildProjectsPermission(ProjectPermission.USER, component);

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkChildProjectsPermission(ProjectPermission.USER, component);
  }

  @Test
  void checkChildProjectsPermission_withEntity_shouldDelegateAndReturnSelf() {
    EntityDto entity = mock(EntityDto.class);
    when(mockUserSession.checkChildProjectsPermission(ProjectPermission.USER, entity)).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkChildProjectsPermission(ProjectPermission.USER, entity);

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkChildProjectsPermission(ProjectPermission.USER, entity);
  }

  @Test
  void checkComponentUuidPermission_shouldDelegateAndReturnSelf() {
    when(mockUserSession.checkComponentUuidPermission(ProjectPermission.ADMIN, "comp-uuid")).thenReturn(mockUserSession);
    setUpSecurityContext(mockUserSession);

    UserSession result = securityContextBackedUserSession.checkComponentUuidPermission(ProjectPermission.ADMIN, "comp-uuid");

    assertThat(result).isSameAs(securityContextBackedUserSession);
    verify(mockUserSession).checkComponentUuidPermission(ProjectPermission.ADMIN, "comp-uuid");
  }

  @Test
  void delegate_withNoAuthentication_shouldThrowUnauthorizedException() {
    // No authentication in SecurityContext

    assertThatThrownBy(() -> securityContextBackedUserSession.getLogin())
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  void delegate_withWrongAuthenticationType_shouldThrowUnauthorizedException() {
    // Set authentication without SonarUserDetails as principal (use 3-arg constructor to make it authenticated)
    UsernamePasswordAuthenticationToken wrongAuth = new UsernamePasswordAuthenticationToken(
      "user", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(wrongAuth);

    assertThatThrownBy(() -> securityContextBackedUserSession.getUuid())
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("UserSession not found in authentication principal");
  }

  @Test
  void integrationTest_withRealUserSession_allMethodsWork() {
    // Use a real MockUserSession to test the full integration
    MockUserSession realUserSession = new MockUserSession("integration-test-user")
      .setUuid("uuid-123")
      .setName("Integration Test User")
      .setSystemAdministrator(true);

    setUpSecurityContext(realUserSession);

    // Test identity methods
    assertThat(securityContextBackedUserSession.getLogin()).isEqualTo("integration-test-user");
    assertThat(securityContextBackedUserSession.getUuid()).isEqualTo("uuid-123");
    assertThat(securityContextBackedUserSession.getName()).isEqualTo("Integration Test User");
    assertThat(securityContextBackedUserSession.isLoggedIn()).isTrue();
    assertThat(securityContextBackedUserSession.isSystemAdministrator()).isTrue();

    // Test check methods return self
    assertThat(securityContextBackedUserSession.checkLoggedIn()).isSameAs(securityContextBackedUserSession);
    assertThat(securityContextBackedUserSession.checkIsSystemAdministrator()).isSameAs(securityContextBackedUserSession);
  }
}
