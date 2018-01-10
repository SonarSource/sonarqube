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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class JwtCsrfVerifierTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final int TIMEOUT = 30;
  private static final String CSRF_STATE = "STATE";
  private static final String JAVA_WS_URL = "/api/metrics/create";
  private static final String LOGIN = "foo login";

  private ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);

  private HttpServletResponse response = mock(HttpServletResponse.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);

  private JwtCsrfVerifier underTest = new JwtCsrfVerifier();

  @Before
  public void setUp() throws Exception {
    when(request.getContextPath()).thenReturn("");
  }

  @Test
  public void generate_state() {
    String state = underTest.generateState(request, response, TIMEOUT);
    assertThat(state).isNotEmpty();

    verify(response).addCookie(cookieArgumentCaptor.capture());
    verifyCookie(cookieArgumentCaptor.getValue());
  }

  @Test
  public void verify_state() {
    mockRequestCsrf(CSRF_STATE);
    mockPostJavaWsRequest();

    underTest.verifyState(request, CSRF_STATE, LOGIN);
  }

  @Test
  public void fail_with_AuthenticationException_when_state_header_is_not_the_same_as_state_parameter() {
    mockRequestCsrf("other value");
    mockPostJavaWsRequest();

    thrown.expect(authenticationException().from(Source.local(Method.JWT)).withLogin(LOGIN).andNoPublicMessage());
    thrown.expectMessage("Wrong CSFR in request");
    underTest.verifyState(request, CSRF_STATE, LOGIN);
  }

  @Test
  public void fail_with_AuthenticationException_when_state_is_null() {
    mockRequestCsrf(CSRF_STATE);
    mockPostJavaWsRequest();

    thrown.expect(authenticationException().from(Source.local(Method.JWT)).withLogin(LOGIN).andNoPublicMessage());
    thrown.expectMessage("Missing reference CSRF value");
    underTest.verifyState(request, null, LOGIN);
  }

  @Test
  public void fail_with_AuthenticationException_when_state_parameter_is_empty() {
    mockRequestCsrf(CSRF_STATE);
    mockPostJavaWsRequest();

    thrown.expect(authenticationException().from(Source.local(Method.JWT)).withLogin(LOGIN).andNoPublicMessage());
    thrown.expectMessage("Missing reference CSRF value");
    underTest.verifyState(request, "", LOGIN);
  }

  @Test
  public void verify_POST_request() {
    mockRequestCsrf("other value");
    when(request.getRequestURI()).thenReturn(JAVA_WS_URL);
    when(request.getMethod()).thenReturn("POST");

    thrown.expect(authenticationException().from(Source.local(Method.JWT)).withLogin(LOGIN).andNoPublicMessage());
    thrown.expectMessage("Wrong CSFR in request");
    underTest.verifyState(request, CSRF_STATE, LOGIN);
  }

  @Test
  public void verify_PUT_request() {
    mockRequestCsrf("other value");
    when(request.getRequestURI()).thenReturn(JAVA_WS_URL);
    when(request.getMethod()).thenReturn("PUT");

    thrown.expect(authenticationException().from(Source.local(Method.JWT)).withLogin(LOGIN).andNoPublicMessage());
    thrown.expectMessage("Wrong CSFR in request");
    underTest.verifyState(request, CSRF_STATE, LOGIN);
  }

  @Test
  public void verify_DELETE_request() {
    mockRequestCsrf("other value");
    when(request.getRequestURI()).thenReturn(JAVA_WS_URL);
    when(request.getMethod()).thenReturn("DELETE");

    thrown.expect(authenticationException().from(Source.local(Method.JWT)).withLogin(LOGIN).andNoPublicMessage());
    thrown.expectMessage("Wrong CSFR in request");
    underTest.verifyState(request, CSRF_STATE, LOGIN);
  }

  @Test
  public void ignore_GET_request() {
    when(request.getRequestURI()).thenReturn(JAVA_WS_URL);
    when(request.getMethod()).thenReturn("GET");

    underTest.verifyState(request, null, LOGIN);
  }

  @Test
  public void ignore_not_api_requests() {
    executeVerifyStateDoesNotFailOnRequest("/events", "POST");
    executeVerifyStateDoesNotFailOnRequest("/favorites", "POST");
  }

  @Test
  public void refresh_state() {
    underTest.refreshState(request, response, CSRF_STATE, 30);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    verifyCookie(cookieArgumentCaptor.getValue());
  }

  @Test
  public void remove_state() {
    underTest.removeState(request, response);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie cookie = cookieArgumentCaptor.getValue();
    assertThat(cookie.getValue()).isNull();
    assertThat(cookie.getMaxAge()).isEqualTo(0);
  }

  private void verifyCookie(Cookie cookie) {
    assertThat(cookie.getName()).isEqualTo("XSRF-TOKEN");
    assertThat(cookie.getValue()).isNotEmpty();
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isFalse();
    assertThat(cookie.getMaxAge()).isEqualTo(TIMEOUT);
    assertThat(cookie.getSecure()).isFalse();
  }

  private void mockPostJavaWsRequest() {
    when(request.getRequestURI()).thenReturn(JAVA_WS_URL);
    when(request.getMethod()).thenReturn("POST");
  }

  private void mockRequestCsrf(String csrfState) {
    when(request.getHeader("X-XSRF-TOKEN")).thenReturn(csrfState);
  }

  private void executeVerifyStateDoesNotFailOnRequest(String uri, String method) {
    when(request.getRequestURI()).thenReturn(uri);
    when(request.getMethod()).thenReturn(method);

    underTest.verifyState(request, null, LOGIN);
  }
}
