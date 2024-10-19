/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.BaseIdentityProvider;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationEvent.Method;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.TokenUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;

public class UserSessionInitializerIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ThreadLocalUserSession threadLocalSession = mock(ThreadLocalUserSession.class);
  private HttpRequest request = mock(HttpRequest.class);
  private HttpResponse response = mock(HttpResponse.class);
  private RequestAuthenticator authenticator = mock(RequestAuthenticator.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private MapSettings settings = new MapSettings();
  private ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);

  private UserSessionInitializer underTest = new UserSessionInitializer(settings.asConfig(), threadLocalSession, authenticationEvent, authenticator);

  @Before
  public void setUp() {
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

    // exclude project_badge url, as they can be auth. by a token as queryparam
    assertPathIsIgnored("/api/project_badges/measure");
    assertPathIsIgnored("/api/project_badges/quality_gate");
    assertPathIsIgnored("/api/project_badges/ai_code_assurance");

    // exlude passcode urls
    assertPathIsIgnoredWithAnonymousAccess("/api/ce/info");
    assertPathIsIgnoredWithAnonymousAccess("/api/ce/pause");
    assertPathIsIgnoredWithAnonymousAccess("/api/ce/resume");
    assertPathIsIgnoredWithAnonymousAccess("/api/system/health");
    assertPathIsIgnoredWithAnonymousAccess("/api/system/liveness");
    assertPathIsIgnoredWithAnonymousAccess("/api/monitoring/metrics");

    // exclude static resources
    assertPathIsIgnored("/css/style.css");
    assertPathIsIgnored("/images/logo.png");
    assertPathIsIgnored("/js/jquery.js");
  }

  @Test
  public void return_code_401_when_not_authenticated_and_with_force_authentication() {
    ArgumentCaptor<AuthenticationException> exceptionArgumentCaptor = ArgumentCaptor.forClass(AuthenticationException.class);
    when(threadLocalSession.isLoggedIn()).thenReturn(false);
    when(authenticator.authenticate(request, response)).thenReturn(new AnonymousMockUserSession());

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verifyNoMoreInteractions(response);
    verify(authenticationEvent).loginFailure(eq(request), exceptionArgumentCaptor.capture());
    verifyNoMoreInteractions(threadLocalSession);
    AuthenticationException authenticationException = exceptionArgumentCaptor.getValue();
    assertThat(authenticationException.getSource()).isEqualTo(Source.local(Method.BASIC));
    assertThat(authenticationException.getLogin()).isNull();
    assertThat(authenticationException.getMessage()).isEqualTo("User must be authenticated");
    assertThat(authenticationException.getPublicMessage()).isNull();
  }

  @Test
  public void does_not_return_code_401_when_not_authenticated_and_with_force_authentication_off() {
    when(threadLocalSession.isLoggedIn()).thenReturn(false);
    when(authenticator.authenticate(request, response)).thenReturn(new AnonymousMockUserSession());
    settings.setProperty("sonar.forceAuthentication", false);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verifyNoMoreInteractions(response);
  }

  @Test
  public void return_401_and_stop_on_ws() {
    when(request.getRequestURI()).thenReturn("/api/issues");
    AuthenticationException authenticationException = AuthenticationException.newBuilder().setSource(Source.jwt()).setMessage("Token id hasn't been found").build();
    doThrow(authenticationException).when(authenticator).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(request, authenticationException);
    verifyNoMoreInteractions(threadLocalSession);
  }

  @Test
  public void return_401_and_stop_on_batch_ws() {
    when(request.getRequestURI()).thenReturn("/batch/global");
    doThrow(AuthenticationException.newBuilder().setSource(Source.jwt()).setMessage("Token id hasn't been found").build())
      .when(authenticator).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verifyNoMoreInteractions(threadLocalSession);
  }

  @Test
  public void return_to_session_unauthorized_when_error_on_from_external_provider() throws Exception {
    doThrow(AuthenticationException.newBuilder().setSource(Source.external(newBasicIdentityProvider("failing"))).setPublicMessage("Token id hasn't been found").build())
      .when(authenticator).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).sendRedirect("/sessions/unauthorized");
    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie cookie = cookieArgumentCaptor.getValue();
    assertThat(cookie.getName()).isEqualTo("AUTHENTICATION-ERROR");
    assertThat(cookie.getValue()).isEqualTo("Token%20id%20hasn%27t%20been%20found");
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isFalse();
    assertThat(cookie.getMaxAge()).isEqualTo(300);
    assertThat(cookie.isSecure()).isFalse();
  }

  @Test
  public void return_to_session_unauthorized_when_error_on_from_external_provider_with_context_path() throws Exception {
    when(request.getContextPath()).thenReturn("/sonarqube");
    doThrow(AuthenticationException.newBuilder().setSource(Source.external(newBasicIdentityProvider("failing"))).setPublicMessage("Token id hasn't been found").build())
      .when(authenticator).authenticate(request, response);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).sendRedirect("/sonarqube/sessions/unauthorized");
  }

  @Test
  public void expiration_header_added_when_authenticating_with_an_expiring_token() {
    long expirationTimestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    UserDto userDto = new UserDto();
    UserTokenDto userTokenDto = new UserTokenDto().setExpirationDate(expirationTimestamp);
    UserSession session = new TokenUserSession(DbTester.create().getDbClient(), userDto, userTokenDto);

    when(authenticator.authenticate(request, response)).thenReturn(session);
    when(threadLocalSession.isLoggedIn()).thenReturn(true);

    assertThat(underTest.initUserSession(request, response)).isTrue();
    verify(response).addHeader("SonarQube-Authentication-Token-Expiration", formatDateTime(expirationTimestamp));
  }

  @Test
  public void initUserSession_shouldPutLoginInMDC() {
    when(threadLocalSession.isLoggedIn()).thenReturn(false);
    when(authenticator.authenticate(request, response)).thenReturn(new MockUserSession("user"));

    underTest.initUserSession(request, response);

    assertThat(MDC.get("LOGIN")).isEqualTo("user");
  }

  @Test
  public void initUserSession_whenSessionLoginIsNull_shouldPutDefaultLoginValueInMDC() {
    when(threadLocalSession.isLoggedIn()).thenReturn(false);
    when(authenticator.authenticate(request, response)).thenReturn(new AnonymousMockUserSession());

    underTest.initUserSession(request, response);

    assertThat(MDC.get("LOGIN")).isEqualTo("-");
  }

  @Test
  public void removeUserSession_shoudlRemoveMDCLogin() {
    when(threadLocalSession.isLoggedIn()).thenReturn(false);
    when(authenticator.authenticate(request, response)).thenReturn(new MockUserSession("user"));
    underTest.initUserSession(request, response);

    underTest.removeUserSession();

    assertThat(MDC.get("LOGIN")).isNull();
  }

  private void assertPathIsIgnored(String path) {
    when(request.getRequestURI()).thenReturn(path);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verifyNoMoreInteractions(threadLocalSession, authenticator);
    reset(threadLocalSession, authenticator);
  }

  private void assertPathIsIgnoredWithAnonymousAccess(String path) {
    when(request.getRequestURI()).thenReturn(path);
    UserSession session = new AnonymousMockUserSession();
    when(authenticator.authenticate(request, response)).thenReturn(session);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(threadLocalSession).set(session);
    reset(threadLocalSession, authenticator);
  }

  private void assertPathIsNotIgnored(String path) {
    when(request.getRequestURI()).thenReturn(path);
    UserSession session = new MockUserSession("foo");
    when(authenticator.authenticate(request, response)).thenReturn(session);

    assertThat(underTest.initUserSession(request, response)).isTrue();

    verify(threadLocalSession).set(session);
    reset(threadLocalSession, authenticator);
  }

  private static BaseIdentityProvider newBasicIdentityProvider(String name) {
    BaseIdentityProvider mock = mock(BaseIdentityProvider.class);
    when(mock.getName()).thenReturn(name);
    return mock;
  }
}
