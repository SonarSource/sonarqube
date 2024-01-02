/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
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
  public void log_and_profile_request_if_debug_level() {
    WsRequest request = newRequest();
    WsResponse response = newResponse().setRequestUrl("https://local/api/issues/search");
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    logTester.setLevel(LoggerLevel.DEBUG);
    DefaultScannerWsClient underTest = new DefaultScannerWsClient(wsClient, false, new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings);

    WsResponse result = underTest.call(request);

    // do not fail the execution -> interceptor returns the response
    assertThat(result).isSameAs(response);

    // check logs
    List<String> debugLogs = logTester.logs(LoggerLevel.DEBUG);
    assertThat(debugLogs).hasSize(1);
    assertThat(debugLogs.get(0)).contains("GET 200 https://local/api/issues/search | time=");
  }

  @Test
  public void create_error_msg_from_json() {
    String content = "{\"errors\":[{\"msg\":\"missing scan permission\"}, {\"msg\":\"missing another permission\"}]}";
    assertThat(DefaultScannerWsClient.createErrorMessage(new HttpException("url", 400, content))).isEqualTo("missing scan permission, missing another permission");
  }

  @Test
  public void create_error_msg_from_html() {
    String content = "<!DOCTYPE html><html>something</html>";
    assertThat(DefaultScannerWsClient.createErrorMessage(new HttpException("url", 400, content))).isEqualTo("HTTP code 400");
  }

  @Test
  public void create_error_msg_from_long_content() {
    String content = StringUtils.repeat("mystring", 1000);
    assertThat(DefaultScannerWsClient.createErrorMessage(new HttpException("url", 400, content))).hasSize(15 + 128);
  }

  @Test
  public void fail_if_requires_credentials() {
    WsRequest request = newRequest();
    WsResponse response = newResponse().setCode(401);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    assertThatThrownBy(() -> new DefaultScannerWsClient(wsClient, false,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings).call(request))
        .isInstanceOf(MessageException.class)
        .hasMessage("Not authorized. Analyzing this project requires authentication. Please provide a user token in sonar.login or other " +
          "credentials in sonar.login and sonar.password.");
  }

  @Test
  public void fail_if_credentials_are_not_valid() {
    WsRequest request = newRequest();
    WsResponse response = newResponse().setCode(401);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    assertThatThrownBy(() -> new DefaultScannerWsClient(wsClient, /* credentials are configured */true,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings).call(request))
        .isInstanceOf(MessageException.class)
        .hasMessage("Not authorized. Please check the properties sonar.login and sonar.password.");
  }

  @Test
  public void fail_if_requires_permission() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setCode(403);
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    assertThatThrownBy(() -> new DefaultScannerWsClient(wsClient, true,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings).call(request))
        .isInstanceOf(MessageException.class)
        .hasMessage("You're not authorized to run analysis. Please contact the project administrator.");
  }

  @Test
  public void warnings_are_added_when_expiration_approaches() {
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
    //the second call should not add the same warning twice
    underTest.call(request);

    // check logs
    List<String> warningLogs = logTester.logs(LoggerLevel.WARN);
    assertThat(warningLogs).hasSize(2);
    assertThat(warningLogs.get(0)).contains("The token used for this analysis will expire on: " + fiveDaysLatter.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
    assertThat(warningLogs.get(1)).contains("Analysis executed with this token will fail after the expiration date.");
  }

  @Test
  public void fail_if_bad_request() {
    WsRequest request = newRequest();
    WsResponse response = newResponse()
      .setCode(400)
      .setContent("{\"errors\":[{\"msg\":\"Boo! bad request! bad!\"}]}");
    when(wsClient.wsConnector().call(request)).thenReturn(response);

    assertThatThrownBy(() -> new DefaultScannerWsClient(wsClient, true,
      new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap())), analysisWarnings).call(request))
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
