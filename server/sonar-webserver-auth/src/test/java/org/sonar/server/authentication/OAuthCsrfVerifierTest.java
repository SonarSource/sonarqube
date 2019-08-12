/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.server.authentication.event.AuthenticationEvent;

import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class OAuthCsrfVerifierTest {
  private static final String PROVIDER_NAME = "provider name";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);

  private OAuth2IdentityProvider identityProvider = mock(OAuth2IdentityProvider.class);
  private Server server = mock(Server.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);

  private OAuthCsrfVerifier underTest = new OAuthCsrfVerifier();

  @Before
  public void setUp() throws Exception {
    when(server.getContextPath()).thenReturn("");
    when(identityProvider.getName()).thenReturn(PROVIDER_NAME);
  }

  @Test
  public void generate_state() {
    String state = underTest.generateState(request, response);
    assertThat(state).isNotEmpty();

    verify(response).addCookie(cookieArgumentCaptor.capture());

    verifyCookie(cookieArgumentCaptor.getValue());
  }

  @Test
  public void verify_state() {
    String state = "state";
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("OAUTHSTATE", sha256Hex(state))});
    when(request.getParameter("aStateParameter")).thenReturn(state);

    underTest.verifyState(request, response, identityProvider, "aStateParameter");

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie updatedCookie = cookieArgumentCaptor.getValue();
    assertThat(updatedCookie.getName()).isEqualTo("OAUTHSTATE");
    assertThat(updatedCookie.getValue()).isNull();
    assertThat(updatedCookie.getPath()).isEqualTo("/");
    assertThat(updatedCookie.getMaxAge()).isEqualTo(0);
  }

  @Test
  public void verify_state_using_default_state_parameter() {
    String state = "state";
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("OAUTHSTATE", sha256Hex(state))});
    when(request.getParameter("state")).thenReturn(state);

    underTest.verifyState(request, response, identityProvider);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie updatedCookie = cookieArgumentCaptor.getValue();
    assertThat(updatedCookie.getName()).isEqualTo("OAUTHSTATE");
    assertThat(updatedCookie.getValue()).isNull();
    assertThat(updatedCookie.getPath()).isEqualTo("/");
    assertThat(updatedCookie.getMaxAge()).isEqualTo(0);
  }

  @Test
  public void fail_with_AuthenticationException_when_state_cookie_is_not_the_same_as_state_parameter() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("OAUTHSTATE", sha1Hex("state"))});
    when(request.getParameter("state")).thenReturn("other value");

    thrown.expect(authenticationException().from(AuthenticationEvent.Source.oauth2(identityProvider)).withoutLogin().andNoPublicMessage());
    thrown.expectMessage("CSRF state value is invalid");
    underTest.verifyState(request, response, identityProvider);
  }

  @Test
  public void fail_with_AuthenticationException_when_state_cookie_is_null() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("OAUTHSTATE", null)});
    when(request.getParameter("state")).thenReturn("state");

    thrown.expect(authenticationException().from(AuthenticationEvent.Source.oauth2(identityProvider)).withoutLogin().andNoPublicMessage());
    thrown.expectMessage("CSRF state value is invalid");
    underTest.verifyState(request, response, identityProvider);
  }

  @Test
  public void fail_with_AuthenticationException_when_state_parameter_is_empty() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("OAUTHSTATE", sha1Hex("state"))});
    when(request.getParameter("state")).thenReturn("");

    thrown.expect(authenticationException().from(AuthenticationEvent.Source.oauth2(identityProvider)).withoutLogin().andNoPublicMessage());
    thrown.expectMessage("CSRF state value is invalid");
    underTest.verifyState(request, response, identityProvider);
  }

  @Test
  public void fail_with_AuthenticationException_when_cookie_is_missing() {
    when(request.getCookies()).thenReturn(new Cookie[] {});

    thrown.expect(authenticationException().from(AuthenticationEvent.Source.oauth2(identityProvider)).withoutLogin().andNoPublicMessage());
    thrown.expectMessage("Cookie 'OAUTHSTATE' is missing");
    underTest.verifyState(request, response, identityProvider);
  }

  private void verifyCookie(Cookie cookie) {
    assertThat(cookie.getName()).isEqualTo("OAUTHSTATE");
    assertThat(cookie.getValue()).isNotEmpty();
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(-1);
    assertThat(cookie.getSecure()).isFalse();
  }
}
