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
package org.sonar.server.platform.web;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ActionDeprecationLoggerInterceptorTest {
  private final ThreadLocalUserSession userSession = mock(ThreadLocalUserSession.class);

  private final ActionDeprecationLoggerInterceptor underTest = new ActionDeprecationLoggerInterceptor(userSession);

  @Rule
  public LogTester logTester = new LogTester().setLevel(Level.DEBUG);

  @Test
  public void preAction_whenParamAndEndpointAreNotDeprecated_shouldLogNothing() {
    WebService.Action action = mock(WebService.Action.class);
    when(action.deprecatedSince()).thenReturn(null);
    WebService.Param mockParam = mock(WebService.Param.class);
    when(mockParam.deprecatedKeySince()).thenReturn(null);
    when(action.params()).thenReturn(List.of(mockParam));

    Request request = mock(Request.class);

    underTest.preAction(action, request);

    verifyNoDeprecatedMsgInLogs(Level.DEBUG);
    verifyNoDeprecatedMsgInLogs(Level.WARN);
  }

  @Test
  @UseDataProvider("userSessions")
  public void preAction_whenEndpointIsDeprecatedAndBrowserSession_shouldLogWarning(boolean isLoggedIn, boolean isAuthenticatedBrowserSession, Level expectedLogLevel) {
    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(isLoggedIn);
    when(userSession.isAuthenticatedBrowserSession()).thenReturn(isAuthenticatedBrowserSession);

    WebService.Action action = mock(WebService.Action.class);
    when(action.path()).thenReturn("api/issues/search");
    when(action.deprecatedSince()).thenReturn("9.8");
    when(action.params()).thenReturn(Collections.emptyList());

    Request request = mock(Request.class);

    underTest.preAction(action, request);

    assertThat(logTester.logs(expectedLogLevel))
      .contains("Web service is deprecated since 9.8 and will be removed in a future version.");
  }

  @Test
  @UseDataProvider("userSessions")
  public void preAction_whenParameterIsDeprecatedAndHasReplacementAndBrowserSession_shouldLogWarning(boolean isLoggedIn, boolean isAuthenticatedBrowserSession, Level expectedLogLevel) {
    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(isLoggedIn);
    when(userSession.isAuthenticatedBrowserSession()).thenReturn(isAuthenticatedBrowserSession);

    WebService.Action action = mock(WebService.Action.class);
    when(action.path()).thenReturn("api/issues/search");
    when(action.deprecatedSince()).thenReturn(null);

    WebService.Param mockParam = mock(WebService.Param.class);
    when(mockParam.deprecatedKeySince()).thenReturn("9.6");
    when(mockParam.deprecatedKey()).thenReturn("sansTop25");
    when(mockParam.key()).thenReturn("sansTop25New");
    when(action.params()).thenReturn(List.of(mockParam));
    when(action.param("sansTop25")).thenReturn(mockParam);

    Request request = mock(Request.class);
    Request.StringParam stringParam = mock(Request.StringParam.class);
    when(stringParam.isPresent()).thenReturn(true);
    when(request.hasParam("sansTop25")).thenReturn(true);
    when(request.getParams()).thenReturn(Map.of("sansTop25", new String[]{}));

    underTest.preAction(action, request);

    assertThat(logTester.logs(expectedLogLevel))
      .contains("Parameter 'sansTop25' is deprecated since 9.6 and will be removed in a future version.");
  }

  @Test
  @UseDataProvider("userSessions")
  public void preAction_whenParameterIsDeprecatedAndNoReplacementAndBrowserSession_shouldLogWarning(boolean isLoggedIn, boolean isAuthenticatedBrowserSession, Level expectedLogLevel) {
    when(userSession.hasSession()).thenReturn(true);
    when(userSession.isLoggedIn()).thenReturn(isLoggedIn);
    when(userSession.isAuthenticatedBrowserSession()).thenReturn(isAuthenticatedBrowserSession);

    WebService.Action action = mock(WebService.Action.class);
    when(action.path()).thenReturn("api/issues/search");
    when(action.deprecatedSince()).thenReturn(null);

    WebService.Param mockParam = mock(WebService.Param.class);
    when(mockParam.key()).thenReturn("sansTop25");
    when(mockParam.deprecatedSince()).thenReturn("9.7");
    when(action.params()).thenReturn(List.of(mockParam));
    when(action.param("sansTop25")).thenReturn(mockParam);

    Request request = mock(Request.class);
    Request.StringParam stringParam = mock(Request.StringParam.class);
    when(stringParam.isPresent()).thenReturn(true);
    when(request.hasParam("sansTop25")).thenReturn(true);
    when(request.getParams()).thenReturn(Map.of("sansTop25", new String[]{}));

    underTest.preAction(action, request);

    assertThat(logTester.logs(expectedLogLevel))
      .contains("Parameter 'sansTop25' is deprecated since 9.7 and will be removed in a future version.");
  }

  @Test
  public void preAction_whenNewParamWithDeprecatedKeyIsUsed_shouldLogNothing() {
    WebService.Action action = mock(WebService.Action.class);
    when(action.deprecatedSince()).thenReturn(null);

    WebService.Param mockParam = mock(WebService.Param.class);
    when(mockParam.key()).thenReturn("sansTop25New");
    when(mockParam.deprecatedSince()).thenReturn(null);
    when(mockParam.deprecatedKeySince()).thenReturn("9.7");
    when(mockParam.deprecatedKey()).thenReturn("sansTop25");
    when(action.params()).thenReturn(List.of(mockParam));

    Request request = mock(Request.class);
    when(request.hasParam("sansTop25New")).thenReturn(true);
    when(request.hasParam("sansTop25")).thenReturn(false);

    underTest.preAction(action, request);

    verifyNoDeprecatedMsgInLogs(Level.DEBUG);
    verifyNoDeprecatedMsgInLogs(Level.WARN);
  }

  @DataProvider
  public static Object[][] userSessions() {
    return new Object[][] {
      {false, false, Level.DEBUG},
      {false, true, Level.DEBUG},
      {true, false, Level.WARN},
      {true, true, Level.DEBUG}
    };
  }

  private void verifyNoDeprecatedMsgInLogs(Level level) {
    assertThat(logTester.getLogs(level))
      .extracting(LogAndArguments::getRawMsg)
      .doesNotContain("Parameter '{}' is deprecated since {} and will be removed in a future version.");
  }

}
