/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbFileSources.Test.CoveredFile;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.server.test.ws.CoveredFilesAction.TEST_ID;
import static org.sonar.test.JsonAssert.assertJson;

public class CoveredFilesActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public EsTester es = new EsTester(new TestIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public DbTester db = DbTester.create();

  private TestIndex testIndex = new TestIndex(es.client(), System2.INSTANCE);
  private TestIndexer testIndexer = new TestIndexer(db.getDbClient(), es.client());

  private WsActionTester ws = new WsActionTester(new CoveredFilesAction(db.getDbClient(), testIndex, userSession));

  @Test
  public void define_covered_files() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void covered_files() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto mainFile1 = db.components().insertComponent(newFileDto(project));
    ComponentDto mainFile2 = db.components().insertComponent(newFileDto(project));
    ComponentDto testFile = db.components().insertComponent(newFileDto(project).setQualifier(UNIT_TEST_FILE));
    userSession.addProjectPermission(CODEVIEWER, project, testFile);

    DbFileSources.Test test = DbFileSources.Test.newBuilder().setUuid(Uuids.create())
      .addCoveredFile(CoveredFile.newBuilder()
        .setFileUuid(mainFile1.uuid())
        .addAllCoveredLine(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
      .addCoveredFile(CoveredFile.newBuilder()
        .setFileUuid(mainFile2.uuid())
        .addAllCoveredLine(asList(1, 2, 3)))
      .build();
    insertTests(testFile, test);

    TestRequest request = ws.newRequest().setParam(TEST_ID, test.getUuid());

    assertJson(request.execute().getInput()).isSimilarTo("{\n" +
      "  \"files\": [\n" +
      "    {\n" +
      "      \"id\": \"" + mainFile1.uuid() + "\",\n" +
      "      \"key\": \"" + mainFile1.getKey() + "\",\n" +
      "      \"longName\": \"" + mainFile1.longName() + "\",\n" +
      "      \"coveredLines\": 10\n" +
      "    },\n" +
      "    {\n" +
      "      \"id\": \"" + mainFile2.uuid() + "\",\n" +
      "      \"key\": \"" + mainFile2.getKey() + "\",\n" +
      "      \"longName\": \"" + mainFile2.longName() + "\",\n" +
      "      \"coveredLines\": 3\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void covered_files_on_branch() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto mainFile = db.components().insertComponent(newFileDto(branch));
    ComponentDto testFile = db.components().insertComponent(newFileDto(branch).setQualifier(UNIT_TEST_FILE));
    userSession.addProjectPermission(CODEVIEWER, project, testFile);
    DbFileSources.Test test = DbFileSources.Test.newBuilder().setUuid(Uuids.create())
      .addCoveredFile(CoveredFile.newBuilder()
        .setFileUuid(mainFile.uuid())
        .addAllCoveredLine(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
      .build();
    insertTests(testFile, test);

    TestRequest request = ws.newRequest().setParam(TEST_ID, test.getUuid());

    assertJson(request.execute().getInput()).isSimilarTo("{\n" +
      "  \"files\": [\n" +
      "    {\n" +
      "      \"id\": \"" + mainFile.uuid() + "\",\n" +
      "      \"key\": \"" + mainFile.getKey() + "\",\n" +
      "      \"branch\": \"" + mainFile.getBranch() + "\",\n" +
      "      \"longName\": \"" + mainFile.longName() + "\",\n" +
      "      \"coveredLines\": 10\n" +
      "    }\n" +
      "  ]\n" +
      "}");
  }

  @Test
  public void fail_when_test_uuid_is_unknown() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Test with id 'unknown' is not found");

    ws.newRequest().setParam(TEST_ID, "unknown").execute();
  }

  private void insertTests(ComponentDto testFile, DbFileSources.Test... tests) {
    db.getDbClient().fileSourceDao().insert(db.getSession(), new FileSourceDto()
      .setProjectUuid(testFile.projectUuid())
      .setFileUuid(testFile.uuid())
      .setTestData(asList(tests)));
    db.commit();
    testIndexer.indexOnStartup(null);
  }
}
