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
package org.sonar.server.authentication;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserSessionInitializerMcpTest {
  private final MapSettings configuration = new MapSettings();
  private final ThreadLocalUserSession session = new ThreadLocalUserSession();
  private final AuthenticationEvent events = mock(AuthenticationEvent.class);
  private final RequestAuthenticator authenticator = mock(RequestAuthenticator.class);
  private final McpOAuthDiscovery discovery = mock(McpOAuthDiscovery.class);
  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final UserSessionInitializer underTest = new UserSessionInitializer(configuration.asConfig(), session, events, authenticator);

  @BeforeEach
  void setUp() {
    underTest.setMcpOAuthDiscovery(discovery);
    when(discovery.isEnabled()).thenReturn(true);
    when(discovery.challenge()).thenReturn("Bearer resource_metadata=\"https://public.example/metadata\"");
    when(request.getContextPath()).thenReturn("/sonar.v1");
    when(request.getRequestURI()).thenReturn("/sonar.v1/mcp");
  }

  @Test
  void discovery_is_public_even_with_invalid_credentials_and_forced_authentication() {
    for (String path : List.of(McpOAuthDiscovery.RESOURCE_METADATA_PATH, McpOAuthDiscovery.AUTHORIZATION_METADATA_PATH,
      McpOAuthDiscovery.AUTHORIZATION_METADATA_PATH + "/sonar.v1")) {
      when(request.getRequestURI()).thenReturn("/sonar.v1" + path);
      assertThat(underTest.initUserSession(request, response)).isTrue();
    }
    verifyNoInteractions(authenticator, response);
  }

  @Test
  void forced_authentication_failure_returns_challenge_instead_of_redirect() {
    UserSession anonymousSession = mock(UserSession.class);
    when(authenticator.authenticate(request, response)).thenReturn(anonymousSession);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verify(response).setHeader("WWW-Authenticate", discovery.challenge());
    verify(response).setHeader("Cache-Control", "no-store");
  }

  @Test
  void invalid_credentials_from_an_identity_provider_do_not_redirect_mcp_clients() {
    AuthenticationException failure = AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.realm(AuthenticationEvent.Method.BASIC, "provider"))
      .setMessage("Invalid credentials").build();
    when(authenticator.authenticate(request, response)).thenThrow(failure);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(events).loginFailure(request, failure);
    verify(response).setStatus(401);
    verify(response).setHeader("WWW-Authenticate", discovery.challenge());
  }

  @Test
  void invalid_tokens_return_an_invalid_token_challenge_on_mcp_subpaths() {
    when(request.getRequestURI()).thenReturn("/sonar.v1/mcp/tools/list");
    AuthenticationException failure = AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.local(AuthenticationEvent.Method.SONARQUBE_TOKEN))
      .setMessage("Token is expired").build();
    when(authenticator.authenticate(request, response)).thenThrow(failure);

    assertThat(underTest.initUserSession(request, response)).isFalse();

    verify(response).setStatus(401);
    verify(response).setHeader("WWW-Authenticate", discovery.challenge() + ", error=\"invalid_token\"");
  }

  @Test
  void authenticated_sessions_are_preserved() {
    UserSession userSession = mock(UserSession.class);
    when(userSession.isLoggedIn()).thenReturn(true);
    when(authenticator.authenticate(request, response)).thenReturn(userSession);

    assertThat(underTest.initUserSession(request, response)).isTrue();
    assertThat(session.isLoggedIn()).isTrue();
    verifyNoInteractions(response);
    underTest.removeUserSession();
    assertThat(session.hasSession()).isFalse();
  }

  @Test
  void anonymous_requests_reach_handler_when_forced_authentication_is_disabled() {
    configuration.setProperty("sonar.forceAuthentication", false);
    UserSession anonymousSession = mock(UserSession.class);
    when(authenticator.authenticate(request, response)).thenReturn(anonymousSession);

    assertThat(underTest.initUserSession(request, response)).isTrue();
    verifyNoInteractions(response);
    underTest.removeUserSession();
  }

  @Test
  void disabled_or_unavailable_oauth_preserves_existing_behavior() {
    UserSession anonymousSession = mock(UserSession.class);
    when(authenticator.authenticate(request, response)).thenReturn(anonymousSession);
    when(discovery.isEnabled()).thenReturn(false);

    assertThat(underTest.initUserSession(request, response)).isTrue();
    UserSessionInitializer withoutExtension = new UserSessionInitializer(configuration.asConfig(), session, events, authenticator);
    assertThat(withoutExtension.initUserSession(request, response)).isTrue();
    verify(discovery, never()).challenge();
    verifyNoInteractions(response);
  }

  @Test
  void similar_paths_do_not_bypass_authentication_or_receive_an_mcp_challenge() {
    UserSession anonymousSession = mock(UserSession.class);
    when(authenticator.authenticate(request, response)).thenReturn(anonymousSession);
    for (String path : List.of("/mcp-other", "/.well-known/unrelated", "/.well-known/oauth-protected-resource/mcp/extra")) {
      when(request.getRequestURI()).thenReturn("/sonar.v1" + path);
      assertThat(underTest.initUserSession(request, response)).isTrue();
    }
    verifyNoInteractions(response);
    verify(discovery, never()).challenge();
  }
}

