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
import org.sonar.wsclient.test.CoveredFile;
import org.sonar.wsclient.test.TestCase;
import org.sonar.wsclient.test.TestClient;
import org.sonar.wsclient.test.TestableTestCases;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultTestClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void show_test_cases() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultTestClientTest/show_test_case.json"), Charsets.UTF_8));

    TestClient client = new DefaultTestClient(requestFactory);
    List<TestCase> tests = client.show("MyTestFile");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/tests/show?key=MyTestFile");
    assertThat(tests).hasSize(2);
    assertThat(tests.get(0).name()).isEqualTo("find_by_params");
    assertThat(tests.get(0).status()).isEqualTo("OK");
    assertThat(tests.get(0).durationInMs()).isEqualTo(10L);
    assertThat(tests.get(0).coveredLines()).isEqualTo(89);
  }

  @Test
  public void show_testable() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultTestClientTest/show_testable.json"), Charsets.UTF_8));

    TestClient client = new DefaultTestClient(requestFactory);
    TestableTestCases coveringTestCases = client.testable("MyFile", 10);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/tests/testable?key=MyFile&line=10");
    assertThat(coveringTestCases.tests()).hasSize(1);
    assertThat(coveringTestCases.files()).hasSize(1);

    TestableTestCases.TestCase testCase = coveringTestCases.tests().get(0);
    assertThat(testCase.name()).isEqualTo("find_by_params");
    assertThat(testCase.status()).isEqualTo("OK");
    assertThat(testCase.durationInMs()).isEqualTo(10L);

    TestableTestCases.File file = testCase.file();
    assertThat(file.key()).isEqualTo("org.codehaus.sonar:sonar-server:src/test/java/org/sonar/server/rule/RubyRuleServiceTest.java");
    assertThat(file.longName()).isEqualTo("src/test/java/org/sonar/server/rule/RubyRuleServiceTest.java");
  }

  @Test
  public void show_plan() throws IOException {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(Resources.toString(Resources.getResource(this.getClass(), "DefaultTestClientTest/show_plan.json"), Charsets.UTF_8));

    TestClient client = new DefaultTestClient(requestFactory);
    List<CoveredFile> files = client.plan("MyTestFile", "find_by_params");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/tests/plan?key=MyTestFile&test=find_by_params");
    assertThat(files).hasSize(2);

    CoveredFile file = files.get(0);
    assertThat(file.key()).isEqualTo("org.codehaus.sonar:sonar-server:src/main/java/org/sonar/server/paging/PagedResult.java");
    assertThat(file.longName()).isEqualTo("src/main/java/org/sonar/server/paging/PagedResult.java");
    assertThat(file.coveredLines()).isEqualTo(5);
  }

}
