/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CookieUtilsTest {

  private static final String HTTPS_HEADER = "X-Forwarded-Proto";

  HttpServletRequest request = mock(HttpServletRequest.class);

  @Test
  public void create_cookie() throws Exception {
    Cookie cookie = CookieUtils.createCookie("name", "value", true, 10, request);
    assertThat(cookie.getName()).isEqualTo("name");
    assertThat(cookie.getValue()).isEqualTo("value");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(10);
    assertThat(cookie.getSecure()).isFalse();
  }

  @Test
  public void create_not_secured_cookie_when_header_is_not_http() throws Exception {
    when(request.getHeader(HTTPS_HEADER)).thenReturn("http");
    Cookie cookie = CookieUtils.createCookie("name", "value", true, 10, request);
    assertThat(cookie.getSecure()).isFalse();
  }

  @Test
  public void create_secured_cookie_when_X_Forwarded_Proto_header_is_https() throws Exception {
    when(request.getHeader(HTTPS_HEADER)).thenReturn("https");
    Cookie cookie = CookieUtils.createCookie("name", "value", true, 10, request);
    assertThat(cookie.getSecure()).isTrue();
  }

  @Test
  public void create_secured_cookie_when_X_Forwarded_Proto_header_is_HTTPS() throws Exception {
    when(request.getHeader(HTTPS_HEADER)).thenReturn("HTTPS");
    Cookie cookie = CookieUtils.createCookie("name", "value", true, 10, request);
    assertThat(cookie.getSecure()).isTrue();
  }

  @Test
  public void find_cookie() throws Exception {
    Cookie cookie = new Cookie("name", "value");
    when(request.getCookies()).thenReturn(new Cookie[] {cookie});

    assertThat(CookieUtils.findCookie("name", request)).isPresent();
    assertThat(CookieUtils.findCookie("NAME", request)).isEmpty();
    assertThat(CookieUtils.findCookie("unknown", request)).isEmpty();
  }

  @Test
  public void does_not_fail_to_find_cookie_when_no_cookie() throws Exception {
    assertThat(CookieUtils.findCookie("unknown", request)).isEmpty();
  }
}
