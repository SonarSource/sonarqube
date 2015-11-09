/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.rules.ExpectedException;
import org.sonar.server.exceptions.BadRequestException;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;

public class WsUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void write_json_by_default() throws Exception {
    TestRequest request = new TestRequest();
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.stream().mediaType()).isEqualTo(MediaTypes.JSON);
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

    assertThat(response.stream().mediaType()).isEqualTo(MediaTypes.PROTOBUF);
    assertThat(Issues.Issue.parseFrom(response.getFlushedOutput()).getKey()).isEqualTo("I1");
  }

  @Test
  public void checkRequest_ok() {
    WsUtils.checkRequest(true, "Missing param: %s", "foo");
    // do not fail
  }

  @Test
  public void checkRequest_ko() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing param: foo");

    WsUtils.checkRequest(false, "Missing param: %s", "foo");
  }

}
