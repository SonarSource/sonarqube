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
package org.sonar.ce.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.httpd.CeHttpUtils;
import org.sonar.server.log.ServerLogging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChangeLogLevelHttpActionTest {
  private ServerLogging serverLogging = mock(ServerLogging.class);
  private ChangeLogLevelHttpAction underTest = new ChangeLogLevelHttpAction(serverLogging);

  @Test
  public void serves_METHOD_NOT_ALLOWED_error_when_method_is_not_POST() throws HttpException, IOException {
    CeHttpUtils.testHandlerForGetWithoutResponseBody(underTest, HttpStatus.SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void serves_BAD_REQUEST_error_when_parameter_level_is_missing() throws IOException, HttpException {
    byte[] responseBody = CeHttpUtils.testHandlerForPostWithResponseBody(underTest, List.of(), List.of(), HttpStatus.SC_BAD_REQUEST);
    assertThat(new String(responseBody, StandardCharsets.UTF_8)).isEqualTo("Parameter 'level' is missing");
  }

  @Test
  public void serves_BAD_REQUEST_error_when_value_of_parameter_level_is_not_LEVEL_in_uppercase() throws IOException, HttpException {
    byte[] responseBody = CeHttpUtils.testHandlerForPostWithResponseBody(
      underTest, List.of(new BasicNameValuePair("level", "info")), List.of(), HttpStatus.SC_BAD_REQUEST);
    assertThat(new String(responseBody, StandardCharsets.UTF_8)).isEqualTo("Value 'info' for parameter 'level' is invalid");
  }

  @Test
  public void changes_server_logging_if_level_is_ERROR() throws HttpException, IOException {
    CeHttpUtils.testHandlerForPostWithoutResponseBody(
      underTest, List.of(new BasicNameValuePair("level", "ERROR")), List.of(), HttpStatus.SC_OK);

    verify(serverLogging).changeLevel(LoggerLevel.ERROR);
  }

  @Test
  public void changes_server_logging_if_level_is_INFO() throws HttpException, IOException {
    CeHttpUtils.testHandlerForPostWithoutResponseBody(
      underTest, List.of(new BasicNameValuePair("level", "INFO")), List.of(), HttpStatus.SC_OK);

    verify(serverLogging).changeLevel(LoggerLevel.INFO);
  }

  @Test
  public void changes_server_logging_if_level_is_DEBUG() throws HttpException, IOException {
    CeHttpUtils.testHandlerForPostWithoutResponseBody(
      underTest, List.of(new BasicNameValuePair("level", "DEBUG")), List.of(), HttpStatus.SC_OK);

    verify(serverLogging).changeLevel(LoggerLevel.DEBUG);
  }

  @Test
  public void changes_server_logging_if_level_is_TRACE() throws HttpException, IOException {
    CeHttpUtils.testHandlerForPostWithoutResponseBody(
      underTest, List.of(new BasicNameValuePair("level", "TRACE")), List.of(), HttpStatus.SC_OK);

    verify(serverLogging).changeLevel(LoggerLevel.TRACE);
  }
}
