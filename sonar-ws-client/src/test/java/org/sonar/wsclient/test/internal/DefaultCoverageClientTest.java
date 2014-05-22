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

package org.sonar.wsclient.test.internal;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.test.Coverage;
import org.sonar.wsclient.test.CoverageClient;
import org.sonar.wsclient.test.CoverageShowQuery;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultCoverageClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void show_coverage() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultCoverageClientTest/show_coverage.json"), Charsets.UTF_8));

    CoverageClient client = new DefaultCoverageClient(requestFactory);
    List<Coverage> coverage = client.show(CoverageShowQuery.create().setKey("MyFile").setFrom("20").setTo("25").setType("UT"));

    assertThat(httpServer.requestedPath()).isEqualTo("/api/coverage/show?key=MyFile&from=20&to=25&type=UT");
    assertThat(coverage).hasSize(3);
    assertThat(coverage.get(0).lineIndex()).isEqualTo(49);
    assertThat(coverage.get(0).isCovered()).isTrue();
    assertThat(coverage.get(0).tests()).isEqualTo(14);
    assertThat(coverage.get(0).branches()).isEqualTo(3);
    assertThat(coverage.get(0).coveredBranches()).isEqualTo(2);
  }

}
