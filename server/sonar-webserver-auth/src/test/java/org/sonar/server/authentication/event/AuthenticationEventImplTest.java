/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.authentication.event;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationException.newBuilder;

public class AuthenticationEventImplTest {
  private static final String LOGIN_129_CHARS = "012345678901234567890123456789012345678901234567890123456789" +
    "012345678901234567890123456789012345678901234567890123456789012345678";

  @Rule
  public LogTester logTester = new LogTester();

  private final AuthenticationEventImpl underTest = new AuthenticationEventImpl();

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    logTester.clear();
  }

  @Test
  public void login_success_fails_with_NPE_if_request_is_null() {
    logTester.setLevel(Level.INFO);

    Source sso = Source.sso();
    assertThatThrownBy(() -> underTest.loginSuccess(null, "login", sso))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("request can't be null");
  }

  @Test
  public void login_success_fails_with_NPE_if_source_is_null() {
    logTester.setLevel(Level.INFO);

    assertThatThrownBy(() -> underTest.loginSuccess(mock(HttpRequest.class), "login", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("source can't be null");
  }

  @Test
  public void login_success_does_not_interact_with_request_if_log_level_is_above_DEBUG() {
    HttpRequest request = mock(HttpRequest.class);
    logTester.setLevel(Level.INFO);

    underTest.loginSuccess(request, "login", Source.sso());

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void login_success_message_is_sanitized() {
    logTester.setLevel(Level.DEBUG);

    underTest.loginSuccess(mockRequest("1.2.3.4"), "login with \n malicious line \r return", Source.sso());

    assertThat(logTester.logs()).isNotEmpty()
      .contains("login success [method|SSO][provider|SSO|sso][IP|1.2.3.4|][login|login with _ malicious line _ return]");
  }

  @Test
  public void login_success_creates_DEBUG_log_with_empty_login_if_login_argument_is_null() {
    underTest.loginSuccess(mockRequest(), null, Source.sso());

    verifyLog("login success [method|SSO][provider|SSO|sso][IP||][login|]", Set.of("logout", "login failure"));
  }

  @Test
  public void login_success_creates_DEBUG_log_with_method_provider_and_login() {
    underTest.loginSuccess(mockRequest(), "foo", Source.realm(Method.BASIC, "some provider name"));

    verifyLog("login success [method|BASIC][provider|REALM|some provider name][IP||][login|foo]", Set.of("logout", "login failure"));
  }

  @Test
  public void login_success_prevents_log_flooding_on_login_starting_from_128_chars() {
    underTest.loginSuccess(mockRequest(), LOGIN_129_CHARS, Source.realm(Method.BASIC, "some provider name"));

    verifyLog("login success [method|BASIC][provider|REALM|some provider name][IP||][login|012345678901234567890123456789012345678901234567890123456789" +
      "01234567890123456789012345678901234567890123456789012345678901234567...(129)]",
      Set.of("logout", "login failure"));
  }

  @Test
  public void login_success_logs_remote_ip_from_request() {
    underTest.loginSuccess(mockRequest("1.2.3.4"), "foo", Source.realm(Method.EXTERNAL, "bar"));

    verifyLog("login success [method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|][login|foo]", Set.of("logout", "login failure"));
  }

  @Test
  public void login_success_logs_X_Forwarded_For_header_from_request() {
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5"));
    underTest.loginSuccess(request, "foo", Source.realm(Method.EXTERNAL, "bar"));

    verifyLog("login success [method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5][login|foo]", Set.of("logout", "login failure"));
  }

  @Test
  public void login_success_logs_X_Forwarded_For_header_from_request_and_supports_multiple_headers() {
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5", "6.5.4.3"), List.of("9.5.6.7"), List.of("6.3.2.4"));
    underTest.loginSuccess(request, "foo", Source.realm(Method.EXTERNAL, "bar"));

    verifyLog("login success [method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5,6.5.4.3,9.5.6.7,6.3.2.4][login|foo]",
      Set.of("logout", "login failure"));
  }

  @Test
  public void login_failure_fails_with_NPE_if_request_is_null() {
    logTester.setLevel(Level.INFO);

    AuthenticationException exception = newBuilder().setSource(Source.sso()).build();
    assertThatThrownBy(() -> underTest.loginFailure(null, exception))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("request can't be null");
  }

  @Test
  public void login_failure_fails_with_NPE_if_AuthenticationException_is_null() {
    logTester.setLevel(Level.INFO);

    assertThatThrownBy(() -> underTest.loginFailure(mock(HttpRequest.class), null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("AuthenticationException can't be null");
  }

  @Test
  public void login_failure_does_not_interact_with_arguments_if_log_level_is_above_DEBUG() {
    HttpRequest request = mock(HttpRequest.class);
    AuthenticationException exception = mock(AuthenticationException.class);
    logTester.setLevel(Level.INFO);

    underTest.loginFailure(request, exception);

    verifyNoInteractions(request, exception);
  }

  @Test
  public void login_failure_creates_DEBUG_log_with_empty_login_if_AuthenticationException_has_no_login() {
    AuthenticationException exception = newBuilder().setSource(Source.sso()).setMessage("message").build();
    underTest.loginFailure(mockRequest(), exception);

    verifyLog("login failure [cause|message][method|SSO][provider|SSO|sso][IP||][login|]", Set.of("logout", "login success"));
  }

  @Test
  public void login_failure_creates_DEBUG_log_with_empty_cause_if_AuthenticationException_has_no_message() {
    AuthenticationException exception = newBuilder().setSource(Source.sso()).setLogin("FoO").build();
    underTest.loginFailure(mockRequest(), exception);

    verifyLog("login failure [cause|][method|SSO][provider|SSO|sso][IP||][login|FoO]", Set.of("logout", "login success"));
  }

  @Test
  public void login_failure_creates_DEBUG_log_with_method_provider_and_login() {
    AuthenticationException exception = newBuilder()
      .setSource(Source.realm(Method.BASIC, "some provider name"))
      .setMessage("something got terribly wrong")
      .setLogin("BaR")
      .build();
    underTest.loginFailure(mockRequest(), exception);

    verifyLog("login failure [cause|something got terribly wrong][method|BASIC][provider|REALM|some provider name][IP||][login|BaR]",
      Set.of("logout", "login success"));
  }

  @Test
  public void login_failure_prevents_log_flooding_on_login_starting_from_128_chars() {
    AuthenticationException exception = newBuilder()
      .setSource(Source.realm(Method.BASIC, "some provider name"))
      .setMessage("pop")
      .setLogin(LOGIN_129_CHARS)
      .build();
    underTest.loginFailure(mockRequest(), exception);

    verifyLog("login failure [cause|pop][method|BASIC][provider|REALM|some provider name][IP||][login|012345678901234567890123456789012345678901234567890123456789" +
      "01234567890123456789012345678901234567890123456789012345678901234567...(129)]",
      Set.of("logout", "login success"));
  }

  @Test
  public void login_failure_logs_remote_ip_from_request() {
    AuthenticationException exception = newBuilder()
      .setSource(Source.realm(Method.EXTERNAL, "bar"))
      .setMessage("Damn it!")
      .setLogin("Baaad")
      .build();
    underTest.loginFailure(mockRequest("1.2.3.4"), exception);

    verifyLog("login failure [cause|Damn it!][method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|][login|Baaad]",
      Set.of("logout", "login success"));
  }

  @Test
  public void login_failure_logs_X_Forwarded_For_header_from_request() {
    AuthenticationException exception = newBuilder()
      .setSource(Source.realm(Method.EXTERNAL, "bar"))
      .setMessage("Hop la!")
      .setLogin("foo")
      .build();
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5"));
    underTest.loginFailure(request, exception);

    verifyLog("login failure [cause|Hop la!][method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5][login|foo]",
      Set.of("logout", "login success"));
  }

  @Test
  public void login_failure_logs_X_Forwarded_For_header_from_request_and_supports_multiple_headers() {
    AuthenticationException exception = newBuilder()
      .setSource(Source.realm(Method.EXTERNAL, "bar"))
      .setMessage("Boom!")
      .setLogin("foo")
      .build();
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5", "6.5.4.3"), List.of("9.5.6.7"), List.of("6.3.2.4"));
    underTest.loginFailure(request, exception);

    verifyLog("login failure [cause|Boom!][method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5,6.5.4.3,9.5.6.7,6.3.2.4][login|foo]",
      Set.of("logout", "login success"));
  }

  @Test
  public void logout_success_fails_with_NPE_if_request_is_null() {
    logTester.setLevel(Level.INFO);

    assertThatThrownBy(() -> underTest.logoutSuccess(null, "foo"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("request can't be null");
  }

  @Test
  public void logout_success_does_not_interact_with_request_if_log_level_is_above_DEBUG() {
    HttpRequest request = mock(HttpRequest.class);
    logTester.setLevel(Level.INFO);

    underTest.logoutSuccess(request, "foo");

    verifyNoInteractions(request);
  }

  @Test
  public void logout_success_creates_DEBUG_log_with_empty_login_if_login_argument_is_null() {
    underTest.logoutSuccess(mockRequest(), null);

    verifyLog("logout success [IP||][login|]", Set.of("login", "logout failure"));
  }

  @Test
  public void logout_success_creates_DEBUG_log_with_login() {
    underTest.logoutSuccess(mockRequest(), "foo");

    verifyLog("logout success [IP||][login|foo]", Set.of("login", "logout failure"));
  }

  @Test
  public void logout_success_logs_remote_ip_from_request() {
    underTest.logoutSuccess(mockRequest("1.2.3.4"), "foo");

    verifyLog("logout success [IP|1.2.3.4|][login|foo]", Set.of("login", "logout failure"));
  }

  @Test
  public void logout_success_logs_X_Forwarded_For_header_from_request() {
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5"));
    underTest.logoutSuccess(request, "foo");

    verifyLog("logout success [IP|1.2.3.4|2.3.4.5][login|foo]", Set.of("login", "logout failure"));
  }

  @Test
  public void logout_success_logs_X_Forwarded_For_header_from_request_and_supports_multiple_headers() {
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5", "6.5.4.3"), List.of("9.5.6.7"), List.of("6.3.2.4"));
    underTest.logoutSuccess(request, "foo");

    verifyLog("logout success [IP|1.2.3.4|2.3.4.5,6.5.4.3,9.5.6.7,6.3.2.4][login|foo]", Set.of("login", "logout failure"));
  }

  @Test
  public void logout_failure_with_NPE_if_request_is_null() {
    logTester.setLevel(Level.INFO);

    assertThatThrownBy(() -> underTest.logoutFailure(null, "bad csrf"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("request can't be null");
  }

  @Test
  public void login_fails_with_NPE_if_error_message_is_null() {
    logTester.setLevel(Level.INFO);

    assertThatThrownBy(() -> underTest.logoutFailure(mock(HttpRequest.class), null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("error message can't be null");
  }

  @Test
  public void logout_does_not_interact_with_request_if_log_level_is_above_DEBUG() {
    HttpRequest request = mock(HttpRequest.class);
    logTester.setLevel(Level.INFO);

    underTest.logoutFailure(request, "bad csrf");

    verifyNoInteractions(request);
  }

  @Test
  public void logout_creates_DEBUG_log_with_error() {
    underTest.logoutFailure(mockRequest(), "bad token");

    verifyLog("logout failure [error|bad token][IP||]", Set.of("login", "logout success"));
  }

  @Test
  public void logout_logs_remote_ip_from_request() {
    underTest.logoutFailure(mockRequest("1.2.3.4"), "bad token");

    verifyLog("logout failure [error|bad token][IP|1.2.3.4|]", Set.of("login", "logout success"));
  }

  @Test
  public void logout_logs_X_Forwarded_For_header_from_request() {
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5"));
    underTest.logoutFailure(request, "bad token");

    verifyLog("logout failure [error|bad token][IP|1.2.3.4|2.3.4.5]", Set.of("login", "logout success"));
  }

  @Test
  public void logout_logs_X_Forwarded_For_header_from_request_and_supports_multiple_headers() {
    HttpRequest request = mockRequest("1.2.3.4", List.of("2.3.4.5", "6.5.4.3"), List.of("9.5.6.7"), List.of("6.3.2.4"));
    underTest.logoutFailure(request, "bad token");

    verifyLog("logout failure [error|bad token][IP|1.2.3.4|2.3.4.5,6.5.4.3,9.5.6.7,6.3.2.4]",
      Set.of("login", "logout success"));
  }

  private void verifyLog(String expected, Set<String> notExpected) {
    assertThat(logTester.logs(Level.DEBUG)).contains(expected);
    assertThat(logTester.logs(Level.DEBUG)).noneMatch(log -> notExpected.stream().anyMatch(log::startsWith));
  }

  private static HttpRequest mockRequest() {
    return mockRequest("");
  }

  private static HttpRequest mockRequest(String remoteAddr, List<String>... remoteIps) {
    HttpRequest res = mock(HttpRequest.class);
    when(res.getRemoteAddr()).thenReturn(remoteAddr);
    when(res.getHeaders("X-Forwarded-For"))
      .thenReturn(Collections.enumeration(
        Arrays.stream(remoteIps)
          .map(Joiner.on(",")::join)
          .toList()));
    return res;
  }
}
