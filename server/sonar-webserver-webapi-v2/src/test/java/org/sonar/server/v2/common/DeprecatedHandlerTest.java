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
package org.sonar.server.v2.common;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.v2.api.ControllerTester;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@RunWith(DataProviderRunner.class)
public class DeprecatedHandlerTest {

  private static final String DEPRECATED_VERSION = "10.0";
  private static final String DEPRECATED_WEB_SERVICE = "Web service is deprecated since %s and will be removed in a future version.";
  private static final String DEPRECATED_PARAM = "Parameter '%s' is deprecated since %s and will be removed in a future version.";

  private final ThreadLocalUserSession userSession = mock(ThreadLocalUserSession.class);
  private final MockMvc mockMvc = ControllerTester.getMockMvcWithHandlerInterceptors(List.of(new DeprecatedHandler(userSession)), new TestController());

  @Rule
  public LogTester logTester = new LogTester().setLevel(Level.DEBUG);

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsDeprecatedGetMethod_shouldShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(GET, sessionData, "/test/get-deprecated-endpoint");

    assertThat(logTester.logs(expectedLogLevel)).contains(DEPRECATED_WEB_SERVICE.formatted(DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsNotDeprecatedGetMethod_shouldNotShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(GET, sessionData, "/test/get-not-deprecated-endpoint");

    assertThat(logTester.logs(expectedLogLevel)).doesNotContain(DEPRECATED_WEB_SERVICE.formatted(DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsGetMethodWithUsedDeprecatedParamObjectField_shouldShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(GET, sessionData, "/test/get-deprecated-param-obj?deprecatedField=foo&notDeprecatedField=bar");

    assertThat(logTester.logs(expectedLogLevel)).contains(DEPRECATED_PARAM.formatted("deprecatedField", DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsGetMethodWithUnusedDeprecatedParam_shouldNotShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(GET, sessionData, "/test/get-deprecated-param-obj?notDeprecatedParam=bar");

    assertThat(logTester.logs(expectedLogLevel)).doesNotContain(DEPRECATED_PARAM.formatted("notDeprecatedParam", DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsGetMethodWithUsedDeprecatedSimpleParam_shouldShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(GET, sessionData, "/test/get-deprecated-param?deprecatedParam=foo");

    assertThat(logTester.logs(expectedLogLevel)).contains(DEPRECATED_PARAM.formatted("deprecatedParam", DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsGetMethodWithUnusedDeprecatedSimpleParam_shouldNotShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(GET, sessionData, "/test/get-deprecated-param?notDeprecatedField=bar");

    assertThat(logTester.logs(expectedLogLevel)).doesNotContain(DEPRECATED_PARAM.formatted("notDeprecatedField", DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsDeprecatedPostMethod_shouldShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(POST, sessionData, "/test/post-deprecated-endpoint");

    assertThat(logTester.logs(expectedLogLevel)).contains(DEPRECATED_WEB_SERVICE.formatted(DEPRECATED_VERSION));
  }

  @Test
  @UseDataProvider("userSessions")
  public void preHandle_whenHandlerContainsNotDeprecatedPostMethod_shouldNotShowDeprecatedLogs(UserSessionData sessionData, Level expectedLogLevel) throws Exception {
    performRequest(POST, sessionData, "/test/post-not-deprecated-endpoint");

    assertThat(logTester.logs(expectedLogLevel)).doesNotContain(DEPRECATED_WEB_SERVICE.formatted(DEPRECATED_VERSION));
  }
  
  @Test
  public void preHandle_whenNotHandlerMethod_shouldLogDebugMessage() {
    DeprecatedHandler handler = new DeprecatedHandler(userSession);

    boolean result = handler.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), "Not handler");

    assertThat(result).isTrue();
    assertThat(logTester.logs(Level.DEBUG)).contains("Handler is not a HandlerMethod, skipping deprecated check.");
  }

  private void performRequest(HttpMethod method, UserSessionData sessionData, String endpoint) throws Exception {
    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(sessionData.isLoggedIn());
    when(userSession.isAuthenticatedBrowserSession()).thenReturn(sessionData.isAuthenticatedBrowserSession());

    mockMvc.perform(request(method, endpoint));
  }

  @DataProvider
  public static Object[][] userSessions() {
    return new Object[][] {
      {new UserSessionData(false, false), Level.DEBUG },
      {new UserSessionData(false, true), Level.DEBUG},
      {new UserSessionData(true, false), Level.WARN},
      {new UserSessionData(true, true), Level.DEBUG}
    };
  }

  @RequestMapping("test")
  @RestController
  private static class TestController {
    @Deprecated(since = DEPRECATED_VERSION)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @GetMapping("/get-deprecated-endpoint")
    void deprecatedGet() {
    }

    @GetMapping("/get-not-deprecated-endpoint")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void notDeprecatedGet() {
    }

    @GetMapping("/get-deprecated-param")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deprecatedGetParam(@Deprecated(since = DEPRECATED_VERSION) @RequestParam(name = "deprecatedParam") String deprecatedParam,
      @RequestParam(name = "notDeprecatedParam") String notDeprecatedParam) {
    }

    @GetMapping("/get-deprecated-param-obj")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deprecatedGetParam(@ParameterObject GetRequest request) {
    }

    @Deprecated(since = DEPRECATED_VERSION)
    @PostMapping("/post-deprecated-endpoint")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deprecatedPost(@RequestBody Object request) {
    }

    @PostMapping("/post-not-deprecated-endpoint")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void notDeprecatedPost(@RequestBody Object request) {
    }
  }

  private static class GetRequest {
    @Deprecated(since = DEPRECATED_VERSION)
    private String deprecatedField;
    private String notDeprecatedField;
  }

  private record UserSessionData(boolean isLoggedIn, boolean isAuthenticatedBrowserSession) {
  }

}

