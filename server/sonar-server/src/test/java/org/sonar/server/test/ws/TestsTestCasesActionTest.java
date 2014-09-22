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

package org.sonar.server.test.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestsTestCasesActionTest {

  static final String FILE_KEY = "src/main/java/org/foo/Foo.java";

  @Mock
  MutableTestable testable;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    SnapshotPerspectives snapshotPerspectives = mock(SnapshotPerspectives.class);
    when(snapshotPerspectives.as(MutableTestable.class, FILE_KEY)).thenReturn(testable);
    tester = new WsTester(new TestsWs(mock(TestsShowAction.class), new TestsTestCasesAction(snapshotPerspectives), mock(TestsCoveredFilesAction.class)));
  }

  @Test
  public void testable() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", FILE_KEY);

    TestCase testCase1 = testCase("test1", TestCase.Status.OK, 10L, "org.foo.BarTest.java", "src/test/java/org/foo/BarTest.java");
    TestCase testCase2 = testCase("test2", TestCase.Status.ERROR, 97L, "org.foo.FileTest.java", "src/test/java/org/foo/FileTest.java");
    when(testable.testCasesOfLine(10)).thenReturn(newArrayList(testCase1, testCase2));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "test_cases").setParam("key", FILE_KEY).setParam("line", "10");

    request.execute().assertJson("{\n" +
      "  \"tests\": [\n" +
      "    {\n" +
      "      \"name\": \"test1\",\n" +
      "      \"status\": \"OK\",\n" +
      "      \"durationInMs\": 10,\n" +
      "      \"_ref\": \"1\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"test2\",\n" +
      "      \"status\": \"ERROR\",\n" +
      "      \"durationInMs\": 97,\n" +
      "      \"_ref\": \"2\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"files\": {\n" +
      "    \"1\": {\n" +
      "      \"key\": \"org.foo.BarTest.java\",\n" +
      "      \"longName\": \"src/test/java/org/foo/BarTest.java\"\n" +
      "    },\n" +
      "    \"2\": {\n" +
      "      \"key\": \"org.foo.FileTest.java\",\n" +
      "      \"longName\": \"src/test/java/org/foo/FileTest.java\"\n" +
      "    }\n" +
      "  }\n" +
      "}");
  }

  private TestCase testCase(String name, TestCase.Status status, Long durationInMs, String testPlanKey, String testPlanLongName) {
    TestCase testCase = mock(TestCase.class);
    when(testCase.name()).thenReturn(name);
    when(testCase.status()).thenReturn(status);
    when(testCase.durationInMs()).thenReturn(durationInMs);

    TestPlan testPlan = mock(TestPlan.class);
    when(testPlan.component()).thenReturn(new ComponentDto().setKey(testPlanKey).setLongName(testPlanLongName));
    when(testCase.testPlan()).thenReturn(testPlan);
    return testCase;
  }

}
