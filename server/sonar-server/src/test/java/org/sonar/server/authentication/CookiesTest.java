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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;

public class CookiesTest {

  private static final String HTTPS_HEADER = "X-Forwarded-Proto";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private HttpServletRequest request = mock(HttpServletRequest.class);

  @Test
  public void create_cookie() {
    Cookie cookie = newCookieBuilder(request).setName("name").setValue("value").setHttpOnly(true).setExpiry(10).build();
    assertThat(cookie.getName()).isEqualTo("name");
    assertThat(cookie.getValue()).isEqualTo("value");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(10);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getPath()).isEqualTo("/");
  }

  @Test
  public void create_cookie_without_value() {
    Cookie cookie = newCookieBuilder(request).setName("name").build();
    assertThat(cookie.getName()).isEqualTo("name");
    assertThat(cookie.getValue()).isNull();
  }

  @Test
  public void create_cookie_when_web_context() {
    when(request.getContextPath()).thenReturn("/sonarqube");
    Cookie cookie = newCookieBuilder(request).setName("name").setValue("value").setHttpOnly(true).setExpiry(10).build();
    assertThat(cookie.getName()).isEqualTo("name");
    assertThat(cookie.getValue()).isEqualTo("value");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(10);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getPath()).isEqualTo("/sonarqube");
  }

  @Test
  public void create_not_secured_cookie_when_header_is_not_http() {
    when(request.getHeader(HTTPS_HEADER)).thenReturn("http");
    Cookie cookie = newCookieBuilder(request).setName("name").setValue("value").setHttpOnly(true).setExpiry(10).build();
    assertThat(cookie.getSecure()).isFalse();
  }

  @Test
  public void create_secured_cookie_when_X_Forwarded_Proto_header_is_https() {
    when(request.getHeader(HTTPS_HEADER)).thenReturn("https");
    Cookie cookie = newCookieBuilder(request).setName("name").setValue("value").setHttpOnly(true).setExpiry(10).build();
    assertThat(cookie.getSecure()).isTrue();
  }

  @Test
  public void create_secured_cookie_when_X_Forwarded_Proto_header_is_HTTPS() {
    when(request.getHeader(HTTPS_HEADER)).thenReturn("HTTPS");
    Cookie cookie = newCookieBuilder(request).setName("name").setValue("value").setHttpOnly(true).setExpiry(10).build();
    assertThat(cookie.getSecure()).isTrue();
  }

  @Test
  public void find_cookie() {
    Cookie cookie = newCookieBuilder(request).setName("name").setValue("value").build();
    when(request.getCookies()).thenReturn(new Cookie[] {cookie});

    assertThat(findCookie("name", request)).isPresent();
    assertThat(findCookie("NAME", request)).isEmpty();
    assertThat(findCookie("unknown", request)).isEmpty();
  }

  @Test
  public void does_not_fail_to_find_cookie_when_no_cookie() {
    assertThat(findCookie("unknown", request)).isEmpty();
  }

  @Test
  public void fail_with_NPE_when_cookie_name_is_null() {
    expectedException.expect(NullPointerException.class);
    newCookieBuilder(request).setName(null);
  }

  @Test
  public void fail_with_NPE_when_cookie_has_no_name() {
    expectedException.expect(NullPointerException.class);
    newCookieBuilder(request).setName(null);
  }

}
