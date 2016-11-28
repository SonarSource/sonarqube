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
package org.sonar.server.authentication.event;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationException.newBuilder;

public class AuthenticationEventImplTest {
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AuthenticationEventImpl underTest = new AuthenticationEventImpl();

  @Test
  public void login_fails_with_NPE_if_request_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.login(null, "login", Source.sso());
  }

  @Test
  public void login_fails_with_NPE_if_source_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.login(mock(HttpServletRequest.class), "login", null);
  }

  @Test
  public void login_creates_INFO_log_with_empty_login_if_login_argument_is_null() {
    underTest.login(mockRequest(), null, Source.sso());

    verifyLog("login success [method|SSO][provider|SSO|sso][IP||][login|]");
  }

  @Test
  public void login_creates_INFO_log_with_method_provider_and_login() {
    underTest.login(mockRequest(), "foo", Source.realm(Method.BASIC, "some provider name"));

    verifyLog("login success [method|BASIC][provider|REALM|some provider name][IP||][login|foo]");
  }

  @Test
  public void login_logs_remote_ip_from_request() {
    underTest.login(mockRequest("1.2.3.4"), "foo", Source.realm(Method.EXTERNAL, "bar"));

    verifyLog("login success [method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|][login|foo]");
  }

  @Test
  public void login_logs_X_Forwarded_For_header_from_request() {
    HttpServletRequest request = mockRequest("1.2.3.4", asList("2.3.4.5"));
    underTest.login(request, "foo", Source.realm(Method.EXTERNAL, "bar"));

    verifyLog("login success [method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5][login|foo]");
  }

  @Test
  public void login_logs_X_Forwarded_For_header_from_request_and_supports_multiple_headers() {
    HttpServletRequest request = mockRequest("1.2.3.4", asList("2.3.4.5", "6.5.4.3"), asList("9.5.6.7"), asList("6.3.2.4"));
    underTest.login(request, "foo", Source.realm(Method.EXTERNAL, "bar"));

    verifyLog("login success [method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5,6.5.4.3,9.5.6.7,6.3.2.4][login|foo]");
  }

  @Test
  public void failure_fails_with_NPE_if_request_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.failure(null, newBuilder().setSource(Source.sso()).build());
  }

  @Test
  public void failure_fails_with_NPE_if_AuthenticationException_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.failure(mock(HttpServletRequest.class), null);
  }

  @Test
  public void failure_creates_INFO_log_with_empty_login_if_AuthenticationException_has_no_login() {
    AuthenticationException exception = newBuilder().setSource(Source.sso()).setMessage("message").build();
    underTest.failure(mockRequest(), exception);

    verifyLog("login failure [cause|message][method|SSO][provider|SSO|sso][IP||][login|]");
  }

  @Test
  public void failure_creates_INFO_log_with_empty_cause_if_AuthenticationException_has_no_message() {
    AuthenticationException exception = newBuilder().setSource(Source.sso()).setLogin("FoO").build();
    underTest.failure(mockRequest(), exception);

    verifyLog("login failure [cause|][method|SSO][provider|SSO|sso][IP||][login|FoO]");
  }

  @Test
  public void failure_creates_INFO_log_with_method_provider_and_login() {
    AuthenticationException exception = newBuilder()
        .setSource(Source.realm(Method.BASIC, "some provider name"))
        .setMessage("something got terribly wrong")
        .setLogin("BaR")
        .build();
    underTest.failure(mockRequest(), exception);

    verifyLog("login failure [cause|something got terribly wrong][method|BASIC][provider|REALM|some provider name][IP||][login|BaR]");
  }

  @Test
  public void failure_logs_remote_ip_from_request() {
    AuthenticationException exception = newBuilder()
        .setSource(Source.realm(Method.EXTERNAL, "bar"))
        .setMessage("Damn it!")
        .setLogin("Baaad")
        .build();
    underTest.failure(mockRequest("1.2.3.4"), exception);

    verifyLog("login failure [cause|Damn it!][method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|][login|Baaad]");
  }

  @Test
  public void failure_logs_X_Forwarded_For_header_from_request() {
    AuthenticationException exception = newBuilder()
        .setSource(Source.realm(Method.EXTERNAL, "bar"))
        .setMessage("Hop la!")
        .setLogin("foo")
        .build();
    HttpServletRequest request = mockRequest("1.2.3.4", asList("2.3.4.5"));
    underTest.failure(request, exception);

    verifyLog("login failure [cause|Hop la!][method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5][login|foo]");
  }

  @Test
  public void failure_logs_X_Forwarded_For_header_from_request_and_supports_multiple_headers() {
    AuthenticationException exception = newBuilder()
        .setSource(Source.realm(Method.EXTERNAL, "bar"))
        .setMessage("Boom!")
        .setLogin("foo")
        .build();
    HttpServletRequest request = mockRequest("1.2.3.4", asList("2.3.4.5", "6.5.4.3"), asList("9.5.6.7"), asList("6.3.2.4"));
    underTest.failure(request, exception);

    verifyLog("login failure [cause|Boom!][method|EXTERNAL][provider|REALM|bar][IP|1.2.3.4|2.3.4.5,6.5.4.3,9.5.6.7,6.3.2.4][login|foo]");
  }

  private void verifyLog(String expected) {
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO))
      .containsOnly(expected);
  }

  private static HttpServletRequest mockRequest() {
    return mockRequest("");
  }

  private static HttpServletRequest mockRequest(String remoteAddr, List<String>... remoteIps) {
    HttpServletRequest res = mock(HttpServletRequest.class);
    when(res.getRemoteAddr()).thenReturn(remoteAddr);
    when(res.getHeaders("X-Forwarded-For"))
      .thenReturn(Collections.enumeration(
        Arrays.stream(remoteIps)
          .map(Joiner.on(",")::join)
          .collect(Collectors.toList())));
    return res;
  }
}
