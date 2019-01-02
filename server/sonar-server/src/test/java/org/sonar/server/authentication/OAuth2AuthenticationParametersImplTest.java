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

import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OAuth2AuthenticationParametersImplTest {

  private static final String AUTHENTICATION_COOKIE_NAME = "AUTH-PARAMS";
  private ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);

  private Server server = mock(Server.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);

  private OAuth2AuthenticationParameters underTest = new OAuth2AuthenticationParametersImpl();

  @Before
  public void setUp() throws Exception {
    when(server.getContextPath()).thenReturn("");
  }

  @Test
  public void init_create_cookie() {
    when(request.getParameter("return_to")).thenReturn("/settings");

    underTest.init(request, response);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie cookie = cookieArgumentCaptor.getValue();
    assertThat(cookie.getName()).isEqualTo(AUTHENTICATION_COOKIE_NAME);
    assertThat(cookie.getValue()).isNotEmpty();
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(300);
    assertThat(cookie.getSecure()).isFalse();
  }

  @Test
  public void init_does_not_create_cookie_when_no_parameter() {
    underTest.init(request, response);

    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void init_does_not_create_cookie_when_parameters_are_empty() {
    when(request.getParameter("return_to")).thenReturn("");
    when(request.getParameter("allowEmailShift")).thenReturn("");
    when(request.getParameter("allowUpdateLogin")).thenReturn("");

    underTest.init(request, response);

    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void init_does_not_create_cookie_when_parameters_are_null() {
    when(request.getParameter("return_to")).thenReturn(null);
    when(request.getParameter("allowEmailShift")).thenReturn(null);
    when(request.getParameter("allowUpdateLogin")).thenReturn(null);

    underTest.init(request, response);

    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void return_to_is_not_set_when_not_local() {
    when(request.getParameter("return_to")).thenReturn("http://external_url");
    underTest.init(request, response);
    verify(response, never()).addCookie(any());

    when(request.getParameter("return_to")).thenReturn("//local_file");
    underTest.init(request, response);
    verify(response, never()).addCookie(any());

    when(request.getParameter("return_to")).thenReturn("/\\local_file");
    underTest.init(request, response);
    verify(response, never()).addCookie(any());

    when(request.getParameter("return_to")).thenReturn("something_else");
    underTest.init(request, response);
    verify(response, never()).addCookie(any());
  }

  @Test
  public void get_return_to_parameter() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{\"return_to\":\"/settings\"}")});

    Optional<String> redirection = underTest.getReturnTo(request);

    assertThat(redirection).isNotEmpty();
    assertThat(redirection.get()).isEqualTo("/settings");
  }

  @Test
  public void get_return_to_is_empty_when_no_cookie() {
    when(request.getCookies()).thenReturn(new Cookie[] {});

    Optional<String> redirection = underTest.getReturnTo(request);

    assertThat(redirection).isEmpty();
  }

  @Test
  public void get_return_to_is_empty_when_no_value() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{}")});

    Optional<String> redirection = underTest.getReturnTo(request);

    assertThat(redirection).isEmpty();
  }

  @Test
  public void get_allowEmailShift_parameter() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{\"allowEmailShift\":\"true\"}")});

    Optional<Boolean> allowEmailShift = underTest.getAllowEmailShift(request);

    assertThat(allowEmailShift).isNotEmpty();
    assertThat(allowEmailShift.get()).isTrue();
  }

  @Test
  public void get_allowEmailShift_is_empty_when_no_cookie() {
    when(request.getCookies()).thenReturn(new Cookie[] {});

    Optional<Boolean> allowEmailShift = underTest.getAllowEmailShift(request);

    assertThat(allowEmailShift).isEmpty();
  }

  @Test
  public void get_allowEmailShift_is_empty_when_no_value() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{}")});

    Optional<Boolean> allowEmailShift = underTest.getAllowEmailShift(request);

    assertThat(allowEmailShift).isEmpty();
  }

  @Test
  public void getAllowUpdateLogin() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{\"allowUpdateLogin\":\"true\"}")});

    Optional<Boolean> allowLoginUpdate = underTest.getAllowUpdateLogin(request);

    assertThat(allowLoginUpdate).isNotEmpty();
    assertThat(allowLoginUpdate.get()).isTrue();
  }

  @Test
  public void getAllowUpdateLogin_is_empty_when_no_cookie() {
    when(request.getCookies()).thenReturn(new Cookie[] {});

    Optional<Boolean> allowLoginUpdate = underTest.getAllowUpdateLogin(request);

    assertThat(allowLoginUpdate).isEmpty();
  }

  @Test
  public void getAllowUpdateLogin_is_empty_when_no_value() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{}")});

    Optional<Boolean> allowLoginUpdate = underTest.getAllowUpdateLogin(request);

    assertThat(allowLoginUpdate).isEmpty();
  }

  @Test
  public void delete() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(AUTHENTICATION_COOKIE_NAME, "{\"return_to\":\"/settings\"}")});

    underTest.delete(request, response);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie updatedCookie = cookieArgumentCaptor.getValue();
    assertThat(updatedCookie.getName()).isEqualTo(AUTHENTICATION_COOKIE_NAME);
    assertThat(updatedCookie.getValue()).isNull();
    assertThat(updatedCookie.getPath()).isEqualTo("/");
    assertThat(updatedCookie.getMaxAge()).isEqualTo(0);
  }
}
