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

package org.sonar.wsclient.source.internal;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.source.*;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultSourceClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void show_source() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultSourceClientTest/show_source.json"), Charsets.UTF_8));

    SourceClient client = new DefaultSourceClient(requestFactory);
    List<Source> sources = client.show(SourceShowQuery.create().setKey("MyFile").setFrom("20").setTo("25"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/sources/show?key=MyFile&from=20&to=25");
    assertThat(sources).hasSize(6);
    assertThat(sources.get(0).lineIndex()).isEqualTo(20);
    assertThat(sources.get(0).lineAsHtml()).isEqualTo("<span class=\"k\">package </span>org.sonar.check;");
  }

  @Test
  public void return_scm() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultSourceClientTest/return_scm.json"), Charsets.UTF_8));

    SourceClient client = new DefaultSourceClient(requestFactory);
    List<Scm> result = client.scm(SourceScmQuery.create().setKey("MyFile").setFrom("1").setTo("3").setGroupCommits(true));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/sources/scm?key=MyFile&from=1&to=3&group_commits=true");
    assertThat(result).hasSize(3);
    assertThat(result.get(0).lineIndex()).isEqualTo(1);
    assertThat(result.get(0).author()).isEqualTo("julien");
    assertThat(result.get(0).date().getTime()).isEqualTo(1363129200000L);
  }

}
