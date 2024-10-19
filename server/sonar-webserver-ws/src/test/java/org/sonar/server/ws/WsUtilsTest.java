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
package org.sonar.server.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WsUtilsTest {

  @Rule
  public LogTester logger = new LogTester();

  @Test
  public void write_json_by_default() {
    TestRequest request = new TestRequest();
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.mediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(response.outputAsString())
      .startsWith("{")
      .contains("\"key\":\"I1\"")
      .endsWith("}");
  }

  @Test
  public void write_protobuf() throws Exception {
    TestRequest request = new TestRequest();
    request.setMediaType(MediaTypes.PROTOBUF);
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.mediaType()).isEqualTo(MediaTypes.PROTOBUF);
    assertThat(Issues.Issue.parseFrom(response.getFlushedOutput()).getKey()).isEqualTo("I1");
  }

  @Test
  public void rethrow_error_as_ISE_when_error_writing_message() {
    TestRequest request = new TestRequest();
    request.setMediaType(MediaTypes.PROTOBUF);

    Permissions.Permission message = Permissions.Permission.newBuilder().setName("permission-name").build();

    // provoke NullPointerException
    assertThatThrownBy(() -> WsUtils.writeProtobuf(message, null, new DumbResponse()))
      .isInstanceOf(IllegalStateException.class)
      .hasCauseInstanceOf(NullPointerException.class)
      .hasMessageContaining("Error while writing protobuf message");
  }

  @Test
  public void checkRequest_ok() {
    BadRequestException.checkRequest(true, "Missing param: %s", "foo");
    // do not fail
  }

  @Test
  public void create_safe_external_link_tag() {
    assertThat(WsUtils.createHtmlExternalLink("http://google.com", "Google"))
      .isEqualTo("<a href=\"http://google.com\" target=\"_blank\" rel=\"noopener noreferrer\">Google</a>");
  }

  @Test
  public void checkRequest_ko() {
    assertThatThrownBy(() -> BadRequestException.checkRequest(false, "Missing param: %s", "foo"))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Missing param: foo");
  }

}
