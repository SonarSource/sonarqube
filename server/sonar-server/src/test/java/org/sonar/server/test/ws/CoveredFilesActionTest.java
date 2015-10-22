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

import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.test.ws.CoveredFilesAction.TEST_ID;

public class CoveredFilesActionTest {

  public static final String FILE_1_ID = "FILE1";
  public static final String FILE_2_ID = "FILE2";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WsTester ws;
  private DbClient dbClient;
  private TestIndex testIndex;

  @Before
  public void setUp() {
    dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
    testIndex = mock(TestIndex.class, RETURNS_DEEP_STUBS);
    ws = new WsTester(new TestsWs(new CoveredFilesAction(dbClient, testIndex, userSessionRule)));
  }

  @Test
  public void covered_files() throws Exception {
    userSessionRule.addComponentUuidPermission(UserRole.CODEVIEWER, "SonarQube", "test-file-uuid");

    when(testIndex.searchByTestUuid(anyString()).fileUuid()).thenReturn("test-file-uuid");
    when(testIndex.coveredFiles("test-uuid")).thenReturn(Arrays.asList(
      new CoveredFileDoc().setFileUuid(FILE_1_ID).setCoveredLines(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
      new CoveredFileDoc().setFileUuid(FILE_2_ID).setCoveredLines(Arrays.asList(1, 2, 3))
      ));
    when(dbClient.componentDao().selectByUuids(any(DbSession.class), anyList())).thenReturn(
      Arrays.asList(
        newFileDto(newProjectDto(), FILE_1_ID).setKey("org.foo.Bar.java").setLongName("src/main/java/org/foo/Bar.java"),
        newFileDto(newProjectDto(), FILE_2_ID).setKey("org.foo.File.java").setLongName("src/main/java/org/foo/File.java")));

    WsTester.TestRequest request = ws.newGetRequest("api/tests", "covered_files").setParam(TEST_ID, "test-uuid");

    request.execute().assertJson(getClass(), "tests-covered-files.json");
  }
}
