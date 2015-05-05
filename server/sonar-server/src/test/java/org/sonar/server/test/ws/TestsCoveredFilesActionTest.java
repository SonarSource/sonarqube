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
import org.sonar.api.web.UserRole;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.component.ComponentTesting.newFileDto;
import static org.sonar.server.component.ComponentTesting.newProjectDto;
import static org.sonar.server.test.ws.TestsCoveredFilesAction.TEST_UUID;

public class TestsCoveredFilesActionTest {

  static final String TEST_PLAN_KEY = "src/test/java/org/foo/BarTest.java";

  WsTester ws;
  private DbClient dbClient;
  private TestIndex testIndex;

  @Before
  public void setUp() throws Exception {
    dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
    testIndex = mock(TestIndex.class, RETURNS_DEEP_STUBS);
    ws = new WsTester(new TestsWs(new TestsCoveredFilesAction(dbClient, testIndex)));
  }

  @Test
  public void covered_files() throws Exception {
    MockUserSession.set().addComponentUuidPermission(UserRole.CODEVIEWER, "SonarQube", "test-file-uuid");

    when(testIndex.searchByTestUuid(anyString()).fileUuid()).thenReturn("test-file-uuid");
    when(testIndex.coveredFiles("test-uuid")).thenReturn(Arrays.asList(
      new CoveredFileDoc().setFileUuid("bar-uuid").setCoveredLines(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
      new CoveredFileDoc().setFileUuid("file-uuid").setCoveredLines(Arrays.asList(1, 2, 3))
      ));
    when(dbClient.componentDao().getByUuids(any(DbSession.class), anyList())).thenReturn(
      Arrays.asList(
        newFileDto(newProjectDto(), "bar-uuid").setKey("org.foo.Bar.java").setLongName("src/main/java/org/foo/Bar.java"),
        newFileDto(newProjectDto(), "file-uuid").setKey("org.foo.File.java").setLongName("src/main/java/org/foo/File.java")));

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "covered_files").setParam(TEST_UUID, "test-uuid");

    request.execute().assertJson(getClass(), "tests-covered-files.json");
  }
}
