/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;

public class PostRequestTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void post_is_post() {
    PostRequest request = new PostRequest("api/issues/search");
    assertThat(request.getMethod()).isEqualTo(WsRequest.Method.POST);
  }

  @Test
  public void empty_parts_and_params_by_default() {
    PostRequest request = new PostRequest("api/issues/search");
    assertThat(request.getParts()).isEmpty();
    assertThat(request.getParams()).isEmpty();
  }

  @Test
  public void add_part() throws IOException {
    PostRequest request = new PostRequest("api/issues/search");
    File reportFile = temp.newFile();
    request.setPart("report", new PostRequest.Part(MediaTypes.JSON, reportFile));

    assertThat(request.getParts()).hasSize(1);
    PostRequest.Part part = request.getParts().get("report");
    assertThat(part.getMediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(part.getFile()).isSameAs(reportFile);
  }
}
