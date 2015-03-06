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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.TestPlan;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentVertex;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestsTestCasesActionTest {

  private static final String FILE_KEY = "src/main/java/org/foo/Foo.java";

  private static final String EXPECTED_JSON = "{\n" +
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
    "      \"uuid\": \"ABCD\",\n" +
    "      \"longName\": \"src/test/java/org/foo/BarTest.java\"\n" +
    "    },\n" +
    "    \"2\": {\n" +
    "      \"key\": \"org.foo.FileTest.java\",\n" +
    "      \"uuid\": \"BCDE\",\n" +
    "      \"longName\": \"src/test/java/org/foo/FileTest.java\"\n" +
    "    }\n" +
    "  }\n" +
    "}";


  @Mock
  MutableTestable testable;

  @Mock
  ComponentService componentService;

  @Mock
  DbClient dbClient;

  @Mock
  ComponentDao componentDao;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    SnapshotPerspectives snapshotPerspectives = mock(SnapshotPerspectives.class);
    when(snapshotPerspectives.as(MutableTestable.class, FILE_KEY)).thenReturn(testable);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.openSession(false)).thenReturn(mock(DbSession.class));
    tester = new WsTester(new TestsWs(mock(TestsShowAction.class), new TestsTestCasesAction(snapshotPerspectives, componentService, dbClient), mock(TestsCoveredFilesAction.class)));
  }

  @Test
  public void testable() throws Exception {
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", FILE_KEY);

    String key1 = "org.foo.BarTest.java";
    String name1 = "src/test/java/org/foo/BarTest.java";
    String uuid1 = "ABCD";
    TestCase testCase1 = testCase("test1", TestCase.Status.OK, 10L, key1);
    ComponentDto component1 = new ComponentDto().setKey(key1).setLongName(name1).setUuid(uuid1);
    String key2 = "org.foo.FileTest.java";
    String name2 = "src/test/java/org/foo/FileTest.java";
    String uuid2 = "BCDE";
    TestCase testCase2 = testCase("test2", TestCase.Status.ERROR, 97L, key2);
    ComponentDto component2 = new ComponentDto().setKey(key2).setLongName(name2).setUuid(uuid2);
    when(testable.testCasesOfLine(10)).thenReturn(newArrayList(testCase1, testCase2));
    when(componentDao.getByKeys(Matchers.isA(DbSession.class), Matchers.anyCollectionOf(String.class))).thenReturn(newArrayList(component1, component2));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "test_cases").setParam("key", FILE_KEY).setParam("line", "10");

    request.execute().assertJson(EXPECTED_JSON);
  }

  @Test
  public void testable_with_uuid() throws Exception {
    String uuid = "1234";
    when(componentService.getByUuid(uuid)).thenReturn(new ComponentDto().setKey(FILE_KEY));
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "SonarQube", FILE_KEY);

    String key1 = "org.foo.BarTest.java";
    String name1 = "src/test/java/org/foo/BarTest.java";
    String uuid1 = "ABCD";
    TestCase testCase1 = testCase("test1", TestCase.Status.OK, 10L, key1);
    ComponentDto component1 = new ComponentDto().setKey(key1).setLongName(name1).setUuid(uuid1);
    String key2 = "org.foo.FileTest.java";
    String name2 = "src/test/java/org/foo/FileTest.java";
    String uuid2 = "BCDE";
    TestCase testCase2 = testCase("test2", TestCase.Status.ERROR, 97L, key2);
    ComponentDto component2 = new ComponentDto().setKey(key2).setLongName(name2).setUuid(uuid2);
    when(testable.testCasesOfLine(10)).thenReturn(newArrayList(testCase1, testCase2));
    when(componentDao.getByKeys(Matchers.isA(DbSession.class), Matchers.anyCollectionOf(String.class))).thenReturn(newArrayList(component1, component2));

    WsTester.TestRequest request = tester.newGetRequest("api/tests", "test_cases").setParam("uuid", uuid).setParam("line", "10");

    request.execute().assertJson(EXPECTED_JSON);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_parameters() throws Exception {
    tester.newGetRequest("api/tests", "test_cases").execute();
  }

  private TestCase testCase(String name, TestCase.Status status, Long durationInMs, String testPlanKey) {
    TestCase testCase = mock(TestCase.class);
    when(testCase.name()).thenReturn(name);
    when(testCase.status()).thenReturn(status);
    when(testCase.durationInMs()).thenReturn(durationInMs);

    TestPlan testPlan = mock(TestPlan.class);
    ComponentVertex component = mock(ComponentVertex.class);
    when(component.key()).thenReturn(testPlanKey);
    when(testPlan.component()).thenReturn(component);
    when(testCase.testPlan()).thenReturn(testPlan);
    return testCase;
  }

}
