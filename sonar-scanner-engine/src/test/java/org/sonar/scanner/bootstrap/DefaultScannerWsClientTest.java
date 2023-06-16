/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.MockWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.DATETIME_FORMAT;

public class DefaultScannerWsClientTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final WsClient wsClient = mock(WsClient.class, Mockito.RETURNS_DEEP_STUBS);

  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);

  @Test
  public void call_whenDebugLevel_shouldLogAndProfileRequest() {
    WsRequest request = newRequest();
    WsResponse response = newResponse().setRequestUrl("https://local/api/issues/search");
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    logTester.setLevel(LoggerLevel.DEBUG);
    DefaultScannerWsClient underTest = new DefaultScannerWsClient(wsClient, false, new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);

    WsResponse result = underTest.call(request);

    // do not fail the execution -> interceptor returns the response
    assertThat(result).isSameAs(response);

    // check logs
    List<String> debugLogs = logTester.logs(Level.DEBUG);
    assertThat(debugLogs).hasSize(1);
    assertThat(debugLogs.get(0)).contains("GET 200 https://local/api/issues/search | time=");
  }

  @Test
  public void createErrorMessage_whenJsonError_shouldCreateErrorMsg() {
    String content = "{\"errors\":[{\"msg\":\"missing scan permission\"}, {\"msg\":\"missing another permission\"}]}";
    assertThat(DefaultScannerWsClient.createErrorMessage(new HttpException("url", 400, content))).isEqualTo("missing scan permission, missing another permission");
  }

  @Test
  public void createErrorMessage_whenHtml_shouldCreateErrorMsg() {
    String content = "<!DOCTYPE html><html>something</html>";
    assertThat(DefaultScannerWsClient.createErrorMessage(new HttpException("url", 400, content))).isEqualTo("HTTP code 400");
  }

  @Test
  public void createErrorMessage_whenLongContent_shouldCreateErrorMsg() {
    String content = StringUtils.repeat("mystring", 1000);
    assertThat(DefaultScannerWsClient.createErrorMessage(new HttpException("url", 400, content))).hasSize(15 + 128);
  }

  @Test
  public void call_whenUnauthorizedAndDebugEnabled_shouldLogResponseDetails() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setContent("Missing credentials")
      .setHeader("Authorization: ", "Bearer ImNotAValidToken")
      .setCode(403);

    logTester.setLevel(LoggerLevel.DEBUG);

    when(wsClient.wsConnector().call(request)).thenReturn(response);

    DefaultScannerWsClient client = new DefaultScannerWsClient(wsClient, false,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    assertThatThrownBy(() -> client.call(request))
      .isInstanceOf(MessageException.class)
      .hasMessage(
        "You're not authorized to analyze this project or the project doesn't exist on SonarQube and you're not authorized to create it. Please contact an administrator.");

    List<String> debugLogs = logTester.logs(Level.DEBUG);
    assertThat(debugLogs).hasSize(2);
    assertThat(debugLogs.get(1)).contains("Error response content: Missing credentials, headers: {Authorization: =[Bearer ImNotAValidToken]}");
  }

  @Test
  public void call_whenUnauthenticatedAndDebugEnabled_shouldLogResponseDetails() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setContent("Missing authentication")
      .setHeader("X-Test-Header: ", "ImATestHeader")
      .setCode(401);

    logTester.setLevel(LoggerLevel.DEBUG);

    when(wsClient.wsConnector().call(request)).thenReturn(response);

    DefaultScannerWsClient client = new DefaultScannerWsClient(wsClient, false,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    assertThatThrownBy(() -> client.call(request))
      .isInstanceOf(MessageException.class)
      .hasMessage("Not authorized. Analyzing this project requires authentication. Please check the user token in the property 'sonar.token' " +
        "or the credentials in the properties 'sonar.login' and 'sonar.password'.");

    List<String> debugLogs = logTester.logs(Level.DEBUG);
    assertThat(debugLogs).hasSize(2);
    assertThat(debugLogs.get(1)).contains("Error response content: Missing authentication, headers: {X-Test-Header: =[ImATestHeader]}");
  }

  @Test
  public void call_whenMissingCredentials_shouldFailWithMsg() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setContent("Missing authentication")
      .setCode(401);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    DefaultScannerWsClient client = new DefaultScannerWsClient(wsClient, false,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    assertThatThrownBy(() -> client.call(request))
      .isInstanceOf(MessageException.class)
      .hasMessage("Not authorized. Analyzing this project requires authentication. Please check the user token in the property 'sonar.token' " +
        "or the credentials in the properties 'sonar.login' and 'sonar.password'.");
  }

  @Test
  public void call_whenInvalidCredentials_shouldFailWithMsg() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setContent("Invalid credentials")
      .setCode(401);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    DefaultScannerWsClient client = new DefaultScannerWsClient(wsClient, /* credentials are configured */true,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    assertThatThrownBy(() -> client.call(request))
      .isInstanceOf(MessageException.class)
      .hasMessage("Not authorized. Please check the user token in the property 'sonar.token' or the credentials in the properties 'sonar.login' and 'sonar.password'.");
  }

  @Test
  public void call_whenMissingPermissions_shouldFailWithMsg() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setContent("Unauthorized")
      .setCode(403);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    DefaultScannerWsClient client = new DefaultScannerWsClient(wsClient, true,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    assertThatThrownBy(() -> client.call(request))
      .isInstanceOf(MessageException.class)
      .hasMessage(
        "You're not authorized to analyze this project or the project doesn't exist on SonarQube and you're not authorized to create it. Please contact an administrator.");
  }

  @Test
  public void call_whenTokenExpirationApproaches_shouldLogWarnings() {
    WsRequest request = newRequest();
    var fiveDaysLatter = LocalDateTime.now().atZone(ZoneOffset.UTC).plusDays(5);
    String expirationDate = DateTimeFormatter
      .ofPattern(DATETIME_FORMAT)
      .format(fiveDaysLatter);
    WsResponse response = newResponse()
      .setCode(200)
      .setExpirationDate(expirationDate);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    logTester.setLevel(LoggerLevel.DEBUG);
    DefaultScannerWsClient underTest = new DefaultScannerWsClient(wsClient, false, new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    underTest.call(request);
    // the second call should not add the same warning twice
    underTest.call(request);

    // check logs
    List<String> warningLogs = logTester.logs(Level.WARN);
    assertThat(warningLogs).hasSize(2);
    assertThat(warningLogs.get(0)).contains("The token used for this analysis will expire on: " + fiveDaysLatter.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
    assertThat(warningLogs.get(1)).contains("Analysis executed with this token will fail after the expiration date.");
  }

  @Test
  public void call_whenBadRequest_shouldFailWithMessage() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setCode(400)
      .setContent("{\"errors\":[{\"msg\":\"Boo! bad request! bad!\"}]}");
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    DefaultScannerWsClient client = new DefaultScannerWsClient(wsClient, true,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);
    assertThatThrownBy(() -> client.call(request))
      .isInstanceOf(MessageException.class)
      .hasMessage("Boo! bad request! bad!");
  }

  private MockWsResponse newResponse() {
    return new MockWsResponse().setRequestUrl("https://local/api/issues/search");
  }

  private WsRequest newRequest() {
    return new GetRequest("api/issues/search");
  }
}
