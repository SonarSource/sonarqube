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
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.Testable;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Arrays;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestsCoveredFilesActionTest {

  static final String TEST_PLAN_KEY = "src/test/java/org/foo/BarTest.java";

  @Mock
  MutableTestPlan testPlan;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    SnapshotPerspectives snapshotPerspectives = mock(SnapshotPerspectives.class);
    when(snapshotPerspectives.as(MutableTestPlan.class, TEST_PLAN_KEY)).thenReturn(testPlan);
    tester = new WsTester(new TestsWs(mock(TestsShowAction.class), mock(TestsTestCasesAction.class), new TestsCoveredFilesAction(snapshotPerspectives)));
  }

  @Test
  public void plan() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", TEST_PLAN_KEY);

    MutableTestCase testCase1 = testCase("org.foo.Bar.java", "src/main/java/org/foo/Bar.java", 10);
    MutableTestCase testCase2 = testCase("org.foo.File.java", "src/main/java/org/foo/File.java", 3);
    when(testPlan.testCasesByName("my_test")).thenReturn(newArrayList(testCase1, testCase2));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "covered_files").setParam("key", TEST_PLAN_KEY).setParam("test", "my_test");

    request.execute().assertJson("{\n" +
      "  \"files\": [\n" +
      "    {\n" +
      "      \"key\": \"org.foo.Bar.java\",\n" +
      "      \"longName\": \"src/main/java/org/foo/Bar.java\",\n" +
      "      \"coveredLines\" : 10\n" +
      "    },\n" +
      "    {\n" +
      "      \"key\": \"org.foo.File.java\",\n" +
      "      \"longName\": \"src/main/java/org/foo/File.java\",\n" +
      "      \"coveredLines\" : 3\n" +
      "    }\n" +
      "  ]\n" +
      "}\n");
  }

  private MutableTestCase testCase(String fileKey, String fileLongName, int coveredLines) {
    Testable testable = mock(Testable.class);
    when(testable.component()).thenReturn(new ComponentDto().setKey(fileKey).setLongName(fileLongName));

    CoverageBlock block = mock(CoverageBlock.class);
    when(block.testable()).thenReturn(testable);
    when(block.lines()).thenReturn(Arrays.asList(new Integer[coveredLines]));

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.coverageBlocks()).thenReturn(newArrayList(block));
    return testCase;
  }

}
