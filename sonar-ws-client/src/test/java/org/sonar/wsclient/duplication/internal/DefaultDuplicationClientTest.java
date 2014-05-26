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

package org.sonar.wsclient.duplication.internal;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.duplication.*;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultDuplicationClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void show_duplications() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultDuplicationClientTest/show_duplications.json"), Charsets.UTF_8));

    DuplicationClient client = new DefaultDuplicationClient(requestFactory);
    Duplications result = client.show("MyFile");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/duplications/show?key=MyFile");

    List<Duplication> duplications = result.duplications();
    assertThat(duplications).hasSize(1);

    Duplication duplication = duplications.get(0);
    assertThat(duplication.blocks()).hasSize(2);

    Block block  = duplication.blocks().get(0);
    assertThat(block.from()).isEqualTo(94);
    assertThat(block.size()).isEqualTo(101);

    File file = block.file();
    assertThat(file.key()).isEqualTo("org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/utils/command/CommandExecutor.java");
    assertThat(file.name()).isEqualTo("CommandExecutor");
    assertThat(file.projectName()).isEqualTo("SonarQube");

    List<File> files = result.files();
    assertThat(files).hasSize(2);
  }

}
