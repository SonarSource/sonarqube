/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.test.ws;

import com.google.common.base.Optional;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.test.ws.CoveredFilesAction.TEST_ID;
import static org.sonar.test.JsonAssert.assertJson;

public class CoveredFilesActionTest {

  private static final String FILE_1_ID = "FILE1";
  private static final String FILE_2_ID = "FILE2";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsActionTester ws;
  private DbClient dbClient;
  private TestIndex testIndex;

  @Before
  public void setUp() {
    dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
    testIndex = mock(TestIndex.class, RETURNS_DEEP_STUBS);

    ws = new WsActionTester(new CoveredFilesAction(dbClient, testIndex, userSessionRule));
  }

  @Test
  public void covered_files() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), "SonarQube");
    ComponentDto file = ComponentTesting.newFileDto(project, null, "test-file-uuid");
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    when(testIndex.getNullableByTestUuid(anyString())).thenReturn(Optional.of(new TestDoc().setFileUuid("test-file-uuid")));
    when(testIndex.coveredFiles("test-uuid")).thenReturn(Arrays.asList(
      new CoveredFileDoc().setFileUuid(FILE_1_ID).setCoveredLines(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
      new CoveredFileDoc().setFileUuid(FILE_2_ID).setCoveredLines(Arrays.asList(1, 2, 3))));
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    when(dbClient.componentDao().selectByUuids(any(DbSession.class), anyList())).thenReturn(
      Arrays.asList(
        newFileDto(ComponentTesting.newPrivateProjectDto(organizationDto), null, FILE_1_ID).setKey("org.foo.Bar.java").setLongName("src/main/java/org/foo/Bar.java"),
        newFileDto(ComponentTesting.newPrivateProjectDto(organizationDto), null, FILE_2_ID).setKey("org.foo.File.java").setLongName("src/main/java/org/foo/File.java")));

    TestRequest request = ws.newRequest().setParam(TEST_ID, "test-uuid");

    assertJson(request.execute().getInput()).isSimilarTo(getClass().getResource("CoveredFilesActionTest/tests-covered-files.json"));
  }

  @Test
  public void fail_when_test_uuid_is_unknown() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), "SonarQube");
    ComponentDto file = ComponentTesting.newFileDto(project);
    userSessionRule.addProjectPermission(UserRole.CODEVIEWER, project, file);

    when(testIndex.getNullableByTestUuid(anyString())).thenReturn(Optional.<TestDoc>absent());
    when(testIndex.coveredFiles("test-uuid")).thenReturn(Arrays.asList(
      new CoveredFileDoc().setFileUuid(FILE_1_ID).setCoveredLines(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
      new CoveredFileDoc().setFileUuid(FILE_2_ID).setCoveredLines(Arrays.asList(1, 2, 3))));
    OrganizationDto organizationDto = OrganizationTesting.newOrganizationDto();
    when(dbClient.componentDao().selectByUuids(any(DbSession.class), anyList())).thenReturn(
      Arrays.asList(
        newFileDto(ComponentTesting.newPrivateProjectDto(organizationDto), null, FILE_1_ID).setKey("org.foo.Bar.java").setLongName("src/main/java/org/foo/Bar.java"),
        newFileDto(ComponentTesting.newPrivateProjectDto(organizationDto), null, FILE_2_ID).setKey("org.foo.File.java").setLongName("src/main/java/org/foo/File.java")));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Test with id 'test-uuid' is not found");

    ws.newRequest().setParam(TEST_ID, "test-uuid").execute();
  }
}
