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

import org.junit.Test;
import org.sonar.server.plugins.MimeTypes;
import org.sonarqube.ws.Issues;

import static org.assertj.core.api.Assertions.assertThat;

public class WsUtilsTest {

  @Test
  public void write_json_by_default() throws Exception {
    TestRequest request = new TestRequest();
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.JSON);
    assertThat(response.outputAsString())
      .startsWith("{")
      .contains("\"key\":\"I1\"")
      .endsWith("}");
  }

  @Test
  public void write_protobuf() throws Exception {
    TestRequest request = new TestRequest();
    request.setMediaType(MimeTypes.PROTOBUF);
    DumbResponse response = new DumbResponse();

    Issues.Issue msg = Issues.Issue.newBuilder().setKey("I1").build();
    WsUtils.writeProtobuf(msg, request, response);

    assertThat(response.stream().mediaType()).isEqualTo(MimeTypes.PROTOBUF);
    assertThat(Issues.Issue.parseFrom(response.getFlushedOutput()).getKey()).isEqualTo("I1");
  }
}
