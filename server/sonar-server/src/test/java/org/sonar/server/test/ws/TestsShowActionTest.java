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
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import javax.annotation.Nullable;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestsShowActionTest {

  static final String TEST_PLAN_KEY = "src/test/java/org/foo/BarTest.java";

  @Mock
  DbSession session;

  @Mock
  MeasureDao measureDao;

  @Mock
  MutableTestPlan testPlan;

  @Mock
  SnapshotPerspectives snapshotPerspectives;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.measureDao()).thenReturn(measureDao);

    tester = new WsTester(new TestsWs(new TestsShowAction(dbClient, snapshotPerspectives), mock(TestsTestCasesAction.class), mock(TestsCoveredFilesAction.class)));
  }

  @Test
  public void show() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", TEST_PLAN_KEY);

    when(snapshotPerspectives.as(MutableTestPlan.class, TEST_PLAN_KEY)).thenReturn(testPlan);

    MutableTestCase testCase1 = testCase("test1", TestCase.Status.OK, 10L, 32, null, null);
    MutableTestCase testCase2 = testCase("test2", TestCase.Status.ERROR, 97L, 21, "expected:<true> but was:<false>",
      "java.lang.AssertionError: expected:<true> but was:<false>\n\t" +
        "at org.junit.Assert.fail(Assert.java:91)\n\t" +
        "at org.junit.Assert.failNotEquals(Assert.java:645)\n\t" +
        "at org.junit.Assert.assertEquals(Assert.java:126)\n\t" +
        "at org.junit.Assert.assertEquals(Assert.java:145)\n");
    when(testPlan.testCases()).thenReturn(newArrayList(testCase1, testCase2));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "show").setParam("key", TEST_PLAN_KEY);

    request.execute().assertJson(getClass(), "show.json");
  }

  @Test
  public void show_from_test_data() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", TEST_PLAN_KEY);

    when(measureDao.findByComponentKeyAndMetricKey(session, TEST_PLAN_KEY, "test_data")).thenReturn(new MeasureDto()
      .setComponentKey(TEST_PLAN_KEY)
      .setMetricKey("test_data")
      .setData("<tests-details>" +
        "<testcase status=\"ok\" time=\"10\" name=\"test1\"/>" +
        "<testcase status=\"error\" time=\"97\" name=\"test2\">" +
        "<error message=\"expected:&lt;true&gt; but was:&lt;false&gt;\">" +
        "<![CDATA[" +
        "java.lang.AssertionError: expected:<true> but was:<false>\n\t" +
        "at org.junit.Assert.fail(Assert.java:91)\n\t" +
        "at org.junit.Assert.failNotEquals(Assert.java:645)\n\t" +
        "at org.junit.Assert.assertEquals(Assert.java:126)\n\t" +
        "at org.junit.Assert.assertEquals(Assert.java:145)\n" +
        "]]>" +
        "</error>" +
        "</testcase>" +
        "</tests-details>"));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "show").setParam("key", TEST_PLAN_KEY);

    request.execute().assertJson(getClass(), "show_from_test_data.json");
  }

  @Test
  public void show_from_test_data_with_a_time_in_float() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", TEST_PLAN_KEY);

    when(measureDao.findByComponentKeyAndMetricKey(session, TEST_PLAN_KEY, "test_data")).thenReturn(
      new MeasureDto()
        .setComponentKey(TEST_PLAN_KEY)
        .setMetricKey("test_data")
        .setData("<tests-details>" +
          "<testcase status=\"ok\" time=\"12.5\" name=\"test1\"/>" +
          "</tests-details>"));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "show").setParam("key", TEST_PLAN_KEY);

    request.execute().assertJson(getClass(), "show_from_test_data_with_a_time_in_float.json");
  }

  private MutableTestCase testCase(String name, TestCase.Status status, Long durationInMs, int coveredLines, @Nullable String message, @Nullable String stackTrace) {
    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.name()).thenReturn(name);
    when(testCase.status()).thenReturn(status);
    when(testCase.durationInMs()).thenReturn(durationInMs);
    when(testCase.countCoveredLines()).thenReturn(coveredLines);
    when(testCase.message()).thenReturn(message);
    when(testCase.stackTrace()).thenReturn(stackTrace);
    return testCase;
  }

}
