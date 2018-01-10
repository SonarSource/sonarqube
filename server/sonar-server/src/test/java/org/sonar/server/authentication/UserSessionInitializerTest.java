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
package org.sonar.server.authentication;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Method;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.TestUserSessionFactory;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserSessionInitializerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  private DbSession dbSession = dbTester.getSession();

  private ThreadLocalUserSession userSession = mock(ThreadLocalUserSession.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private Authenticators authenticators = mock(Authenticators.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();
  private MapSettings settings = new MapSettings();
  private UserDto user = newUserDto();
  private UserSessionInitializer underTest = new UserSessionInitializer(settings.asConfig(), userSession, authenticationEvent, userSessionFactory, authenticators);

  @Before
  public void setUp() throws Exception {
    dbClient.userDao().insert(dbSession, user);
    dbSession.commit();
    when(request.getContextPath()).thenReturn("");
    when(request.getRequestURI()).thenReturn("/measures");
  }

  @Test
  public void check_urls() {
    assertPathIsNotIgnored("/");
    assertPathIsNotIgnored("/foo");
    assertPathIsNotIgnored("/api/server_id/show");

    assertPathIsIgnored("/api/authentication/login");
    assertPathIsIgnored("/api/authentication/logout");
    assertPathIsIgnored("/api/authentication/validate");
    assertPathIsIgnored("/batch/index");
    assertPathIsIgnored("/batch/file");
    assertPathIsIgnored("/maintenance/index");
    assertPathIsIgnored("/setup/index");
    assertPathIsIgnored("/sessions/new");
    assertPathIsIgnored("/sessions/logout");
    assertPathIsIgnored("/sessions/unauthorized");
    assertPathIsIgnored("/oauth2/callback/github");
    assertPathIsIgnored("/oauth2/callback/foo");
    assertPathIsIgnored("/api/system/db_migration_status");
    assertPathIsIgnored("/api/system/status");
    assertPathIsIgnored("/api/system/migrate_db");
    assertPathIsIgnored("/api/server/version");
    assertPathIsIgnored("/api/users/identity_providers");
    assertPathIsIgnored("/api/l10n/index");

    // exclude static resources
    assertPathIsIgnored("/css/style.css");
    assertPathIsIgnored("/fonts/font.ttf");
    assertPathIsIgnored("/images/logo.png");
    assertPathIsIgnored("/js/jquery.js");
  }

  @Test
  public void return_code_401_when_not_authenticated_and_with_force_authentication() {
    ArgumentCaptor<AuthenticationException> exceptionArgumentCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
    when(userSession.isLoggedIn()).thenReturn(false);
    when(authenticators.authenticate(request, response)).thenReturn(Optional.empty());
    settings.setProperty("sonar.forceAuthentication", true);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verifyZeroInteractions(response);
    verify(authenticationEvent).loginFailure(eq(request), exceptionArgumentCaptor.capture());
    verifyZeroInteractions(userSession);
    AuthenticationException authenticationException = exceptionArgumentCaptor.getValue();
    assertThat(authenticationException.getSource()).isEqualTo(Source.local(Method.BASIC));
    assertThat(authenticationException.getLogin()).isNull();
    assertThat(authenticationException.getMessage()).isEqualTo("User must be authenticated");
    assertThat(authenticationException.getPublicMessage()).isNull();
  }

  @Test
  public void return_401_and_stop_on_ws() {
    when(request.getRequestURI()).thenReturn("/api/issues");
    AuthenticationException authenticationException = AuthenticationException.newBuilder().setSource(Source.jwt()).setMessage("Token id hasn't been found").build();
    doThrow(authenticationException).when(authenticators).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(request, authenticationException);
    verifyZeroInteractions(userSession);
  }

  @Test
  public void return_401_and_stop_on_batch_ws() {
    when(request.getRequestURI()).thenReturn("/batch/global");
    doThrow(AuthenticationException.newBuilder().setSource(Source.jwt()).setMessage("Token id hasn't been found").build())
      .when(authenticators).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verifyZeroInteractions(userSession);
  }

  @Test
  public void return_to_session_unauthorized_when_error_on_from_external_provider() throws Exception {
    doThrow(AuthenticationException.newBuilder().setSource(Source.external(newBasicIdentityProvider("failing"))).setPublicMessage("Token id hasn't been found").build())
      .when(authenticators).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).sendRedirect("/sessions/unauthorized?message=Token+id+hasn%27t+been+found");
  }

  @Test
  public void return_to_session_unauthorized_when_error_on_from_external_provider_with_context_path() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
    doThrow(AuthenticationException.newBuilder().setSource(Source.external(newBasicIdentityProvider("failing"))).setPublicMessage("Token id hasn't been found").build())
      .when(authenticators).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized?message=Token+id+hasn%27t+been+found");
  }

  private void assertPathIsIgnored(String path) {
    when(request.getRequestURI()).thenReturn(path);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verifyZeroInteractions(userSession, authenticators);
    reset(userSession, authenticators);
  }

  private void assertPathIsNotIgnored(String path) {
    when(request.getRequestURI()).thenReturn(path);
    when(authenticators.authenticate(request, response)).thenReturn(Optional.of(user));

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(userSession).set(any(UserSession.class));
    reset(userSession, authenticators);
  }

  private static BaseIdentityProvider newBasicIdentityProvider(String name) {
    BaseIdentityProvider mock = mock(BaseIdentityProvider.class);
    when(mock.getName()).thenReturn(name);
    return mock;
  }
}
