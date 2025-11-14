/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.server.http.JakartaHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class OAuth2AuthenticationParametersImplTest {

  private static final String AUTHENTICATION_COOKIE_NAME = "AUTH-PARAMS";
  private final ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final HttpRequest request = mock(HttpRequest.class);

  private final OAuth2AuthenticationParameters underTest = new OAuth2AuthenticationParametersImpl();

  @Before
  public void setUp() {
    when(request.getContextPath()).thenReturn("");
  }

  @Test
  public void init_create_cookie() {
    when(request.getParameter("return_to")).thenReturn("/admin/settings");

    underTest.init(request, response);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie cookie = cookieArgumentCaptor.getValue();
    assertThat(cookie.getName()).isEqualTo(AUTHENTICATION_COOKIE_NAME);
    assertThat(cookie.getValue()).isNotEmpty();
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(300);
    assertThat(cookie.isSecure()).isFalse();
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

    underTest.init(request, response);

    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  public void init_does_not_create_cookie_when_parameters_are_null() {
    when(request.getParameter("return_to")).thenReturn(null);
    when(request.getParameter("allowEmailShift")).thenReturn(null);

    underTest.init(request, response);

    verify(response, never()).addCookie(any(Cookie.class));
  }

  @Test
  @DataProvider({"http://example.com", "/\t/example.com", "//local_file", "/\\local_file", "something_else"})
  public void get_return_to_is_not_set_when_not_local(String url) {
    when(request.getParameter("return_to")).thenReturn(url);

    assertThat(underTest.getReturnTo(request)).isEmpty();
  }

  @Test
  public void get_return_to_parameter() {
    when(request.getCookies()).thenReturn(new Cookie[]{wrapCookie(AUTHENTICATION_COOKIE_NAME, "{\"return_to\":\"/admin/settings\"}")});

    Optional<String> redirection = underTest.getReturnTo(request);

    assertThat(redirection).isPresent();
    String actualUrl = redirection.get();
    assertThat(actualUrl).satisfiesAnyOf(
      url -> assertThat(url).isEqualTo("/admin/settings"),
      url -> assertThat(url).isEqualTo("%5Cadmin%5Csettings")
    );
  }

  @Test
  public void get_return_to_is_empty_when_no_cookie() {
    when(request.getCookies()).thenReturn(new Cookie[]{});

    Optional<String> redirection = underTest.getReturnTo(request);

    assertThat(redirection).isEmpty();
  }

  @Test
  public void get_return_to_is_empty_when_no_value() {
    when(request.getCookies()).thenReturn(new Cookie[]{wrapCookie(AUTHENTICATION_COOKIE_NAME, "{}")});

    Optional<String> redirection = underTest.getReturnTo(request);

    assertThat(redirection).isEmpty();
  }

  @Test
  public void delete() {
    when(request.getCookies()).thenReturn(new Cookie[]{wrapCookie(AUTHENTICATION_COOKIE_NAME, "{\"return_to\":\"/admin/settings\"}")});

    underTest.delete(request, response);

    verify(response).addCookie(cookieArgumentCaptor.capture());
    Cookie updatedCookie = cookieArgumentCaptor.getValue();
    assertThat(updatedCookie.getName()).isEqualTo(AUTHENTICATION_COOKIE_NAME);
    assertThat(updatedCookie.getValue()).isNull();
    assertThat(updatedCookie.getPath()).isEqualTo("/");
    assertThat(updatedCookie.getMaxAge()).isZero();
  }

  @DataProvider
  public static Object[][] payloadToSanitizeAndExpectedOutcome() {
    return new Object[][]{
      {generatePath("/admin/settings"), "/admin/settings", "%5Cadmin%5Csettings"},
      {generatePath("/admin/../../settings"), "/settings", "%5Csettings"},
      {generatePath("/admin/../settings"), "/settings", "%5Csettings"},
      {generatePath("/admin/settings/.."), "/admin", "%5Cadmin"},
      {generatePath("/admin/..%2fsettings/"), "/settings", "%5Csettings"},
      {generatePath("/admin/%2e%2e%2fsettings/"), "/settings", "%5Csettings"},
      {generatePath("../admin/settings"), null, null},
      {generatePath("/dashboard?id=project&pullRequest=PRID"), "/dashboard?id=project&pullRequest=PRID", "%5Cdashboard?id=project&pullRequest=PRID"},
      {generatePath("%2Fdashboard%3Fid%3Dproject%26pullRequest%3DPRID&authorizationError=true"), "/dashboard?id=project&pullRequest=PRID&authorizationError=true", "%5Cdashboard?id=project&pullRequest=PRID&authorizationError=true"},
    };
  }

  private static String generatePath(String returnTo) {
    return "{\"return_to\":\"" + returnTo + "\"}";
  }

  @Test
  @UseDataProvider("payloadToSanitizeAndExpectedOutcome")
  public void getReturnTo_whenContainingPathTraversalCharacters_sanitizeThem(String payload, @Nullable String expectedSanitizedUrl, @Nullable String expectedWindowsSanitizedUrl) {
    when(request.getCookies()).thenReturn(new Cookie[]{wrapCookie(AUTHENTICATION_COOKIE_NAME, payload)});

    Optional<String> redirection = underTest.getReturnTo(request);

    if (expectedSanitizedUrl == null) {
      assertThat(redirection).isEmpty();
    } else {
      String actualUrl = redirection.orElseThrow();
      assertThat(actualUrl).satisfiesAnyOf(
        url -> assertThat(url).isEqualTo(expectedSanitizedUrl),
        url -> assertThat(url).isEqualTo(expectedWindowsSanitizedUrl)
      );
    }
  }

  private JakartaHttpRequest.JakartaCookie wrapCookie(String name, String value) {
    return new JakartaHttpRequest.JakartaCookie(new jakarta.servlet.http.Cookie(name, value));
  }
}
